
package charts;

import java.awt.Color;
import java.awt.Dimension;
import java.time.Duration;
import java.util.List;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
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
        
        XYItemRenderer renderer = plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(130, 202, 157));
        renderer.setSeriesPaint(1, new Color(255, 118, 117));
        
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(1300, 250));
        
        add(chartPanel);
    }
    
    private double convertToHours(Trade trade) {
        long minutes = Duration.between(trade.getOpenTime(), trade.getCloseTime()).toMinutes();
        return minutes / 60.0; // Konvertiere zu Stunden
    }
}
