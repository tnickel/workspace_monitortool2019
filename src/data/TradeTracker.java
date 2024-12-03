package data;

import java.time.LocalDateTime;
import java.util.*;

public class TradeTracker {
    private final List<Trade> trades;
    private int maxConcurrentTrades;
    private boolean isDirty;
    
    public TradeTracker() {
        this.trades = new ArrayList<>();
        this.maxConcurrentTrades = 0;
        this.isDirty = false;
    }
    
    public void addTrade(Trade trade) {
        trades.add(trade);
        isDirty = true;
    }
    
    private void updateMaxConcurrentTrades() {
        if (!isDirty) return;
        
        // Verwende PriorityQueue für die Schließzeiten
        PriorityQueue<LocalDateTime> activeTradeEndTimes = new PriorityQueue<>();
        // Sortiere Trades nach Startzeit
        trades.sort((t1, t2) -> t1.getOpenTime().compareTo(t2.getOpenTime()));
        
        int currentTrades = 0;
        
        // Durchlaufe alle Trades in Startzeit-Reihenfolge
        for (Trade trade : trades) {
            // Entferne alle Trades die vor der aktuellen Startzeit beendet wurden
            while (!activeTradeEndTimes.isEmpty() && 
                   activeTradeEndTimes.peek().compareTo(trade.getOpenTime()) <= 0) {
                activeTradeEndTimes.poll();
                currentTrades--;
            }
            
            // Füge den neuen Trade hinzu
            activeTradeEndTimes.offer(trade.getCloseTime());
            currentTrades++;
            
            // Update Maximum
            maxConcurrentTrades = Math.max(maxConcurrentTrades, currentTrades);
        }
        
        isDirty = false;
    }
    
    public int getMaxConcurrentTrades() {
        updateMaxConcurrentTrades();
        return maxConcurrentTrades;
    }
}