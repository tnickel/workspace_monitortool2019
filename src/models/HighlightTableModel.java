package models;

import javax.swing.table.DefaultTableModel;
import java.util.Map;
import data.ProviderStats;
import services.RiskAnalysisServ;

public class HighlightTableModel extends DefaultTableModel {
    
    private static final String[] COLUMN_NAMES = {
        "No.", "Signal Provider", "Trades", "Win Rate %", "Total Profit",
        "Avg Profit/Trade", "Max Drawdown %", "Profit Factor", 
        "MaxTrades", "MaxLots", "Max Duration (h)", "Risk Score",
        "S/L", "T/P", "Start Date", "End Date"
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
        switch (columnIndex) {
            case 0:  // No
            case 2:  // Trades
            case 8:  // MaxTrades
            case 10: // Max Duration
                return Integer.class;
            case 3:  // Win Rate
            case 4:  // Total Profit
            case 5:  // Avg Profit/Trade
            case 6:  // Max Drawdown
            case 7:  // Profit Factor
            case 9:  // MaxLots
                return Double.class;
            case 11: // Risk Score
            case 12: // S/L
            case 13: // T/P
                return Integer.class;
            default:
                return String.class;
        }
    }
    
    public void populateData(Map<String, ProviderStats> statsMap) {
        setRowCount(0);
        int rowNum = 1;
        
        for (Map.Entry<String, ProviderStats> entry : statsMap.entrySet()) {
            ProviderStats stats = entry.getValue();
            int riskScore = RiskAnalysisServ.calculateRiskScore(stats);
            
            addRow(new Object[]{
                rowNum++,
                entry.getKey(),
                stats.getTrades().size(),
                stats.getWinRate(),
                stats.getTotalProfit(),
                stats.getAverageProfit(),
                stats.getMaxDrawdown(),
                stats.getProfitFactor(),
                stats.getMaxConcurrentTrades(),
                stats.getMaxConcurrentLots(),
                stats.getMaxDuration(),
                riskScore,
                stats.hasStopLoss() ? 1 : 0,    
                stats.hasTakeProfit() ? 1 : 0,  
                stats.getStartDate(),
                stats.getEndDate()
            });
        }
        fireTableDataChanged();
    }
}