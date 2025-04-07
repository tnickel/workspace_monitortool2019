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
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import data.DataManager;
import data.ProviderStats;
import utils.MqlAnalyserConf;

public class DeleteProviderDialog extends JDialog {
    private static final Logger LOGGER = Logger.getLogger(DeleteProviderDialog.class.getName());
    private final DataManager dataManager;
    private final Runnable refreshCallback;
    private final MqlAnalyserConf config;

    public DeleteProviderDialog(JFrame parent, String rootPath, DataManager dataManager, 
                              Map<String, ProviderStats> selectedProviders, Runnable refreshCallback) {
        super(parent, "Signal Provider l�schen", true);
        this.dataManager = dataManager;
        this.refreshCallback = refreshCallback;
        this.config = new MqlAnalyserConf(rootPath);

        setLayout(new BorderLayout(10, 10));
        JPanel messagePanel = new JPanel(new BorderLayout(10, 10));
        messagePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel messageLabel = new JLabel(String.format(
            "<html>M�chten Sie wirklich <b>%d</b> ausgew�hlte Signal Provider l�schen?<br>" +
            "Die Dateien werden in den Ordner 'deleted' verschoben.</html>", 
            selectedProviders.size()));
        messagePanel.add(messageLabel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton deleteButton = new JButton("L�schen");
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

        setSize(400, 150);
        setLocationRelativeTo(parent);
    }

    private boolean deleteProviders(Map<String, ProviderStats> selectedProviders) {
        try {
            String downloadPath = config.getDownloadPath();
            File deleteDir = new File(downloadPath, "deleted");
            if (!deleteDir.exists()) {
                deleteDir.mkdir();
            }

            for (String provider : selectedProviders.keySet()) {
                moveProviderToDeletedFolder(provider);
            }

            return true;
        } catch (Exception e) {
            LOGGER.severe("Fehler beim L�schen der Provider: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                "Fehler beim L�schen der Provider: " + e.getMessage(),
                "Fehler",
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void moveProviderToDeletedFolder(String providerName) throws IOException {
        String downloadPath = config.getDownloadPath();
        Path csvSource = Paths.get(downloadPath, providerName);
        Path csvTarget = Paths.get(downloadPath, "deleted", providerName);
        
        LOGGER.info("Moving from: " + csvSource + " to: " + csvTarget);
        
        Files.createDirectories(csvTarget.getParent());
        Files.move(csvSource, csvTarget, StandardCopyOption.REPLACE_EXISTING);

        String htmlFileName = providerName.replace(".csv", "_root.html");
        Path htmlSource = Paths.get(downloadPath, htmlFileName);
        Path htmlTarget = Paths.get(downloadPath, "deleted", htmlFileName);
        
        if (Files.exists(htmlSource)) {
            Files.move(htmlSource, htmlTarget, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}