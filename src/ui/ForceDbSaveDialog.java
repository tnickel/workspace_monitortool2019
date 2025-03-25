package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
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
    private final String rootPath; // Hier wird rootPath als Instanzvariable definiert
    
    public ForceDbSaveDialog(JFrame parent, ProviderHistoryService historyService, 
            Map<String, ProviderStats> providers, String rootPath) { // rootPath als Parameter hinzugefügt
        super(parent, "Datenbank-Speicherung erzwingen", true);
        this.historyService = historyService;
        this.providers = providers;
        this.rootPath = rootPath; // rootPath speichern
        
        // UI-Komponenten
        logArea = new JTextArea();
        logArea.setEditable(false);
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        
        initUI();
        setSize(600, 400);
        setLocationRelativeTo(parent);
    }
    
    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        
        // Log-Bereich
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Log"));
        scrollPane.setPreferredSize(new Dimension(580, 300));
        
        // Progress-Panel
        JPanel progressPanel = new JPanel(new BorderLayout(5, 5));
        progressPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        progressPanel.add(progressBar, BorderLayout.CENTER);
        
        // Button-Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        saveButton = new JButton("Speicherung starten");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startSaving();
            }
        });
        
        closeButton = new JButton("Schließen");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        
        buttonPanel.add(saveButton);
        buttonPanel.add(closeButton);
        
        // Layout zusammensetzen
        add(scrollPane, BorderLayout.CENTER);
        add(progressPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void startSaving() {
        // UI-Status aktualisieren
        saveButton.setEnabled(false);
        closeButton.setEnabled(false);
        logArea.setText("");
        progressBar.setValue(0);
        
        // SwingWorker für Hintergrundverarbeitung
        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    int total = providers.size();
                    int count = 0;
                    
                    publish("Start der Datenbank-Speicherung...");
                    publish(String.format("Gefundene Provider: %d", total));
                    
                    // Erstelle eine Instanz von HtmlDatabase mit dem korrekten rootPath
                    HtmlDatabase htmlDb = new HtmlDatabase(rootPath);
                    
                    // Erzwinge die Speicherung für jeden Provider
                    for (Map.Entry<String, ProviderStats> entry : providers.entrySet()) {
                        String providerName = entry.getKey();
                        
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
                        
                        // Fortschritt aktualisieren
                        count++;
                        int progressValue = (int)((count / (double)total) * 100);
                        progressBar.setValue(progressValue);
                        
                        // Kurze Pause für bessere UI-Reaktionsfähigkeit
                        Thread.sleep(10);
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