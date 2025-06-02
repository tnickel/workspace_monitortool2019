package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility-Klasse zum Laden der drei wichtigen Signal Provider Dateien:
 * - conversionLog.txt
 * - mql4download.txt  
 * - mql5download.txt
 */
public class SignalProviderFileReader {
    private static final Logger logger = Logger.getLogger(SignalProviderFileReader.class.getName());
    
    private final String downloadPath;
    private final String[] fileNames = {
        "conversionLog.txt",
        "mql4download.txt", 
        "mql5download.txt"
    };
    
    public SignalProviderFileReader(String downloadPath) {
        // Die Dateien liegen direkt im download-Verzeichnis, nicht im Unterverzeichnis
        // Falls der Pfad auf mql4/mql5 endet, gehe ein Verzeichnis höher
        if (downloadPath.endsWith("mql4") || downloadPath.endsWith("mql5")) {
            File parentDir = new File(downloadPath).getParentFile();
            this.downloadPath = parentDir != null ? parentDir.getAbsolutePath() : downloadPath;
        } else {
            this.downloadPath = downloadPath;
        }
        
        logger.info("SignalProviderFileReader initialized with path: " + this.downloadPath);
    }
    
    /**
     * Lädt alle drei Dateien und gibt den formatierten Content zurück
     * @return Formatierter String mit allen Dateiinhalten
     */
    public String loadAllFiles() {
        StringBuilder content = new StringBuilder();
        
        for (String fileName : fileNames) {
            content.append(loadSingleFile(fileName));
            content.append("\n\n"); // Trennung zwischen Dateien
        }
        
        return content.toString();
    }
    
    /**
     * Lädt eine einzelne Datei und formatiert sie mit Header
     * @param fileName Name der zu ladenden Datei
     * @return Formatierter Content mit Dateiname als Header
     */
    private String loadSingleFile(String fileName) {
        StringBuilder content = new StringBuilder();
        
        // Header mit Dateiname
        content.append("=".repeat(80)).append("\n");
        content.append("DATEI: ").append(fileName.toUpperCase()).append("\n");
        content.append("=".repeat(80)).append("\n");
        
        File file = new File(downloadPath, fileName);
        
        if (!file.exists()) {
            content.append("FEHLER: Datei nicht gefunden: ").append(file.getAbsolutePath()).append("\n");
            logger.warning("Datei nicht gefunden: " + file.getAbsolutePath());
            return content.toString();
        }
        
        if (!file.canRead()) {
            content.append("FEHLER: Datei kann nicht gelesen werden: ").append(file.getAbsolutePath()).append("\n");
            logger.warning("Datei kann nicht gelesen werden: " + file.getAbsolutePath());
            return content.toString();
        }
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            
            String line;
            int lineNumber = 1;
            
            while ((line = reader.readLine()) != null) {
                content.append(String.format("%4d: %s%n", lineNumber++, line));
            }
            
            if (lineNumber == 1) {
                content.append("(Datei ist leer)\n");
            }
            
        } catch (IOException e) {
            content.append("FEHLER beim Lesen der Datei: ").append(e.getMessage()).append("\n");
            logger.log(Level.WARNING, "Fehler beim Lesen der Datei: " + file.getAbsolutePath(), e);
        }
        
        return content.toString();
    }
    
    /**
     * Überprüft ob alle drei Dateien existieren
     * @return true wenn alle Dateien existieren
     */
    public boolean allFilesExist() {
        for (String fileName : fileNames) {
            File file = new File(downloadPath, fileName);
            if (!file.exists()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Gibt eine Liste der nicht gefundenen Dateien zurück
     * @return Liste der fehlenden Dateinamen
     */
    public List<String> getMissingFiles() {
        List<String> missingFiles = new ArrayList<>();
        
        for (String fileName : fileNames) {
            File file = new File(downloadPath, fileName);
            if (!file.exists()) {
                missingFiles.add(fileName);
            }
        }
        
        return missingFiles;
    }
    
    /**
     * Gibt Informationen über die Dateien zurück
     * @return Formatierte Dateiinformationen
     */
    public String getFileInfo() {
        StringBuilder info = new StringBuilder();
        info.append("DATEI-INFORMATIONEN\n");
        info.append("=".repeat(50)).append("\n");
        info.append("Download-Pfad: ").append(downloadPath).append("\n\n");
        
        for (String fileName : fileNames) {
            File file = new File(downloadPath, fileName);
            info.append("Datei: ").append(fileName).append("\n");
            
            if (file.exists()) {
                info.append("  Status: Vorhanden\n");
                info.append("  Größe: ").append(formatFileSize(file.length())).append("\n");
                info.append("  Letzte Änderung: ").append(new java.util.Date(file.lastModified())).append("\n");
            } else {
                info.append("  Status: NICHT VORHANDEN\n");
            }
            info.append("\n");
        }
        
        return info.toString();
    }
    
    /**
     * Formatiert die Dateigröße human-readable
     * @param bytes Größe in Bytes
     * @return Formatierte Größe
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " Bytes";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }
    
    /**
     * Lädt eine einzelne Datei ohne Header-Formatierung (nur Raw-Content)
     * @param fileName Name der zu ladenden Datei
     * @return Raw Content der Datei
     */
    public String loadSingleFileRaw(String fileName) {
        StringBuilder content = new StringBuilder();
        
        File file = new File(downloadPath, fileName);
        
        if (!file.exists()) {
            content.append("FEHLER: Datei nicht gefunden: ").append(file.getAbsolutePath()).append("\n");
            logger.warning("Datei nicht gefunden: " + file.getAbsolutePath());
            return content.toString();
        }
        
        if (!file.canRead()) {
            content.append("FEHLER: Datei kann nicht gelesen werden: ").append(file.getAbsolutePath()).append("\n");
            logger.warning("Datei kann nicht gelesen werden: " + file.getAbsolutePath());
            return content.toString();
        }
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            
            String line;
            int lineNumber = 1;
            
            while ((line = reader.readLine()) != null) {
                content.append(String.format("%4d: %s%n", lineNumber++, line));
            }
            
            if (lineNumber == 1) {
                content.append("(Datei ist leer)\n");
            }
            
        } catch (IOException e) {
            content.append("FEHLER beim Lesen der Datei: ").append(e.getMessage()).append("\n");
            logger.log(Level.WARNING, "Fehler beim Lesen der Datei: " + file.getAbsolutePath(), e);
        }
        
        return content.toString();
    }

    /**
     * Sucht nach einem Text in allen Dateien und gibt die Zeilen mit Treffern zurück
     * @param searchText Suchtext
     * @return Liste der Treffer mit Dateiname und Zeilennummer
     */
    public List<SearchResult> searchInAllFiles(String searchText) {
        List<SearchResult> results = new ArrayList<>();
        
        if (searchText == null || searchText.trim().isEmpty()) {
            return results;
        }
        
        String lowerSearchText = searchText.toLowerCase();
        
        for (String fileName : fileNames) {
            File file = new File(downloadPath, fileName);
            
            if (!file.exists() || !file.canRead()) {
                continue;
            }
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                
                String line;
                int lineNumber = 1;
                
                while ((line = reader.readLine()) != null) {
                    if (line.toLowerCase().contains(lowerSearchText)) {
                        results.add(new SearchResult(fileName, lineNumber, line));
                    }
                    lineNumber++;
                }
                
            } catch (IOException e) {
                logger.log(Level.WARNING, "Fehler beim Durchsuchen der Datei: " + file.getAbsolutePath(), e);
            }
        }
        
        return results;
    }
    
    /**
     * Datenklasse für Suchergebnisse
     */
    public static class SearchResult {
        private final String fileName;
        private final int lineNumber;
        private final String lineContent;
        
        public SearchResult(String fileName, int lineNumber, String lineContent) {
            this.fileName = fileName;
            this.lineNumber = lineNumber;
            this.lineContent = lineContent;
        }
        
        public String getFileName() { return fileName; }
        public int getLineNumber() { return lineNumber; }
        public String getLineContent() { return lineContent; }
        
        @Override
        public String toString() {
            return String.format("%s:%d - %s", fileName, lineNumber, lineContent);
        }
    }
}