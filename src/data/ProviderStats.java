package data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import utils.TradeUtils;

public class ProviderStats {
    private final List<Trade> trades;
    private final List<Double> profits;
    private final Map<YearMonth, Double> monthlyProfitPercentages;
    private double initialBalance;
    private boolean hasStopLoss = false;
    private boolean hasTakeProfit = false;
    private String signalProvider;
    private String signalProviderURL;
    
    public ProviderStats() {
        this.trades = new ArrayList<>();
        this.profits = new ArrayList<>();
        this.monthlyProfitPercentages = new TreeMap<>();
        this.initialBalance = 0.0;
    }

    public void setSignalProviderInfo(String provider, String url) {
        this.signalProvider = provider;
        this.signalProviderURL = url;
    }

    public String getSignalProvider() {
        return signalProvider;
    }

    public String getSignalProviderURL() {
        return signalProviderURL;
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
                              signalProvider, signalProviderURL,
                              commission, swap, profit);
        trades.add(trade);
        profits.add(profit);
        
        if (stopLoss != 0.0) hasStopLoss = true;
        if (takeProfit != 0.0) hasTakeProfit = true;
    }
    
    public void setMonthlyProfits(Map<String, Double> monthProfits) {
        monthlyProfitPercentages.clear();
        for (Map.Entry<String, Double> entry : monthProfits.entrySet()) {
            String[] parts = entry.getKey().split("/");
            if (parts.length == 2) {
                try {
                    int year = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    monthlyProfitPercentages.put(YearMonth.of(year, month), entry.getValue());
                } catch (NumberFormatException e) {
                    // Skip invalid entries
                }
            }
        }
    }
    
    public Map<YearMonth, Double> getMonthlyProfitPercentages() {
        return monthlyProfitPercentages;
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
        
        double currentBalance = initialBalance; // Wichtig: Stelle sicher, dass initialBalance korrekt gesetzt ist (z.B. auf 2000)
        double highWaterMark = initialBalance;
        double maxDrawdownPercent = 0.0;
        
        for (double profit : profits) {
            currentBalance += profit;
            
            if (currentBalance > highWaterMark) {
                highWaterMark = currentBalance;
            } 
            else if (highWaterMark > 0) {
                double drawdownPercent = (highWaterMark - currentBalance) / highWaterMark * 100;
                maxDrawdownPercent = Math.max(maxDrawdownPercent, drawdownPercent);
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
    
    public double getEquityDrawdown() {
        if (trades.isEmpty()) return 0.0;
        
        double currentBalance = initialBalance;
        double highWaterMark = initialBalance;
        double maxDrawdownPercent = 0.0;
        
        for (double profit : profits) {
            currentBalance += profit;
            highWaterMark = Math.max(currentBalance, highWaterMark);
            
            if (currentBalance < highWaterMark) {
                double drawdownPercent = (highWaterMark - currentBalance) / highWaterMark * 100;
                maxDrawdownPercent = Math.max(maxDrawdownPercent, drawdownPercent);
            }
        }
        return 0;
    }
}