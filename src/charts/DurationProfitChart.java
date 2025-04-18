package charts;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.time.Duration;
import java.util.List;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import data.Trade;

public class DurationProfitChart extends JPanel {
    
    public DurationProfitChart(List<Trade> trades) {
        XYSeries winnerSeries = new XYSeries("Winners");
        XYSeries loserSeries = new XYSeries("Losers");
        
        for (Trade trade : trades) {
            double durationInHours = convertToHours(trade);
            double profit = trade.getProfit();
            
            if (profit >= 0) {
                winnerSeries.add(durationInHours, profit);
            } else {
                loserSeries.add(durationInHours, profit);
            }
        }
        
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(winnerSeries);
        dataset.addSeries(loserSeries);
        
        JFreeChart chart = ChartFactory.createScatterPlot(
            "Duration (Profit)",
            "Duration (hours)",
            "Profit (€)",
            dataset
        );
        
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // Verbessere die Achsenbeschriftung
        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        domainAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 14));
        domainAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 14));
        rangeAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        
        // Titel größer machen
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 16));
        
        // Verbessere die Punktdarstellung
        XYItemRenderer renderer = plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(34, 177, 76));  // Dunkleres Grün für Gewinner
        renderer.setSeriesPaint(1, new Color(237, 28, 36));  // Kräftigeres Rot für Verlierer
        renderer.setSeriesShape(0, new java.awt.geom.Ellipse2D.Double(-4, -4, 8, 8));
        renderer.setSeriesShape(1, new java.awt.geom.Ellipse2D.Double(-4, -4, 8, 8));
        
        ChartPanel chartPanel = new ChartPanel(chart);
        // Größer machen: 1300 -> 1500 Breite, 250 -> 400 Höhe
        chartPanel.setPreferredSize(new Dimension(1500, 400));
        
        add(chartPanel);
    }
    
    private double convertToHours(Trade trade) {
        long minutes = Duration.between(trade.getOpenTime(), trade.getCloseTime()).toMinutes();
        return minutes / 60.0; // Konvertiere zu Stunden
    }
}