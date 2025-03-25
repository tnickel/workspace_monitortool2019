package db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Verwaltet die H2-Datenbankverbindung und Operationen für die Speicherung
 * von historischen Werten für Signal Provider
 */
public class HistoryDatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(HistoryDatabaseManager.class.getName());
    private static HistoryDatabaseManager instance;
    private Connection connection;
    private String rootPath;
    
    // SQL-Statements für Datenbankoperationen
    private static final String CREATE_PROVIDERS_TABLE = 
            "CREATE TABLE IF NOT EXISTS signal_providers (" +
            "provider_id INT AUTO_INCREMENT PRIMARY KEY, " +
            "provider_name VARCHAR(255) UNIQUE NOT NULL)";
    
    private static final String CREATE_STAT_VALUES_TABLE = 
    	    "CREATE TABLE IF NOT EXISTS stat_values (" +
    	    "id INT AUTO_INCREMENT PRIMARY KEY, " +
    	    "provider_id INT NOT NULL, " +
    	    "stat_type VARCHAR(50) NOT NULL, " +
    	    "recorded_date TIMESTAMP NOT NULL, " +
    	    "\"value\" DOUBLE NOT NULL, " +
    	    "FOREIGN KEY (provider_id) REFERENCES signal_providers(provider_id), " +
    	    "UNIQUE (provider_id, stat_type, recorded_date))";
    
    private static final String INSERT_PROVIDER = 
            "INSERT INTO signal_providers (provider_name) VALUES (?)";
    
    private static final String GET_PROVIDER_ID = 
            "SELECT provider_id FROM signal_providers WHERE provider_name = ?";
    
    private static final String INSERT_STAT_VALUE = 
    	    "INSERT INTO stat_values (provider_id, stat_type, recorded_date, \"value\") VALUES (?, ?, ?, ?)";
    
    private static final String GET_LATEST_STAT_VALUE = 
    	    "SELECT \"value\" FROM stat_values " +
    	    "WHERE provider_id = ? AND stat_type = ? " +
    	    "ORDER BY recorded_date DESC LIMIT 1";
    
    private static final String GET_STAT_HISTORY = 
    	    "SELECT recorded_date, \"value\" FROM stat_values " +
    	    "WHERE provider_id = ? AND stat_type = ? " +
    	    "ORDER BY recorded_date DESC";
    
    /**
     * Privater Konstruktor für Singleton-Pattern
     */
    private HistoryDatabaseManager(String rootPath) {
        this.rootPath = rootPath;
        initDatabase();
    }

    
    public static synchronized HistoryDatabaseManager getInstance(String rootPath) {
        if (instance == null) {
            instance = new HistoryDatabaseManager(rootPath);
        }
        return instance;
    }

    public static synchronized HistoryDatabaseManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("HistoryDatabaseManager muss zuerst mit einem Pfad initialisiert werden");
        }
        return instance;
    }
    
    /**
     * Initialisiert die Datenbankverbindung und erstellt die Tabellen
     */
    private void initDatabase() {
        try {
            // H2-Treiber laden
            Class.forName("org.h2.Driver");
            
            // Verbindung zur Datenbank auf der Festplatte
            String dbPath = rootPath + File.separator + "database" + File.separator + "providerhistorydb";
            File dbDir = new File(rootPath + File.separator + "database");
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }
            
            connection = DriverManager.getConnection(
                "jdbc:h2:file:" + dbPath + ";DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE;DATABASE_TO_UPPER=false", 
                "sa", "");
            
            // Tabellen erstellen
            try (Statement stmt = connection.createStatement()) {
                // Tabelle für Provider erstellen
                boolean result1 = stmt.execute(CREATE_PROVIDERS_TABLE);
                LOGGER.info("Provider-Tabelle erstellt: " + result1);
                
                // Tabelle für statistische Werte erstellen
                boolean result2 = stmt.execute(CREATE_STAT_VALUES_TABLE);
                LOGGER.info("Statistik-Werte-Tabelle erstellt: " + result2);
                
                // Prüfen, ob Tabellen existieren
                ResultSet rs1 = connection.getMetaData().getTables(null, null, "signal_providers", null);
                boolean providersTableExists = rs1.next();
                
                ResultSet rs2 = connection.getMetaData().getTables(null, null, "stat_values", null);
                boolean statValuesTableExists = rs2.next();
                
                LOGGER.info("Tabellen existieren: signal_providers=" + providersTableExists + 
                            ", stat_values=" + statValuesTableExists);
                
                if (!providersTableExists || !statValuesTableExists) {
                    LOGGER.severe("Tabellen konnten nicht erstellt werden!");
                }
            }
            
            LOGGER.info("Provider History Datenbank erfolgreich initialisiert: " + dbPath);
        } catch (ClassNotFoundException | SQLException e) {
            LOGGER.severe("Fehler beim Initialisieren der Datenbank: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public boolean storeStatValue(String providerName, String statType, double value, boolean forceUpdate) {
        if (connection == null) {
            LOGGER.warning("Keine Datenbankverbindung verfügbar");
            return false;
        }
        
        try {
            // Prüfen, ob der Wert sich geändert hat (nur wenn forceUpdate=false)
            if (!forceUpdate) {
                Double latestValue = getLatestStatValue(providerName, statType);
                if (latestValue != null && Math.abs(latestValue - value) < 0.001) {
                    // Wert hat sich nicht signifikant geändert, keine Speicherung notwendig
                    LOGGER.fine(statType + "-Wert für " + providerName + " hat sich nicht geändert, keine Speicherung");
                    return true;
                }
            }
            
            // Provider-ID holen oder erstellen
            int providerId = getOrCreateProvider(providerName);
            
            // Aktuelles Datum/Zeit
            LocalDateTime now = LocalDateTime.now();
            
            // Für H2 angepasste Version
            try {
                // Zuerst prüfen, ob ein Eintrag mit identischem Schlüssel existiert
                try (PreparedStatement checkStmt = connection.prepareStatement(
                        "SELECT COUNT(*) FROM stat_values WHERE provider_id = ? AND stat_type = ? AND recorded_date = ?")) {
                    checkStmt.setInt(1, providerId);
                    checkStmt.setString(2, statType);
                    checkStmt.setObject(3, now);
                    
                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next() && rs.getInt(1) > 0) {
                        // Eintrag existiert, UPDATE verwenden
                        try (PreparedStatement updateStmt = connection.prepareStatement(
                        		"UPDATE stat_values SET \"value\" = ? WHERE provider_id = ? AND stat_type = ? AND recorded_date = ?"
                        		)) 
                        {
                            
                        	updateStmt.setDouble(1, value);
                            updateStmt.setInt(2, providerId);
                            updateStmt.setString(3, statType);
                            updateStmt.setObject(4, now);
                            updateStmt.executeUpdate();
                        }
                    } else {
                        // Eintrag existiert nicht, INSERT verwenden
                        try (PreparedStatement insertStmt = connection.prepareStatement(
                        		"INSERT INTO stat_values (provider_id, stat_type, recorded_date, \"value\") VALUES (?, ?, ?, ?)"
                        		)) 
                        {
                            insertStmt.setInt(1, providerId);
                            insertStmt.setString(2, statType);
                            insertStmt.setObject(3, now);
                            insertStmt.setDouble(4, value);
                            insertStmt.executeUpdate();
                        }
                    }
                }
                
                LOGGER.info(String.format("%s-Wert %.4f für %s gespeichert (Datum: %s)", 
                        statType, value, providerName, now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
                return true;
            } catch (SQLException e) {
                LOGGER.severe("Fehler beim Speichern des " + statType + "-Werts: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } catch (SQLException e) {
            LOGGER.severe("Fehler beim Speichern des " + statType + "-Werts: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
   
    
    private int getOrCreateProvider(String providerName) throws SQLException {
        if (providerName == null || providerName.trim().isEmpty()) {
            throw new SQLException("Provider-Name darf nicht leer sein");
        }

        // Versuche zuerst, die ID zu bekommen
        try (PreparedStatement stmt = connection.prepareStatement(GET_PROVIDER_ID)) {
            stmt.setString(1, providerName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        
        // Wenn nicht gefunden, füge neuen Provider hinzu
        try (PreparedStatement stmt = connection.prepareStatement(
                INSERT_PROVIDER, 
                Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, providerName);
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Provider konnte nicht erstellt werden, keine Zeilen betroffen");
            }
            
            // Hole generierte ID
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new SQLException("Konnte keine ID für den neu eingefügten Provider erhalten");
                }
            }
        } catch (SQLException e) {
            // Falls es einen Unique-Constraint-Fehler gibt (z.B. wenn der Provider in der Zwischenzeit
            // von einem anderen Thread eingefügt wurde), versuche es noch einmal mit SELECT
            if (e.getMessage().contains("Unique index or primary key violation") || 
                e.getMessage().contains("Unique constraint violation") ||
                e.getMessage().contains("Eindeutiger Index oder Primärschlüsselverletzung")) {
                try (PreparedStatement stmt = connection.prepareStatement(GET_PROVIDER_ID)) {
                    stmt.setString(1, providerName);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            throw e;
        }
    }
    /**
     * Holt den letzten gespeicherten statistischen Wert für einen Provider
     * 
     * @param providerName Name des Signal Providers
     * @param statType Art des statistischen Werts
     * @return Der letzte gespeicherte Wert oder null, wenn keiner gefunden wurde
     */
    public Double getLatestStatValue(String providerName, String statType) {
        if (connection == null) {
            LOGGER.warning("Keine Datenbankverbindung verfügbar");
            return null;
        }
        
        try {
            // Provider-ID holen
            int providerId = -1;
            try (PreparedStatement stmt = connection.prepareStatement(GET_PROVIDER_ID)) {
                stmt.setString(1, providerName);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    providerId = rs.getInt(1);
                } else {
                    // Provider nicht gefunden
                    return null;
                }
            }
            
            // Letzten Wert holen
            try (PreparedStatement stmt = connection.prepareStatement(GET_LATEST_STAT_VALUE)) {
                stmt.setInt(1, providerId);
                stmt.setString(2, statType);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getDouble(1);
                } else {
                    // Keine Werte für diesen Provider
                    return null;
                }
            }
        } catch (SQLException e) {
            LOGGER.severe("Fehler beim Abrufen des letzten " + statType + "-Werts: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Holt die Historie der statistischen Werte für einen Provider
     * 
     * @param providerName Name des Signal Providers
     * @param statType Art des statistischen Werts
     * @return Liste der historischen Einträge
     */
    public List<HistoryEntry> getStatHistory(String providerName, String statType) {
        List<HistoryEntry> history = new ArrayList<>();
        
        if (connection == null) {
            LOGGER.warning("Keine Datenbankverbindung verfügbar");
            return history;
        }
        
        try {
            // Provider-ID holen
            int providerId = -1;
            try (PreparedStatement stmt = connection.prepareStatement(GET_PROVIDER_ID)) {
                stmt.setString(1, providerName);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    providerId = rs.getInt(1);
                } else {
                    // Provider nicht gefunden
                    return history;
                }
            }
            
            // Historie holen
            try (PreparedStatement stmt = connection.prepareStatement(GET_STAT_HISTORY)) {
                stmt.setInt(1, providerId);
                stmt.setString(2, statType);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    LocalDateTime date = rs.getObject(1, LocalDateTime.class);
                    double value = rs.getDouble(2);
                    history.add(new HistoryEntry(date, value));
                }
            }
            
            return history;
        } catch (SQLException e) {
            LOGGER.severe("Fehler beim Abrufen der " + statType + "-Historie: " + e.getMessage());
            e.printStackTrace();
            return history;
        }
    }
    
    /**
     * Schließt die Datenbankverbindung
     */
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                LOGGER.info("Datenbankverbindung geschlossen");
            } catch (SQLException e) {
                LOGGER.severe("Fehler beim Schließen der Datenbankverbindung: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Klasse für Historieneinträge
     */
    public static class HistoryEntry {
        private final LocalDateTime date;
        private final double value;
        
        public HistoryEntry(LocalDateTime date, double value) {
            this.date = date;
            this.value = value;
        }
        
        public LocalDateTime getDate() {
            return date;
        }
        
        public double getValue() {
            return value;
        }
        
        @Override
        public String toString() {
            return String.format("[%s] %.4f", 
                    date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), value);
        }
    }
    /**
     * Holt eine Liste aller Provider aus der Datenbank
     * 
     * @return Liste aller Providernamen
     */
    public List<String> getAllProviders() {
        List<String> providers = new ArrayList<>();
        
        if (connection == null) {
            LOGGER.warning("Keine Datenbankverbindung verfügbar");
            return providers;
        }
        
        try (Statement stmt = connection.createStatement()) {
            String query = "SELECT provider_name FROM signal_providers ORDER BY provider_name";
            ResultSet rs = stmt.executeQuery(query);
            
            while (rs.next()) {
                providers.add(rs.getString(1));
            }
            
            return providers;
        } catch (SQLException e) {
            LOGGER.severe("Fehler beim Abrufen aller Provider: " + e.getMessage());
            e.printStackTrace();
            return providers;
        }
    }
}