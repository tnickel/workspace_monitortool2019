package ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import db.HistoryDatabaseManager.HistoryEntry;
import services.ProviderHistoryService;
import ui.UIConstants;
import ui.components.UIComponentFactory;

/**
 * Dialog zur Anzeige der Datenbank-Informationen für einen Signal Provider
 */
public class DatabaseInfoDialog extends JDialog {
    private final String providerName;
    private final ProviderHistoryService historyService;
    
    /**
     * Konstruktor für den DatabaseInfoDialog
     * 
     * @param parent Das übergeordnete Fenster
     * @param providerName Der Name des Signal Providers
     */
    public DatabaseInfoDialog(Window parent, String providerName) {
        super(parent, "Datenbank-Informationen für " + providerName, ModalityType.APPLICATION_MODAL);
        this.providerName = providerName;
        this.historyService = ProviderHistoryService.getInstance();
        
        initializeUI();
        
        // Dialog-Eigenschaften setzen
        pack();
        setLocationRelativeTo(parent);
    }
    
    /**
     * Initialisiert die UI-Komponenten
     */
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(UIConstants.BG_COLOR);
        
        // Hole die Daten aus der Datenbank
        List<HistoryEntry> mpddHistory = historyService.get3MpddHistory(providerName);
        
        // Erstelle ein Modell für die Tabelle mit den Datenbankinformationen
        DefaultTableModel model = createTableModel(mpddHistory);
        
        // Je nachdem, ob Daten vorhanden sind, füge die Tabelle oder eine Meldung hinzu
        if (mpddHistory.isEmpty()) {
            JLabel noDataLabel = createNoDataLabel();
            add(noDataLabel, BorderLayout.CENTER);
        } else {
            JTable table = createDbInfoTable(model);
            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setPreferredSize(UIConstants.DB_DIALOG_SIZE);
            scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            add(scrollPane, BorderLayout.CENTER);
        }
        
        // Schließen-Button hinzufügen
        JButton closeButton = UIComponentFactory.createStyledButton("Schließen");
        closeButton.addActionListener(e -> dispose());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Erstellt das Tabellenmodell mit den Datenbankeinträgen
     */
    private DefaultTableModel createTableModel(List<HistoryEntry> mpddHistory) {
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Datum");
        model.addColumn("Statistiktyp");
        model.addColumn("Wert");
        
        // Füge die MPDD-Werte hinzu
        for (HistoryEntry entry : mpddHistory) {
            model.addRow(new Object[] {
                entry.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "3MPDD",
                String.format("%.4f", entry.getValue())
            });
        }
        
        return model;
    }
    
    /**
     * Erstellt ein Label für die "Keine Daten" Meldung
     */
    private JLabel createNoDataLabel() {
        JLabel noDataLabel = new JLabel("Keine Datenbank-Einträge für diesen Provider vorhanden.");
        noDataLabel.setHorizontalAlignment(JLabel.CENTER);
        noDataLabel.setForeground(UIConstants.TEXT_COLOR);
        noDataLabel.setFont(UIConstants.ITALIC_FONT);
        return noDataLabel;
    }
    
    /**
     * Erstellt die Tabelle mit angepasstem Renderer
     */
    private JTable createDbInfoTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.setRowHeight(25);
        table.setBackground(Color.WHITE);
        table.setForeground(UIConstants.TEXT_COLOR);
        table.setGridColor(new Color(230, 230, 230));
        table.getTableHeader().setBackground(UIConstants.SECONDARY_COLOR);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(UIConstants.BOLD_FONT);
        
        // Benutzerdefinierten Renderer hinzufügen
        table.setDefaultRenderer(Object.class, createCustomRenderer());
        
        return table;
    }
    
    /**
     * Erstellt einen benutzerdefinierten Renderer für die Tabellenzellen
     */
    private DefaultTableCellRenderer createCustomRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                         boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (column == 2 && value != null) {
                    try {
                        double val = Double.parseDouble(value.toString().replace(",", "."));
                        if (val > 0) {
                            c.setForeground(UIConstants.POSITIVE_COLOR);
                        } else {
                            c.setForeground(UIConstants.NEGATIVE_COLOR);
                        }
                    } catch (Exception e) {
                        // Ignorieren, falls kein gültiger Zahlenwert
                    }
                }
                return c;
            }
        };
    }
}