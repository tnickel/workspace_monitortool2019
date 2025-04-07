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
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import data.Trade;

public class OpenTradesChart extends JPanel {
    private final JFreeChart chart;
    private final TimeSeriesCollection dataset;
    
    public OpenTradesChart() {
        dataset = new TimeSeriesCollection();
        
        chart = ChartFactory.createTimeSeriesChart(
            null,  // Kein Titel, wird sp�ter gesetzt
            "Zeit",
            "Anzahl",
            dataset,
            true,
            true,
            false
        );
        
        // Plot-Grundeinstellungen
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // Renderer anpassen
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setDefaultShapesVisible(false);
        plot.setRenderer(renderer);
        
        // Datumsachse (X-Achse) anpassen
        DateAxis dateAxis = (DateAxis) plot.getDomainAxis();
        dateAxis.setDateFormatOverride(new SimpleDateFormat("yyyy-MM-dd HH:mm"));
        dateAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        dateAxis.setVerticalTickLabels(true);
        dateAxis.setLowerMargin(0.02);
        dateAxis.setUpperMargin(0.02);
        
        // Y-Achse anpassen
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setAutoRangeIncludesZero(true);
        
        // Layout 
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 400));
        
        setLayout(new BorderLayout());
        add(chartPanel, BorderLayout.CENTER);
    }
    
    public void addProvider(String providerName, List<Trade> trades) {
        TimeSeries series = new TimeSeries(providerName);
        
        TreeMap<LocalDateTime, Integer> changes = new TreeMap<>();
        for (Trade trade : trades) {
            changes.merge(trade.getOpenTime(), 1, Integer::sum);
            changes.merge(trade.getCloseTime(), -1, Integer::sum);
        }
        
        int currentOpen = 0;
        for (Map.Entry<LocalDateTime, Integer> entry : changes.entrySet()) {
            currentOpen += entry.getValue();
            series.addOrUpdate(
                new Millisecond(
                    Date.from(entry.getKey().atZone(ZoneId.systemDefault()).toInstant())
                ),
                currentOpen
            );
        }
        
        dataset.addSeries(series);
        chart.setTitle("Verlauf der gleichzeitig ge�ffneten Trades");
    }
    
    public void addLotsProvider(String providerName, List<Trade> trades) {
        TimeSeries series = new TimeSeries(providerName);
        
        TreeMap<LocalDateTime, Double> changes = new TreeMap<>();
        for (Trade trade : trades) {
            changes.merge(trade.getOpenTime(), trade.getLots(), Double::sum);
            changes.merge(trade.getCloseTime(), -trade.getLots(), Double::sum);
        }
        
        double currentLots = 0.0;
        for (Map.Entry<LocalDateTime, Double> entry : changes.entrySet()) {
            currentLots += entry.getValue();
            series.addOrUpdate(
                new Millisecond(
                    Date.from(entry.getKey().atZone(ZoneId.systemDefault()).toInstant())
                ),
                currentLots
            );
        }
        
        dataset.addSeries(series);
        chart.setTitle("Summe der offenen Lots");
    }
    
    public void clear() {
        dataset.removeAllSeries();
    }
    
    public void setTitle(String title) {
        chart.setTitle(title);
    }
}