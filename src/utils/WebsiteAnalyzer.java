package utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

/**
 * Hilfklasse zum Herunterladen und Analysieren von Webseiten
 */
public class WebsiteAnalyzer {
    private static final Logger LOGGER = Logger.getLogger(WebsiteAnalyzer.class.getName());
    
    // Konstante für die maximale Gültigkeit der Dateien in Tagen
    private static final int MAX_FILE_AGE_DAYS = 3;
    
    // Konstante für die Mindestgröße der Datei in KB, um als verfügbar zu gelten
    private static final long MIN_FILE_SIZE_KB = 60;
    
    /**
     * Mögliche Status einer Webseite
     */
    public enum WebsiteStatus {
        LOADING, AVAILABLE, UNAVAILABLE, ERROR
    }
    
    /**
     * Interface für Status-Callbacks
     */
    public interface StatusCallback {
        void onStatusChanged(WebsiteStatus status, String message);
    }
    
    // Pfad zum Stammverzeichnis
    private final String rootPath;
    
    /**
     * Konstruktor
     * 
     * @param rootPath Pfad zum Stammverzeichnis, in dem das tmp-Verzeichnis erstellt wird
     */
    public WebsiteAnalyzer(String rootPath) {
        this.rootPath = rootPath;
    }
    
    /**
     * Lädt eine Webseite herunter und analysiert sie im Hintergrund
     * 
     * @param url URL der Webseite
     * @param filename Dateiname für die gespeicherte Webseite
     * @param statusCallback Callback für Statusänderungen
     */
    public void analyzeWebsiteAsync(String url, String filename, StatusCallback statusCallback) {
        try {
            // Status auf "Lade..." setzen
            statusCallback.onStatusChanged(WebsiteStatus.LOADING, "Webseite wird geprüft...");
            
            // Tmp-Verzeichnis erstellen, wenn es nicht existiert
            File tmpDir = createTmpDirectory();
            if (tmpDir == null) {
                statusCallback.onStatusChanged(WebsiteStatus.ERROR, "Fehler beim Erstellen des tmp-Verzeichnisses");
                return;
            }
            
            // Dateiname für die Webseite erstellen
            File outputFile = new File(tmpDir, filename);
            
            // Überprüfe, ob die Datei existiert und nicht zu alt ist
            boolean needsDownload = !outputFile.exists() || isFileOlderThanDays(outputFile, MAX_FILE_AGE_DAYS);
            
            if (needsDownload) {
                // Alte Datei löschen, falls sie existiert
                if (outputFile.exists() && !outputFile.delete()) {
                    LOGGER.warning("Konnte alte Datei nicht löschen: " + outputFile.getAbsolutePath());
                }
                
                statusCallback.onStatusChanged(WebsiteStatus.LOADING, "Webseite wird heruntergeladen...");
                
                // Webseite herunterladen im Hintergrund
                new SwingWorker<Boolean, Void>() {
                    @Override
                    protected Boolean doInBackground() throws Exception {
                        return downloadWebsite(url, outputFile);
                    }
                    
                    @Override
                    protected void done() {
                        try {
                            boolean success = get();
                            if (success) {
                                // Prüfe die Dateigröße
                                analyzeFileSize(outputFile, statusCallback);
                            } else {
                                statusCallback.onStatusChanged(
                                    WebsiteStatus.ERROR, 
                                    "Download fehlgeschlagen"
                                );
                            }
                        } catch (Exception e) {
                            LOGGER.severe("Fehler bei der Webseiten-Analyse: " + e.getMessage());
                            e.printStackTrace();
                            statusCallback.onStatusChanged(
                                WebsiteStatus.ERROR, 
                                "Fehler: " + e.getMessage()
                            );
                        }
                    }
                }.execute();
                
            } else {
                // Datei existiert und ist nicht zu alt, prüfe nur die Dateigröße
                LOGGER.info("Verwende existierende Datei: " + outputFile.getAbsolutePath());
                analyzeFileSize(outputFile, statusCallback);
            }
            
        } catch (Exception e) {
            LOGGER.severe("Allgemeiner Fehler bei der Webseiten-Analyse: " + e.getMessage());
            e.printStackTrace();
            statusCallback.onStatusChanged(WebsiteStatus.ERROR, "Fehler: " + e.getMessage());
        }
    }
    
    /**
     * Prüft, ob eine Datei älter als die angegebene Anzahl von Tagen ist
     * 
     * @param file Die zu prüfende Datei
     * @param days Anzahl der Tage
     * @return true wenn die Datei älter als die angegebene Anzahl von Tagen ist, false sonst
     */
    private boolean isFileOlderThanDays(File file, int days) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            Instant creationTime = attrs.creationTime().toInstant();
            LocalDateTime fileDate = LocalDateTime.ofInstant(creationTime, ZoneId.systemDefault());
            LocalDateTime now = LocalDateTime.now();
            
            return ChronoUnit.DAYS.between(fileDate, now) > days;
        } catch (Exception e) {
            LOGGER.warning("Fehler beim Prüfen des Dateialters, nehme an, dass die Datei zu alt ist: " + e.getMessage());
            return true;
        }
    }
    
    /**
     * Analysiert die Dateigröße und setzt den Status entsprechend
     * 
     * @param file Die zu analysierende Datei
     * @param statusCallback Callback für Statusänderungen
     */
    private void analyzeFileSize(File file, StatusCallback statusCallback) {
        long fileSizeKB = file.length() / 1024;
        
        if (fileSizeKB >= MIN_FILE_SIZE_KB) {
            // Datei ist größer als die Mindestgröße, daher wahrscheinlich verfügbar
            statusCallback.onStatusChanged(
                WebsiteStatus.AVAILABLE, 
                "Webseite verfügbar (" + fileSizeKB + " KB)"
            );
        } else {
            // Datei ist kleiner als die Mindestgröße, daher wahrscheinlich nicht verfügbar
            statusCallback.onStatusChanged(
                WebsiteStatus.UNAVAILABLE, 
                "Webseite nicht verfügbar oder eingeschränkt (" + fileSizeKB + " KB)"
            );
        }
    }
    
    /**
     * Erstellt das tmp-Verzeichnis, wenn es nicht existiert
     * 
     * @return Das tmp-Verzeichnis oder null bei Fehler
     */
    private File createTmpDirectory() {
        try {
            File tmpDir = new File(rootPath, "tmp");
            if (!tmpDir.exists()) {
                if (!tmpDir.mkdirs()) {
                    LOGGER.severe("Konnte tmp-Verzeichnis nicht erstellen: " + tmpDir.getAbsolutePath());
                    return null;
                }
            }
            return tmpDir;
        } catch (Exception e) {
            LOGGER.severe("Fehler beim Erstellen des tmp-Verzeichnisses: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Lädt die Webseite herunter und speichert sie in der angegebenen Datei
     * 
     * @param url URL der Webseite
     * @param outputFile Ausgabedatei
     * @return true wenn erfolgreich, false bei Fehler
     */
    private boolean downloadWebsite(String url, File outputFile) {
        try {
            LOGGER.info("Lade URL: " + url);
            java.net.URL website = new java.net.URL(url);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) website.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            
            try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                 FileOutputStream fos = new FileOutputStream(outputFile)) {
                
                byte[] dataBuffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(dataBuffer, 0, dataBuffer.length)) != -1) {
                    fos.write(dataBuffer, 0, bytesRead);
                }
            }
            
            LOGGER.info("Download abgeschlossen: " + outputFile.getAbsolutePath() + " (" + outputFile.length() + " bytes)");
            return true;
        } catch (Exception e) {
            LOGGER.severe("Fehler beim Herunterladen der Webseite: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}