package charts;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import data.ProviderStats;
import utils.HtmlDatabase;

public class EquityDrawdownChart extends JPanel {
    private final JFreeChart chart;
    private final TimeSeriesCollection dataset;
    private final ProviderStats stats;
    private final double maxDrawdownGraphic;
    private final HtmlDatabase htmlDatabase;
    
    /**
     * Konstruktor für die EquityDrawdownChart-Komponente mit HtmlDatabase
     * 
     * @param stats ProviderStats-Objekt mit allen Trades
     * @param maxDrawdownGraphic Wert des maximalen Drawdowns aus der HTML-Datenbank
     * @param htmlDatabase Die HtmlDatabase-Instanz für den Zugriff auf Drawdown-Daten
     */
    public EquityDrawdownChart(ProviderStats stats, double maxDrawdownGraphic, HtmlDatabase htmlDatabase) {
        this.stats = stats;
        this.maxDrawdownGraphic = maxDrawdownGraphic;
        this.htmlDatabase = htmlDatabase;
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
            "Equity Drawdown",
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
        
        // Linienrenderer anstelle von Flächenrenderer verwenden
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, new Color(180, 0, 0)); // Kräftiges Rot für die Linie
        renderer.setSeriesStroke(0, new java.awt.BasicStroke(2.0f)); // Linienstärke
        renderer.setSeriesShapesVisible(0, false); // Keine Datenpunktmarker anzeigen
        plot.setRenderer(renderer);
        
        // X-Achse (Zeit) anpassen
        DateAxis dateAxis = (DateAxis) plot.getDomainAxis();
        dateAxis.setDateFormatOverride(new SimpleDateFormat("yyyy-MM-dd"));
        dateAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        dateAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 12));
        
        // Y-Achse (Drawdown) anpassen
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());
        rangeAxis.setInverted(true); // Y-Achse umdrehen, damit Drawdown nach unten geht
        rangeAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 12));
        rangeAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        rangeAxis.setLabelPaint(new Color(180, 0, 0)); // Rote Beschriftung
        rangeAxis.setTickLabelPaint(new Color(180, 0, 0)); // Rote Tick-Beschriftungen
        
        // Titel anpassen
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 14));
    }
    
    /**
     * Fügt horizontale Linien und farbliche Hintergrundmarkierungen zum Plot hinzu
     * @param plot Der XYPlot, zu dem die Markierungen hinzugefügt werden sollen
     * @param maxValue Der maximale Wert für die Skalierung
     */
    private void addHorizontalLines(XYPlot plot, double maxValue) {
        // Wir definieren die Farbzonen für verschiedene Drawdown-Bereiche
        // von 0% bis 50% in 10% Schritten
        
        // Abgerundeter Maximalwert auf nächste 10er Einheit
        double roundedMax = Math.ceil(maxValue / 10.0) * 10.0;
        
        // Farben für verschiedene Drawdown-Bereiche
        Color[] colors = {
            new Color(220, 255, 220), // 0-10% - Sehr hell grün
            new Color(200, 255, 200), // 10-20% - Hell grün
            new Color(255, 255, 200), // 20-30% - Hell gelb
            new Color(255, 220, 180), // 30-40% - Hell orange
            new Color(255, 200, 180)  // 40-50% - Hell rot
        };
        
        // Hintergrundmarkierungen für die Farbzonen
        for (int i = 0; i < colors.length; i++) {
            double lowerBound = i * 10.0;
            double upperBound = (i + 1) * 10.0;
            
            // Nur Zonen bis zum gerundeten Maximum anzeigen
            if (lowerBound <= roundedMax) {
                IntervalMarker marker = new IntervalMarker(lowerBound, Math.min(upperBound, roundedMax));
                marker.setPaint(colors[i]);
                marker.setAlpha(0.3f); // Transparenz
                plot.addRangeMarker(marker, Layer.BACKGROUND);
            }
        }
        
        // Horizontale Linien für jede 10%-Marke
        for (double i = 0; i <= roundedMax; i += 10.0) {
            ValueMarker marker = new ValueMarker(i);
            marker.setPaint(Color.GRAY);
            marker.setStroke(new java.awt.BasicStroke(1.0f, 
                                                    java.awt.BasicStroke.CAP_BUTT, 
                                                    java.awt.BasicStroke.JOIN_MITER, 
                                                    1.0f, 
                                                    new float[] {3.0f, 3.0f}, 
                                                    0.0f)); // Gestrichelte Linie
            
            // Beschriftung für die Linie
            marker.setLabel(String.format("%.0f%%", i));
            marker.setLabelAnchor(RectangleAnchor.LEFT);
            marker.setLabelTextAnchor(TextAnchor.CENTER_LEFT);
            marker.setLabelFont(new Font("SansSerif", Font.PLAIN, 9));
            
            plot.addRangeMarker(marker);
        }
    }
    
    /**
     * Berechnet die Drawdown-Daten und füllt das Chart ausschließlich mit Daten aus der HTML-Datenbank
     */
    private void populateChart() {
        // TimeSeries für den Drawdown erstellen
        TimeSeries drawdownSeries = new TimeSeries("Drawdown");
        
        // Versuche, die Daten aus der HTML-Datenbank zu laden
        if (htmlDatabase != null && stats.getSignalProvider() != null) {
            // Korrigiert: Verwende den richtigen Dateinamen für _root.txt Dateien
            String txtFileName = stats.getSignalProvider() + "_root.txt";
            
            // Verwende die getDrawdownChartData-Methode aus HtmlDatabase
            String drawdownData = htmlDatabase.getDrawdownChartData(txtFileName);
            
            if (drawdownData != null && !drawdownData.isEmpty()) {
                processDrawdownData(drawdownData, drawdownSeries);
            } else {
                System.err.println("Keine Drawdown-Daten in der HTML-Datenbank gefunden für: " + txtFileName);
                // Keine Daten - leeres Chart anzeigen
            }
        } else {
            System.err.println("HTML-Datenbank oder SignalProvider nicht verfügbar");
            // Keine Daten - leeres Chart anzeigen
        }
        
        // Serie zum Dataset hinzufügen (auch wenn leer)
        dataset.addSeries(drawdownSeries);
        
        // Y-Achsen-Skalierung anpassen
        XYPlot plot = (XYPlot) chart.getPlot();
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        
        // Maximalwert für die Y-Achse bestimmen
        double calculatedMax = 0.0;
        if (!drawdownSeries.isEmpty()) {
            for (int i = 0; i < drawdownSeries.getItemCount(); i++) {
                calculatedMax = Math.max(calculatedMax, drawdownSeries.getDataItem(i).getValue().doubleValue());
            }
        }
        
        // Nehme den übergebenen maxDrawdownGraphic-Wert
        double maxFromData = maxDrawdownGraphic;
        
        // Nehme den größeren der beiden Werte mit etwas Abstand nach oben
        double maxUpperBound = Math.max(calculatedMax, maxFromData) * 1.1; // 10% mehr als das Maximum
        
        // Setze Grenzen, damit die Skala nicht zu groß oder zu klein wird
        maxUpperBound = Math.max(maxUpperBound, 5.0); // Mindestens 5%
        
        rangeAxis.setLowerBound(0.0);
        rangeAxis.setUpperBound(maxUpperBound);
        
        // Entferne das % Zeichen aus den Tick-Labels
        rangeAxis.setNumberFormatOverride(new java.text.DecimalFormat("0"));
        
        // Füge horizontale Linien und farbliche Markierungen hinzu
        addHorizontalLines(plot, maxUpperBound);
    }
    
    /**
     * Verarbeitet die Drawdown-Daten aus dem Datenfile
     */
    private void processDrawdownData(String drawdownData, TimeSeries drawdownSeries) {
        String[] lines = drawdownData.split("\n");
        
        int timeOffset = 0; // Für Einträge mit gleichem Datum
        String lastDate = null;
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            String[] parts = line.split(":");
            if (parts.length != 2) continue;
            
            String dateStr = parts[0].trim();
            String valueStr = parts[1].trim().replace("%", "").replace(",", ".");
            
            try {
                // Prüfen, ob das Datum gleich dem vorherigen ist
                if (dateStr.equals(lastDate)) {
                    timeOffset += 1; // Inkrementiere den Zeitoffset für doppelte Daten
                } else {
                    timeOffset = 0; // Zurücksetzen für neues Datum
                    lastDate = dateStr;
                }
                
                // Statt LocalDateTime.parse() verwenden wir LocalDate.parse() und konvertieren dann zu LocalDateTime
                LocalDate date = LocalDate.parse(dateStr);
                LocalDateTime dateTime = date.atStartOfDay().plusSeconds(timeOffset);
                
                double value = Double.parseDouble(valueStr);
                
                // Zum Dataset hinzufügen
                Date javaDate = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
                drawdownSeries.addOrUpdate(new Millisecond(javaDate), value);
            } catch (Exception e) {
                System.err.println("Fehler beim Parsen der Drawdown-Daten: " + e.getMessage() + " für Zeile: " + line);
            }
        }
    }
    
    /**
     * Gibt das JFreeChart-Objekt zurück
     */
    public JFreeChart getChart() {
        return chart;
    }
}