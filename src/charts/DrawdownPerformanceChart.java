package charts;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import data.ProviderStats;
import data.Trade;

/**
 * Chart-Komponente zur Anzeige der Drawdown-Daten im Zeitverlauf
 */
public class DrawdownPerformanceChart extends JPanel {
    private final JFreeChart chart;
    private final TimeSeriesCollection dataset;
    private final ProviderStats stats;
    private final double maxDrawdownGraphic;
    
    /**
     * Konstruktor für die DrawdownPerformanceChart-Komponente
     * 
     * @param stats ProviderStats-Objekt mit allen Trades
     * @param maxDrawdownGraphic Wert des maximalen Drawdowns aus der HTML-Datenbank
     */
    public DrawdownPerformanceChart(ProviderStats stats, double maxDrawdownGraphic) {
        this.stats = stats;
        this.maxDrawdownGraphic = maxDrawdownGraphic;
        setLayout(new BorderLayout());
        
        // Dataset erstellen
        dataset = new TimeSeriesCollection();
        
        // Chart erstellen
        chart = createChart();
        
        // Chart anpassen und mit Daten füllen
        customizeChart();
        populateChart();
        
        // ChartPanel erstellen und hinzufügen
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(950, 300));
        chartPanel.setMouseWheelEnabled(true);
        
        add(chartPanel, BorderLayout.CENTER);
    }
    
    /**
     * Erstellt das JFreeChart-Objekt für die Drawdown-Darstellung
     */
    private JFreeChart createChart() {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            "Drawdown Performance",
            "Zeit",
            "Drawdown (%)",
            dataset,
            true,  // Legende anzeigen
            true,  // Tooltips anzeigen
            false  // URLs nicht anzeigen
        );
        
        return chart;
    }
    
    /**
     * Passt das Chart-Design an
     */
    private void customizeChart() {
        // Hintergrund anpassen
        chart.setBackgroundPaint(Color.WHITE);
        
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // Renderer für die Drawdown-Linie anpassen
        XYAreaRenderer renderer = new XYAreaRenderer();
        renderer.setSeriesPaint(0, new Color(220, 50, 50, 80)); // Transparentes Rot für die Fläche
        renderer.setSeriesOutlinePaint(0, new Color(180, 0, 0)); // Kräftigeres Rot für die Outline
        renderer.setSeriesOutlineStroke(0, new java.awt.BasicStroke(2.0f));
        plot.setRenderer(renderer);
        
        // X-Achse (Zeit) anpassen
        DateAxis dateAxis = (DateAxis) plot.getDomainAxis();
        dateAxis.setDateFormatOverride(new SimpleDateFormat("yyyy-MM"));
        dateAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        dateAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 12));
        
        // Y-Achse (Drawdown) anpassen
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());
        rangeAxis.setNumberFormatOverride(new DecimalFormat("0.00%"));
        rangeAxis.setUpperBound(maxDrawdownGraphic / 100.0 * 1.1); // 10% mehr als der maximale Drawdown
        rangeAxis.setLowerBound(0.0);
        rangeAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 12));
        rangeAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        
        // Titel anpassen
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 14));
    }
    
    /**
     * Berechnet die Drawdown-Daten und füllt das Chart
     */
    private void populateChart() {
        // Trades sortieren nach Schließungszeit
        List<Trade> sortedTrades = new ArrayList<>(stats.getTrades());
        Collections.sort(sortedTrades, (t1, t2) -> t1.getCloseTime().compareTo(t2.getCloseTime()));
        
        // TimeSeries für den Drawdown erstellen
        TimeSeries drawdownSeries = new TimeSeries("Drawdown");
        
        double currentBalance = stats.getInitialBalance();
        double highWaterMark = currentBalance;
        double currentDrawdownPercentage = 0.0;
        
        for (Trade trade : sortedTrades) {
            // Balance aktualisieren
            currentBalance += trade.getTotalProfit();
            
            // Höchststand aktualisieren, wenn die aktuelle Balance höher ist
            if (currentBalance > highWaterMark) {
                highWaterMark = currentBalance;
                currentDrawdownPercentage = 0.0;
            } else if (highWaterMark > 0) {
                // Drawdown als Prozentsatz berechnen
                currentDrawdownPercentage = (highWaterMark - currentBalance) / highWaterMark;
            }
            
            // Zeitpunkt aus dem Trade holen
            Date date = Date.from(trade.getCloseTime()
                .atZone(ZoneId.systemDefault())
                .toInstant());
            
            // Drawdown zum Dataset hinzufügen (als Dezimalwert, nicht Prozent)
            drawdownSeries.addOrUpdate(new Day(date), currentDrawdownPercentage);
        }
        
        // Serie zum Dataset hinzufügen
        dataset.addSeries(drawdownSeries);
    }
        
     
    
    /**
     * Gibt das JFreeChart-Objekt zurück
     */
    public JFreeChart getChart() {
        return chart;
    }
}