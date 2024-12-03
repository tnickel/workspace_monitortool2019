package models;

import data.ProviderStats;

public class FilterCriteria {
    private int minTradeDays = 0;
    private double minProfit = 0.0;
    private double minProfitFactor = 0.0;
    private double minWinRate = 0.0;
    private double maxDrawdown = 100.0;

    public void setMinTradeDays(int days) {
        this.minTradeDays = days;
    }

    public void setMinProfit(double profit) {
        this.minProfit = profit;
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

    public int getMinTradeDays() {
        return minTradeDays;
    }

    public double getMinProfit() {
        return minProfit;
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

    public boolean matches(ProviderStats stats) {
        return stats.getTradeCount() >= minTradeDays &&
               stats.getTotalProfit() >= minProfit &&
               stats.getProfitFactor() >= minProfitFactor &&
               stats.getWinRate() >= minWinRate &&
               stats.getMaxDrawdown() <= maxDrawdown;
    }

    @Override
    public String toString() {
        return String.format("FilterCriteria{minTradeDays=%d, minProfit=%.2f, minProfitFactor=%.2f, minWinRate=%.2f, maxDrawdown=%.2f}",
            minTradeDays, minProfit, minProfitFactor, minWinRate, maxDrawdown);
    }
}