package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import models.FilterCriteria;
import models.FilterCriteria.FilterRange;

public class FilterDialog extends JDialog {
    private final JTable filterTable;
    private JTextField currencyPairsField; // Neu: Textfeld für Währungspaare
    private FilterCriteria currentFilters;
    
    private static final String[] COLUMN_NAMES = {
        "Column Name", "Min Value", "Max Value"
    };
    
    private static final String[] TABLE_COLUMNS = {
    	    "No.", "Signal Provider", "Balance", "3MPDD", "6MPDD", "9MPDD", "12MPDD", 
    	    "3MProfProz", "Trades", "Trade Days", "Days", "Win Rate %", "Total Profit", 
    	    "Avg Profit/Trade", "Max Drawdown %", "Equity Drawdown %", 
    	    "Profit Factor", "MaxTrades", "MaxLots", "Max Duration (h)", 
    	    "Risk Score", "S/L", "T/P", "Start Date", "End Date", "Stabilitaet", "Steigung", "MaxDDGraphic"
    	};

    public FilterDialog(JFrame parent, FilterCriteria filters) {
        super(parent, "Filter Settings", true);
        this.currentFilters = filters;
        
        DefaultTableModel model = new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column > 0; 
            }
            
            @Override
            public void setValueAt(Object value, int row, int col) {
                // Validiere Eingabe sofort
                if (col > 0) {  // Nur für Min und Max Spalten
                    String strValue = value.toString().trim();
                    
                    // Für Text-Spalten (Signal Provider, Start Date, End Date)
                    if (row == 1 || row == 23 || row == 24) {
                        super.setValueAt(strValue, row, col);
                        return;
                    }
                    
                    // Für numerische Spalten
                    if (!strValue.isEmpty()) {
                        try {
                            Double.parseDouble(strValue);
                            super.setValueAt(strValue, row, col);
                        } catch (NumberFormatException e) {
                            JOptionPane.showMessageDialog(null,
                                "Bitte geben Sie eine gültige Zahl ein.",
                                "Ungültige Eingabe",
                                JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    } else {
                        super.setValueAt(strValue, row, col);
                    }
                }
            }
        };
        
        // Model mit existierenden Filtern befüllen
        for (int i = 0; i < TABLE_COLUMNS.length; i++) {
            Object minValue = "";
            Object maxValue = "";
            if (filters.getFilters().containsKey(i)) {
                FilterRange range = filters.getFilters().get(i);
                minValue = range.getMin() != null ? range.getMin().toString() : "";
                maxValue = range.getMax() != null ? range.getMax().toString() : "";
            }
            model.addRow(new Object[]{TABLE_COLUMNS[i], minValue, maxValue});
        }
        
        filterTable = new JTable(model);
        
        // Verbesserte Zellenbearbeitung
        filterTable.setDefaultEditor(Object.class, new DefaultCellEditor(new JTextField()) {
            {
                getComponent().addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusLost(FocusEvent e) {
                        stopCellEditing();
                    }
                });
            }
            
            @Override
            public boolean stopCellEditing() {
                try {
                    JTextField textField = (JTextField) getComponent();
                    String value = textField.getText().trim();
                    if (!value.isEmpty()) {
                        int row = filterTable.getEditingRow();
                        if (!(row == 1 || row == 23 || row == 24)) {  // Nicht für Text-Spalten
                            Double.parseDouble(value);
                        }
                    }
                    return super.stopCellEditing();
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        });
        
        // Verbesserte Tabelleneinstellungen
        filterTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        filterTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        filterTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        filterTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        
        setLayout(new BorderLayout(5, 5));
        
        // Panel für Checkboxen und Währungspaar-Filter
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        
        JScrollPane scrollPane = new JScrollPane(filterTable);
        scrollPane.setPreferredSize(new Dimension(400, 400));
        
        // Panel für Währungspaar-Filter
        JPanel currencyPanel = new JPanel(new BorderLayout(5, 0));
        currencyPanel.setBorder(BorderFactory.createTitledBorder("Währungspaare Filter"));
        
        JLabel currencyLabel = new JLabel("Währungspaare (durch Komma getrennt):");
        currencyPairsField = new JTextField(30);
        currencyPairsField.setText(filters.getCurrencyPairsFilter());
        
        JPanel currencyFieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        currencyFieldPanel.add(currencyLabel);
        currencyFieldPanel.add(currencyPairsField);
        
        JLabel currencyHintLabel = new JLabel(
                "<html>Geben Sie Währungspaare ein, die alle vorhanden sein müssen (z.B. EURUSD, GBPUSD).<br>" +
                "Die Einträge werden als Präfixe behandelt, d.h. EURUSD findet auch EURUSD.ru, EURUSD.X, etc.</html>");
        currencyHintLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        
        currencyPanel.add(currencyFieldPanel, BorderLayout.NORTH);
        currencyPanel.add(currencyHintLabel, BorderLayout.CENTER);
        
        // Haupt-Panel zusammensetzen
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(currencyPanel, BorderLayout.SOUTH);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("Apply");
        JButton cancelButton = new JButton("Cancel");
        JButton resetButton = new JButton("Reset");
        
        okButton.addActionListener(e -> {
            stopEditing();
            if (validateAndSaveFilters()) {
                dispose();
            }
        });
        
        cancelButton.addActionListener(e -> dispose());
        
        resetButton.addActionListener(e -> {
            setupDefaultValues();
        });
        
        buttonPanel.add(resetButton);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        add(new JLabel("Set min/max values for filtering:"), BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                stopEditing();
            }
        });
        
        pack();
        setLocationRelativeTo(parent);
    }
    
    private void stopEditing() {
        if (filterTable.isEditing()) {
            filterTable.getCellEditor().stopCellEditing();
        }
    }
    
    // Neue Methode zum Setzen der Standardwerte
    private void setupDefaultValues() {
        // Standardwerte definieren - basierend auf dem Screenshot
        Map<String, Double> defaultMinValues = new HashMap<>();
        defaultMinValues.put("No.", 1.0);
        defaultMinValues.put("3MPDD", 2.0);
        defaultMinValues.put("Trades", 50.0);
        
        // Über alle Zeilen gehen und Standardwerte setzen wo vorhanden
        for (int row = 0; row < filterTable.getRowCount(); row++) {
            String columnName = (String) filterTable.getValueAt(row, 0);
            if (defaultMinValues.containsKey(columnName)) {
                filterTable.setValueAt(defaultMinValues.get(columnName).toString(), row, 1); // Min-Wert
            } else {
                filterTable.setValueAt("", row, 1); // Andere auf leer setzen
            }
            filterTable.setValueAt("", row, 2); // Max-Werte immer auf leer setzen
        }
        currencyPairsField.setText(""); // Währungspaar-Feld zurücksetzen
    }
    
    private boolean validateAndSaveFilters() {
        stopEditing();
        
        FilterCriteria criteria = new FilterCriteria();
        boolean hasAnyFilter = false;
        
        // Währungspaar-Filter einstellen
        String currencyFilter = currencyPairsField.getText().trim();
        criteria.setCurrencyPairsFilter(currencyFilter);
        
        if (!currencyFilter.isEmpty()) {
            hasAnyFilter = true;
        }
        
        for (int row = 0; row < filterTable.getRowCount(); row++) {
            String minStr = ((String) filterTable.getValueAt(row, 1)).trim();
            String maxStr = ((String) filterTable.getValueAt(row, 2)).trim();
            
            if (minStr.isEmpty() && maxStr.isEmpty()) {
                continue;
            }
            
            // Textfilter für Signal Provider, Start Date und End Date
            if (row == 1 || row == 23 || row == 24) {
                if (!minStr.isEmpty()) {
                    criteria.addFilter(row, new FilterRange(minStr));
                    hasAnyFilter = true;
                }
                continue;
            }
            
            try {
                Double min = minStr.isEmpty() ? null : Double.parseDouble(minStr);
                Double max = maxStr.isEmpty() ? null : Double.parseDouble(maxStr);
                
                // Prüfe, ob Min kleiner als Max ist
                if (min != null && max != null && min > max) {
                    JOptionPane.showMessageDialog(this,
                        "Minimum muss kleiner als Maximum sein in Zeile " + (row + 1),
                        "Validierungsfehler",
                        JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                
                if (min != null || max != null) {
                    criteria.addFilter(row, new FilterRange(min, max));
                    hasAnyFilter = true;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                    "Ungültiges Zahlenformat in Zeile " + (row + 1),
                    "Validierungsfehler",
                    JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        
        if (!hasAnyFilter) {
            JOptionPane.showMessageDialog(this,
                "Bitte mindestens ein Filterkriterium setzen",
                "Validierungsfehler",
                JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        this.currentFilters = criteria;
        criteria.saveFilters();
        return true;
    }
    
    public FilterCriteria showDialog() {
        setVisible(true);
        return currentFilters;
    }
}