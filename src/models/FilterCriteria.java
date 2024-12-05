
package models;

import data.ProviderStats;

public class FilterCriteria {
    private int minTradeDays;
    private double minProfit;
    private double minTotalProfit;
    private double minProfitFactor;
    private double minWinRate;
    private double maxDrawdown;
    private int minMaxConcurrentTrades;
    private double minMaxConcurrentLots;

    public FilterCriteria() {
        this.minTradeDays = 0;
        this.minProfit = 0.0;
        this.minTotalProfit = 0.0;
        this.minProfitFactor = 0.0;
        this.minWinRate = 0.0;
        this.maxDrawdown = 100.0;
        this.minMaxConcurrentTrades = 0;
        this.minMaxConcurrentLots = 0.0;
    }

    // Getter und Setter
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

    public int getMinMaxConcurrentTrades() {
        return minMaxConcurrentTrades;
    }

    public void setMinMaxConcurrentTrades(int minMaxConcurrentTrades) {
        this.minMaxConcurrentTrades = minMaxConcurrentTrades;
    }

    public double getMinMaxConcurrentLots() {
        return minMaxConcurrentLots;
    }

    public void setMinMaxConcurrentLots(double minMaxConcurrentLots) {
        this.minMaxConcurrentLots = minMaxConcurrentLots;
    }

    public boolean matches(ProviderStats stats) {
        return stats.getTrades().size() >= minTradeDays &&
               stats.getTotalProfit() >= minTotalProfit &&
               stats.getAverageProfitPerTrade() >= minProfit &&
               stats.getProfitFactor() >= minProfitFactor &&
               stats.getWinRate() >= minWinRate &&
               stats.getMaxDrawdownPercent() <= maxDrawdown &&
               stats.getMaxConcurrentTrades() >= minMaxConcurrentTrades &&
               stats.getMaxConcurrentLots() >= minMaxConcurrentLots;
    }

    @Override
    public String toString() {
        return String.format("FilterCriteria{minTradeDays=%d, minProfit=%.2f, minTotalProfit=%.2f, " +
                           "minProfitFactor=%.2f, minWinRate=%.2f, maxDrawdown=%.2f, " +
                           "minMaxConcurrentTrades=%d, minMaxConcurrentLots=%.2f}",
                           minTradeDays, minProfit, minTotalProfit,
                           minProfitFactor, minWinRate, maxDrawdown,
                           minMaxConcurrentTrades, minMaxConcurrentLots);
    }
}
