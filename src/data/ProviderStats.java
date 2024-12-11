package data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.jfree.data.category.DefaultCategoryDataset;

import utils.TradeUtils;

public class ProviderStats {
    private final List<Trade> trades;
    private final List<Double> profits;
    private double initialBalance;
    
    public ProviderStats() {
        this.trades = new ArrayList<>();
        this.profits = new ArrayList<>();
        this.initialBalance = 0.0;
    }
    
    public void setInitialBalance(double balance) {
        this.initialBalance = balance;
    }
    
    public double getInitialBalance() {
        return initialBalance;
    }
    
    public void addTrade(LocalDateTime openTime, LocalDateTime closeTime,
                        String type, String symbol, double lots,
                        double openPrice, double closePrice,
                        double commission, double swap, double profit) {
        Trade trade = new Trade(openTime, closeTime, type, symbol, lots,
                              openPrice, closePrice, commission, swap, profit);
        trades.add(trade);
        profits.add(profit);
    }
    
    public List<Trade> getTrades() {
        return trades;
    }
    
    public List<Double> getProfits() {
        return profits;
    }
    
    public double getTotalProfit() {
        return profits.stream().mapToDouble(Double::doubleValue).sum();
    }
    
    public double getWinRate() {
        long winningTrades = profits.stream().filter(p -> p > 0).count();
        return profits.isEmpty() ? 0.0 : (winningTrades * 100.0) / profits.size();
    }
    
    public double getAverageProfitPerTrade() {
        return profits.isEmpty() ? 0.0 : getTotalProfit() / profits.size();
    }
    
    public double getProfitFactor() {
        double totalGain = profits.stream().filter(p -> p > 0).mapToDouble(Double::doubleValue).sum();
        double totalLoss = Math.abs(profits.stream().filter(p -> p < 0).mapToDouble(Double::doubleValue).sum());
        return totalLoss == 0 ? totalGain : totalGain / totalLoss;
    }
    
    public double getMaxDrawdownPercent() {
        if (profits.isEmpty()) return 0.0;
        
        double peak = initialBalance;
        double maxDrawdown = 0.0;
        double currentBalance = initialBalance;
        
        for (double profit : profits) {
            currentBalance += profit;
            peak = Math.max(currentBalance, peak);
            double drawdown = (peak - currentBalance) / peak * 100.0;
            maxDrawdown = Math.max(maxDrawdown, drawdown);
        }
        
        return maxDrawdown;
    }
    
    public LocalDate getStartDate() {
        if (trades.isEmpty()) {
            return LocalDate.now();
        }
        return trades.stream()
            .map(trade -> trade.getOpenTime().toLocalDate())
            .min(LocalDate::compareTo)
            .orElse(LocalDate.now());
    }
    
    public LocalDate getEndDate() {
        if (trades.isEmpty()) {
            return LocalDate.now();
        }
        return trades.stream()
            .map(trade -> trade.getCloseTime().toLocalDate())
            .max(LocalDate::compareTo)
            .orElse(LocalDate.now());
    }
    
    public int getMaxConcurrentTrades() {
        return TradeUtils.findMaxConcurrentTrades(trades);
    }

    public double getMaxConcurrentLots() {
        return TradeUtils.findMaxConcurrentLots(trades);
    }
    
    public DefaultCategoryDataset getMonthlyProfitData() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        // Implementation...
        return dataset;
    }
    
    public double getMaxProfit() {
        return profits.stream()
            .mapToDouble(Double::doubleValue)
            .max()
            .orElse(0.0);
    }

    public double getMaxLoss() {
        return profits.stream()
            .mapToDouble(Double::doubleValue)
            .min()
            .orElse(0.0);
    }

    public int getTradeCount() {
        return trades.size();
    }

    public double getMaxDrawdown() {
        return getMaxDrawdownPercent();
    }

    public double getAverageProfit() {
        return getAverageProfitPerTrade();
    }
}