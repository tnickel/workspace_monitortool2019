import java.time.*;
import java.time.format.*;
import java.text.DecimalFormat;
import java.util.*;
import org.jfree.data.category.DefaultCategoryDataset;

class ProviderStats {
    private int tradeCount;
    private LocalDate startDate;
    private LocalDate endDate;
    private final List<Double> profits;
    private final List<LocalDate> tradeDates;
    private double initialBalance;
    private double totalProfit;
    private int winningTrades;
    private double totalWinAmount;
    private double totalLossAmount;
    private double maxDrawdown;
    private double peakBalance;
    private double maxProfit;
    private double maxLoss;
    private final Map<YearMonth, Double> monthlyProfits;

    public ProviderStats() {
        this.profits = new ArrayList<>();
        this.tradeDates = new ArrayList<>();
        this.monthlyProfits = new TreeMap<>();
        this.maxProfit = Double.MIN_VALUE;
        this.maxLoss = Double.MAX_VALUE;
        this.totalProfit = 0.0;
        this.initialBalance = 0.0;
        this.tradeCount = 0;
        this.winningTrades = 0;
        this.totalWinAmount = 0.0;
        this.totalLossAmount = 0.0;
        this.maxDrawdown = 0.0;
        this.peakBalance = 0.0;
    }

    public void setInitialBalance(double balance) {
        this.initialBalance = balance;
        this.peakBalance = balance;
    }

    public void addTrade(double profit, LocalDate date) {
        profits.add(profit);
        tradeDates.add(date);
        tradeCount++;
        totalProfit += profit;

        if (profit > 0) {
            winningTrades++;
            totalWinAmount += profit;
            maxProfit = Math.max(maxProfit, profit);
        } else if (profit < 0) {
            totalLossAmount += profit;
            maxLoss = Math.min(maxLoss, profit);
        }

        if (startDate == null || date.isBefore(startDate)) startDate = date;
        if (endDate == null || date.isAfter(endDate)) endDate = date;

        double currentBalance = getCurrentBalance();
        if (currentBalance > peakBalance) {
            peakBalance = currentBalance;
        } else {
            double currentDrawdown = (peakBalance - currentBalance) / peakBalance * 100;
            maxDrawdown = Math.max(maxDrawdown, currentDrawdown);
        }

        YearMonth yearMonth = YearMonth.from(date);
        monthlyProfits.merge(yearMonth, profit, Double::sum);
    }

    // Getter methods
    public double getCurrentBalance() { 
        return initialBalance + totalProfit; 
    }
    
    public double getInitialBalance() { 
        return initialBalance; 
    }
    
    public double getTotalProfit() { 
        return totalProfit; 
    }
    
    public List<Double> getProfits() { 
        return profits; 
    }
    
    public List<LocalDate> getTradeDates() { 
        return tradeDates; 
    }
    
    public int getTradeCount() { 
        return tradeCount; 
    }
    
    public LocalDate getStartDate() { 
        return startDate; 
    }
    
    public LocalDate getEndDate() { 
        return endDate; 
    }
    
    public double getWinRate() { 
        return tradeCount > 0 ? (winningTrades * 100.0) / tradeCount : 0; 
    }
    
    public double getProfitFactor() { 
        return totalLossAmount != 0 ? Math.abs(totalWinAmount / totalLossAmount) : 0; 
    }
    
    public double getAverageProfit() { 
        return tradeCount > 0 ? totalProfit / tradeCount : 0; 
    }
    
    public double getMaxDrawdown() { 
        return maxDrawdown; 
    }
    
    public double getMaxProfit() { 
        return maxProfit != Double.MIN_VALUE ? maxProfit : 0; 
    }
    
    public double getMaxLoss() { 
        return maxLoss != Double.MAX_VALUE ? maxLoss : 0; 
    }

    public long getDaysBetween() {
        if (startDate != null && endDate != null) {
            return Duration.between(startDate.atStartOfDay(), endDate.atStartOfDay()).toDays();
        }
        return 0;
    }

    public DefaultCategoryDataset getMonthlyProfitData() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        monthlyProfits.forEach((month, profit) -> 
            dataset.addValue(profit, "Monthly Profit", 
                           month.format(DateTimeFormatter.ofPattern("MMM yyyy"))));
        return dataset;
    }

    public String getAnalysisString() {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        DecimalFormat pf = new DecimalFormat("#,##0.00'%'");
        
        StringBuilder report = new StringBuilder();
        report.append(String.format("Current Balance: %s%n", df.format(getCurrentBalance())));
        report.append(String.format("Total Profit/Loss: %s%n", df.format(getTotalProfit())));
        report.append(String.format("Number of Trades: %d%n", getTradeCount()));
        report.append(String.format("Win Rate: %s%n", pf.format(getWinRate())));
        report.append(String.format("Profit Factor: %s%n", df.format(getProfitFactor())));
        report.append(String.format("Average Profit per Trade: %s%n", df.format(getAverageProfit())));
        report.append(String.format("Maximum Drawdown: %s%n", pf.format(getMaxDrawdown())));
        report.append(String.format("Largest Winning Trade: %s%n", df.format(getMaxProfit())));
        report.append(String.format("Largest Losing Trade: %s%n", df.format(getMaxLoss())));
        report.append(String.format("Total Winning Amount: %s%n", df.format(totalWinAmount)));
        report.append(String.format("Total Losing Amount: %s%n", df.format(totalLossAmount)));
        report.append(String.format("Trading Period: %d days%n", getDaysBetween()));
        
        report.append("\nMonthly Performance:\n");
        monthlyProfits.forEach((month, profit) -> 
            report.append(String.format("%s: %s%n", 
                month.format(DateTimeFormatter.ofPattern("MMM yyyy")), 
                df.format(profit))));
        
        return report.toString();
    }
}