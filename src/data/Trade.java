package data;

import java.time.LocalDateTime;

public class Trade {
    // Zeitinformationen
    private final LocalDateTime openTime;
    private final LocalDateTime closeTime;
    
    // Handelsinformationen
    private final String type;      // "Buy" oder "Sell"
    private final String symbol;    // z.B. "AUDNZD"
    private final double lots;
    private final double openPrice;
    private final double closePrice;
    private final double stopLoss;    // Neu
    private final double takeProfit;  // Neu
    
    // Finanzielle Informationen
    private final double commission;
    private final double swap;
    private final double profit;
    
    public Trade(LocalDateTime openTime, LocalDateTime closeTime, 
                String type, String symbol, double lots,
                double openPrice, double closePrice,
                double stopLoss, double takeProfit,
                double commission, double swap, double profit) {
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.type = type;
        this.symbol = symbol;
        this.lots = lots;
        this.openPrice = openPrice;
        this.closePrice = closePrice;
        this.stopLoss = stopLoss;
        this.takeProfit = takeProfit;
        this.commission = commission;
        this.swap = swap;
        this.profit = profit;
    }
    
    // Getter für alle Felder
    public LocalDateTime getOpenTime() { 
        return openTime; 
    }
    
    public LocalDateTime getCloseTime() { 
        return closeTime; 
    }
    
    public String getType() {
        return type;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public double getLots() {
        return lots;
    }
    
    public double getOpenPrice() {
        return openPrice;
    }
    
    public double getClosePrice() {
        return closePrice;
    }
    
    public double getStopLoss() {
        return stopLoss;
    }
    
    public double getTakeProfit() {
        return takeProfit;
    }
    
    public double getCommission() {
        return commission;
    }
    
    public double getSwap() {
        return swap;
    }
    
    public double getProfit() {
        return profit;
    }
    
    // Berechnet den Gesamtprofit inklusive Kommission und Swap
    public double getTotalProfit() {
        return profit;// + commission + swap;  // Commission und Swap sind normalerweise negativ
    }

    @Override
    public String toString() {
        return "Trade{" +
            "openTime=" + openTime +
            ", closeTime=" + closeTime +
            ", type='" + type + '\'' +
            ", symbol='" + symbol + '\'' +
            ", lots=" + lots +
            ", openPrice=" + openPrice +
            ", closePrice=" + closePrice +
            ", stopLoss=" + stopLoss +
            ", takeProfit=" + takeProfit +
            ", commission=" + commission +
            ", swap=" + swap +
            ", profit=" + profit +
            '}';
    }
}