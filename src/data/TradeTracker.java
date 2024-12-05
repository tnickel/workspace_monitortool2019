package data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TradeTracker {
   private final List<Trade> trades;
   private int maxConcurrentTrades;  
   private double maxConcurrentLots;
   private boolean isDirty;
   
   public TradeTracker() {
       this.trades = new ArrayList<>();
       this.maxConcurrentTrades = 0;
       this.maxConcurrentLots = 0.0;
       this.isDirty = false;
   }
   
   public void addTrade(Trade trade) {
       trades.add(trade);
       isDirty = true;
   }
   
   private void updateMaxConcurrentTrades() {
       if (!isDirty) return;
       
       trades.sort((t1, t2) -> t1.getCloseTime().compareTo(t2.getCloseTime()));
       
       for (Trade currentTrade : trades) {
           LocalDateTime checkTime = currentTrade.getCloseTime().minusSeconds(1);
           int currentTrades = 0;
           double currentLots = 0.0;
           
           for (Trade trade : trades) {
               if (trade.getOpenTime().compareTo(checkTime) <= 0 && 
                   trade.getCloseTime().compareTo(checkTime) >= 0) {
                   currentTrades++;
                   currentLots += trade.getLots();
               }
           }
           
           maxConcurrentTrades = Math.max(maxConcurrentTrades, currentTrades);
           maxConcurrentLots = Math.max(maxConcurrentLots, currentLots);
       }
       
       isDirty = false;
   }
   
   public int getMaxConcurrentTrades() {
       updateMaxConcurrentTrades();
       return maxConcurrentTrades;
   }

   public double getMaxConcurrentLots() {
       updateMaxConcurrentTrades();
       return maxConcurrentLots;
   }

   public static int calculateOpenTradesAt(List<Trade> allTrades, LocalDateTime closeTime) {
       LocalDateTime checkTime = closeTime.minusSeconds(1);
       int openTradesCount = 0;
       for (Trade trade : allTrades) {
           if (trade.getOpenTime().compareTo(checkTime) <= 0 && 
               trade.getCloseTime().compareTo(checkTime) >= 0) {
               openTradesCount++;
           }
       }
       return openTradesCount;
   }

   public static double calculateOpenLotsAt(List<Trade> allTrades, LocalDateTime closeTime) {
       LocalDateTime checkTime = closeTime.minusSeconds(1);
       double openLotsCount = 0.0;
       for (Trade trade : allTrades) {
           if (trade.getOpenTime().compareTo(checkTime) <= 0 && 
               trade.getCloseTime().compareTo(checkTime) >= 0) {
               openLotsCount += trade.getLots();
           }
       }
       return openLotsCount;
   }

    
 
}