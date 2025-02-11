package models;

import javax.swing.table.DefaultTableModel;
import java.util.Map;
import data.ProviderStats;
import services.RiskAnalysisServ;
import utils.HtmlParser;

public class HighlightTableModel extends DefaultTableModel {
    
    private static final String[] COLUMN_NAMES = {
        "No.", "Signal Provider", "Balance", "3MPDD", "3MProfProz", "Trades", "Trade Days", 
        "Win Rate %", "Total Profit", "Avg Profit/Trade", "Max Drawdown %", 
        "Equity Drawdown %", "Profit Factor", "MaxTrades", "MaxLots", 
        "Max Duration (h)", "Risk Score", "S/L", "T/P", "Start Date", "End Date", "Stabilitaet"
    };
    
    private final HtmlParser htmlParser;
    
    public HighlightTableModel(String rootPath) {
        super(COLUMN_NAMES, 0);
        this.htmlParser = new HtmlParser(rootPath);
    }
    public HtmlParser getHtmlParser() {
        return this.htmlParser;
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
            case 20: // Stabilit�t
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
            double equityDrawdown = htmlParser.getEquityDrawdown(providerName);  // Holt den maximalen Drawdown
            double balance = htmlParser.getBalance(providerName);
            double threeMonthProfit = stats.getLastThreeMonthsProfit();
            double threeMonthProfitPercent = htmlParser.getAvr3MonthProfit(providerName); // Holt den 3MProfProz-Wert
            double mpdd = calculate3MPDD(threeMonthProfitPercent, equityDrawdown);  // NEUE FORMEL
            double trendwert = htmlParser.getStabilitaetswert(providerName);  // Holt den Trendwert

            addRow(new Object[]{
                rowNum++, providerName, balance, mpdd, 
                threeMonthProfitPercent, 
                stats.getTrades().size(), stats.getTradeDays(), stats.getWinRate(), 
                stats.getTotalProfit(), stats.getAverageProfit(), stats.getMaxDrawdown(), 
                equityDrawdown, stats.getProfitFactor(), stats.getMaxConcurrentTrades(), 
                stats.getMaxConcurrentLots(), stats.getMaxDuration(), riskScore, 
                stats.hasStopLoss() ? 1 : 0, stats.hasTakeProfit() ? 1 : 0, 
                stats.getStartDate(), stats.getEndDate(), trendwert
            });
        }
        fireTableDataChanged();
    }

    public Object[] createRowDataForProvider(String providerName, ProviderStats stats) {
        double equityDrawdown = htmlParser.getEquityDrawdown(providerName);
        double balance = htmlParser.getBalance(providerName);
        double threeMonthProfitPercent = htmlParser.getAvr3MonthProfit(providerName);
        double mpdd = calculate3MPDD(threeMonthProfitPercent, equityDrawdown);
        int riskScore = RiskAnalysisServ.calculateRiskScore(stats);
        double stabilitaet = htmlParser.getStabilitaetswert(providerName);  // Holt den Trendwert

        return new Object[]{
            0, // Platzhalter f�r die Nummer, wird in populateData gesetzt
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