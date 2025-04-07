package utils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import data.Trade;

public class TradeCalculator {
    public static class OpenTradeStats {
        public final int openTradesCount;
        public final double openLotsCount;
        
        public OpenTradeStats(int trades, double lots) {
            this.openTradesCount = trades;
            this.openLotsCount = lots;
        }
    }
    
    public static OpenTradeStats calculateOpenTradesAt(List<Trade> allTrades, LocalDateTime timePoint) {
        int openTradesCount = 0;
        double openLotsCount = 0.0;
        List<Trade> activeTrades = new ArrayList<>();
        
        for (Trade trade : allTrades) {
            // Pr�fe ob Trade zum Zeitpunkt aktiv war
            if (trade.getOpenTime().compareTo(timePoint) <= 0 && 
                trade.getCloseTime().compareTo(timePoint) > 0) {
                activeTrades.add(trade);
                openTradesCount++;
                openLotsCount += trade.getLots();
            }
        }
        
        return new OpenTradeStats(openTradesCount, openLotsCount);
    }
}