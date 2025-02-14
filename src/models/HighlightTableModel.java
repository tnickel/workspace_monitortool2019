package models;

import javax.swing.table.DefaultTableModel;
import java.util.Map;
import data.ProviderStats;
import services.RiskAnalysisServ;
import utils.HtmlDatabase;

public class HighlightTableModel extends DefaultTableModel {
    
    private static final String[] COLUMN_NAMES = {
        "No.", "Signal Provider", "Balance", "3MPDD", "3MProfProz", "Trades", "Trade Days", 
        "Win Rate %", "Total Profit", "Avg Profit/Trade", "Max Drawdown %", 
        "Equity Drawdown %", "Profit Factor", "MaxTrades", "MaxLots", 
        "Max Duration (h)", "Risk Score", "S/L", "T/P", "Start Date", "End Date", "Stabilitaet"
    };
    
    private final HtmlDatabase htmlDatabase;
    
    public HighlightTableModel(String rootPath) {
        super(COLUMN_NAMES, 0);
        this.htmlDatabase = new HtmlDatabase(rootPath);
    }

    public HtmlDatabase getHtmlDatabase() {
        return this.htmlDatabase;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
    
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:  // No
            case 6:  // Trades
            case 7:  // Trade Days
            case 14: // MaxTrades
            case 16: // Max Duration
            case 17: // Risk Score
            case 18: // S/L
            case 19: // T/P
                return Integer.class;
            case 2:  // Balance
            case 3:  // 3MonProfit
            case 4:  // 3MPDD
            case 5:  // 3MProfProz
            case 8:  // Win Rate
            case 9:  // Total Profit
            case 10: // Avg Profit/Trade
            case 11: // Max Drawdown
            case 12: // Equity Drawdown
            case 13: // Profit Factor
            case 15: // MaxLots
            case 20: // Stabilität
               return Double.class; 
            default:
                return String.class;
        }
    }
    
    private double calculate3MPDD(double threeMonthProfitPercent, double maxEquityDrawdown) {
        if (maxEquityDrawdown == 0.0) {
            return 0.0;  // Verhindert Division durch Null
        }
        return threeMonthProfitPercent / maxEquityDrawdown;
    }

    public void populateData(Map<String, ProviderStats> statsMap) {
        setRowCount(0);
        int rowNum = 1;

        for (Map.Entry<String, ProviderStats> entry : statsMap.entrySet()) {
            String providerName = entry.getKey();
            ProviderStats stats = entry.getValue();
            int riskScore = RiskAnalysisServ.calculateRiskScore(stats);
            double equityDrawdown = htmlDatabase.getEquityDrawdown(providerName);
            double balance = htmlDatabase.getBalance(providerName);
            double threeMonthProfit = stats.getLastThreeMonthsProfit();
            double threeMonthProfitPercent = htmlDatabase.getAvr3MonthProfit(providerName);
            double mpdd = calculate3MPDD(threeMonthProfitPercent, equityDrawdown);
            double stabilitaet = htmlDatabase.getStabilitaetswert(providerName);

            addRow(new Object[]{
                rowNum++, providerName, balance, mpdd, 
                threeMonthProfitPercent, 
                stats.getTrades().size(), stats.getTradeDays(), stats.getWinRate(), 
                stats.getTotalProfit(), stats.getAverageProfit(), stats.getMaxDrawdown(), 
                equityDrawdown, stats.getProfitFactor(), stats.getMaxConcurrentTrades(), 
                stats.getMaxConcurrentLots(), stats.getMaxDuration(), riskScore, 
                stats.hasStopLoss() ? 1 : 0, stats.hasTakeProfit() ? 1 : 0, 
                stats.getStartDate(), stats.getEndDate(), stabilitaet
            });
        }
        fireTableDataChanged();
    }

    public Object[] createRowDataForProvider(String providerName, ProviderStats stats) {
        double equityDrawdown = htmlDatabase.getEquityDrawdown(providerName);
        double balance = htmlDatabase.getBalance(providerName);
        double threeMonthProfitPercent = htmlDatabase.getAvr3MonthProfit(providerName);
        double mpdd = calculate3MPDD(threeMonthProfitPercent, equityDrawdown);
        int riskScore = RiskAnalysisServ.calculateRiskScore(stats);
        double stabilitaet = htmlDatabase.getStabilitaetswert(providerName);

        return new Object[]{
            0, // Platzhalter für die Nummer, wird in populateData gesetzt
            providerName,
            balance,
            mpdd,
            threeMonthProfitPercent,
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
            stats.getEndDate(),
            stabilitaet
        };
    }
}