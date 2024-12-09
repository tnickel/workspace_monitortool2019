package models;

import data.ProviderStats;
import utils.TradeUtils;
import data.Trade;
import data.TradeComparator;
import java.time.format.DateTimeFormatter;
import javax.swing.table.AbstractTableModel;
import java.util.*;

public class HighlightTableModel extends AbstractTableModel {
    private final List<Object[]> data = new ArrayList<>();
    private final String[] columnNames = {
        "No.", "Signal Provider", "Trades", "Win Rate %", "Total Profit", 
        "Avg Profit/Trade", "Max Drawdown %", "Profit Factor", 
        "Max Concurrent Trades", "Max Concurrent Lots", "Start Date", "End Date"
    };
    
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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
    public Object getValueAt(int row, int col) {
        return data.get(row)[col];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (data.size() > 0) {
            return getValueAt(0, columnIndex).getClass();
        }
        return Object.class;
    }

    public void addRow(String providerName, ProviderStats stats) {
        List<Trade> trades = new ArrayList<>(stats.getTrades());
        trades.sort(new TradeComparator());

        Object[] row = new Object[] {
            data.size() + 1,
            providerName,
            trades.size(),
            stats.getWinRate(),
            stats.getTotalProfit(),
            stats.getAverageProfitPerTrade(),
            stats.getMaxDrawdownPercent(),
            stats.getProfitFactor(),
            stats.getMaxConcurrentTrades(),
            stats.getMaxConcurrentLots(),
            stats.getStartDate().format(dateFormatter),
            stats.getEndDate().format(dateFormatter)
        };
        data.add(row);
    }

    public void populateData(Map<String, ProviderStats> stats) {
        data.clear();
        // Batching f�r bessere Performance
        List<Object[]> newData = new ArrayList<>(stats.size());
        for (Map.Entry<String, ProviderStats> entry : stats.entrySet()) {
            ProviderStats providerStats = entry.getValue();
            List<Trade> trades = new ArrayList<>(providerStats.getTrades());
            
            Object[] row = new Object[] {
                newData.size() + 1,
                entry.getKey(),
                trades.size(),
                providerStats.getWinRate(),
                providerStats.getTotalProfit(),
                providerStats.getAverageProfitPerTrade(),
                providerStats.getMaxDrawdownPercent(),
                providerStats.getProfitFactor(),
                providerStats.getMaxConcurrentTrades(),
                providerStats.getMaxConcurrentLots(),
                providerStats.getStartDate().format(dateFormatter),
                providerStats.getEndDate().format(dateFormatter)
            };
            newData.add(row);
        }
        data.addAll(newData);
        fireTableDataChanged();
    }

    public void clear() {
        data.clear();
        fireTableDataChanged();
    }
}