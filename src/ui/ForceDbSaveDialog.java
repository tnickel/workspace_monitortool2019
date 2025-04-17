package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import data.ProviderStats;
import services.ProviderHistoryService;
import utils.HtmlDatabase;

/**
 * Dialog zum Erzwingen der Speicherung aller Provider-Daten in die Datenbank
 */
public class ForceDbSaveDialog extends JDialog {
    private final ProviderHistoryService historyService;
    private final Map<String, ProviderStats> providers;
    private final JTextArea logArea;
    private final JProgressBar progressBar;
    private JButton closeButton;
    private JButton saveButton;
    private JButton backupButton; // Neuer Button für manuelles Backup
    private final String rootPath;
    
    public ForceDbSaveDialog(JFrame parent, ProviderHistoryService historyService, 
            Map<String, ProviderStats> providers, String rootPath) {
        super(parent, "Datenbank-Speicherung erzwingen", true);
        this.historyService = historyService;
        this.providers = providers;
        this.rootPath = rootPath;
        
        // UI-Komponenten
        logArea = new JTextArea();
        logArea.setEditable(false);
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        
        initUI();
        // Größere Höhe für den Dialog um sicherzustellen, dass alles sichtbar ist
        setSize(600, 500);
        setLocationRelativeTo(parent);
    }
    
    private void initUI() {
        // Hauptlayout auf Box-Layout ändern für bessere Kontrolle
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        
        // Informations-Panel am oberen Rand
        JPanel infoPanel = new JPanel(new BorderLayout(5, 5));
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Sicherheitshinweis"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        JLabel infoLabel = new JLabel("<html><div style='width: 500px;'>" +
            "<b>Wichtig:</b> Diese Funktion speichert alle aktuellen Daten zusätzlich in der Datenbank. " +
            "Vorhandene Einträge werden niemals überschrieben oder gelöscht. " +
            "Vor dem Speichern wird automatisch ein Backup erstellt.<br><br>" +
            "Bei der normalen Datenbanknutzung werden nur neue oder geänderte Werte gespeichert. " +
            "Nutzen Sie diese Funktion nur, wenn Sie alle Werte explizit neu speichern möchten.</div></html>");
        
        // Hinzufügen einer extra Anleitung in rot
        JLabel instructionLabel = new JLabel("<html><font color='red'><b>Um den Speichervorgang zu starten, klicken Sie auf den \"Speicherung starten\" Button.</b></font></html>");
        instructionLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Infopanel zusammenbauen
        infoPanel.add(infoLabel, BorderLayout.CENTER);
        infoPanel.add(instructionLabel, BorderLayout.SOUTH);
        
        // Log-Bereich
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Log"));
        scrollPane.setPreferredSize(new Dimension(580, 250));
        
        // Initial Log-Eintrag anzeigen
        logArea.setText("Bereit für den Start der Speicherung. Klicken Sie auf \"Speicherung starten\"...\n" +
                        "Gefundene Provider: " + providers.size() + "\n");
        
        // Progress-Panel
        JPanel progressPanel = new JPanel(new BorderLayout(5, 5));
        progressPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        progressPanel.add(progressBar, BorderLayout.CENTER);
        
        // Button-Panel - explizite Größe setzen
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));
        buttonPanel.setPreferredSize(new Dimension(600, 50));
        
        saveButton = new JButton("Speicherung starten");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startSaving();
            }
        });
        
        backupButton = new JButton("Backup erstellen");
        backupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createBackup();
            }
        });
        
        closeButton = new JButton("Schließen");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        
        // Wichtig: Buttons in der richtigen Reihenfolge hinzufügen
        buttonPanel.add(backupButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(closeButton);
        
        // Hauptpanel zusammenbauen
        mainPanel.add(infoPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(scrollPane);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(progressPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(buttonPanel);
        
        // ContentPane setzen
        setContentPane(mainPanel);
    }
    
    private void createBackup() {
        // UI-Status aktualisieren
        backupButton.setEnabled(false);
        saveButton.setEnabled(false);
        closeButton.setEnabled(false);
        logArea.setText("");
        progressBar.setValue(0);
        progressBar.setIndeterminate(true);
        
        // SwingWorker für Hintergrundverarbeitung
        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                publish("Backup der Datenbank wird erstellt...");
                
                try {
                    // Hole den HistoryDatabaseManager über den ProviderHistoryService
                    boolean success = historyService.createBackup();
                    
                    if (success) {
                        publish("Backup erfolgreich erstellt!");
                    } else {
                        publish("Fehler beim Erstellen des Backups!");
                    }
                } catch (Exception e) {
                    publish("FEHLER: " + e.getMessage());
                    e.printStackTrace();
                }
                
                return null;
            }
            
            @Override
            protected void process(List<String> chunks) {
                for (String message : chunks) {
                    logArea.append(message + "\n");
                    // Zum Ende scrollen
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                }
            }
            
            @Override
            protected void done() {
                // UI-Status zurücksetzen
                backupButton.setEnabled(true);
                saveButton.setEnabled(true);
                closeButton.setEnabled(true);
                progressBar.setIndeterminate(false);
                progressBar.setValue(100);
                
                JOptionPane.showMessageDialog(ForceDbSaveDialog.this,
                        "Backup abgeschlossen.\nBitte überprüfen Sie das Log für Details.",
                        "Backup abgeschlossen",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        };
        
        worker.execute();
    }
    
    private void startSaving() {
        // UI-Status aktualisieren
        saveButton.setEnabled(false);
        backupButton.setEnabled(false);
        closeButton.setEnabled(false);
        logArea.setText("");
        progressBar.setValue(0);
        
        // Direkt einen initialen Log-Eintrag anzeigen, um zu zeigen dass der Prozess startet
        logArea.append("Starte den Speicherungsprozess...\n");
        logArea.append("Gefundene Provider: " + providers.size() + "\n");
        logArea.append("Ein Backup wird vor der Speicherung erstellt...\n");
        
        // SwingWorker für Hintergrundverarbeitung
        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    int total = providers.size();
                    int count = 0;
                    
                    // Automatisch ein Backup erstellen
                    try {
                        boolean backupSuccess = historyService.createBackup();
                        if (backupSuccess) {
                            publish("Backup erfolgreich erstellt.");
                        } else {
                            publish("WARNUNG: Backup konnte nicht erstellt werden!");
                        }
                    } catch (Exception e) {
                        publish("FEHLER beim Erstellen des Backups: " + e.getMessage());
                    }
                    
                    // Erstelle eine Instanz von HtmlDatabase mit dem korrekten rootPath
                    HtmlDatabase htmlDb = new HtmlDatabase(rootPath);
                    
                    // Erzwinge die Speicherung für jeden Provider
                    for (Map.Entry<String, ProviderStats> entry : providers.entrySet()) {
                        final String providerName = entry.getKey();
                        
                        // Updates für die UI im EDT ausführen
                        SwingUtilities.invokeLater(() -> {
                            logArea.append("Verarbeite Provider: " + providerName + "\n");
                            logArea.setCaretPosition(logArea.getDocument().getLength());
                        });
                        
                        // Validiere den Provider-Namen
                        if (providerName == null || providerName.trim().isEmpty()) {
                            publish("Ungültiger Provider-Name, wird übersprungen.");
                            continue;
                        }
                        
                        try {
                            // Berechne 3MPDD-Wert
                            double threeMonthProfit = htmlDb.getAverageMonthlyProfit(providerName, 3);
                            double equityDrawdown = htmlDb.getEquityDrawdown(providerName);
                            double mpdd3 = threeMonthProfit / (equityDrawdown > 0 ? equityDrawdown : 1.0);
                            
                            // In DB speichern mit force=true
                            boolean success = historyService.store3MpddValue(providerName, mpdd3, true);
                            
                            // Status loggen
                            if (success) {
                                publish(String.format("Provider '%s': 3MPDD-Wert %.4f gespeichert", 
                                        providerName, mpdd3));
                            } else {
                                publish(String.format("Provider '%s': Fehler bei der Speicherung", 
                                        providerName));
                            }
                        } catch (Exception e) {
                            publish("FEHLER bei " + providerName + ": " + e.getMessage());
                        }
                        
                        // Fortschritt aktualisieren
                        count++;
                        final int progressValue = (int)((count / (double)total) * 100);
                        
                        // Updates für die UI im EDT ausführen
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setValue(progressValue);
                        });
                        
                        // Kurze Pause für bessere UI-Reaktionsfähigkeit
                        Thread.sleep(50);
                    }
                    
                    publish("\nSpeicherung abgeschlossen.");
                    publish(String.format("%d von %d Provider erfolgreich gespeichert.", count, total));
                    
                    return null;
                } catch (Exception e) {
                    publish("\nFEHLER: " + e.getMessage());
                    e.printStackTrace();
                    return null;
                }
            }
            
            @Override
            protected void process(List<String> chunks) {
                for (String message : chunks) {
                    logArea.append(message + "\n");
                    // Zum Ende scrollen
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                }
            }
            
            @Override
            protected void done() {
                // UI-Status zurücksetzen
                saveButton.setEnabled(true);
                backupButton.setEnabled(true);
                closeButton.setEnabled(true);
                progressBar.setValue(100);
                
                JOptionPane.showMessageDialog(ForceDbSaveDialog.this,
                        "Speicherung abgeschlossen.\nBitte überprüfen Sie das Log für Details.",
                        "Speicherung abgeschlossen",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        };
        
        worker.execute();
    }
}