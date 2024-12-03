package models;

import javax.swing.table.DefaultTableModel;
import java.util.Map;
import data.ProviderStats;

public class HighlightTableModel extends DefaultTableModel {
    private static final String[] COLUMN_NAMES = {
        "No.", "Signal Provider", "Trades", "Win Rate %", "Total Profit", 
        "Avg Profit/Trade", "Max Drawdown %", "Profit Factor", 
        "Max Concurrent Trades", "Start Date", "End Date"
    };
    
    public HighlightTableModel() {
        super(COLUMN_NAMES, 0);
    }
    
    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
    
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0) return Integer.class;
        if (columnIndex == 2) return Integer.class;
        if (columnIndex == 8) return Integer.class;  // Max Concurrent Trades
        return String.class;
    }
    
    public void populateData(Map<String, ProviderStats> stats) {
        setRowCount(0);
        int rowNum = 1;
        
        for (Map.Entry<String, ProviderStats> entry : stats.entrySet()) {
            ProviderStats stat = entry.getValue();
            addRow(new Object[]{
                rowNum++,
                entry.getKey(),
                stat.getTradeCount(),
                String.format("%.2f", stat.getWinRate()),
                String.format("%.2f", stat.getTotalProfit()),
                String.format("%.2f", stat.getAverageProfit()),
                String.format("%.2f", stat.getMaxDrawdown()),
                String.format("%.2f", stat.getProfitFactor()),
                stat.getMaxConcurrentTrades(),  // Neue Spalte
                stat.getStartDate(),
                stat.getEndDate()
            });
        }
        
        fireTableDataChanged();
    }
    
    @Override
    public void setValueAt(Object value, int row, int column) {
        super.setValueAt(value, row, column);
        fireTableCellUpdated(row, column);
    }
}