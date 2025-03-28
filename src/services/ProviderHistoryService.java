package services;

import java.io.File;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import db.HistoryDatabaseManager;
import db.HistoryDatabaseManager.HistoryEntry;
import utils.HtmlDatabase;

/**
 * Service für die Verwaltung und Speicherung von historischen Statistik-Werten für Signal Provider
 */
public class ProviderHistoryService {
    private static final Logger LOGGER = Logger.getLogger(ProviderHistoryService.class.getName());
    private static ProviderHistoryService instance;
    
    private final Map<String, Map<String, Double>> lastValues;
    private final Preferences prefs;
    private String rootPath;
    private HistoryDatabaseManager dbManager;
    
    // Konstanten für Statistiktypen
    public static final String STAT_TYPE_3MPDD = "3MPDD";
    
    // Schlüssel für die Preferences
    private static final String PREF_LAST_WEEKLY_SAVE = "last_weekly_stat_save";
    
    // Singleton-Pattern
    private ProviderHistoryService() {
        this.lastValues = new ConcurrentHashMap<>();
        this.prefs = Preferences.userNodeForPackage(ProviderHistoryService.class);
        this.rootPath = null; // Wird über initialize gesetzt
    }
    
    /**
     * Gibt die Singleton-Instanz zurück
     */
    public static synchronized ProviderHistoryService getInstance() {
        if (instance == null) {
            instance = new ProviderHistoryService();
        }
        return instance;
    }
    
    /**
     * Initialisiert den Service mit dem Root-Pfad und lädt bestehende Werte
     * 
     * @param rootPath Pfad zum Root-Verzeichnis mit den CSV-Dateien
     */
    public void initialize(String rootPath) {
        this.rootPath = rootPath;
        
        // HistoryDatabaseManager mit dem Pfad initialisieren
        this.dbManager = HistoryDatabaseManager.getInstance(rootPath);
        
        // Lade die zuletzt gespeicherten Werte für bestehende Provider
        loadExistingValues();
        
        LOGGER.info("Provider History Service mit Root-Pfad initialisiert: " + rootPath);
    }

    /**
     * Lädt die zuletzt gespeicherten Werte aller Provider aus der Datenbank
     * und speichert sie im Cache, um unnötige Speicheroperationen zu vermeiden
     */
    private void loadExistingValues() {
        if (rootPath == null) {
            LOGGER.warning("Root-Pfad nicht gesetzt. Konnte existierende Werte nicht laden.");
            return;
        }
        
        LOGGER.info("Lade bestehende Statistik-Werte...");
        
        try {
            // Hole alle CSV-Dateien im Root-Verzeichnis
            File downloadDirectory = new File(rootPath);
            if (downloadDirectory.exists() && downloadDirectory.isDirectory()) {
                File[] files = downloadDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
                
                if (files != null) {
                    for (File file : files) {
                        String providerName = file.getName();
                        
                        // Hole den letzten 3MPDD-Wert für diesen Provider aus der Datenbank
                        Double mpddValue = dbManager.getLatestStatValue(providerName, STAT_TYPE_3MPDD);
                        
                        if (mpddValue != null) {
                            // Speichere den Wert im Cache
                            getProviderCache(providerName).put(STAT_TYPE_3MPDD, mpddValue);
                            LOGGER.fine("Letzter 3MPDD-Wert für " + providerName + " geladen: " + mpddValue);
                        }
                        
                        // Hier können später weitere Statistik-Typen hinzugefügt werden
                    }
                }
            }
            
            LOGGER.info("Bestehende Statistik-Werte für " + lastValues.size() + " Provider geladen");
        } catch (Exception e) {
            LOGGER.warning("Fehler beim Laden bestehender Statistik-Werte: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Gibt die Cache-Map für einen Provider zurück oder erstellt sie, falls noch nicht vorhanden
     */
    private Map<String, Double> getProviderCache(String providerName) {
        return lastValues.computeIfAbsent(providerName, k -> new ConcurrentHashMap<>());
    }
    
    /**
     * Prüft, ob seit der letzten wöchentlichen Speicherung
     * genügend Zeit vergangen ist und führt die Speicherung durch, wenn nötig
     */
    public void checkAndPerformWeeklySave() {
        LocalDate today = LocalDate.now();
        LocalDate lastSaveDate = getLastWeeklySaveDate();
        
        // Prüfe, ob seit dem letzten Speichern mindestens 7 Tage vergangen sind
        if (lastSaveDate == null || ChronoUnit.DAYS.between(lastSaveDate, today) >= 7) {
            LOGGER.info("Es sind mindestens 7 Tage seit der letzten wöchentlichen Speicherung vergangen. Führe Speicherung durch...");
            
            // Speichere alle Werte
            storeAllStatValues(true);
            
            // Aktualisiere das Datum der letzten Speicherung
            saveLastWeeklySaveDate(today);
            
            LOGGER.info("Wöchentliche Statistik-Speicherung abgeschlossen. Nächste Speicherung ab: " + today.plusDays(7));
        } else {
            LOGGER.fine("Letzte wöchentliche Speicherung war am " + lastSaveDate + 
                    ". Nächste Speicherung ab: " + lastSaveDate.plusDays(7));
        }
    }
    
    /**
     * Speichert das Datum der letzten wöchentlichen Speicherung
     */
    private void saveLastWeeklySaveDate(LocalDate date) {
        prefs.put(PREF_LAST_WEEKLY_SAVE, date.toString());
    }
    
    /**
     * Holt das Datum der letzten wöchentlichen Speicherung
     */
    private LocalDate getLastWeeklySaveDate() {
        String dateStr = prefs.get(PREF_LAST_WEEKLY_SAVE, null);
        if (dateStr != null) {
            try {
                return LocalDate.parse(dateStr);
            } catch (Exception e) {
                LOGGER.warning("Fehler beim Parsen des letzten Speicherdatums: " + e.getMessage());
            }
        }
        return null;
    }
    
    /**
     * Speichert einen statistischen Wert für einen Signal Provider und
     * prüft auf Änderungen im Vergleich zum letzten gespeicherten Wert
     * 
     * @param providerName Signal Provider Name
     * @param statType Art des statistischen Werts
     * @param value Der zu speichernde Wert
     * @return true bei erfolgreicher Speicherung, false bei Fehler
     */
    public boolean storeStatValue(String providerName, String statType, double value) {
        return storeStatValue(providerName, statType, value, false);
    }
    
    /**
     * Speichert einen statistischen Wert für einen Signal Provider und
     * prüft auf Änderungen im Vergleich zum letzten gespeicherten Wert
     * 
     * @param providerName Signal Provider Name
     * @param statType Art des statistischen Werts
     * @param value Der zu speichernde Wert
     * @param force Wenn true, wird die Speicherung erzwungen, auch wenn der Wert unverändert ist
     * @return true bei erfolgreicher Speicherung, false bei Fehler
     */
    public boolean storeStatValue(String providerName, String statType, double value, boolean force) {
        // Prüfen, ob sich der Wert geändert hat
        Map<String, Double> providerCache = getProviderCache(providerName);
        Double lastValue = providerCache.get(statType);
        boolean hasChanged = lastValue == null || Math.abs(lastValue - value) > 0.001;
        
        if (hasChanged || force) {
            LOGGER.info(String.format("%s-Wert für %s hat sich geändert: %.4f → %.4f", 
                    statType, 
                    providerName, 
                    (lastValue != null ? lastValue : 0.0), 
                    value));
                    
            // Nur speichern, wenn der Wert sich geändert hat oder force=true
            boolean success = dbManager.storeStatValue(providerName, statType, value, force);
            if (success) {
                providerCache.put(statType, value);
            }
            return success;
        } else {
            LOGGER.fine(String.format("%s-Wert für %s unverändert (%.4f)", 
                    statType, providerName, value));
        }
        
        return true; // Keine Änderung, kein Fehler
    }
    
    /**
     * Speichert den 3MPDD-Wert für einen Signal Provider
     * 
     * @param providerName Signal Provider Name
     * @param mpddValue 3MPDD-Wert
     * @return true bei erfolgreicher Speicherung, false bei Fehler
     */
    public boolean store3MpddValue(String providerName, double mpddValue) {
        return storeStatValue(providerName, STAT_TYPE_3MPDD, mpddValue);
    }
    
    /**
     * Speichert den 3MPDD-Wert für einen Signal Provider mit Option zum Erzwingen der Speicherung
     * 
     * @param providerName Signal Provider Name
     * @param mpddValue 3MPDD-Wert
     * @param force Wenn true, wird die Speicherung erzwungen, auch wenn der Wert sich nicht geändert hat
     * @return true bei erfolgreicher Speicherung, false bei Fehler
     */
    public boolean store3MpddValue(String providerName, double mpddValue, boolean force) {
        return storeStatValue(providerName, STAT_TYPE_3MPDD, mpddValue, force);
    }
    
    /**
     * Speichert statistische Werte für alle Signal Provider
     * 
     * @param forceUpdate Erzwingt Speicherung auch wenn keine Änderung
     */
    public void storeAllStatValues(boolean forceUpdate) {
        if (rootPath == null) {
            LOGGER.warning("Root-Pfad nicht gesetzt. Konnte Statistik-Werte nicht speichern.");
            return;
        }
        
        HtmlDatabase htmlDb = new HtmlDatabase(rootPath);
        
        // Für jeden Provider in der HTML-Datenbank
        Map<String, Map<String, Double>> currentValues = collectAllStatValues(htmlDb);
        
        for (Map.Entry<String, Map<String, Double>> entry : currentValues.entrySet()) {
            String providerName = entry.getKey();
            Map<String, Double> statValues = entry.getValue();
            
            // Alle Statistiktypen für diesen Provider speichern
            for (Map.Entry<String, Double> statEntry : statValues.entrySet()) {
                String statType = statEntry.getKey();
                double value = statEntry.getValue();
                
                // Wert speichern (mit forceUpdate)
                dbManager.storeStatValue(providerName, statType, value, forceUpdate);
                
                // Cache aktualisieren
                getProviderCache(providerName).put(statType, value);
            }
        }
        
        LOGGER.info("Statistik-Werte für alle Provider aktualisiert" + (forceUpdate ? " (erzwungen)" : ""));
    }
    
    /**
     * Erzwingt die sofortige Speicherung aller vorhandenen Statistikwerte für alle Provider
     * und erstellt einen Eintrag auch für Provider, die bisher keine Einträge haben
     */
    public void forceInitialSave() {
        if (rootPath == null) {
            LOGGER.warning("Root-Pfad nicht gesetzt. Konnte Statistik-Werte nicht speichern.");
            return;
        }
        
        try {
            // Hole alle CSV-Dateien im Root-Verzeichnis
            File downloadDirectory = new File(rootPath);
            if (downloadDirectory.exists() && downloadDirectory.isDirectory()) {
                File[] files = downloadDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
                
                if (files != null) {
                    LOGGER.info("Erzwinge Speicherung für " + files.length + " Provider");
                    
                    // Verwende die gleiche Berechnung wie in der Haupttabelle
                    HtmlDatabase htmlDb = new HtmlDatabase(rootPath);
                    
                    for (File file : files) {
                        String providerName = file.getName();
                        
                        // Berechne 3MPDD-Wert
                        double threeMonthProfit = htmlDb.getAverageMonthlyProfit(providerName, 3);
                        double equityDrawdown = htmlDb.getEquityDrawdown(providerName);
                        double mpdd3 = calculateMPDD(threeMonthProfit, equityDrawdown);
                        
                        // In DB speichern
                        dbManager.storeStatValue(providerName, STAT_TYPE_3MPDD, mpdd3, true);
                        
                        LOGGER.info(String.format("3MPDD-Wert %.4f für Provider %s gespeichert", 
                                mpdd3, providerName));
                    }
                }
            }
            
            LOGGER.info("Initiale Speicherung abgeschlossen");
        } catch (Exception e) {
            LOGGER.severe("Fehler bei der initialen Speicherung: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Sammelt alle aktuellen Statistik-Werte aus der vorhandenen Datenquelle
     *
     * @param htmlDb Die HTML-Datenbank
     * @return Map mit Provider-Namen als Schlüssel und innerer Map mit Statistiktypen und Werten
     */
    private Map<String, Map<String, Double>> collectAllStatValues(HtmlDatabase htmlDb) {
        Map<String, Map<String, Double>> allValues = new HashMap<>();
        
        // Hole den Root-Pfad aus der HtmlDatabase
        String rootPath = htmlDb.getRootPath();
        
        // Hole alle CSV-Dateien im Root-Verzeichnis
        File downloadDirectory = new File(rootPath);
        if (downloadDirectory.exists() && downloadDirectory.isDirectory()) {
            File[] files = downloadDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
            
            if (files != null) {
                for (File file : files) {
                    String providerName = file.getName();
                    Map<String, Double> providerValues = new HashMap<>();
                    
                    // Berechne 3MPDD für jeden Provider
                    double threeMonthProfit = htmlDb.getAverageMonthlyProfit(providerName, 3);
                    double equityDrawdown = htmlDb.getEquityDrawdown(providerName);
                    double mpdd3 = calculateMPDD(threeMonthProfit, equityDrawdown);
                    
                    // Speichere den 3MPDD-Wert
                    providerValues.put(STAT_TYPE_3MPDD, mpdd3);
                    
                    // Hier können später weitere Statistik-Typen hinzugefügt werden
                    
                    allValues.put(providerName, providerValues);
                }
            }
        }
        
        return allValues;
    }

    /**
     * Hilfsmethode für die MPDD-Berechnung
     */
    private double calculateMPDD(double monthlyProfitPercent, double maxEquityDrawdown) {
        if (maxEquityDrawdown == 0.0) {
            return 0.0;  // Verhindert Division durch Null
        }
        return monthlyProfitPercent / maxEquityDrawdown;
    }

    /**
     * Holt die Historie der 3MPDD-Werte für einen Signal Provider
     * 
     * @param providerName Signal Provider Name
     * @return Liste von 3MPDD-Werten mit Zeitstempeln
     */
    public List<HistoryEntry> get3MpddHistory(String providerName) {
        return dbManager.getStatHistory(providerName, STAT_TYPE_3MPDD);
    }

    /**
     * Holt die Historie eines statistischen Werts für einen Signal Provider
     * 
     * @param providerName Signal Provider Name
     * @param statType Art des statistischen Werts
     * @return Liste von Werten mit Zeitstempeln
     */
    public List<HistoryEntry> getStatHistory(String providerName, String statType) {
        return dbManager.getStatHistory(providerName, statType);
    }

    /**
     * Holt alle Historieneinträge für alle Provider und Statistiktypen
     * 
     * @return Map mit Providername als Schlüssel und einer Map von Statistiktypen zu HistoryEntry-Listen als Wert
     */
    public Map<String, Map<String, List<HistoryEntry>>> getAllHistoryEntries() {
        if (dbManager == null) {
            LOGGER.warning("Keine Datenbankverbindung verfügbar");
            return new HashMap<>();
        }
        
        Map<String, Map<String, List<HistoryEntry>>> result = new HashMap<>();
        
        try {
            // Alle Provider abrufen
            List<String> allProviders = dbManager.getAllProviders();
            
            // Für jeden Provider alle Stats abrufen
            for (String providerName : allProviders) {
                Map<String, List<HistoryEntry>> providerStats = new HashMap<>();
                
                // 3MPDD-Historie abrufen
                List<HistoryEntry> mpddHistory = dbManager.getStatHistory(providerName, STAT_TYPE_3MPDD);
                if (!mpddHistory.isEmpty()) {
                    providerStats.put(STAT_TYPE_3MPDD, mpddHistory);
                }
                
                // Hier können weitere Statistiktypen hinzugefügt werden, wenn sie implementiert werden
                
                if (!providerStats.isEmpty()) {
                    result.put(providerName, providerStats);
                }
            }
            
            return result;
        } catch (Exception e) {
            LOGGER.severe("Fehler beim Abrufen aller Historieneinträge: " + e.getMessage());
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    /**
     * Prüft, ob die Datenbank Einträge enthält
     * 
     * @return true wenn Einträge vorhanden sind, false sonst
     */
    public boolean hasDatabaseEntries() {
        if (dbManager == null) {
            return false;
        }
        
        try {
            // Prüfe, ob es Provider gibt
            List<String> providers = dbManager.getAllProviders();
            return !providers.isEmpty();
        } catch (Exception e) {
            LOGGER.severe("Fehler beim Prüfen auf Datenbankeinträge: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Beendet den Service und gibt Ressourcen frei
     */
    public void shutdown() {
        if (dbManager != null) {
            dbManager.closeConnection();
        }
        LOGGER.info("Provider History Service beendet");
    }
    }