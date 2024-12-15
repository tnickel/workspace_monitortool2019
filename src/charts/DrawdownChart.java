package charts;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import data.Trade;

public class DrawdownChart extends JPanel {
    private final JFreeChart chart;
    private final TimeSeriesCollection dataset;
    private double currentBalance;
    private double peak;
    
    public DrawdownChart(List<Trade> trades, double initialBalance) {
        setLayout(new BorderLayout());
        dataset = new TimeSeriesCollection();
        this.currentBalance = initialBalance;
        this.peak = initialBalance;
        
        TimeSeries realizedSeries = new TimeSeries("Realized Drawdown");
        TimeSeries openRiskSeries = new TimeSeries("Potential Risk");
        
        // Trades nach Schließzeit sortieren für chronologische Verarbeitung
        List<Trade> sortedTrades = new ArrayList<>(trades);
        sortedTrades.sort(Comparator.comparing(Trade::getCloseTime));
        
        // Map für das Tracking der offenen Lots pro Zeitpunkt
        TreeMap<LocalDateTime, Double> openLotsAtTime = new TreeMap<>();
        
        // Erfasse alle Lot-Änderungen über die Zeit
        for (Trade trade : sortedTrades) {
            LocalDateTime openTime = trade.getOpenTime();
            LocalDateTime closeTime = trade.getCloseTime();
            double lots = trade.getLots();
            
            // Lots beim Trade-Öffnen addieren
            openLotsAtTime.merge(openTime, lots, Double::sum);
            // Lots beim Trade-Schließen subtrahieren
            openLotsAtTime.merge(closeTime, -lots, Double::sum);
        }
        
        // Berechne kumulierte Lots für jeden Zeitpunkt
        double runningLots = 0;
        TreeMap<LocalDateTime, Double> cumulativeLotsAtTime = new TreeMap<>();
        for (Map.Entry<LocalDateTime, Double> entry : openLotsAtTime.entrySet()) {
            runningLots += entry.getValue();
            cumulativeLotsAtTime.put(entry.getKey(), runningLots);
        }
        
        // Berechne durchschnittlichen Verlust pro Lot
        double avgLossPerLot = calculateAvgLossPerLot(sortedTrades);
        
        // Verarbeite jeden Trade für die Drawdown-Berechnung
        for (Trade trade : sortedTrades) {
            // Balance und Peak aktualisieren
            currentBalance += trade.getTotalProfit();
            peak = Math.max(currentBalance, peak);
            
            // Realisierten Drawdown berechnen
            double realizedDrawdown = calculateDrawdownPercentage(currentBalance, peak);
            
            // Finde die aktuell offenen Lots zum Zeitpunkt des Trades
            LocalDateTime currentTime = trade.getCloseTime();
            Map.Entry<LocalDateTime, Double> lotsEntry = cumulativeLotsAtTime.floorEntry(currentTime);
            double currentOpenLots = lotsEntry != null ? lotsEntry.getValue() : 0;
            
            // Potentielles Risiko basierend auf offenen Lots berechnen
            double potentialLoss = estimatePotentialLoss(currentOpenLots, avgLossPerLot, currentBalance);
            double potentialDrawdown = calculateDrawdownPercentage(currentBalance - potentialLoss, peak);
            
            // Zeitpunkt für den Chart
            Date tradeDate = Date.from(currentTime
                .atZone(ZoneId.systemDefault())
                .toInstant());
                
            // Datenpunkte zu den Serien hinzufügen
            realizedSeries.addOrUpdate(new Day(tradeDate), realizedDrawdown);
            openRiskSeries.addOrUpdate(new Day(tradeDate), Math.max(realizedDrawdown, potentialDrawdown));
        }
        
        // Serien zum Dataset hinzufügen
        dataset.addSeries(realizedSeries);
        dataset.addSeries(openRiskSeries);
        
        // Chart erstellen
        chart = ChartFactory.createTimeSeriesChart(
            "Risk Exposure Over Time",  
            "Time",               
            "Risk %",        
            dataset,            
            true,              
            true,              
            false             
        );
        
        // Chart anpassen
        customizeChart();
        
        // ChartPanel erstellen und hinzufügen
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(950, 300));
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.setMouseZoomable(true);
        add(chartPanel, BorderLayout.CENTER);
    }
    
    private double calculateAvgLossPerLot(List<Trade> trades) {
        double totalLoss = 0;
        double totalLossLots = 0;
        int lossCount = 0;
        
        for (Trade trade : trades) {
            if (trade.getTotalProfit() < 0) {
                totalLoss += Math.abs(trade.getTotalProfit());
                totalLossLots += trade.getLots();
                lossCount++;
            }
        }
        
        // Wenn keine Verluste, verwende einen konservativen Standardwert
        if (totalLossLots == 0 || lossCount < 5) {
            return 500; // Konservativer Standardwert
        }
        
        return (totalLoss / totalLossLots) * 1.5; // 50% Sicherheitsaufschlag
    }
    
    private double estimatePotentialLoss(double openLots, double avgLossPerLot, double balance) {
        // Konservativere Basis für den Verlust pro Lot (basierend auf realen Maximalverlusten)
        double worstCaseLossPerLot = 800;  // Basierend auf der Analyse der Verlustdaten
        
        // Basis-Risikoberechnung pro Lot
        double baseLossEstimate = openLots * worstCaseLossPerLot;
        
        // Progressiver Risiko-Multiplikator für Margin-Calls und Slippage
        double riskMultiplier = 1.0;
        if (openLots > 4) riskMultiplier = 1.5;      // 50% mehr Risiko über 4 Lots
        if (openLots > 8) riskMultiplier = 2.0;      // 100% mehr Risiko über 8 Lots
        if (openLots > 12) riskMultiplier = 3.0;     // 200% mehr Risiko über 12 Lots
        if (openLots > 15) riskMultiplier = 4.0;     // 300% mehr Risiko über 15 Lots
        
        double estimatedLoss = baseLossEstimate * riskMultiplier;
        
        // Bei sehr hohen Lot-Größen zusätzliches Risiko für Liquiditätsprobleme
        if (openLots > 10) {
            estimatedLoss *= (1 + (openLots - 10) * 0.1);  // 10% extra pro Lot über 10
        }
        
        return estimatedLoss;  // Keine Begrenzung mehr, da wir das reale Risiko zeigen wollen
    }
    
    private double calculateDrawdownPercentage(double current, double peakValue) {
        if (peakValue <= 0) return 0;
        return Math.min(100, Math.max(0, ((peakValue - current) / peakValue) * 100.0));
    }
    
    private void customizeChart() {
        chart.setBackgroundPaint(Color.WHITE);
        
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // Renderer für beide Linien
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        
        // Realized Drawdown: Durchgezogene blaue Linie
        renderer.setSeriesPaint(0, new Color(0, 0, 220));
        renderer.setSeriesShapesVisible(0, false);
        renderer.setSeriesStroke(0, new java.awt.BasicStroke(1.5f));
        
        // Potential Risk: Gestrichelte rote Linie
        renderer.setSeriesPaint(1, new Color(220, 0, 0));
        renderer.setSeriesShapesVisible(1, false);
        renderer.setSeriesStroke(1, new java.awt.BasicStroke(1.5f, 
            java.awt.BasicStroke.CAP_BUTT, 
            java.awt.BasicStroke.JOIN_MITER, 
            10.0f, new float[]{6.0f, 4.0f}, 0.0f));
        
        plot.setRenderer(renderer);
        
        // Zeit-Achse anpassen
        DateAxis dateAxis = (DateAxis) plot.getDomainAxis();
        dateAxis.setDateFormatOverride(new SimpleDateFormat("yyyy-MM"));
        dateAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        dateAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 12));
        
        // Werte-Achse anpassen
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setAutoRangeIncludesZero(true);
        rangeAxis.setUpperBound(100.0);  // Max 100% anzeigen
        rangeAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 12));
        rangeAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
    }
    
    public JFreeChart getChart() {
        return chart;
    }
    
    public void setTitle(String title) {
        chart.setTitle(title);
    }
}