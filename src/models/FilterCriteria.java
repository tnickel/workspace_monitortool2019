package models;

import data.ProviderStats;

public class FilterCriteria {
   private int minTradeDays = 0;
   private int minTrades = 0;
   private double minProfit = 0;
   private double minTotalProfit = 0;
   private double minProfitFactor = 0;
   private double minWinRate = 0;
   private double maxDrawdown = 100;
   private int maxConcurrentTrades = Integer.MAX_VALUE;
   private double maxConcurrentLots = Double.MAX_VALUE;
   private long maxDuration = Long.MAX_VALUE;

   public boolean matches(ProviderStats stats) {
       return stats.getTradeDays() >= minTradeDays &&
              stats.getTrades().size() >= minTrades &&
              stats.getAverageProfit() >= minProfit &&
              stats.getTotalProfit() >= minTotalProfit &&
              stats.getProfitFactor() >= minProfitFactor &&
              stats.getWinRate() >= minWinRate &&
              stats.getMaxDrawdown() <= maxDrawdown &&
              stats.getMaxConcurrentTrades() <= maxConcurrentTrades &&
              stats.getMaxConcurrentLots() <= maxConcurrentLots &&
              stats.getMaxDuration() <= maxDuration;
   }

   // Getter und Setter
   public int getMinTradeDays() { return minTradeDays; }
   public void setMinTradeDays(int days) { this.minTradeDays = days; }
   
   public int getMinTrades() { return minTrades; }
   public void setMinTrades(int trades) { this.minTrades = trades; }
   
   public double getMinProfit() { return minProfit; }
   public void setMinProfit(double profit) { this.minProfit = profit; }
   
   public double getMinTotalProfit() { return minTotalProfit; }
   public void setMinTotalProfit(double totalProfit) { this.minTotalProfit = totalProfit; }
   
   public double getMinProfitFactor() { return minProfitFactor; }
   public void setMinProfitFactor(double factor) { this.minProfitFactor = factor; }
   
   public double getMinWinRate() { return minWinRate; }
   public void setMinWinRate(double rate) { this.minWinRate = rate; }
   
   public double getMaxDrawdown() { return maxDrawdown; }
   public void setMaxDrawdown(double drawdown) { this.maxDrawdown = drawdown; }
   
   public int getMaxConcurrentTrades() { return maxConcurrentTrades; }
   public void setMaxConcurrentTrades(int trades) { this.maxConcurrentTrades = trades; }
   
   public double getMaxConcurrentLots() { return maxConcurrentLots; }
   public void setMaxConcurrentLots(double lots) { this.maxConcurrentLots = lots; }
   
   public long getMaxDuration() { return maxDuration; }
   public void setMaxDuration(long duration) { this.maxDuration = duration; }
}