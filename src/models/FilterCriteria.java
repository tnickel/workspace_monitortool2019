package models;

import data.ProviderStats;

public class FilterCriteria {
    private int minTradeDays = 0;
    private double minProfit = 0.0;
    private double minTotalProfit = 0.0;
    private double minProfitFactor = 0.0;
    private double minWinRate = 0.0;
    private double maxDrawdown = 100.0;
    private int minMaxConcurrentTrades = 0;
    private double minMaxConcurrentLots = 0.0;

    public void setMinTradeDays(int days) {
        this.minTradeDays = days;
    }

    public void setMinProfit(double profit) {
        this.minProfit = profit;
    }

    public void setMinTotalProfit(double profit) {
        this.minTotalProfit = profit;
    }

    public void setMinProfitFactor(double factor) {
        this.minProfitFactor = factor;
    }

    public void setMinWinRate(double rate) {
        this.minWinRate = rate;
    }

    public void setMaxDrawdown(double drawdown) {
        this.maxDrawdown = drawdown;
    }

    public void setMinMaxConcurrentTrades(int trades) {
        this.minMaxConcurrentTrades = trades;
    }

    public void setMinMaxConcurrentLots(double lots) {
        this.minMaxConcurrentLots = lots;
    }

    public int getMinTradeDays() {
        return minTradeDays;
    }

    public double getMinProfit() {
        return minProfit;
    }

    public double getMinTotalProfit() {
        return minTotalProfit;
    }

    public double getMinProfitFactor() {
        return minProfitFactor;
    }

    public double getMinWinRate() {
        return minWinRate;
    }

    public double getMaxDrawdown() {
        return maxDrawdown;
    }

    public int getMinMaxConcurrentTrades() {
        return minMaxConcurrentTrades;
    }

    public double getMinMaxConcurrentLots() {
        return minMaxConcurrentLots;
    }

    public boolean matches(ProviderStats stats) {
        return stats.getTradeCount() >= minTradeDays &&
               stats.getTotalProfit() >= minTotalProfit &&
               stats.getAverageProfit() >= minProfit &&
               stats.getProfitFactor() >= minProfitFactor &&
               stats.getWinRate() >= minWinRate &&
               stats.getMaxDrawdown() <= maxDrawdown &&
               stats.getMaxConcurrentTrades() >= minMaxConcurrentTrades &&
               stats.getMaxConcurrentLots() >= minMaxConcurrentLots;
    }

    @Override
    public String toString() {
        return String.format("FilterCriteria{minTradeDays=%d, minProfit=%.2f, minTotalProfit=%.2f, " +
                           "minProfitFactor=%.2f, minWinRate=%.2f, maxDrawdown=%.2f, " +
                           "minMaxConcurrentTrades=%d, minMaxConcurrentLots=%.2f}",
            minTradeDays, minProfit, minTotalProfit, minProfitFactor, minWinRate, 
            maxDrawdown, minMaxConcurrentTrades, minMaxConcurrentLots);
    }
}