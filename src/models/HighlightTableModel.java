package models;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import data.ProviderStats;

public class HighlightTableModel extends AbstractTableModel {
    private final String[] columnNames = {
        "No.", "Signal Provider", "Trades", "Win Rate %", "Total Profit",
        "Avg Profit/Trade", "Max Drawdown %", "Profit Factor",
        "Max Concurrent Trades", "Max Concurrent Lots", "Start Date", "End Date",
        "Users", "Invested Capital"
    };
    
    private final List<Object[]> data = new ArrayList<>();
    private final DateTimeFormatter dateFormatter;
    
    public HighlightTableModel(DateTimeFormatter formatter) {
        this.dateFormatter = formatter;
    }
    
    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }
    
    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
    
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:  // No
            case 2:  // Trades
            case 8:  // Max Concurrent Trades
            case 12: // Users
                return Integer.class;
            case 3:  // Win Rate
            case 4:  // Total Profit
            case 5:  // Avg Profit/Trade
            case 6:  // Max Drawdown
            case 7:  // Profit Factor
            case 9:  // Max Concurrent Lots
                return Double.class;
            default:
                return String.class;
        }
    }
    
    @Override
    public Object getValueAt(int row, int column) {
        return data.get(row)[column];
    }
    
    public void addRow(String providerName, ProviderStats stats) {
        Object[] row = {
            data.size() + 1,
            providerName,
            stats.getTradeCount(),
            stats.getWinRate(),
            stats.getTotalProfit(),
            stats.getAverageProfitPerTrade(),
            stats.getMaxDrawdownPercent(),
            stats.getProfitFactor(),
            stats.getMaxConcurrentTrades(),
            stats.getMaxConcurrentLots(),
            stats.getStartDate().format(dateFormatter),
            stats.getEndDate().format(dateFormatter),
            stats.getUserCount(),
            String.format("%.0f %s", stats.getInvestedCapital(), stats.getCurrency()) // Formatierung mit Währung
        };
        data.add(row);
        fireTableRowsInserted(data.size() - 1, data.size() - 1);
    }
    
    public void clearData() {
        int oldSize = data.size();
        data.clear();
        if (oldSize > 0) {
            fireTableRowsDeleted(0, oldSize - 1);
        }
    }
}