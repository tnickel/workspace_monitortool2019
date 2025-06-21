package reports;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Klasse für die Verwaltung und das Kopieren von PDF-Dokumenten für den Report
 */
public class PdfManager {
    private static final Logger LOGGER = Logger.getLogger(PdfManager.class.getName());
    
    private final String rootPath;
    private final String pdfSourceDir;
    private final String pdfTargetDir;
    
    /**
     * Konstruktor für den PdfManager
     * 
     * @param rootPath Root-Pfad der Anwendung
     */
    public PdfManager(String rootPath) {
        this.rootPath = rootPath;
        // KORREKTUR: "anlalysen" -> "analysen"
        this.pdfSourceDir = rootPath + File.separator + "database" + File.separator + "analysen";
        this.pdfTargetDir = rootPath + File.separator + "report" + File.separator + "pdfs";
        
        LOGGER.info("PdfManager initialisiert:");
        LOGGER.info("  Root-Pfad: " + rootPath);
        LOGGER.info("  Quell-Verzeichnis: " + pdfSourceDir);
        LOGGER.info("  Ziel-Verzeichnis: " + pdfTargetDir);
        
        // Überprüfe Quellverzeichnis
        checkSourceDirectory();
        
        // Stelle sicher, dass das PDF-Zielverzeichnis existiert
        createPdfDirectory();
    }
    
    /**
     * Überprüft das Quellverzeichnis und loggt den Status
     */
    private void checkSourceDirectory() {
        File sourceDir = new File(pdfSourceDir);
        if (!sourceDir.exists()) {
            LOGGER.warning("PDF-Quellverzeichnis existiert nicht: " + pdfSourceDir);
            
            // Prüfe alternative Schreibweisen
            String alternativeDir1 = rootPath + File.separator + "database" + File.separator + "anlalysen";
            String alternativeDir2 = rootPath + File.separator + "database" + File.separator + "analysis";
            
            if (new File(alternativeDir1).exists()) {
                LOGGER.info("Alternative gefunden: " + alternativeDir1);
            }
            if (new File(alternativeDir2).exists()) {
                LOGGER.info("Alternative gefunden: " + alternativeDir2);
            }
        } else {
            LOGGER.info("PDF-Quellverzeichnis gefunden: " + pdfSourceDir);
            
            // Liste alle PDF-Dateien auf
            File[] pdfFiles = sourceDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
            if (pdfFiles != null && pdfFiles.length > 0) {
                LOGGER.info("Verfügbare PDF-Dateien im Quellverzeichnis (" + pdfFiles.length + " Dateien):");
                for (File pdf : pdfFiles) {
                    LOGGER.info("  - " + pdf.getName());
                }
            } else {
                LOGGER.warning("Keine PDF-Dateien im Quellverzeichnis gefunden");
            }
        }
    }
    
    /**
     * Erstellt das PDF-Zielverzeichnis, falls es nicht existiert
     */
    private void createPdfDirectory() {
        File pdfDir = new File(pdfTargetDir);
        if (!pdfDir.exists()) {
            boolean created = pdfDir.mkdirs();
            if (!created) {
                LOGGER.warning("Konnte PDF-Verzeichnis nicht erstellen: " + pdfDir.getAbsolutePath());
            } else {
                LOGGER.info("PDF-Verzeichnis erfolgreich erstellt: " + pdfDir.getAbsolutePath());
            }
        } else {
            LOGGER.info("PDF-Zielverzeichnis bereits vorhanden: " + pdfTargetDir);
        }
    }
    
    /**
     * Findet alle PDF-Dateien für einen bestimmten Provider
     * 
     * @param providerId Die Provider-ID
     * @return Liste der gefundenen PDF-Dateien (nur Dateinamen)
     */
    public List<String> findPdfsForProvider(String providerId) {
        List<String> pdfFiles = new ArrayList<>();
        File sourceDir = new File(pdfSourceDir);
        
        LOGGER.info("Suche PDFs für Provider-ID: " + providerId);
        
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            LOGGER.warning("PDF-Quellverzeichnis existiert nicht: " + pdfSourceDir);
            return pdfFiles;
        }
        
        // Erweiterte Suche - mehrere Varianten der Provider-ID
        List<String> searchPatterns = createSearchPatterns(providerId);
        
        // Durchsuche alle Dateien im Quellverzeichnis
        File[] allFiles = sourceDir.listFiles();
        if (allFiles != null) {
            LOGGER.info("Durchsuche " + allFiles.length + " Dateien im Quellverzeichnis");
            
            for (File file : allFiles) {
                if (file.isFile() && file.getName().toLowerCase().endsWith(".pdf")) {
                    String fileName = file.getName();
                    
                    // Prüfe alle Suchmuster
                    for (String pattern : searchPatterns) {
                        if (fileName.contains(pattern)) {
                            pdfFiles.add(fileName);
                            LOGGER.info("PDF für Provider " + providerId + " gefunden: " + fileName + " (Pattern: " + pattern + ")");
                            break; // Nicht doppelt hinzufügen
                        }
                    }
                }
            }
        } else {
            LOGGER.warning("Konnte Dateien im Quellverzeichnis nicht auflisten");
        }
        
        if (pdfFiles.isEmpty()) {
            LOGGER.warning("Keine PDFs für Provider " + providerId + " gefunden. Suchpatterns waren: " + searchPatterns);
        }
        
        return pdfFiles;
    }
    
    /**
     * Erstellt verschiedene Suchmuster für eine Provider-ID
     * 
     * @param providerId Die Provider-ID
     * @return Liste der Suchmuster
     */
    private List<String> createSearchPatterns(String providerId) {
        List<String> patterns = new ArrayList<>();
        
        // Ursprüngliche Provider-ID
        patterns.add(providerId);
        
        // Mit Unterstrichen
        patterns.add("_" + providerId);
        patterns.add(providerId + "_");
        patterns.add("_" + providerId + "_");
        
        // Mit verschiedenen Trennzeichen
        patterns.add("-" + providerId);
        patterns.add(providerId + "-");
        
        // Falls die Provider-ID Zahlen enthält, auch ohne führende Nullen
        if (providerId.matches("\\d+")) {
            String trimmedId = String.valueOf(Integer.parseInt(providerId));
            if (!trimmedId.equals(providerId)) {
                patterns.add(trimmedId);
                patterns.add("_" + trimmedId);
                patterns.add(trimmedId + "_");
            }
        }
        
        LOGGER.info("Erstelle Suchmuster für Provider-ID " + providerId + ": " + patterns);
        return patterns;
    }
    
    /**
     * Kopiert alle PDF-Dateien für einen Provider ins Zielverzeichnis
     * 
     * @param providerId Die Provider-ID
     * @return Liste der erfolgreich kopierten PDF-Dateien
     */
    public List<String> copyPdfsForProvider(String providerId) {
        List<String> copiedFiles = new ArrayList<>();
        List<String> pdfFiles = findPdfsForProvider(providerId);
        
        LOGGER.info("Beginne Kopieren von " + pdfFiles.size() + " PDFs für Provider " + providerId);
        
        for (String pdfFileName : pdfFiles) {
            if (copyPdfFile(pdfFileName)) {
                copiedFiles.add(pdfFileName);
                LOGGER.info("PDF erfolgreich kopiert: " + pdfFileName);
            } else {
                LOGGER.warning("Fehler beim Kopieren der PDF: " + pdfFileName);
            }
        }
        
        LOGGER.info("Kopiervorgang abgeschlossen für Provider " + providerId + ": " + copiedFiles.size() + " von " + pdfFiles.size() + " PDFs kopiert");
        return copiedFiles;
    }
    
    /**
     * Kopiert alle PDFs für mehrere Provider
     * 
     * @param providerIds Liste der Provider-IDs
     * @return Map mit Provider-ID als Key und Liste der kopierten PDFs als Value
     */
    public java.util.Map<String, List<String>> copyPdfsForProviders(List<String> providerIds) {
        java.util.Map<String, List<String>> result = new java.util.HashMap<>();
        
        LOGGER.info("Beginne Kopieren von PDFs für " + providerIds.size() + " Provider");
        
        for (String providerId : providerIds) {
            List<String> copiedPdfs = copyPdfsForProvider(providerId);
            if (!copiedPdfs.isEmpty()) {
                result.put(providerId, copiedPdfs);
            }
        }
        
        LOGGER.info("PDF-Kopiervorgang für alle Provider abgeschlossen. " + result.size() + " Provider hatten PDFs");
        return result;
    }
    
    /**
     * Kopiert eine einzelne PDF-Datei ins Zielverzeichnis
     * 
     * @param pdfFileName Name der PDF-Datei
     * @return true wenn erfolgreich kopiert, false sonst
     */
    private boolean copyPdfFile(String pdfFileName) {
        try {
            Path sourcePath = Paths.get(pdfSourceDir, pdfFileName);
            Path targetPath = Paths.get(pdfTargetDir, pdfFileName);
            
            LOGGER.info("Versuche zu kopieren: " + sourcePath + " -> " + targetPath);
            
            // Prüfe, ob die Quelldatei existiert
            if (!Files.exists(sourcePath)) {
                LOGGER.warning("PDF-Quelldatei existiert nicht: " + sourcePath);
                return false;
            }
            
            // Prüfe, ob die Zieldatei bereits existiert
            if (Files.exists(targetPath)) {
                LOGGER.info("PDF-Datei bereits vorhanden, überspringe: " + pdfFileName);
                return true; // Bereits vorhanden zählt als Erfolg
            }
            
            // Prüfe Dateigröße
            long fileSize = Files.size(sourcePath);
            LOGGER.info("Kopiere PDF-Datei (Größe: " + fileSize + " Bytes): " + pdfFileName);
            
            // Kopiere die Datei
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            // Verifiziere die Kopie
            if (Files.exists(targetPath)) {
                long copiedSize = Files.size(targetPath);
                if (copiedSize == fileSize) {
                    LOGGER.info("PDF erfolgreich kopiert und verifiziert: " + pdfFileName);
                    return true;
                } else {
                    LOGGER.warning("PDF kopiert, aber Größe stimmt nicht überein: " + pdfFileName + " (Original: " + fileSize + ", Kopie: " + copiedSize + ")");
                    return false;
                }
            } else {
                LOGGER.warning("PDF wurde angeblich kopiert, existiert aber nicht im Zielverzeichnis: " + pdfFileName);
                return false;
            }
            
        } catch (IOException e) {
            LOGGER.severe("Fehler beim Kopieren der PDF-Datei " + pdfFileName + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Überprüft, ob PDF-Dateien für einen Provider vorhanden sind
     * 
     * @param providerId Die Provider-ID
     * @return true wenn PDFs gefunden wurden, false sonst
     */
    public boolean hasPdfsForProvider(String providerId) {
        boolean hasPdfs = !findPdfsForProvider(providerId).isEmpty();
        LOGGER.info("Provider " + providerId + " hat PDFs: " + hasPdfs);
        return hasPdfs;
    }
    
    /**
     * Bereinigt PDF-Dateinamen für die Anzeige
     * 
     * @param pdfFileName Der ursprüngliche Dateiname
     * @return Bereinigter Anzeigename
     */
    public String getDisplayNameForPdf(String pdfFileName) {
        // Entferne .pdf-Endung
        String displayName = pdfFileName;
        if (displayName.toLowerCase().endsWith(".pdf")) {
            displayName = displayName.substring(0, displayName.length() - 4);
        }
        
        // Ersetze Unterstriche durch Leerzeichen
        displayName = displayName.replace("_", " ");
        
        // Kapitalisiere den ersten Buchstaben
        if (!displayName.isEmpty()) {
            displayName = displayName.substring(0, 1).toUpperCase() + 
                         (displayName.length() > 1 ? displayName.substring(1) : "");
        }
        
        return displayName;
    }
    
    /**
     * Gibt den relativen Pfad zur PDF-Datei für HTML-Links zurück
     * 
     * @param pdfFileName Name der PDF-Datei
     * @return Relativer Pfad für HTML-Links
     */
    public String getRelativePdfPath(String pdfFileName) {
        return "pdfs/" + pdfFileName;
    }
    
    /**
     * Löscht alle kopierten PDF-Dateien (Cleanup)
     */
    public void cleanupPdfs() {
        File pdfDir = new File(pdfTargetDir);
        if (pdfDir.exists() && pdfDir.isDirectory()) {
            File[] files = pdfDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
            if (files != null) {
                for (File file : files) {
                    if (file.delete()) {
                        LOGGER.info("PDF-Datei gelöscht: " + file.getName());
                    } else {
                        LOGGER.warning("Konnte PDF-Datei nicht löschen: " + file.getName());
                    }
                }
            }
        }
    }
    
    /**
     * Debug-Methode: Zeigt alle verfügbaren PDFs im Quellverzeichnis
     */
    public void debugListAllPdfs() {
        File sourceDir = new File(pdfSourceDir);
        LOGGER.info("=== DEBUG: Alle PDFs im Quellverzeichnis ===");
        LOGGER.info("Verzeichnis: " + pdfSourceDir);
        
        if (!sourceDir.exists()) {
            LOGGER.info("Verzeichnis existiert nicht!");
            return;
        }
        
        File[] allFiles = sourceDir.listFiles();
        if (allFiles == null) {
            LOGGER.info("Konnte Verzeichnis nicht lesen!");
            return;
        }
        
        LOGGER.info("Insgesamt " + allFiles.length + " Dateien gefunden:");
        for (File file : allFiles) {
            if (file.getName().toLowerCase().endsWith(".pdf")) {
                LOGGER.info("  PDF: " + file.getName());
            }
        }
        LOGGER.info("=== Ende Debug-Liste ===");
    }
}