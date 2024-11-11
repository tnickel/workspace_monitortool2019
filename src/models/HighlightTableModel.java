package models;

import data.ProviderStats;
import javax.swing.table.DefaultTableModel;
import java.time.LocalDate;
import java.text.DecimalFormat;
import java.util.Map;

public class HighlightTableModel extends DefaultTableModel {
    private static final String[] COLUMN_NAMES = {
        "No.", "Signal Provider", "Trades", "Win Rate %", "Total Profit", 
        "Avg Profit/Trade", "Max Drawdown %", "Profit Factor", "Start Date", "End Date"
    };
    
    private final DecimalFormat df = new DecimalFormat("#,##0.00");
    
    public HighlightTableModel() {
        super(COLUMN_NAMES, 0);
    }
    
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:
            case 2:
                return Integer.class;
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
                return Double.class;
            case 8:
            case 9:
                return LocalDate.class;
            default:
                return String.class;
        }
    }
    
    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
    
    @Override
    public Object getValueAt(int row, int column) {
        Object value = super.getValueAt(row, column);
        if (value instanceof Double) {
            // Formatiere nur für die Anzeige, behalte den Original-Wert
            return ((Double) value).doubleValue();
        }
        return value;
    }
    
    public void populateData(Map<String, ProviderStats> signalProviderStats) {
        setRowCount(0);  // Clear existing data
        int index = 1;
        
        for (Map.Entry<String, ProviderStats> entry : signalProviderStats.entrySet()) {
            ProviderStats stats = entry.getValue();
            addRow(new Object[]{
                index++,
                entry.getKey(),
                stats.getTradeCount(),
                roundToTwoDecimals(stats.getWinRate()),
                roundToTwoDecimals(stats.getTotalProfit()),
                roundToTwoDecimals(stats.getAverageProfit()),
                roundToTwoDecimals(stats.getMaxDrawdown()),
                roundToTwoDecimals(stats.getProfitFactor()),
                stats.getStartDate(),
                stats.getEndDate()
            });
        }
    }
    
    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}