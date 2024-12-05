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
       this.maxConcurrentLots = 0;
       this.isDirty = false;
   }
   
   public void addTrade(Trade trade) {
       trades.add(trade);
       isDirty = true;
   }
   
   private void updateMaxConcurrentTrades() {
       if (!isDirty) return;
       
       trades.sort((t1, t2) -> t1.getOpenTime().compareTo(t2.getOpenTime()));
       List<Trade> activeTrades = new ArrayList<>();
       
       for (Trade currentTrade : trades) {
           // Entferne geschlossene Trades
           activeTrades.removeIf(t -> t.getCloseTime().compareTo(currentTrade.getOpenTime()) <= 0);
           
           // Füge neuen Trade hinzu
           activeTrades.add(currentTrade);
           
           // Update Maxima
           int currentTradeCount = activeTrades.size();
           double currentLotsCount = activeTrades.stream().mapToDouble(Trade::getLots).sum();
           
           maxConcurrentTrades = Math.max(maxConcurrentTrades, currentTradeCount);
           maxConcurrentLots = Math.max(maxConcurrentLots, currentLotsCount);
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

   public static int calculateOpenTradesAt(List<Trade> allTrades, LocalDateTime checkTime) {
       List<Trade> activeTrades = new ArrayList<>();
       for (Trade trade : allTrades) {
           if (trade.getOpenTime().compareTo(checkTime) <= 0) {
               activeTrades.removeIf(t -> t.getCloseTime().compareTo(checkTime) <= 0);
               activeTrades.add(trade);
           }
       }
       return activeTrades.size();
   }

   public static double calculateOpenLotsAt(List<Trade> allTrades, LocalDateTime checkTime) {
       List<Trade> activeTrades = new ArrayList<>();
       for (Trade trade : allTrades) {
           if (trade.getOpenTime().compareTo(checkTime) <= 0) {
               activeTrades.removeIf(t -> t.getCloseTime().compareTo(checkTime) <= 0);
               activeTrades.add(trade);
           }
       }
       return activeTrades.stream().mapToDouble(Trade::getLots).sum();
   }
}