package charts;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

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
    
    public DrawdownChart(List<Trade> trades) {
        setLayout(new BorderLayout());
        dataset = new TimeSeriesCollection();
        
        // Drawdown Series erstellen
        TimeSeries series = new TimeSeries("Drawdown %");
        
        // Trades nach Schließzeit sortieren
        trades.sort((t1, t2) -> t1.getCloseTime().compareTo(t2.getCloseTime()));
        
        double peak = 0.0;
        double currentBalance = 0.0;
        
        for (Trade trade : trades) {
            currentBalance += trade.getTotalProfit();
            peak = Math.max(currentBalance, peak);
            
            // Drawdown in Prozent berechnen
            double drawdown = 0.0;
            if (peak > 0) {
                drawdown = ((peak - currentBalance) / peak) * 100.0;
            }
            
            // Datum konvertieren und Drawdown hinzufügen
            series.addOrUpdate(
                new Day(Date.from(trade.getCloseTime()
                    .atZone(ZoneId.systemDefault())
                    .toInstant())),
                drawdown
            );
        }
        
        dataset.addSeries(series);
        
        // Chart erstellen
        chart = ChartFactory.createTimeSeriesChart(
            "Drawdown Over Time",  // Titel
            "Time",               // x-Achse Label
            "Drawdown %",        // y-Achse Label
            dataset,            // Daten
            true,              // Legende anzeigen
            true,              // Tooltips
            false             // URLs
        );
        
        customizeChart();
        
        // ChartPanel erstellen und hinzufügen
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(950, 300));
        add(chartPanel, BorderLayout.CENTER);
    }
    
    private void customizeChart() {
        chart.setBackgroundPaint(Color.WHITE);
        
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // Renderer für die Linie anpassen
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.RED);  // Rote Linie für Drawdown
        renderer.setSeriesShapesVisible(0, false);  // Keine Punkte auf der Linie
        plot.setRenderer(renderer);
        
        // X-Achse (Zeit) anpassen
        DateAxis dateAxis = (DateAxis) plot.getDomainAxis();
        dateAxis.setDateFormatOverride(new SimpleDateFormat("yyyy-MM"));
        dateAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        dateAxis.setLabelAngle(Math.PI / 2.0);
        
        // Y-Achse anpassen
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setAutoRangeIncludesZero(true);
        
        // Titel-Font anpassen
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 14));
    }
    
    public JFreeChart getChart() {
        return chart;
    }
    
    public void setTitle(String title) {
        chart.setTitle(title);
    }
}