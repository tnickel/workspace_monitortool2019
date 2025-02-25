package services;

import data.Trade;
import data.ProviderStats;
import java.util.*;
import java.time.LocalDateTime;

public class RiskAnalysisServ {
    public static int calculateRiskScore(ProviderStats stats) {
        List<Trade> trades = stats.getTrades();
        double score = 0;
        
        // 1. Martingale Detection (25%)
        double martingaleScore = analyzeMartingalePattern(trades);
        score += martingaleScore * 0.25;
        
        // 2. Open Equity Risk (25%) - NEU
        double openEquityScore = calculateOpenEquityRisk(trades);
        score += openEquityScore * 0.25;
        
        // 3. Concurrent Trading Risk (20%) - Angepasst mit maximalen Lots
        double concurrentRiskScore = analyzeConcurrentRisk(trades);
        score += concurrentRiskScore * 0.20;
        
        // 4. Drawdown Risk (15%) - Gewichtung reduziert
        double drawdownScore = Math.min(100, stats.getMaxDrawdown() * 100);
        score += drawdownScore * 0.15;
        
        // 5. Profit Factor Stability (15%) - Inverse relationship
        double profitFactorScore = Math.max(0, Math.min(100, (1 - stats.getProfitFactor()) * 100));
        score += profitFactorScore * 0.15;
        
        return Math.max(1, Math.min(100, (int)Math.round(score)));
    }
    
    private static double calculateOpenEquityRisk(List<Trade> trades) {
        // Map für das Tracking der offenen Lots pro Zeitpunkt
        TreeMap<LocalDateTime, Double> openLotsAtTime = new TreeMap<>();
        
        // Erfasse alle Lot-Änderungen über die Zeit
        for (Trade trade : trades) {
            openLotsAtTime.merge(trade.getOpenTime(), trade.getLots(), Double::sum);
            openLotsAtTime.merge(trade.getCloseTime(), -trade.getLots(), Double::sum);
        }
        
        // Berechne kumulierte Lots für jeden Zeitpunkt
        double maxOpenLots = 0;
        double runningLots = 0;
        
        for (Double lotChange : openLotsAtTime.values()) {
            runningLots += lotChange;
            maxOpenLots = Math.max(maxOpenLots, runningLots);
        }
        
        // Berechne Risikoscore basierend auf maximalen offenen Lots
        double riskScore = 0;
        
        if (maxOpenLots > 15) riskScore = 100;
        else if (maxOpenLots > 12) riskScore = 80;
        else if (maxOpenLots > 8) riskScore = 60;
        else if (maxOpenLots > 4) riskScore = 40;
        else riskScore = maxOpenLots * 10;
        
        return riskScore;
    }
    
    private static double analyzeConcurrentRisk(List<Trade> trades) {
        if (trades.isEmpty()) return 0;
        
        // Map für das Tracking der offenen Lots pro Zeitpunkt
        TreeMap<LocalDateTime, Double> lotsAtTime = new TreeMap<>();
        
        // Erfasse Lot-Änderungen und deren Werte
        for (Trade trade : trades) {
            lotsAtTime.merge(trade.getOpenTime(), trade.getLots(), Double::sum);
            lotsAtTime.merge(trade.getCloseTime(), -trade.getLots(), Double::sum);
        }
        
        // Berechne maximale gleichzeitige Lots
        double maxLots = 0;
        double runningLots = 0;
        
        for (Double lotChange : lotsAtTime.values()) {
            runningLots += lotChange;
            maxLots = Math.max(maxLots, runningLots);
        }
        
        // Berechne Risikoscore basierend auf maximalen Lots und Anzahl Trades
        double lotRiskScore = 0;
        if (maxLots > 15) lotRiskScore = 100;
        else if (maxLots > 12) lotRiskScore = 80;
        else if (maxLots > 8) lotRiskScore = 60;
        else if (maxLots > 4) lotRiskScore = 40;
        else lotRiskScore = maxLots * 10;
        
        // Kombiniere mit Anzahl gleichzeitiger Trades
        return lotRiskScore;
    }
    
    private static double analyzeMartingalePattern(List<Trade> trades) {
        if (trades.isEmpty()) return 0;
        
        List<Trade> sortedTrades = new ArrayList<>(trades);
        sortedTrades.sort((t1, t2) -> t1.getOpenTime().compareTo(t2.getOpenTime()));
        
        double maxLotIncrease = 0;
        int consecutiveLossesWithIncrease = 0;
        double maxConsecutiveLots = 0;
        
        for (int i = 1; i < sortedTrades.size(); i++) {
            Trade prevTrade = sortedTrades.get(i-1);
            Trade currentTrade = sortedTrades.get(i);
            
            if (prevTrade.getProfit() < 0) {
                double lotIncrease = currentTrade.getLots() / prevTrade.getLots();
                maxLotIncrease = Math.max(maxLotIncrease, lotIncrease);
                maxConsecutiveLots = Math.max(maxConsecutiveLots, currentTrade.getLots());
                
                if (lotIncrease > 1.5) {
                    consecutiveLossesWithIncrease++;
                }
            }
        }
        
        // Kombinierte Bewertung aus:
        // - Maximaler Lot-Erhöhung
        // - Anzahl aufeinanderfolgender Verluste mit Erhöhung
        // - Maximale Lot-Größe in einer Verlustsequenz
        double baseScore = Math.min(100, (maxLotIncrease * 20) + (consecutiveLossesWithIncrease * 5));
        double lotSizeScore = Math.min(100, (maxConsecutiveLots / 0.1) * 10); // 0.1 Lot als Basis
        
        return Math.max(baseScore, lotSizeScore);
    }
    
    public static String getRiskCategory(int score) {
        if (score <= 20) return "Konservativ";
        if (score <= 40) return "Moderat";
        if (score <= 60) return "Medium-Risiko";
        if (score <= 80) return "Hoch-Risiko";
        return "Extrem-Risiko";
    }
}