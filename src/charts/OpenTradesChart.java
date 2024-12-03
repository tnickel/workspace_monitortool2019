package charts;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
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
            "Verlauf Der Gleichzeitig Geöffneten Trades Über Die Zeit",
            "Zeit",
            "Anzahl geöffneter Trades",
            dataset,
            true,   // Legend
            true,   // Tooltips
            false   // URLs
        );
        
        // Customize the chart
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // Customize the renderer
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setDefaultShapesVisible(false);
        plot.setRenderer(renderer);
        
        // Customize the domain axis
        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new java.text.SimpleDateFormat("yyyy-MM"));
        
        // Create the chart panel
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 400));
        
        setLayout(new BorderLayout());
        add(chartPanel, BorderLayout.CENTER);
    }
    
    public void addProvider(String providerName, List<Trade> trades) {
        TimeSeries series = new TimeSeries(providerName);
        
        // Erstelle eine sortierte Liste aller Zeitpunkte
        TreeMap<LocalDateTime, Integer> changes = new TreeMap<>();
        
        // Füge alle Öffnungs- und Schließzeitpunkte hinzu
        for (Trade trade : trades) {
            changes.merge(trade.getOpenTime(), 1, Integer::sum);
            changes.merge(trade.getCloseTime(), -1, Integer::sum);
        }
        
        // Berechne den kumulativen Verlauf
        int currentOpen = 0;
        for (Map.Entry<LocalDateTime, Integer> entry : changes.entrySet()) {
            currentOpen += entry.getValue();
            series.add(
                new Millisecond(
                    Date.from(entry.getKey().atZone(ZoneId.systemDefault()).toInstant())
                ),
                currentOpen
            );
        }
        
        dataset.addSeries(series);
    }
    
    public void clear() {
        dataset.removeAllSeries();
    }
}