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
    private boolean hasStopLoss = false;
    private boolean hasTakeProfit = false;
    
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
                        double stopLoss, double takeProfit,
                        double commission, double swap, double profit) {
        Trade trade = new Trade(openTime, closeTime, type, symbol, lots,
                              openPrice, closePrice, stopLoss, takeProfit,
                              commission, swap, profit);
        trades.add(trade);
        profits.add(profit);
        
        if (stopLoss != 0.0) hasStopLoss = true;
        if (takeProfit != 0.0) hasTakeProfit = true;
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
    
    public double getLastThreeMonthsProfit() {
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        return trades.stream()
            .filter(trade -> trade.getCloseTime().isAfter(threeMonthsAgo))
            .mapToDouble(Trade::getProfit)
            .sum();
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
        
        boolean allProfitable = profits.stream().allMatch(profit -> profit > 0);
        if (allProfitable) {
            return 99.99;
        }
        
        double currentBalance = initialBalance;
        double highWaterMark = initialBalance;
        double maxDrawdownPercent = 0.0;
        
        for (double profit : profits) {
            currentBalance += profit;
            
            if (currentBalance < highWaterMark) {
                double drawdownPercent = (highWaterMark - currentBalance) / highWaterMark * 100;
                maxDrawdownPercent = Math.max(maxDrawdownPercent, drawdownPercent);
            } else {
                highWaterMark = currentBalance;
            }
        }
        
        return maxDrawdownPercent;
    }

    public double getMaxDrawdown() {
        return getMaxDrawdownPercent();
    }
    
    public LocalDate getStartDate() {
        return trades.isEmpty() ? LocalDate.now() : 
               trades.get(0).getOpenTime().toLocalDate();
    }
    
    public LocalDate getEndDate() {
        return trades.isEmpty() ? LocalDate.now() : 
               trades.get(trades.size() - 1).getCloseTime().toLocalDate();
    }
    
    public boolean hasStopLoss() {
        return hasStopLoss;
    }
    
    public boolean hasTakeProfit() {
        return hasTakeProfit;
    }
    
    public int getMaxConcurrentTrades() {
        return TradeUtils.findMaxConcurrentTrades(trades);
    }

    public double getMaxConcurrentLots() {
        return TradeUtils.findMaxConcurrentLots(trades);
    }
    
    public int getTradeDays() {
        if (trades.isEmpty()) return 0;
        
        return (int) trades.stream()
            .map(trade -> trade.getOpenTime().toLocalDate())
            .distinct()
            .count();
    }
    
    public double getAverageProfit() {
        return getAverageProfitPerTrade();
    }
    
    public long getMaxDuration() {
        return trades.stream()
            .mapToLong(trade -> 
                java.time.Duration.between(trade.getOpenTime(), trade.getCloseTime()).toHours())
            .max()
            .orElse(0);
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
}