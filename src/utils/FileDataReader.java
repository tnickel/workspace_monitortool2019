package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Klasse für den grundlegenden Dateizugriff und das Parsen von _root.txt Dateien.
 * Verwaltet auch den Cache für bereits gelesene Dateien.
 */
public class FileDataReader {
    private static final Logger LOGGER = Logger.getLogger(FileDataReader.class.getName());
    
    private final String downloadPath;
    private final Map<String, Map<String, String>> dataCache;
    
    public FileDataReader(String downloadPath) {
        this.downloadPath = downloadPath;
        this.dataCache = new HashMap<>();
        
        // Protokolliere den tatsächlich verwendeten Pfad
        LOGGER.info("FileDataReader initialisiert mit Pfad: " + downloadPath);
    }
    
    /**
     * Hilfsmethode, um einen konsistenten Dateinamen als Cache-Schlüssel zu erstellen
     */
    private String createCacheKey(String fileName) {
        if (fileName.endsWith(".csv")) {
            return fileName; // CSV-Dateien als Schlüssel belassen
        } else if (fileName.endsWith("_root.txt")) {
            return fileName.replace("_root.txt", ".csv"); // Zu CSV-Format konvertieren
        } else {
            return fileName + ".csv"; // Suffix hinzufügen für Konsistenz
        }
    }
    
    /**
     * Hilfsmethode, um einen korrekten Pfad zur TXT-Datei zu erstellen
     */
    private String createTxtFilePath(String fileName) {
        if (fileName.endsWith("_root.txt")) {
            return fileName; // Bereits korrekt
        } else if (fileName.endsWith(".csv")) {
            return fileName.replace(".csv", "") + "_root.txt";
        } else {
            return fileName + "_root.txt"; // Suffix hinzufügen
        }
    }
    
    /**
     * Liest und parst eine _root.txt Datei und gibt die Daten als Map zurück.
     * Nutzt Caching für bessere Performance.
     * 
     * @param fileName Name der Datei (z.B. provider_123456.csv)
     * @return Map mit Schlüssel-Wert-Paaren aus der Datei
     */
    public Map<String, String> getFileData(String fileName) {
        String cacheKey = createCacheKey(fileName);
        
        if (dataCache.containsKey(cacheKey)) {
            return dataCache.get(cacheKey);
        }

        String txtFileName = createTxtFilePath(fileName);
        File txtFile = new File(downloadPath, txtFileName);
        
        // Protokolliere den vollständigen Pfad zur Datei
        LOGGER.info("Versuche Textdatei zu lesen: " + txtFile.getAbsolutePath());
        
        if (!txtFile.exists()) {
            LOGGER.warning("Text file not found: " + txtFile.getAbsolutePath());
            System.err.println("WARNUNG: Die Datei " + txtFile.getAbsolutePath() + " wurde nicht gefunden!");
            
            // Prüfe, ob das Verzeichnis überhaupt existiert
            File dir = new File(downloadPath);
            if (!dir.exists() || !dir.isDirectory()) {
                LOGGER.severe("Das Verzeichnis existiert nicht: " + downloadPath);
                System.err.println("FEHLER: Das Verzeichnis " + downloadPath + " existiert nicht!");
            } else {
                // Liste die Dateien im Verzeichnis auf, um zu debuggen
                File[] files = dir.listFiles((d, name) -> name.endsWith("_root.txt"));
                if (files != null && files.length > 0) {
                    System.out.println("Gefundene _root.txt Dateien im Verzeichnis (" + files.length + "):");
                    for (int i = 0; i < Math.min(files.length, 5); i++) {
                        System.out.println(" - " + files[i].getName());
                    }
                    if (files.length > 5) {
                        System.out.println(" ... und " + (files.length - 5) + " weitere");
                    }
                } else {
                    System.err.println("Keine _root.txt Dateien im Verzeichnis gefunden: " + downloadPath);
                }
            }
            
            return new HashMap<>();
        }
        
        Map<String, String> data = new HashMap<>();
        StringBuilder currentSection = new StringBuilder();
        String currentKey = null;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(txtFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Ignoriere Asterisk-Linien
                if (line.trim().matches("\\*+")) {
                    if (currentKey != null && currentSection.length() > 0) {
                        data.put(currentKey, currentSection.toString().trim());
                        currentSection = new StringBuilder();
                    }
                    continue;
                }
                
                // Prüfe ob es eine neue Sektion ist
                if (line.contains("=")) {
                    // Speichere die vorherige Sektion, falls vorhanden
                    if (currentKey != null && currentSection.length() > 0) {
                        data.put(currentKey, currentSection.toString().trim());
                        currentSection = new StringBuilder();
                    }
                    
                    String[] parts = line.split("=", 2);
                    currentKey = parts[0].trim();
                    if (parts.length > 1) {
                        currentSection.append(parts[1].trim());
                    }
                } else if (currentKey != null && !line.trim().isEmpty()) {
                    // Füge die Zeile zur aktuellen Sektion hinzu
                    if (currentSection.length() > 0) {
                        currentSection.append("\n");
                    }
                    currentSection.append(line.trim());
                }
            }
            
            // Speichere die letzte Sektion
            if (currentKey != null && currentSection.length() > 0) {
                data.put(currentKey, currentSection.toString().trim());
            }
            
            dataCache.put(cacheKey, data);
            
            // Log die gelesenen Schlüssel
            LOGGER.info("Gelesen aus " + txtFileName + ", gefundene Schlüssel: " + data.keySet());
            
            return data;
        } catch (IOException e) {
            LOGGER.severe("Error reading text file: " + e.getMessage());
            System.err.println("FEHLER beim Lesen der Datei " + txtFile.getAbsolutePath() + ": " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * Liest die Drawdown-Chart-Daten direkt aus der .txt-Datei
     * 
     * @param fileName Name der Datei (z.B. provider_123456.csv)
     * @return String mit Drawdown-Chart-Daten oder null wenn nicht gefunden
     */
    public String readDrawdownChartDataFromFile(String fileName) {
        String txtFileName = createTxtFilePath(fileName);
        File txtFile = new File(downloadPath, txtFileName);
        
        if (!txtFile.exists()) {
            LOGGER.warning("Textdatei nicht gefunden: " + txtFile.getAbsolutePath());
            return null;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(txtFile))) {
            StringBuilder drawdownData = new StringBuilder();
            boolean inDrawdownSection = false;
            String line;
            
            while ((line = reader.readLine()) != null) {
                // Prüfe, ob die Drawdown-Sektion beginnt
                if (line.startsWith("Drawdown Chart Data=")) {
                    inDrawdownSection = true;
                    
                    // Wenn die Zeile bereits Daten enthält, füge sie hinzu
                    if (line.contains(":")) {
                        String dataLine = line.substring(line.indexOf("=") + 1).trim();
                        if (!dataLine.isEmpty()) {
                            drawdownData.append(dataLine).append("\n");
                        }
                    }
                    continue;
                }
                
                // Wenn wir in der Drawdown-Sektion sind und auf eine leere Zeile oder neue Sektion stoßen, beende die Sektion
                if (inDrawdownSection && (line.trim().isEmpty() || line.contains("="))) {
                    inDrawdownSection = false;
                }
                
                // Füge Zeilen innerhalb der Drawdown-Sektion hinzu
                if (inDrawdownSection && line.contains(":")) {
                    drawdownData.append(line).append("\n");
                }
            }
            
            String result = drawdownData.toString().trim();
            return result.isEmpty() ? null : result;
            
        } catch (IOException e) {
            LOGGER.warning("Fehler beim Lesen der Drawdown-Daten aus der Datei: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Überprüft den Zugriff auf das Dateisystem
     * 
     * @return true wenn das Verzeichnis existiert und lesbar ist
     */
    public boolean checkFileAccess() {
        File dir = new File(downloadPath);
        if (!dir.exists()) {
            LOGGER.severe("Das Verzeichnis existiert nicht: " + downloadPath);
            return false;
        }
        
        if (!dir.isDirectory()) {
            LOGGER.severe("Der Pfad ist kein Verzeichnis: " + downloadPath);
            return false;
        }
        
        if (!dir.canRead()) {
            LOGGER.severe("Keine Leseberechtigung für: " + downloadPath);
            return false;
        }
        
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            LOGGER.warning("Verzeichnis leer oder Zugriff nicht möglich: " + downloadPath);
            return false;
        }
        
        return true;
    }
    
    /**
     * Gibt den Root-Pfad zurück
     * 
     * @return Der Download-Pfad
     */
    public String getRootPath() {
        return downloadPath;
    }
    
    /**
     * Erlaubt externen Klassen, Daten im Cache zu speichern
     * 
     * @param fileName Name der Datei
     * @param key Schlüssel für den Wert
     * @param value Wert der gespeichert werden soll
     */
    public void updateCacheData(String fileName, String key, String value) {
        String cacheKey = createCacheKey(fileName);
        Map<String, String> data = dataCache.get(cacheKey);
        if (data != null) {
            data.put(key, value);
        }
    }
    
    /**
     * Leert den Cache für eine bestimmte Datei
     * 
     * @param fileName Name der Datei
     */
    public void clearCache(String fileName) {
        String cacheKey = createCacheKey(fileName);
        dataCache.remove(cacheKey);
    }
    
    /**
     * Leert den gesamten Cache
     */
    public void clearAllCache() {
        dataCache.clear();
    }
}