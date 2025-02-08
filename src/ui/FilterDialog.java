package ui;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import models.FilterCriteria;
import models.FilterCriteria.FilterRange;

public class FilterDialog extends JDialog {
    private final JTable filterTable;
    private FilterCriteria currentFilters;
    
    private static final String[] COLUMN_NAMES = {
        "Column Name", "Min Value", "Max Value"
    };
    
    private static final String[] TABLE_COLUMNS = {
        "No.", "Signal Provider", "Balance", "3MPDD", "3MProfProz", 
        "Trades", "Trade Days", "Win Rate %", "Total Profit", 
        "Avg Profit/Trade", "Max Drawdown %", "Equity Drawdown %", 
        "Profit Factor", "MaxTrades", "MaxLots", "Max Duration (h)", 
        "Risk Score", "S/L", "T/P", "Start Date", "End Date"
    };

    public FilterDialog(JFrame parent, FilterCriteria filters) {
        super(parent, "Filter Settings", true);
        this.currentFilters = filters;
        
        DefaultTableModel model = new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column > 0; 
            }
        };
        
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
        filterTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        filterTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        filterTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        
        setLayout(new BorderLayout(5, 5));
        
        JScrollPane scrollPane = new JScrollPane(filterTable);
        scrollPane.setPreferredSize(new Dimension(400, 500));
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("Apply");
        JButton cancelButton = new JButton("Cancel");
        JButton resetButton = new JButton("Reset");
        
        okButton.addActionListener(e -> {
            if (validateAndSaveFilters()) {
                dispose();
            }
        });
        
        cancelButton.addActionListener(e -> dispose());
        
        resetButton.addActionListener(e -> {
            for (int row = 0; row < filterTable.getRowCount(); row++) {
                filterTable.setValueAt("", row, 1);
                filterTable.setValueAt("", row, 2);
            }
        });
        
        buttonPanel.add(resetButton);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        add(new JLabel("Set min/max values for filtering:"), BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(parent);
    }
    
    private boolean validateAndSaveFilters() {
        FilterCriteria criteria = new FilterCriteria();
        boolean hasAnyFilter = false;
        
        for (int row = 0; row < filterTable.getRowCount(); row++) {
            String minStr = (String) filterTable.getValueAt(row, 1);
            String maxStr = (String) filterTable.getValueAt(row, 2);
            
            if (minStr.isEmpty() && maxStr.isEmpty()) {
                continue;
            }
            
            if (row == 1 || row == 19 || row == 20) {
                if (!minStr.isEmpty()) {
                    criteria.addFilter(row, new FilterRange(minStr));
                    hasAnyFilter = true;
                }
                continue;
            }
            
            try {
                Double min = minStr.isEmpty() ? null : Double.parseDouble(minStr);
                Double max = maxStr.isEmpty() ? null : Double.parseDouble(maxStr);
                
                if (min != null || max != null) {
                    criteria.addFilter(row, new FilterRange(min, max));
                    hasAnyFilter = true;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                    "Invalid number format in row " + (row + 1),
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        
        if (!hasAnyFilter) {
            JOptionPane.showMessageDialog(this,
                "Please set at least one filter criteria",
                "Validation Error",
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
