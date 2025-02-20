package models;

import java.time.temporal.ChronoUnit;
import java.util.Map;

import javax.swing.table.DefaultTableModel;

import data.ProviderStats;
import services.RiskAnalysisServ;
import utils.HtmlDatabase;

public class HighlightTableModel extends DefaultTableModel {
   
   private static final String[] COLUMN_NAMES = {
       "No.", "Signal Provider", "Balance", "3MPDD", "6MPDD", "9MPDD", "12MPDD", 
       "3MProfProz", "Trades", "Trade Days", "Days", "Win Rate %", "Total Profit", 
       "Avg Profit/Trade", "Max Drawdown %", "Equity Drawdown %", "Profit Factor", 
       "MaxTrades", "MaxLots", "Max Duration (h)", "Risk Score", "S/L", "T/P", 
       "Start Date", "End Date", "Stabilitaet"
   };
   
   private final HtmlDatabase htmlDatabase;
   
   @Override
   public boolean isCellEditable(int row, int column) {
       return false;  // Verhindert das Editieren aller Zellen
   }

   public HighlightTableModel(String rootPath) {
       super(COLUMN_NAMES, 0);
       this.htmlDatabase = new HtmlDatabase(rootPath);
   }

   public HtmlDatabase getHtmlDatabase() {
       return this.htmlDatabase;
   }

   @Override
   public Class<?> getColumnClass(int columnIndex) {
       switch (columnIndex) {
           case 0:  // No
           case 8:  // Trades
           case 9:  // Trade Days
           case 10: // Days
           case 16: // MaxTrades
           case 18: // Max Duration
           case 19: // Risk Score
           case 20: // S/L
           case 21: // T/P
               return Integer.class;
           case 2:  // Balance
           case 3:  // 3MPDD
           case 4:  // 6MPDD
           case 5:  // 9MPDD
           case 6:  // 12MPDD
           case 7:  // 3MProfProz
           case 11: // Win Rate
           case 12: // Total Profit
           case 13: // Avg Profit/Trade
           case 14: // Max Drawdown
           case 15: // Equity Drawdown
           case 17: // MaxLots
           case 25: // Stabilität
               return Double.class;
           default:
               return String.class;
       }
   }
   
   private double calculateMPDD(double monthlyProfitPercent, double maxEquityDrawdown) {
       if (maxEquityDrawdown == 0.0) {
           return 0.0;  // Verhindert Division durch Null
       }
       return monthlyProfitPercent / maxEquityDrawdown;
   }

   private long calculateDaysBetween(ProviderStats stats) {
       return Math.abs(ChronoUnit.DAYS.between(stats.getStartDate(), stats.getEndDate())) + 1;
   }

   public void populateData(Map<String, ProviderStats> statsMap) {
       setRowCount(0);
       int rowNum = 1;

       for (Map.Entry<String, ProviderStats> entry : statsMap.entrySet()) {
           String providerName = entry.getKey();
           ProviderStats stats = entry.getValue();
           
           double equityDrawdown = htmlDatabase.getEquityDrawdown(providerName);
           double balance = htmlDatabase.getBalance(providerName);
           
           // Berechne MPDD für verschiedene Zeiträume und aktualisiere die Tooltips
           htmlDatabase.getMPDD(providerName, 3);  // Dies aktualisiert auch die Tooltip-Berechnung
           htmlDatabase.getMPDD(providerName, 6);
           htmlDatabase.getMPDD(providerName, 9);
           htmlDatabase.getMPDD(providerName, 12);
           
           // Berechne die MPDD-Werte für die Anzeige
           double threeMonthProfit = htmlDatabase.getAverageMonthlyProfit(providerName, 3);
           double sixMonthProfit = htmlDatabase.getAverageMonthlyProfit(providerName, 6);
           double nineMonthProfit = htmlDatabase.getAverageMonthlyProfit(providerName, 9);
           double twelveMonthProfit = htmlDatabase.getAverageMonthlyProfit(providerName, 12);
           
           double mpdd3 = calculateMPDD(threeMonthProfit, equityDrawdown);
           double mpdd6 = calculateMPDD(sixMonthProfit, equityDrawdown);
           double mpdd9 = calculateMPDD(nineMonthProfit, equityDrawdown);
           double mpdd12 = calculateMPDD(twelveMonthProfit, equityDrawdown);
           
           int riskScore = RiskAnalysisServ.calculateRiskScore(stats);
           double stabilitaet = htmlDatabase.getStabilitaetswert(providerName);

           long daysBetween = calculateDaysBetween(stats);
           
           addRow(new Object[]{
               rowNum++, 
               providerName, 
               balance,
               mpdd3,
               mpdd6,
               mpdd9,
               mpdd12,
               threeMonthProfit,
               stats.getTrades().size(),
               stats.getTradeDays(),
               daysBetween,
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
           });
       }
       fireTableDataChanged();
   }
     
   public Object[] createRowDataForProvider(String providerName, ProviderStats stats) {
       double equityDrawdown = htmlDatabase.getEquityDrawdown(providerName);
       double balance = htmlDatabase.getBalance(providerName);
       
       // Berechne MPDD für verschiedene Zeiträume
       double threeMonthProfit = htmlDatabase.getAverageMonthlyProfit(providerName, 3);
       double sixMonthProfit = htmlDatabase.getAverageMonthlyProfit(providerName, 6);
       double nineMonthProfit = htmlDatabase.getAverageMonthlyProfit(providerName, 9);
       double twelveMonthProfit = htmlDatabase.getAverageMonthlyProfit(providerName, 12);
       
       double mpdd3 = calculateMPDD(threeMonthProfit, equityDrawdown);
       double mpdd6 = calculateMPDD(sixMonthProfit, equityDrawdown);
       double mpdd9 = calculateMPDD(nineMonthProfit, equityDrawdown);
       double mpdd12 = calculateMPDD(twelveMonthProfit, equityDrawdown);
       
       int riskScore = RiskAnalysisServ.calculateRiskScore(stats);
       double stabilitaet = htmlDatabase.getStabilitaetswert(providerName);

       long daysBetween = calculateDaysBetween(stats);

       return new Object[]{
           0, // Platzhalter für die Nummer
           providerName,
           balance,
           mpdd3,
           mpdd6,
           mpdd9,
           mpdd12,
           threeMonthProfit,
           stats.getTrades().size(),
           stats.getTradeDays(),
           daysBetween,
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