package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableModel;

import db.HistoryDatabaseManager.HistoryEntry;
import services.ProviderHistoryService;

/**
 * Dialog zur Anzeige aller Provider-Historie-Einträge in der Datenbank
 */
public class DatabaseViewerDialog extends JDialog {
    private final ProviderHistoryService historyService;
    private JTable dataTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> statTypeComboBox;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public DatabaseViewerDialog(JFrame parent, ProviderHistoryService historyService) {
        super(parent, "Datenbank-Einträge", true);
        this.historyService = historyService;
        
        initUI();
        setSize(800, 600);
        setLocationRelativeTo(parent);
        
        // ESC-Taste zum Schließen des Dialogs
        KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        Action escapeAction = new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispose();
            }
        };
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "ESCAPE");
        getRootPane().getActionMap().put("ESCAPE", escapeAction);
        
        // Window Close Handler
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
    }
    
    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        
        // Table setup
        String[] columnNames = {"Provider", "Stat Type", "Datum", "Wert"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Keine Zellen editierbar
            }
        };
        dataTable = new JTable(tableModel);
        
        // Spaltenbreiten anpassen
        dataTable.getColumnModel().getColumn(0).setPreferredWidth(200); // Provider
        dataTable.getColumnModel().getColumn(1).setPreferredWidth(80);  // Stat Type
        dataTable.getColumnModel().getColumn(2).setPreferredWidth(150); // Datum
        dataTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // Wert
        
        JScrollPane scrollPane = new JScrollPane(dataTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Kontrollpanel für Filter und Refresh
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JLabel statTypeLabel = new JLabel("Statistik-Typ:");
        statTypeComboBox = new JComboBox<>(new String[] {
            "Alle Typen",
            ProviderHistoryService.STAT_TYPE_3MPDD
            // Weitere Stat-Typen hier hinzufügen, wenn verfügbar
        });
        
        JButton refreshButton = new JButton("Aktualisieren");
        refreshButton.addActionListener(e -> loadData());
        
        controlPanel.add(statTypeLabel);
        controlPanel.add(statTypeComboBox);
        controlPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        controlPanel.add(refreshButton);
        
        // Export-Button-Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = new JButton("Schließen");
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);
        
        // Layout zusammensetzen
        add(controlPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Daten initial laden
        loadData();
    }
    
    private void loadData() {
        // Tabelle leeren
        tableModel.setRowCount(0);
        
        try {
            String selectedStatType = (String) statTypeComboBox.getSelectedItem();
            boolean showAllTypes = "Alle Typen".equals(selectedStatType);
            
            // Alle Provider mit Einträgen abrufen
            Map<String, Map<String, List<HistoryEntry>>> allEntries = 
                historyService.getAllHistoryEntries();
            
            // Für jeden Provider
            for (Map.Entry<String, Map<String, List<HistoryEntry>>> providerEntry : allEntries.entrySet()) {
                String providerName = providerEntry.getKey();
                Map<String, List<HistoryEntry>> statEntries = providerEntry.getValue();
                
                // Für jeden Stat-Typ des Providers
                for (Map.Entry<String, List<HistoryEntry>> statEntry : statEntries.entrySet()) {
                    String statType = statEntry.getKey();
                    
                    // Filter nach Typ, falls gewählt
                    if (!showAllTypes && !statType.equals(selectedStatType)) {
                        continue;
                    }
                    
                    List<HistoryEntry> entries = statEntry.getValue();
                    
                    // Jeder Eintrag für diesen Provider und Stat-Typ
                    for (HistoryEntry entry : entries) {
                        tableModel.addRow(new Object[] {
                            providerName,
                            statType,
                            entry.getDate().format(DATE_FORMATTER),
                            String.format("%.4f", entry.getValue())
                        });
                    }
                }
            }
            
            // Info-Zeile am Ende
            tableModel.addRow(new Object[] {
                "Gesamt", "-", "-", tableModel.getRowCount() + " Einträge"
            });
            
        } catch (Exception e) {
            e.printStackTrace();
            // Fehlerbehandlung: Zeige Fehlermeldung in der Tabelle
            tableModel.addRow(new Object[] {
                "Fehler beim Laden der Daten", "", "", e.getMessage()
            });
        }
    }
}