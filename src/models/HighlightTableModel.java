package models;

import javax.swing.table.DefaultTableModel;
import java.util.Map;
import data.ProviderStats;
import services.RiskAnalysisServ;
import utils.HtmlParser;

public class HighlightTableModel extends DefaultTableModel {
    
    private static final String[] COLUMN_NAMES = {
        "No.", "Signal Provider", "Balance", "3MonProfit", "3MPDD", "Trades", "Trade Days", 
        "Win Rate %", "Total Profit", "Avg Profit/Trade", "Max Drawdown %", 
        "Equity Drawdown %", "Profit Factor", "MaxTrades", "MaxLots", 
        "Max Duration (h)", "Risk Score", "S/L", "T/P", "Start Date", "End Date"
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
            case 5:  // Trades
            case 6:  // Trade Days
            case 13: // MaxTrades
            case 15: // Max Duration
            case 16: // Risk Score
            case 17: // S/L
            case 18: // T/P
                return Integer.class;
            case 2:  // Balance
            case 3:  // 3MonProfit
            case 4:  // 3MPDD
            case 7:  // Win Rate
            case 8:  // Total Profit
            case 9:  // Avg Profit/Trade
            case 10: // Max Drawdown
            case 11: // Equity Drawdown
            case 12: // Profit Factor
            case 14: // MaxLots
                return Double.class;
            default:
                return String.class;
        }
    }
    
    private double calculate3MPDD(double threeMonthProfit, double balance, double equityDrawdown) {
        if (balance <= 0 || equityDrawdown <= 0) {
            return 0.0;
        }
        
        // Berechnung: 3MonProfit / (Balance * (EquityDrawdown/100))
        return threeMonthProfit / (balance * (equityDrawdown/100));
    }
    
    public void populateData(Map<String, ProviderStats> statsMap) {
        setRowCount(0);
        int rowNum = 1;
        
        for (Map.Entry<String, ProviderStats> entry : statsMap.entrySet()) {
            String providerName = entry.getKey();
            ProviderStats stats = entry.getValue();
            int riskScore = RiskAnalysisServ.calculateRiskScore(stats);
            double equityDrawdown = htmlParser.getEquityDrawdown(providerName);
            double balance = htmlParser.getBalance(providerName);
            double threeMonthProfit = stats.getLastThreeMonthsProfit();
            double mpdd = calculate3MPDD(threeMonthProfit, balance, equityDrawdown);
            
            addRow(new Object[]{
                rowNum++,                         // No.
                providerName,                     // Signal Provider
                balance,                          // Balance
                threeMonthProfit,                 // 3MonProfit
                mpdd,                             // 3MPDD
                stats.getTrades().size(),         // Trades
                stats.getTradeDays(),             // Trade Days
                stats.getWinRate(),               // Win Rate %
                stats.getTotalProfit(),           // Total Profit
                stats.getAverageProfit(),         // Avg Profit/Trade
                stats.getMaxDrawdown(),           // Max Drawdown %
                equityDrawdown,                   // Equity Drawdown %
                stats.getProfitFactor(),          // Profit Factor
                stats.getMaxConcurrentTrades(),   // MaxTrades
                stats.getMaxConcurrentLots(),     // MaxLots
                stats.getMaxDuration(),           // Max Duration (h)
                riskScore,                        // Risk Score
                stats.hasStopLoss() ? 1 : 0,     // S/L
                stats.hasTakeProfit() ? 1 : 0,   // T/P
                stats.getStartDate(),             // Start Date
                stats.getEndDate()                // End Date
            });
        }
        fireTableDataChanged();
    }
}