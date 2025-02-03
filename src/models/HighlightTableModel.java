package models;

import javax.swing.table.DefaultTableModel;
import java.util.Map;
import data.ProviderStats;
import services.RiskAnalysisServ;
import utils.HtmlParser;

public class HighlightTableModel extends DefaultTableModel {
    
    private static final String[] COLUMN_NAMES = {
        "No.", "Signal Provider", "Trades", "Trade Days", "Win Rate %", "Total Profit",
        "Avg Profit/Trade", "Max Drawdown %", "Equity Drawdown %", "Profit Factor", 
        "MaxTrades", "MaxLots", "Max Duration (h)", "Risk Score",
        "S/L", "T/P", "Start Date", "End Date"
    };
    
    private final HtmlParser htmlParser;
    
    public HighlightTableModel(String rootPath) {
        super(COLUMN_NAMES, 0);
        this.htmlParser = new HtmlParser(rootPath);
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
            case 3:  // Trade Days
            case 10: // MaxTrades
            case 12: // Max Duration
            case 13: // Risk Score
            case 14: // S/L
            case 15: // T/P
                return Integer.class;
            case 4:  // Win Rate
            case 5:  // Total Profit
            case 6:  // Avg Profit/Trade
            case 7:  // Max Drawdown
            case 8:  // Equity Drawdown
            case 9:  // Profit Factor
            case 11: // MaxLots
                return Double.class;
            default:
                return String.class;
        }
    }
    
    public void populateData(Map<String, ProviderStats> statsMap) {
        setRowCount(0);
        int rowNum = 1;
        
        for (Map.Entry<String, ProviderStats> entry : statsMap.entrySet()) {
            String providerName = entry.getKey();
            ProviderStats stats = entry.getValue();
            int riskScore = RiskAnalysisServ.calculateRiskScore(stats);
            double equityDrawdown = htmlParser.getEquityDrawdown(providerName);
            
            addRow(new Object[]{
                rowNum++,
                providerName,
                stats.getTrades().size(),
                stats.getTradeDays(),
                stats.getWinRate(),
                stats.getTotalProfit(),
                stats.getAverageProfit(),
                stats.getMaxDrawdown(),
                equityDrawdown,
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