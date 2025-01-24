package models;

import data.ProviderStats;

public class FilterCriteria {
    private int minTradeDays = 0;
    private double minProfit = 0;
    private double minTotalProfit = 0;
    private double minProfitFactor = 0;
    private double minWinRate = 0;
    private double maxDrawdown = 100;
    private int maxConcurrentTrades = Integer.MAX_VALUE;
    private double maxConcurrentLots = Double.MAX_VALUE;
    private long maxDuration = Long.MAX_VALUE;

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

    public long getMaxDuration() {
        return maxDuration;
    }

    public void setMaxDuration(long maxDuration) {
        this.maxDuration = maxDuration;
    }

    public boolean matches(ProviderStats stats) {
        if (stats.getTrades().size() < minTradeDays) return false;
        if (stats.getAverageProfit() < minProfit) return false;
        if (stats.getTotalProfit() < minTotalProfit) return false;
        if (stats.getProfitFactor() < minProfitFactor) return false;
        if (stats.getWinRate() < minWinRate) return false;
        if (stats.getMaxDrawdown() > maxDrawdown) return false;
        if (stats.getMaxConcurrentTrades() > maxConcurrentTrades) return false;
        if (stats.getMaxConcurrentLots() > maxConcurrentLots) return false;
        if (stats.getMaxDuration() > maxDuration) return false;
        
        return true;
    }
}