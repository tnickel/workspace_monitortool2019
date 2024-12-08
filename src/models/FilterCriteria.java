package models;

import data.ProviderStats;

public class FilterCriteria {
    private int minTradeDays;
    private double minProfit;
    private double minTotalProfit;
    private double minProfitFactor;
    private double minWinRate;
    private double maxDrawdown;
    private int maxConcurrentTrades;
    private double maxConcurrentLots;

    public FilterCriteria() {
        this.minTradeDays = 0;
        this.minProfit = 0.0;
        this.minTotalProfit = 0.0;
        this.minProfitFactor = 0.0;
        this.minWinRate = 0.0;
        this.maxDrawdown = 100.0;
        this.maxConcurrentTrades = Integer.MAX_VALUE;
        this.maxConcurrentLots = Double.MAX_VALUE;
    }

    public int getMinTradeDays() {
        return minTradeDays;
    }

    public void setMinTradeDays(int minTradeDays) {
        this.minTradeDays = minTradeDays;
    }

    public double getMinProfit() {
        return minProfit;
    }

    public void setMinProfit(double minProfit) {
        this.minProfit = minProfit;
    }

    public double getMinTotalProfit() {
        return minTotalProfit;
    }

    public void setMinTotalProfit(double minTotalProfit) {
        this.minTotalProfit = minTotalProfit;
    }

    public double getMinProfitFactor() {
        return minProfitFactor;
    }

    public void setMinProfitFactor(double minProfitFactor) {
        this.minProfitFactor = minProfitFactor;
    }

    public double getMinWinRate() {
        return minWinRate;
    }

    public void setMinWinRate(double minWinRate) {
        this.minWinRate = minWinRate;
    }

    public double getMaxDrawdown() {
        return maxDrawdown;
    }

    public void setMaxDrawdown(double maxDrawdown) {
        this.maxDrawdown = maxDrawdown;
    }

    public int getMaxConcurrentTrades() {
        return maxConcurrentTrades;
    }

    public void setMaxConcurrentTrades(int maxConcurrentTrades) {
        this.maxConcurrentTrades = maxConcurrentTrades;
    }

    public double getMaxConcurrentLots() {
        return maxConcurrentLots;
    }

    public void setMaxConcurrentLots(double maxConcurrentLots) {
        this.maxConcurrentLots = maxConcurrentLots;
    }

    public boolean matches(ProviderStats stats) {
        return stats.getTrades().size() >= minTradeDays &&
               stats.getTotalProfit() >= minTotalProfit &&
               stats.getAverageProfitPerTrade() >= minProfit &&
               stats.getProfitFactor() >= minProfitFactor &&
               stats.getWinRate() >= minWinRate &&
               stats.getMaxDrawdownPercent() <= maxDrawdown &&
               stats.getMaxConcurrentTrades() <= maxConcurrentTrades &&
               stats.getMaxConcurrentLots() <= maxConcurrentLots;
    }

    @Override
    public String toString() {
        return String.format("FilterCriteria{minTradeDays=%d, minProfit=%.2f, minTotalProfit=%.2f, " +
                           "minProfitFactor=%.2f, minWinRate=%.2f, maxDrawdown=%.2f, " +
                           "maxConcurrentTrades=%d, maxConcurrentLots=%.2f}",
                           minTradeDays, minProfit, minTotalProfit,
                           minProfitFactor, minWinRate, maxDrawdown,
                           maxConcurrentTrades, maxConcurrentLots);
    }
}