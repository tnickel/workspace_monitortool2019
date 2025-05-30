package ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import ui.components.AppUIStyle;

public class TextFileViewerDialog extends JDialog {
    private static final Logger LOGGER = Logger.getLogger(TextFileViewerDialog.class.getName());
    
    private final String downloadPath;
    private final String providerName;
    private final JTextArea textArea;
    private final JLabel statusLabel;
    
    public TextFileViewerDialog(JFrame parent, String downloadPath, String providerName) {
        super(parent, "Root Textfile Viewer - " + providerName, true);
        this.downloadPath = downloadPath;
        this.providerName = providerName;
        
        // Text Area für Dateiinhalt
        this.textArea = new JTextArea();
        this.textArea.setEditable(false);
        this.textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        this.textArea.setBackground(Color.WHITE);
        this.textArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Status Label
        this.statusLabel = AppUIStyle.createStyledLabel("");
        this.statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        
        setupUI();
        loadTextFile();
    }
    
    private void setupUI() {
        setLayout(new BorderLayout());
        
        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(AppUIStyle.PRIMARY_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        JLabel titleLabel = new JLabel("Root Textfile für Provider: " + providerName);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(statusLabel, BorderLayout.EAST);
        
        add(headerPanel, BorderLayout.NORTH);
        
        // Text Area mit Scroll Pane
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(800, 600));
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(10, 10, 0, 10),
            BorderFactory.createLineBorder(AppUIStyle.SECONDARY_COLOR, 1)
        ));
        
        add(scrollPane, BorderLayout.CENTER);
        
        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JButton refreshButton = AppUIStyle.createStyledButton("Aktualisieren");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadTextFile();
            }
        });
        
        JButton closeButton = AppUIStyle.createStyledButton("Schließen");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        
        buttonPanel.add(refreshButton);
        buttonPanel.add(closeButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Dialog Eigenschaften
        setSize(850, 700);
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
    
    private void loadTextFile() {
        try {
            // Provider-Namen für Dateiname vorbereiten (ersten Buchstaben groß machen)
            String fileName = capitalizeFirstLetter(providerName) + "_root.txt";
            
            // Direkt im aktuellen Download-Path suchen
            String filePath = downloadPath + File.separator + fileName;
            File textFile = new File(filePath);
            
            LOGGER.info("Suche nach Datei: " + fileName + " für Provider: " + providerName);
            LOGGER.info("Verwende Download-Path: " + downloadPath);
            LOGGER.info("Vollständiger Pfad: " + filePath);
            
            if (!textFile.exists()) {
                String errorMessage = "Die Root-Textdatei für Provider '" + providerName + "' wurde nicht gefunden:\n" +
                                     "Gesuchte Datei: " + fileName + "\n" +
                                     "Download-Path: " + downloadPath + "\n" +
                                     "Vollständiger Pfad: " + filePath + "\n\n";
                
                // Zeige alle Dateien im Verzeichnis zum Debugging
                File downloadDir = new File(downloadPath);
                if (downloadDir.exists() && downloadDir.isDirectory()) {
                    File[] files = downloadDir.listFiles((dir, name) -> name.toLowerCase().endsWith("_root.txt"));
                    if (files != null && files.length > 0) {
                        errorMessage += "Gefundene _root.txt Dateien im Verzeichnis:\n";
                        for (File file : files) {
                            errorMessage += "• " + file.getName() + "\n";
                        }
                    } else {
                        errorMessage += "Keine _root.txt Dateien im Verzeichnis gefunden.";
                    }
                } else {
                    errorMessage += "Das Download-Verzeichnis existiert nicht oder ist nicht lesbar.";
                }
                
                textArea.setText(errorMessage);
                statusLabel.setText("Datei nicht gefunden");
                statusLabel.setForeground(Color.RED);
                
                LOGGER.warning("Root-Textdatei nicht gefunden: " + filePath);
                return;
            }
            
            if (!textFile.canRead()) {
                String errorMessage = "Die Root-Textdatei kann nicht gelesen werden:\n" + filePath + 
                                     "\nBitte prüfen Sie die Dateiberechtigungen.";
                textArea.setText(errorMessage);
                statusLabel.setText("Keine Leseberechtigung");
                statusLabel.setForeground(Color.RED);
                
                LOGGER.warning("Keine Leseberechtigung für Datei: " + filePath);
                return;
            }
            
            // Datei lesen
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(textFile))) {
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                    lineCount++;
                }
                
                textArea.setText(content.toString());
                textArea.setCaretPosition(0); // Scroll to top
                
                // Status anzeigen
                long fileSize = textFile.length();
                String sizeStr = fileSize < 1024 ? fileSize + " Bytes" : 
                                fileSize < 1024 * 1024 ? (fileSize / 1024) + " KB" : 
                                (fileSize / (1024 * 1024)) + " MB";
                
                statusLabel.setText("Zeilen: " + lineCount + " | Größe: " + sizeStr + " | Path: " + downloadPath);
                statusLabel.setForeground(AppUIStyle.SUCCESS_COLOR);
                
                LOGGER.info("Root-Textdatei erfolgreich geladen: " + lineCount + " Zeilen, " + sizeStr + " von " + filePath);
                
            }
            
        } catch (IOException e) {
            String errorMessage = "Fehler beim Lesen der Root-Textdatei für Provider '" + providerName + "':\n" + e.getMessage();
            textArea.setText(errorMessage);
            statusLabel.setText("Lesefehler");
            statusLabel.setForeground(Color.RED);
            
            LOGGER.severe("Fehler beim Lesen der Root-Textdatei: " + e.getMessage());
            
            JOptionPane.showMessageDialog(this,
                "Fehler beim Lesen der Root-Textdatei:\n" + e.getMessage(),
                "Datei-Lesefehler",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Macht den ersten Buchstaben eines Strings groß
     * z.B. "a_flying_2296908" -> "A_flying_2296908"
     */
    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}