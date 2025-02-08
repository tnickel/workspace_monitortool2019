package ui;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.logging.Logger;

import data.DataManager;
import data.ProviderStats;
import utils.MqlAnalyserConf;

public class DeleteProviderDialog extends JDialog {
    private static final Logger LOGGER = Logger.getLogger(DeleteProviderDialog.class.getName());
    private final String rootPath;
    private final DataManager dataManager;
    private final Runnable refreshCallback;
    private final MqlAnalyserConf config;

    public DeleteProviderDialog(JFrame parent, String rootPath, DataManager dataManager, 
                              Map<String, ProviderStats> selectedProviders, Runnable refreshCallback) {
        super(parent, "Signal Provider löschen", true);
        this.rootPath = rootPath;
        this.dataManager = dataManager;
        this.refreshCallback = refreshCallback;
        this.config = new MqlAnalyserConf(rootPath);

        // Berechne Anzahl zu löschender Provider
        int deleteCount = dataManager.getStats().size() - selectedProviders.size();

        // Dialog-Layout
        setLayout(new BorderLayout(10, 10));
        JPanel messagePanel = new JPanel(new BorderLayout(10, 10));
        messagePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Nachricht
        JLabel messageLabel = new JLabel(String.format(
            "<html>Möchten Sie wirklich <b>%d</b> nicht selektierte Signal Provider löschen?<br>" +
            "Die Dateien werden in den Ordner 'deleted' verschoben.</html>", deleteCount));
        messagePanel.add(messageLabel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton deleteButton = new JButton("Löschen");
        JButton cancelButton = new JButton("Abbrechen");

        deleteButton.addActionListener(e -> {
            if (deleteProviders(selectedProviders)) {
                JOptionPane.showMessageDialog(this,
                    "Signal Provider wurden erfolgreich verschoben.",
                    "Erfolg",
                    JOptionPane.INFORMATION_MESSAGE);
                dispose();
                refreshCallback.run();
            }
        });

        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(deleteButton);
        buttonPanel.add(cancelButton);

        add(messagePanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Dialog-Größe und Position
        setSize(400, 150);
        setLocationRelativeTo(parent);
    }

    private boolean deleteProviders(Map<String, ProviderStats> selectedProviders) {
        try {
            String downloadPath = config.getDownloadPath();
            
            // Erstelle deleted Verzeichnis falls nicht vorhanden
            File deleteDir = new File(downloadPath, "deleted");
            if (!deleteDir.exists()) {
                deleteDir.mkdir();
            }

            // Verschiebe nicht selektierte Provider
            for (String provider : dataManager.getStats().keySet()) {
                if (!selectedProviders.containsKey(provider)) {
                    moveProviderToDeletedFolder(provider);
                }
            }

            return true;
        } catch (Exception e) {
            LOGGER.severe("Fehler beim Löschen der Provider: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                "Fehler beim Löschen der Provider: " + e.getMessage(),
                "Fehler",
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void moveProviderToDeletedFolder(String providerName) throws IOException {
        String downloadPath = config.getDownloadPath();
        
        // Korrekte Pfadkonstruktion mit downloadPath aus der Config
        Path csvSource = Paths.get(downloadPath, providerName);
        Path csvTarget = Paths.get(downloadPath, "deleted", providerName);
        
        // Debug-Logging
        LOGGER.info("Source path: " + csvSource);
        LOGGER.info("Target path: " + csvTarget);
        
        // Stelle sicher, dass das deleted Verzeichnis existiert
        Files.createDirectories(csvTarget.getParent());
        
        // Verschiebe CSV-Datei
        Files.move(csvSource, csvTarget, StandardCopyOption.REPLACE_EXISTING);

        // Verschiebe HTML-Datei wenn vorhanden
        String htmlFileName = providerName.replace(".csv", "_root.html");
        Path htmlSource = Paths.get(downloadPath, htmlFileName);
        Path htmlTarget = Paths.get(downloadPath, "deleted", htmlFileName);
        
        if (Files.exists(htmlSource)) {
            Files.move(htmlSource, htmlTarget, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}