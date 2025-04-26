package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Window;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import data.ProviderStats;
import utils.HtmlDatabase;

public class EquityDrawdownDialog extends JDialog {
    private static final Logger LOGGER = Logger.getLogger(EquityDrawdownDialog.class.getName());
    private final Map<String, ProviderStats> providers;
    private final HtmlDatabase htmlDatabase;
    private final String rootPath;
    private final JTable providerTable;
    private final JLabel statusLabel;

    /**
     * Konstruktor für den EquityDrawdownDialog
     */
    public EquityDrawdownDialog(Window owner, Map<String, ProviderStats> providers, 
            HtmlDatabase htmlDatabase, String rootPath) {
        super(owner, "Equity Drawdown Comparison", Dialog.ModalityType.MODELESS);
        
        LOGGER.info("EquityDrawdownDialog wird erstellt");
        
        // Defensive Kopie der Map erstellen, um null zu vermeiden
        if (providers == null) {
            LOGGER.severe("Providers Map ist null, erstelle leere Map");
            this.providers = new HashMap<>();
        } else {
            this.providers = new HashMap<>(providers);
        }
        
        this.htmlDatabase = htmlDatabase;
        this.rootPath = rootPath;

        // Debug-Informationen loggen
        LOGGER.info("Anzahl der Provider: " + this.providers.size());
        if (this.providers.size() > 0) {
            int count = 0;
            for (String key : this.providers.keySet()) {
                LOGGER.info("Provider " + count + ": " + key);
                count++;
                if (count >= 5) {
                    LOGGER.info("... und " + (this.providers.size() - 5) + " weitere Provider");
                    break;
                }
            }
        }
        LOGGER.info("htmlDatabase ist " + (htmlDatabase != null ? "vorhanden" : "null"));
        LOGGER.info("rootPath: " + rootPath);

        // Status Label erstellen
        statusLabel = new JLabel(" ");
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        statusPanel.setBackground(new Color(238, 238, 238));

        // Tabelle erstellen - Differenzierung nach Provider-Verfügbarkeit
        if (this.providers.isEmpty()) {
            LOGGER.warning("Keine Provider-Daten zum Anzeigen verfügbar!");
            providerTable = createEmptyTable();
            statusLabel.setText("Keine Provider gefunden. Bitte laden Sie Provider oder überprüfen Sie Ihre Filter.");
        } else {
            providerTable = createProviderTable();
            statusLabel.setText(this.providers.size() + " Provider gefunden.");
        }
        
        // Layout zusammenbauen
        JScrollPane scrollPane = new JScrollPane(providerTable);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        setSize(1560, 800);
        setLocationRelativeTo(owner);

        // ESC-Taste zum Schließen des Dialogs
        javax.swing.KeyStroke escapeKeyStroke = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0, false);
        javax.swing.Action escapeAction = new javax.swing.AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispose();
            }
        };
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "ESCAPE");
        getRootPane().getActionMap().put("ESCAPE", escapeAction);
        
        // DEBUG: Noch einmal Größe und Sichtbarkeit überprüfen
        LOGGER.info("Dialog wurde erstellt mit Größe " + getSize());
        LOGGER.info("Dialog wird jetzt angezeigt");
    }
    
    /**
     * Erstellt die Tabelle mit Provider-Daten
     */
    private JTable createProviderTable() {
        LOGGER.info("Erstelle Provider-Tabelle mit " + providers.size() + " Providern");
        
        // Erstelle ein Model mit zwei Spalten
        DefaultTableModel model = new DefaultTableModel(
            new String[]{"Signal Provider", "Drawdown Chart"}, 0);
        
        // Füge für jeden Provider eine Zeile hinzu
        for (Map.Entry<String, ProviderStats> entry : providers.entrySet()) {
            String providerName = entry.getKey();
            
            // NULL-Check für den Namen
            if (providerName == null) {
                LOGGER.warning("Provider-Name ist null, wird übersprungen");
                continue;
            }
            
            ProviderStats stats = entry.getValue();
            
            // Überspringe Provider mit null-Stats
            if (stats == null) {
                LOGGER.warning("Provider " + providerName + " hat null ProviderStats, wird übersprungen");
                continue;
            }
            
            LOGGER.info("Füge Provider hinzu: " + providerName);
            model.addRow(new Object[]{providerName, ""});
        }
        
        // Prüfen ob nach dem Filtern noch Provider übrig sind
        if (model.getRowCount() == 0) {
            LOGGER.warning("Nach dem Hinzufügen sind keine Provider in der Tabelle");
            model.addRow(new Object[]{"KEINE PROVIDER GEFUNDEN", "KEINE CHART-DATEN VERFÜGBAR"});
        } else {
            LOGGER.info("Tabelle enthält " + model.getRowCount() + " Zeilen");
        }
        
        return createTableWithModel(model);
    }
    
    /**
     * Erstellt eine leere Tabelle mit Hinweismeldung
     */
    private JTable createEmptyTable() {
        LOGGER.info("Erstelle leere Tabelle mit Hinweismeldung");
        DefaultTableModel model = new DefaultTableModel(
            new String[]{"Signal Provider", "Drawdown Chart"}, 0);
        model.addRow(new Object[]{"KEINE PROVIDER GEFUNDEN", "KEINE CHART-DATEN VERFÜGBAR"});
        return createTableWithModel(model);
    }
    
    /**
     * Erstellt eine JTable mit dem übergebenen TableModel
     */
    private JTable createTableWithModel(DefaultTableModel model) {
        // Tabelle erstellen
        JTable table = new JTable(model) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Keine Zellen editierbar
            }
        };
        
        // Spaltenbreiten anpassen
        table.getColumnModel().getColumn(0).setPreferredWidth(400);
        table.getColumnModel().getColumn(1).setPreferredWidth(1100);
        
        // Zeilenhöhe anpassen
        table.setRowHeight(30);
        
        // Tabellengröße anpassen
        table.setPreferredScrollableViewportSize(new Dimension(1500, 700));
        
        return table;
    }
}