package services;



import data.Trade;
import data.ProviderStats;
import java.util.List;
import java.util.ArrayList;

public class RiskAnalysisServ {
    public static int calculateRiskScore(ProviderStats stats) {
        List<Trade> trades = stats.getTrades();
        double score = 0;
        
        // 1. Martingale Detection (35%)
        double martingaleScore = analyzeMartingalePattern(trades);
        score += martingaleScore * 0.35;
        
        // 2. Drawdown Risk (20%)
        double drawdownScore = Math.min(100, stats.getMaxDrawdown() * 100);
        score += drawdownScore * 0.20;
        
        // 3. Concurrent Trading Risk (15%)
        double concurrentScore = Math.min(100, (stats.getMaxConcurrentTrades() / 2.5) * 10);
        score += concurrentScore * 0.15;
        
        // 4. Lot Size Volatility (15%)
        double lotVolatilityScore = analyzeLotVolatility(trades);
        score += lotVolatilityScore * 0.15;
        
        // 5. Profit Factor Stability (15%) - Inverse relationship
        double profitFactorScore = Math.max(0, Math.min(100, (1 - stats.getProfitFactor()) * 100));
        score += profitFactorScore * 0.15;
        
        return Math.max(1, Math.min(100, (int)Math.round(score)));
    }
    
    private static double analyzeMartingalePattern(List<Trade> trades) {
        if (trades.isEmpty()) return 0;
        
        double maxLotIncrease = 0;
        int consecutiveLossesWithIncrease = 0;
        
        List<Trade> sortedTrades = new ArrayList<>(trades);
        sortedTrades.sort((t1, t2) -> t1.getOpenTime().compareTo(t2.getOpenTime()));
        
        for (int i = 1; i < sortedTrades.size(); i++) {
            Trade prevTrade = sortedTrades.get(i-1);
            Trade currentTrade = sortedTrades.get(i);
            
            if (prevTrade.getProfit() < 0) {
                double lotIncrease = currentTrade.getLots() / prevTrade.getLots();
                maxLotIncrease = Math.max(maxLotIncrease, lotIncrease);
                
                if (lotIncrease > 1.5) {
                    consecutiveLossesWithIncrease++;
                }
            }
        }
        
        return Math.min(100, (maxLotIncrease * 20) + (consecutiveLossesWithIncrease * 5));
    }
    
    private static double analyzeLotVolatility(List<Trade> trades) {
        if (trades.isEmpty()) return 0;
        
        double avgLot = trades.stream()
            .mapToDouble(Trade::getLots)
            .average()
            .orElse(0);
            
        double maxDev = trades.stream()
            .mapToDouble(t -> Math.abs(t.getLots() - avgLot))
            .max()
            .orElse(0);
            
        return Math.min(100, (maxDev / avgLot) * 100);
    }
    
    public static String getRiskCategory(int score) {
        if (score <= 20) return "Konservativ";
        if (score <= 40) return "Moderat";
        if (score <= 60) return "Medium-Risiko";
        if (score <= 80) return "Hoch-Risiko";
        return "Extrem-Risiko";
    }
}