package utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

/**
 * Hilfklasse zum Herunterladen und Analysieren von Webseiten
 */
public class WebsiteAnalyzer {
    private static final Logger LOGGER = Logger.getLogger(WebsiteAnalyzer.class.getName());
    
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
            statusCallback.onStatusChanged(WebsiteStatus.LOADING, "Webseite wird geladen...");
            
            // Tmp-Verzeichnis erstellen und leeren
            File tmpDir = createAndCleanTmpDirectory();
            if (tmpDir == null) {
                statusCallback.onStatusChanged(WebsiteStatus.ERROR, "Fehler beim Erstellen des tmp-Verzeichnisses");
                return;
            }
            
            // Dateiname für die Webseite erstellen
            File outputFile = new File(tmpDir, filename);
            
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
                            // Analysiere Webseite
                            boolean isAvailable = analyzeWebsite(outputFile);
                            if (isAvailable) {
                                statusCallback.onStatusChanged(
                                    WebsiteStatus.AVAILABLE, 
                                    "Webseite verfügbar"
                                );
                            } else {
                                statusCallback.onStatusChanged(
                                    WebsiteStatus.UNAVAILABLE, 
                                    "Webseite nicht verfügbar oder eingeschränkt"
                                );
                            }
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
            
        } catch (Exception e) {
            LOGGER.severe("Allgemeiner Fehler bei der Webseiten-Analyse: " + e.getMessage());
            e.printStackTrace();
            statusCallback.onStatusChanged(WebsiteStatus.ERROR, "Fehler: " + e.getMessage());
        }
    }
    
    /**
     * Erstellt und leert das tmp-Verzeichnis
     * 
     * @return Das tmp-Verzeichnis oder null bei Fehler
     */
    private File createAndCleanTmpDirectory() {
        try {
            File tmpDir = new File(rootPath, "tmp");
            if (!tmpDir.exists()) {
                if (!tmpDir.mkdirs()) {
                    LOGGER.severe("Konnte tmp-Verzeichnis nicht erstellen: " + tmpDir.getAbsolutePath());
                    return null;
                }
            } else {
                // Verzeichnis leeren
                File[] files = tmpDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (!file.delete()) {
                            LOGGER.warning("Konnte Datei nicht löschen: " + file.getAbsolutePath());
                        }
                    }
                }
            }
            return tmpDir;
        } catch (Exception e) {
            LOGGER.severe("Fehler beim Erstellen/Leeren des tmp-Verzeichnisses: " + e.getMessage());
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
    
    /**
     * Analysiert die heruntergeladene Webseite, um zu prüfen, ob sie verfügbar ist
     * 
     * @param file Die heruntergeladene Webseite-Datei
     * @return true wenn die Webseite verfügbar und OK ist, false sonst
     */
    private boolean analyzeWebsite(File file) {
        try {
            if (!file.exists() || file.length() == 0) {
                LOGGER.warning("Webseite-Datei existiert nicht oder ist leer: " + file.getAbsolutePath());
                return false;
            }
            
            // Inhalt der Datei lesen und auf bestimmte Phrasen prüfen
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Prüfe auf Hinweise, dass die Seite nicht verfügbar ist
                    if (line.contains("not available") || 
                        line.contains("not found") || 
                        line.contains("404") || 
                        line.contains("error") ||
                        line.contains("nicht verfügbar") ||
                        line.contains("nicht gefunden")) {
                        LOGGER.warning("Webseite enthält Hinweise auf Nichtverfügbarkeit: " + line);
                        return false;
                    }
                }
            }
            
            LOGGER.info("Webseite erfolgreich analysiert und als verfügbar eingestuft");
            return true;
        } catch (Exception e) {
            LOGGER.severe("Fehler bei der Analyse der Webseite: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}