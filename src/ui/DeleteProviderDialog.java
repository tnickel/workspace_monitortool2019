package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import data.DataManager;
import data.ProviderStats;
import utils.MqlAnalyserConf;

public class DeleteProviderDialog extends JDialog {
    private static final Logger LOGGER = Logger.getLogger(DeleteProviderDialog.class.getName());
    private final DataManager dataManager;
    private final Runnable refreshCallback;
    private final MqlAnalyserConf config;
    private final Map<String, ProviderStats> selectedProviders;

    public DeleteProviderDialog(JFrame parent, String rootPath, DataManager dataManager, 
                              Map<String, ProviderStats> selectedProviders, Runnable refreshCallback) {
        super(parent, "Signal Provider löschen", true);
        this.dataManager = dataManager;
        this.refreshCallback = refreshCallback;
        this.config = new MqlAnalyserConf(rootPath);
        this.selectedProviders = selectedProviders;

        initializeUI();
    }
    
    private void initializeUI() {
        LOGGER.info("Initialisiere DeleteProviderDialog UI");
        
        setLayout(new BorderLayout(10, 10));
        JPanel messagePanel = new JPanel(new BorderLayout(10, 10));
        messagePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel messageLabel = new JLabel(String.format(
            "<html>Möchten Sie wirklich <b>%d</b> ausgewählte Signal Provider löschen?<br>" +
            "Die Dateien werden in den Ordner 'deleted' verschoben.</html>", 
            selectedProviders.size()));
        messagePanel.add(messageLabel, BorderLayout.CENTER);

        // Buttons erstellen
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton deleteButton = new JButton("Löschen");
        JButton cancelButton = new JButton("Abbrechen");

        // Direktere ActionListener-Implementation
        deleteButton.addActionListener(e -> {
            LOGGER.info("Löschen-Button wurde geklickt");
            handleDeleteAction();
        });

        cancelButton.addActionListener(e -> {
            LOGGER.info("Abbrechen-Button wurde geklickt");
            dispose();
        });

        buttonPanel.add(deleteButton);
        buttonPanel.add(cancelButton);

        add(messagePanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        setSize(400, 150);
        setLocationRelativeTo(getParent());
        
        LOGGER.info("DeleteProviderDialog UI wurde initialisiert");
    }
    
    // Separate Methode für die Löschaktion zur besseren Übersichtlichkeit
    private void handleDeleteAction() {
        LOGGER.info("Beginne mit dem Löschen der Provider...");
        
        try {
            if (deleteProviders()) {
                LOGGER.info("Provider erfolgreich gelöscht, aktualisiere UI");
                
                // Cache-Aktualisierung sofort durchführen
                removeProvidersFromCache();
                
                // Dialog schließen
                dispose();
                
                // Nachricht anzeigen und UI aktualisieren
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                        getParent(),
                        "Signal Provider wurden erfolgreich verschoben.",
                        "Erfolg",
                        JOptionPane.INFORMATION_MESSAGE);
                    
                    // Tabelle aktualisieren
                    if (refreshCallback != null) {
                        LOGGER.info("Führe refreshCallback aus");
                        refreshCallback.run();
                    } else {
                        LOGGER.warning("refreshCallback ist null!");
                    }
                });
            }
        } catch (Exception ex) {
            LOGGER.severe("Fehler beim Löschen der Provider: " + ex.getMessage());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "Fehler beim Löschen: " + ex.getMessage(),
                "Fehler",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Entfernt die gelöschten Provider aus dem DataManager-Cache
     */
    private void removeProvidersFromCache() {
        try {
            // Hole aktuelle Provider-Daten
            Map<String, ProviderStats> currentStats = dataManager.getStats();
            
            // Entferne gelöschte Provider aus der Map
            for (String providerName : selectedProviders.keySet()) {
                LOGGER.info("Entferne Provider aus Cache: " + providerName);
                currentStats.remove(providerName);
            }
            
            LOGGER.info("Cache aktualisiert, " + currentStats.size() + " Provider verbleiben");
        } catch (Exception e) {
            LOGGER.severe("Fehler beim Aktualisieren des Caches: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean deleteProviders() {
        try {
            String downloadPath = config.getDownloadPath();
            File deleteDir = new File(downloadPath, "deleted");
            if (!deleteDir.exists()) {
                if (!deleteDir.mkdir()) {
                    LOGGER.warning("Konnte deleted-Verzeichnis nicht erstellen: " + deleteDir.getAbsolutePath());
                    return false;
                }
            }

            Set<String> processedProviders = new HashSet<>();
            for (String provider : selectedProviders.keySet()) {
                moveProviderToDeletedFolder(provider);
                processedProviders.add(provider);
            }

            LOGGER.info("Erfolgreich " + processedProviders.size() + " Provider in deleted-Ordner verschoben");
            return true;
        } catch (Exception e) {
            LOGGER.severe("Fehler beim Löschen der Provider: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void moveProviderToDeletedFolder(String providerName) throws IOException {
        String downloadPath = config.getDownloadPath();
        
        // Extrahiere den Basis-Providernamen ohne die Endung
        String baseProviderName = providerName.replace(".csv", "");
        
        LOGGER.info("Verarbeite Provider: " + baseProviderName);
        
        // Stelle sicher, dass der Zielordner existiert
        Path deletedDir = Paths.get(downloadPath, "deleted");
        if (!Files.exists(deletedDir)) {
            Files.createDirectories(deletedDir);
        }
        
        // Verschiebe die CSV-Datei
        Path csvSource = Paths.get(downloadPath, providerName);
        Path csvTarget = Paths.get(downloadPath, "deleted", providerName);
        
        if (Files.exists(csvSource)) {
            LOGGER.info("Verschiebe CSV von: " + csvSource + " nach: " + csvTarget);
            Files.move(csvSource, csvTarget, StandardCopyOption.REPLACE_EXISTING);
        } else {
            LOGGER.warning("CSV-Datei nicht gefunden: " + csvSource);
        }
        
        // Durchsuche das Download-Verzeichnis nach allen zugehörigen Dateien
        File downloadDir = new File(downloadPath);
        File[] files = downloadDir.listFiles();
        int movedFiles = 1; // CSV bereits gezählt
        
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                
                // Überprüfe, ob die Datei zum Provider gehört (HTML oder TXT)
                if ((fileName.endsWith(".html") || fileName.endsWith(".txt")) && 
                    (fileName.startsWith(baseProviderName) || fileName.contains(baseProviderName))) {
                    
                    Path source = file.toPath();
                    Path target = Paths.get(downloadPath, "deleted", fileName);
                    
                    LOGGER.info("Verschiebe zugehörige Datei: " + source + " nach: " + target);
                    
                    try {
                        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                        movedFiles++;
                    } catch (IOException e) {
                        LOGGER.warning("Fehler beim Verschieben der Datei: " + source + ", Grund: " + e.getMessage());
                    }
                }
            }
        }
        
        LOGGER.info("Insgesamt verschobene Dateien für Provider " + baseProviderName + ": " + movedFiles);
    }
}