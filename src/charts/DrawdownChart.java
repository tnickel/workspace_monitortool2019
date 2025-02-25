package charts;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
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
        
        List<Trade> sortedTrades = new ArrayList<>(trades);
        sortedTrades.sort(Comparator.comparing(Trade::getCloseTime));
        
        TreeMap<LocalDateTime, Double> openLotsAtTime = new TreeMap<>();
        
        for (Trade trade : sortedTrades) {
            LocalDateTime openTime = trade.getOpenTime();
            LocalDateTime closeTime = trade.getCloseTime();
            double lots = trade.getLots();
            
            openLotsAtTime.merge(openTime, lots, Double::sum);
            openLotsAtTime.merge(closeTime, -lots, Double::sum);
        }
        
        double runningLots = 0;
        TreeMap<LocalDateTime, Double> cumulativeLotsAtTime = new TreeMap<>();
        for (Map.Entry<LocalDateTime, Double> entry : openLotsAtTime.entrySet()) {
            runningLots += entry.getValue();
            cumulativeLotsAtTime.put(entry.getKey(), runningLots);
        }
        
        double avgLossPerLot = calculateAvgLossPerLot(sortedTrades);
        
        for (Trade trade : sortedTrades) {
            double previousBalance = currentBalance;
            currentBalance += trade.getTotalProfit();
            
            double drawdown = calculateDrawdownPercentage(currentBalance, peak);
            peak = Math.max(currentBalance, peak);
            
            LocalDateTime currentTime = trade.getCloseTime();
            Map.Entry<LocalDateTime, Double> lotsEntry = cumulativeLotsAtTime.floorEntry(currentTime);
            double currentOpenLots = lotsEntry != null ? lotsEntry.getValue() : 0;
            
            double potentialLoss = estimatePotentialLoss(currentOpenLots, avgLossPerLot, currentBalance);
            double potentialDrawdown = calculateDrawdownPercentage(currentBalance - potentialLoss, peak);
            
            Date tradeDate = Date.from(currentTime
                .atZone(ZoneId.systemDefault())
                .toInstant());
            
            realizedSeries.addOrUpdate(new Day(tradeDate), drawdown);
            openRiskSeries.addOrUpdate(new Day(tradeDate), Math.max(drawdown, potentialDrawdown));
        }
        
        dataset.addSeries(realizedSeries);
        dataset.addSeries(openRiskSeries);
        
        chart = ChartFactory.createTimeSeriesChart(
            "Risk Exposure Over Time",  
            "Time",               
            "Risk %",        
            dataset,            
            true,              
            true,              
            false             
        );
        
        customizeChart();
        
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(950, 300));
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.setMouseZoomable(true);
        add(chartPanel, BorderLayout.CENTER);
    }
    
    private double calculateDrawdownPercentage(double current, double peakValue) {
        if (peakValue <= 0) return 0;
        return Math.min(100, Math.max(0, ((peakValue - current) / peakValue) * 100.0));
    }
    
    private double estimatePotentialLoss(double openLots, double avgLossPerLot, double balance) {
        double worstCaseLossPerLot = 800;
        double baseLossEstimate = openLots * worstCaseLossPerLot;
        
        double riskMultiplier = 1.0;
        if (openLots > 4) riskMultiplier = 1.5;
        if (openLots > 8) riskMultiplier = 2.0;
        if (openLots > 12) riskMultiplier = 3.0;
        if (openLots > 15) riskMultiplier = 4.0;
        
        double estimatedLoss = baseLossEstimate * riskMultiplier;
        
        if (openLots > 10) {
            estimatedLoss *= (1 + (openLots - 10) * 0.1);
        }
        
        return estimatedLoss;
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
        
        if (totalLossLots == 0 || lossCount < 5) {
            return 500;
        }
        
        return (totalLoss / totalLossLots) * 1.5;
    }
    
    private void customizeChart() {
        chart.setBackgroundPaint(Color.WHITE);
        
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        
        renderer.setSeriesPaint(0, new Color(0, 0, 220));
        renderer.setSeriesShapesVisible(0, false);
        renderer.setSeriesStroke(0, new java.awt.BasicStroke(1.5f));
        
        renderer.setSeriesPaint(1, new Color(220, 0, 0));
        renderer.setSeriesShapesVisible(1, false);
        renderer.setSeriesStroke(1, new java.awt.BasicStroke(1.5f, 
            java.awt.BasicStroke.CAP_BUTT, 
            java.awt.BasicStroke.JOIN_MITER, 
            10.0f, new float[]{6.0f, 4.0f}, 0.0f));
        
        plot.setRenderer(renderer);
        
        DateAxis dateAxis = (DateAxis) plot.getDomainAxis();
        dateAxis.setDateFormatOverride(new SimpleDateFormat("yyyy-MM"));
        dateAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        dateAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 12));
        
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setAutoRangeIncludesZero(true);
        rangeAxis.setUpperBound(100.0);
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