package models;



import data.ProviderStats;
import utils.LoggerUtil;

public class FilterCriteria {
    private final int minTradeDays;
    private final double minProfit;
    private final double minProfitFactor;
    private final double minWinRate;
    private final double maxDrawdown;
    
    public FilterCriteria(int minTradeDays, double minProfit, 
            double minProfitFactor, double minWinRate, double maxDrawdown) {
        this.minTradeDays = minTradeDays;
        this.minProfit = minProfit;
        this.minProfitFactor = minProfitFactor;
        this.minWinRate = minWinRate;
        this.maxDrawdown = maxDrawdown;
        
        LoggerUtil.debug(String.format("Created filter criteria: minTradeDays=%d, minProfit=%.2f, " +
            "minProfitFactor=%.2f, minWinRate=%.2f, maxDrawdown=%.2f",
            minTradeDays, minProfit, minProfitFactor, minWinRate, maxDrawdown));
    }
    
    public boolean matches(ProviderStats stats) {
        if (stats == null) {
            LoggerUtil.warn("Attempting to match null ProviderStats");
            return false;
        }
        
        boolean matches = stats.getDaysBetween() >= minTradeDays &&
               stats.getTotalProfit() >= minProfit &&
               stats.getProfitFactor() >= minProfitFactor &&
               stats.getWinRate() >= minWinRate &&
               stats.getMaxDrawdown() <= maxDrawdown;
        
        if (LoggerUtil.isDebugEnabled()) {
            LoggerUtil.debug(String.format("Filter match result for provider: " +
                "tradeDays=%d/%d, profit=%.2f/%.2f, profitFactor=%.2f/%.2f, " +
                "winRate=%.2f/%.2f, drawdown=%.2f/%.2f -> %s",
                stats.getDaysBetween(), minTradeDays,
                stats.getTotalProfit(), minProfit,
                stats.getProfitFactor(), minProfitFactor,
                stats.getWinRate(), minWinRate,
                stats.getMaxDrawdown(), maxDrawdown,
                matches ? "MATCH" : "NO MATCH"));
        }
        
        return matches;
    }
    
    // Getter für alle Kriterien
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
    
    @Override
    public String toString() {
        return String.format("FilterCriteria[tradeDays>=%d, profit>=%.2f, " +
            "profitFactor>=%.2f, winRate>=%.2f, drawdown<=%.2f]",
            minTradeDays, minProfit, minProfitFactor, minWinRate, maxDrawdown);
    }
}