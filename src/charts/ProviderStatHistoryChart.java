package charts;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
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

import db.HistoryDatabaseManager.HistoryEntry;
import services.ProviderHistoryService;

/**
 * Chart-Komponente zur Anzeige der Statistik-Historie für Signal Provider
 */
public class ProviderStatHistoryChart extends JPanel {
    private final JFreeChart chart;
    private final TimeSeriesCollection dataset;
    private final ProviderHistoryService historyService;
    private String currentProvider;
    private String currentStatType;
    private String chartTitle;
    private String yAxisLabel;
    
    /**
     * Konstruktor für ein allgemeines Statistik-Chart
     * 
     * @param title Der Titel des Charts
     * @param yLabel Die Beschriftung der Y-Achse
     */
    public ProviderStatHistoryChart(String title, String yLabel) {
        setLayout(new BorderLayout());
        
        this.historyService = ProviderHistoryService.getInstance();
        this.dataset = new TimeSeriesCollection();
        this.chartTitle = title;
        this.yAxisLabel = yLabel;
        
        // Chart erstellen
        chart = ChartFactory.createTimeSeriesChart(
            chartTitle,
            "Datum",
            yAxisLabel,
            dataset,
            true,
            true,
            false
        );
        
        // Chart anpassen
        customizeChart();
        
        // ChartPanel hinzufügen
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 300));
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.setMouseZoomable(true);
        
        add(chartPanel, BorderLayout.CENTER);
    }
    
    /**
     * Spezifischer Konstruktor für 3MPDD-Chart
     */
    public ProviderStatHistoryChart() {
        this("3MPDD-Verlauf", "3MPDD-Wert");
        this.currentStatType = ProviderHistoryService.STAT_TYPE_3MPDD;
    }
    
    /**
     * Lädt und zeigt die Historie eines statistischen Werts für einen Provider
     * 
     * @param providerName Signal Provider Name
     * @param statType Art des statistischen Werts
     */
    public void loadProviderHistory(String providerName, String statType) {
        this.currentProvider = providerName;
        this.currentStatType = statType;
        
        // Dataset leeren
        dataset.removeAllSeries();
        
        // Neue Serie für den Provider erstellen
        TimeSeries series = new TimeSeries(providerName);
        
        // Historie abrufen
        List<HistoryEntry> history = historyService.getStatHistory(providerName, statType);
        
        // Serie befüllen
        for (HistoryEntry entry : history) {
            Date date = Date.from(entry.getDate().atZone(ZoneId.systemDefault()).toInstant());
            series.addOrUpdate(new Day(date), entry.getValue());
        }
        
        // Serie zum Dataset hinzufügen
        dataset.addSeries(series);
        
        // Chart-Titel aktualisieren
        chart.setTitle(chartTitle + " für " + providerName);
    }
    
    /**
     * Lädt und zeigt die 3MPDD-Historie für einen Provider
     * 
     * @param providerName Signal Provider Name
     */
    public void loadProviderHistory(String providerName) {
        loadProviderHistory(providerName, ProviderHistoryService.STAT_TYPE_3MPDD);
    }
    
    /**
     * Passt das Aussehen des Charts an
     */
    private void customizeChart() {
        chart.setBackgroundPaint(Color.WHITE);
        
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // Renderer für Linien und Punkte
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, new Color(0, 0, 220));
        renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesStroke(0, new java.awt.BasicStroke(2.0f));
        plot.setRenderer(renderer);
        
        // Datums-Achse (X-Achse)
        DateAxis dateAxis = (DateAxis) plot.getDomainAxis();
        dateAxis.setDateFormatOverride(new java.text.SimpleDateFormat("yyyy-MM-dd"));
        dateAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        dateAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 12));
        
        // Werte-Achse (Y-Achse)
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setAutoRangeIncludesZero(true);
        rangeAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 12));
        rangeAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
    }
    
    /**
     * Aktualisiert die Daten des Charts
     */
    public void refreshData() {
        if (currentProvider != null && currentStatType != null) {
            loadProviderHistory(currentProvider, currentStatType);
        }
    }
}