package ui;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableModel;

import data.ProviderStats;

public class CompareEquityCurvesDialog extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(CompareEquityCurvesDialog.class.getName());
    private final Map<String, ProviderStats> providerStats;
    private final JTable providerTable;

    public CompareEquityCurvesDialog(JFrame parent, Map<String, ProviderStats> stats, String rootPath) {
        super("Compare Equity Curves");
        this.providerStats = stats;
        
        // Erstelle die Tabelle zuerst, da providerTable als final deklariert ist
        this.providerTable = createProviderTable();
        
        // Logging zur Fehlerdiagnose
        LOGGER.info("CompareEquityCurvesDialog wird geöffnet");
        LOGGER.info("Anzahl der Provider: " + (stats != null ? stats.size() : "null"));
        
        // Überprüfe, ob Daten verfügbar sind
        if (stats == null || stats.isEmpty()) {
            LOGGER.warning("Keine Provider-Daten zum Anzeigen verfügbar!");
            JOptionPane.showMessageDialog(
                parent,
                "Es sind keine Provider-Daten zum Anzeigen verfügbar.\n" +
                "Bitte stellen Sie sicher, dass Provider geladen wurden und keine zu strengen Filter gesetzt sind.",
                "Keine Daten verfügbar",
                JOptionPane.WARNING_MESSAGE
            );
            dispose(); // Dialog sofort schließen
            return;
        }
        
        // Zeige die vorhandenen Provider an (für Debug)
        if (stats.size() < 20) {
            for (String key : stats.keySet()) {
                LOGGER.info("Verfügbarer Provider: " + key);
            }
        } else {
            LOGGER.info("Es sind " + stats.size() + " Provider verfügbar");
        }
        
        // Tabelle wurde bereits im Konstruktor erstellt
        JScrollPane scrollPane = new JScrollPane(providerTable);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        setSize(1000, 800);
        setLocationRelativeTo(parent);
        
        // ESC zum Schließen
        KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        Action escapeAction = new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispose();
            }
        };
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "ESCAPE");
        getRootPane().getActionMap().put("ESCAPE", escapeAction);
    }

    private JTable createProviderTable() {
        // Erstelle Tabellen-Modell mit zwei Spalten
        DefaultTableModel model = new DefaultTableModel(
            new String[]{"Signal Provider", "Leer"}, 0);
        
        // Prüfe explizit, ob providerStats nicht null und nicht leer ist
        if (providerStats == null || providerStats.isEmpty()) {
            LOGGER.warning("Keine Provider zum Verarbeiten in createProviderTable vorhanden!");
            // Dummy-Zeile hinzufügen
            model.addRow(new Object[]{"KEINE PROVIDER GEFUNDEN", "KEINE DATEN VERFÜGBAR"});
            LOGGER.info("Dummy-Zeile hinzugefügt");
            return new JTable(model);
        }
        
        // Füge Zeilen für jeden Provider hinzu
        for (Map.Entry<String, ProviderStats> entry : providerStats.entrySet()) {
            String providerName = entry.getKey();
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
            LOGGER.warning("Nach Filtern sind keine Provider mehr übrig");
            model.addRow(new Object[]{"KEINE PROVIDER GEFUNDEN", "KEINE DATEN VERFÜGBAR"});
        }
        
        // Erstelle die Tabelle
        JTable table = new JTable(model) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Keine Zellen editierbar
            }
        };
        
        // Spaltenbreiten anpassen
        table.getColumnModel().getColumn(0).setPreferredWidth(300);
        table.getColumnModel().getColumn(1).setPreferredWidth(700);
        
        return table;
    }
}