package utils;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import data.Trade;
/* Erkennung eines Martingale-Systems
Ein Martingale-System zeichnet sich typischerweise durch folgende Merkmale aus:
1.	Erhöhung der Positionsgröße nach Verlusten: Die Lots werden nach einem Verlust erhöht, oft verdoppelt.
2.	Gleiche Handelsrichtung nach Verlusten: Nach einem Verlust wird in dieselbe Richtung (Buy/Sell) erneut gehandelt.
3.	Reduzierung auf Normalgröße nach Gewinnen: Nach einem erfolgreichen Trade wird die Positionsgröße wieder auf den Ausgangswert zurückgesetzt.
4.	Kurze Zeitabstände zwischen Verlust-Trades: Verlusttrades werden oft schnell nacheinander eröffnet.

*/
public class MartingaleAnalyzer {
    private final List<Trade> trades;
    
    public MartingaleAnalyzer(List<Trade> trades) {
        this.trades = new ArrayList<Trade>(trades);
        // Sortiere nach Eröffnungszeit
        Collections.sort(this.trades, new Comparator<Trade>() {
            @Override
            public int compare(Trade t1, Trade t2) {
                return t1.getOpenTime().compareTo(t2.getOpenTime());
            }
        });
    }
    
    /**
     * Analysiert Trades auf Martingale-Muster
     * @return Map mit Symbol als Schlüssel und Liste von Martingale-Sequenzen als Wert
     */
    public Map<String, List<MartingaleSequence>> findMartingaleSequences() {
        Map<String, List<MartingaleSequence>> result = new HashMap<>();
        
        // Gruppiere nach Symbol
        Map<String, List<Trade>> tradesBySymbol = trades.stream()
            .collect(Collectors.groupingBy(Trade::getSymbol));
            
        for (Map.Entry<String, List<Trade>> entry : tradesBySymbol.entrySet()) {
            String symbol = entry.getKey();
            List<Trade> symbolTrades = entry.getValue();
            
            List<MartingaleSequence> sequences = new ArrayList<>();
            MartingaleSequence currentSequence = null;
            
            for (int i = 0; i < symbolTrades.size(); i++) {
                Trade currentTrade = symbolTrades.get(i);
                
                // Wenn dies der letzte Trade ist oder der nächste Trade zu weit entfernt ist
                if (i == symbolTrades.size() - 1) {
                    if (currentSequence != null && currentSequence.getTrades().size() > 1) {
                        sequences.add(currentSequence);
                    }
                    break;
                }
                
                Trade nextTrade = symbolTrades.get(i + 1);
                
                // Prüfe Zeit zwischen Trades (max. 24 Stunden)
                long minutesBetween = java.time.Duration.between(
                    currentTrade.getCloseTime(), nextTrade.getOpenTime()).toMinutes();
                
                if (minutesBetween > 1440) { // 24 Stunden
                    if (currentSequence != null && currentSequence.getTrades().size() > 1) {
                        sequences.add(currentSequence);
                    }
                    currentSequence = null;
                    continue;
                }
                
                // Prüfe auf Martingale-Muster
                if (currentTrade.getProfit() < 0 && 
                    currentTrade.getType().equals(nextTrade.getType()) &&
                    nextTrade.getLots() > currentTrade.getLots() * 1.5) {
                    
                    if (currentSequence == null) {
                        currentSequence = new MartingaleSequence();
                        currentSequence.addTrade(currentTrade);
                    }
                    currentSequence.addTrade(nextTrade);
                } else {
                    if (currentSequence != null && currentSequence.getTrades().size() > 1) {
                        sequences.add(currentSequence);
                    }
                    currentSequence = null;
                }
            }
            
            if (!sequences.isEmpty()) {
                result.put(symbol, sequences);
            }
        }
        
        return result;
    }
    
    /**
     * Berechnet einen Martingale-Score zwischen 0 und 100
     * Je höher, desto wahrscheinlicher ist das Verwenden eines Martingale-Systems
     */
    public double calculateMartingaleScore() {
        Map<String, List<MartingaleSequence>> sequences = findMartingaleSequences();
        
        if (sequences.isEmpty()) {
            return 0.0;
        }
        
        int totalMartingaleTrades = 0;
        int maxSequenceLength = 0;
        
        for (List<MartingaleSequence> symbolSequences : sequences.values()) {
            for (MartingaleSequence sequence : symbolSequences) {
                totalMartingaleTrades += sequence.getTrades().size();
                maxSequenceLength = Math.max(maxSequenceLength, sequence.getTrades().size());
            }
        }
        
        // Gewichtung: 70% basierend auf dem Anteil der Martingale-Trades, 30% auf der maximalen Sequenzlänge
        double percentageMartingaleTrades = (double) totalMartingaleTrades / trades.size() * 100;
        double sequenceFactor = Math.min(100.0, maxSequenceLength * 20.0); // Maximal 100 bei 5er-Sequenz
        
        return 0.7 * percentageMartingaleTrades + 0.3 * sequenceFactor;
    }
    
    /**
     * Klasse zur Repräsentation einer Martingale-Sequenz
     */
    public static class MartingaleSequence {
        private final List<Trade> trades = new ArrayList<>();
        
        public void addTrade(Trade trade) {
            trades.add(trade);
        }
        
        public List<Trade> getTrades() {
            return trades;
        }
        
        public double getInitialLots() {
            if (trades.isEmpty()) return 0;
            return trades.get(0).getLots();
        }
        
        public double getMaxLots() {
            return trades.stream()
                .mapToDouble(Trade::getLots)
                .max().orElse(0);
        }
        
        public double getTotalProfit() {
            return trades.stream()
                .mapToDouble(Trade::getTotalProfit)
                .sum();
        }
    }
}