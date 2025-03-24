package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
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
            "value DOUBLE NOT NULL, " +
            "FOREIGN KEY (provider_id) REFERENCES signal_providers(provider_id), " +
            "UNIQUE (provider_id, stat_type, recorded_date))";
    
    private static final String INSERT_PROVIDER = 
            "INSERT INTO signal_providers (provider_name) VALUES (?) " +
            "ON DUPLICATE KEY UPDATE provider_id = provider_id";
    
    private static final String GET_PROVIDER_ID = 
            "SELECT provider_id FROM signal_providers WHERE provider_name = ?";
    
    private static final String INSERT_STAT_VALUE = 
            "INSERT INTO stat_values (provider_id, stat_type, recorded_date, value) VALUES (?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE value = ?";
    
    private static final String GET_LATEST_STAT_VALUE = 
            "SELECT value FROM stat_values " +
            "WHERE provider_id = ? AND stat_type = ? " +
            "ORDER BY recorded_date DESC LIMIT 1";
    
    private static final String GET_STAT_HISTORY = 
            "SELECT recorded_date, value FROM stat_values " +
            "WHERE provider_id = ? AND stat_type = ? " +
            "ORDER BY recorded_date DESC";
    
    /**
     * Privater Konstruktor für Singleton-Pattern
     */
    private HistoryDatabaseManager() {
        initDatabase();
    }
    
    /**
     * Gibt die Singleton-Instanz zurück
     */
    public static synchronized HistoryDatabaseManager getInstance() {
        if (instance == null) {
            instance = new HistoryDatabaseManager();
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
            
            // Verbindung zur In-Memory-Datenbank herstellen
            // DB_CLOSE_DELAY=-1 verhindert, dass die Datenbank gelöscht wird, wenn die letzte Verbindung geschlossen wird
            // DATABASE_TO_UPPER=false sorgt dafür, dass die Tabellen- und Spaltennamen nicht automatisch in Großbuchstaben umgewandelt werden
            connection = DriverManager.getConnection("jdbc:h2:mem:providerhistorydb;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false", "sa", "");
            
            // Tabellen erstellen, falls sie noch nicht existieren
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(CREATE_PROVIDERS_TABLE);
                stmt.execute(CREATE_STAT_VALUES_TABLE);
            }
            
            LOGGER.info("Provider History Datenbank erfolgreich initialisiert");
        } catch (ClassNotFoundException | SQLException e) {
            LOGGER.severe("Fehler beim Initialisieren der Datenbank: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Speichert einen neuen statistischen Wert in der Datenbank
     * 
     * @param providerName Name des Signal Providers
     * @param statType Art des statistischen Werts (z.B. "3MPDD", "DRAWDOWN", etc.)
     * @param value Der zu speichernde Wert
     * @param forceUpdate erzwingt Update auch bei gleichem Wert
     * @return true wenn erfolgreich, false bei Fehler
     */
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
            
            // Wert speichern
            try (PreparedStatement stmt = connection.prepareStatement(INSERT_STAT_VALUE)) {
                stmt.setInt(1, providerId);
                stmt.setString(2, statType);
                stmt.setObject(3, now);
                stmt.setDouble(4, value);
                stmt.setDouble(5, value); // Für das UPDATE bei Duplikat
                stmt.executeUpdate();
                
                LOGGER.info(String.format("%s-Wert %.4f für %s gespeichert (Datum: %s)", 
                        statType, value, providerName, now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
                return true;
            }
        } catch (SQLException e) {
            LOGGER.severe("Fehler beim Speichern des " + statType + "-Werts: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Holt die Provider-ID aus der Datenbank oder erstellt einen neuen Eintrag
     */
    private int getOrCreateProvider(String providerName) throws SQLException {
        // Versuche zuerst, die ID zu bekommen
        try (PreparedStatement stmt = connection.prepareStatement(GET_PROVIDER_ID)) {
            stmt.setString(1, providerName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        
        // Wenn nicht gefunden, füge neuen Provider hinzu
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_PROVIDER, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, providerName);
            stmt.executeUpdate();
            
            // Hole generierte ID
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                throw new SQLException("Konnte keine ID für den neu eingefügten Provider erhalten");
            }
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
}