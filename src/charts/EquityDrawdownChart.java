package charts;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

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
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import data.ProviderStats;
import data.Trade;
import utils.HtmlDatabase;

/**
 * Chart-Komponente zur Anzeige des Equity Drawdowns
 */
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
        setBorder(new EmptyBorder(0, 0, 0, 0)); // Entferne Padding
        
        // Debug-Ausgaben für die übergebenen Parameter
        System.out.println("EquityDrawdownChart erstellt für Provider: " + (stats != null ? stats.getSignalProvider() : "null"));
        System.out.println("MaxDrawdownGraphic Wert: " + maxDrawdownGraphic);
        System.out.println("HtmlDatabase ist " + (htmlDatabase != null ? "verfügbar" : "null"));
        
        // Dataset erstellen
        dataset = new TimeSeriesCollection();
        
        // Chart erstellen
        chart = createChart();
        
        // Chart anpassen und mit Daten füllen
        customizeChart();
        populateChart();
        
        // ChartPanel erstellen und konfigurieren
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(950, 300));
        chartPanel.setMinimumSize(new Dimension(400, 200));
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.setBorder(null); // Entferne Border vom ChartPanel
        
        // Explizit setSize aufrufen, damit das Chart garantiert eine Größe hat
        chartPanel.setSize(new Dimension(950, 300));
        
        add(chartPanel, BorderLayout.CENTER);
        
        // Auch hier explizit Größe setzen
        setPreferredSize(new Dimension(950, 300));
        setMinimumSize(new Dimension(400, 200));
        
        // Debug Ausgabe der Größe nach dem Setup
        System.out.println("EquityDrawdownChart Panel Größe: " + getPreferredSize());
    }
    
    /**
     * Erstellt das JFreeChart-Objekt für die Drawdown-Darstellung
     */
    private JFreeChart createChart() {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            "Equity Drawdown",     // Titel
            "Zeit",                // X-Achsenbeschriftung
            "Drawdown (%)",        // Y-Achsenbeschriftung
            dataset,               // Datensatz
            true,                  // Legende anzeigen
            true,                  // Tooltips anzeigen
            false                  // URLs nicht anzeigen
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
     * Berechnet die Drawdown-Daten und füllt das Chart
     */
    private void populateChart() {
        // TimeSeries für den Drawdown erstellen
        TimeSeries drawdownSeries = new TimeSeries("Drawdown");
        
        // Versuche, die Daten aus der HTML-Datenbank zu laden
        boolean dataFound = false;
        
        if (htmlDatabase != null && stats.getSignalProvider() != null) {
            // Beide Varianten der Dateipfade versuchen
            String txtFileName = stats.getSignalProvider() + "_root.txt";
            String csvFileName = stats.getSignalProvider() + ".csv";
            
            System.out.println("Versuche Drawdown-Daten zu laden für: " + txtFileName);
            
            // Verwende die getDrawdownChartData-Methode aus HtmlDatabase mit beiden möglichen Dateinamen
            String drawdownData = htmlDatabase.getDrawdownChartData(txtFileName);
            if (drawdownData == null || drawdownData.isEmpty()) {
                drawdownData = htmlDatabase.getDrawdownChartData(csvFileName);
            }
            
            if (drawdownData != null && !drawdownData.isEmpty()) {
                System.out.println("Drawdown-Daten gefunden! Länge: " + drawdownData.length() + " Zeichen");
                processDrawdownData(drawdownData, drawdownSeries);
                dataFound = true;
            } else {
                System.err.println("Keine Drawdown-Daten in der HTML-Datenbank gefunden für: " + txtFileName);
                
                // Debug-Ausgaben
                System.err.println("DrawdownData ist leer oder null. htmlDatabase: " + (htmlDatabase != null));
                System.err.println("Provider: " + stats.getSignalProvider());
                System.err.println("Dateipfad: " + htmlDatabase.getRootPath() + "/" + txtFileName);
            }
        } else {
            System.err.println("HTML-Datenbank oder SignalProvider nicht verfügbar");
        }
        
        // Wenn keine Daten in der DB gefunden wurden, berechne selbst
        if (!dataFound || drawdownSeries.isEmpty()) {
            System.out.println("Berechne Drawdown-Daten aus den Trades...");
            calculateDrawdownFromTrades(drawdownSeries);
        }
        
        // Stelle sicher, dass die Serie nicht leer ist
        if (drawdownSeries.isEmpty()) {
            System.out.println("Keine Daten für das Chart gefunden. Füge Dummy-Daten hinzu...");
            addDummyData(drawdownSeries);
        } else {
            System.out.println("Chart enthält " + drawdownSeries.getItemCount() + " Datenpunkte");
        }
        
        // Serie zum Dataset hinzufügen
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
     * Berechnet Drawdown-Daten aus den Trades selbst, falls keine HTML-Daten verfügbar sind
     */
    private void calculateDrawdownFromTrades(TimeSeries drawdownSeries) {
        if (stats.getTrades().isEmpty()) {
            System.out.println("Keine Trades zum Berechnen des Drawdowns vorhanden");
            return;
        }
        
        // Trades nach Schließzeit sortieren
        List<Trade> sortedTrades = new ArrayList<>(stats.getTrades());
        sortedTrades.sort((t1, t2) -> t1.getCloseTime().compareTo(t2.getCloseTime()));
        
        double currentBalance = stats.getInitialBalance();
        double peak = currentBalance;
        
        System.out.println("Berechne Drawdown aus " + sortedTrades.size() + " Trades, Startguthaben: " + currentBalance);
        
        for (Trade trade : sortedTrades) {
            currentBalance += trade.getTotalProfit();
            
            if (currentBalance > peak) {
                peak = currentBalance;
            }
            
            if (peak > 0) {
                double drawdownPercent = ((peak - currentBalance) / peak) * 100.0;
                
                Date date = Date.from(trade.getCloseTime()
                    .atZone(ZoneId.systemDefault())
                    .toInstant());
                
                // Verwende Day statt Millisecond für bessere Performance und weniger Datenpunkte
                drawdownSeries.addOrUpdate(new Day(date), drawdownPercent);
                
                // Debug: Jede 100. Trade-Information ausgeben
                if (sortedTrades.indexOf(trade) % 100 == 0) {
                    System.out.println("Trade " + sortedTrades.indexOf(trade) + 
                                       ": Balance=" + currentBalance + 
                                       ", Peak=" + peak + 
                                       ", Drawdown=" + drawdownPercent + "%");
                }
            }
        }
        
        System.out.println("Drawdown-Berechnung abgeschlossen. Datenpunkte: " + drawdownSeries.getItemCount());
    }
    
    /**
     * Fügt Dummy-Daten hinzu, wenn keine richtigen Daten gefunden wurden (nur für Debug-Zwecke)
     */
    private void addDummyData(TimeSeries drawdownSeries) {
        LocalDate now = LocalDate.now();
        for (int i = 0; i < 10; i++) {
            LocalDate date = now.minusDays(i * 30);
            Day day = new Day(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            drawdownSeries.addOrUpdate(day, i * 5.0); // Dummy-Werte: 0%, 5%, 10%, ...
        }
    }
    
    /**
     * Verarbeitet die Drawdown-Daten aus dem Datenfile
     */
    private void processDrawdownData(String drawdownData, TimeSeries drawdownSeries) {
        String[] lines = drawdownData.split("\n");
        
        System.out.println("Verarbeite " + lines.length + " Zeilen mit Drawdown-Daten");
        
        int timeOffset = 0; // Für Einträge mit gleichem Datum
        String lastDate = null;
        int processedLines = 0;
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            String[] parts = line.split(":");
            if (parts.length != 2) {
                System.out.println("Ungültiges Datenformat: " + line);
                continue;
            }
            
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
                drawdownSeries.addOrUpdate(new Day(javaDate), value); // Day statt Millisecond für bessere Performance
                
                processedLines++;
                
                // Debug: Nur alle 100 Zeilen ausgeben
                if (processedLines % 100 == 0) {
                    System.out.println("Verarbeitet: " + processedLines + " Datenpunkte");
                }
            } catch (Exception e) {
                System.err.println("Fehler beim Parsen der Drawdown-Daten: " + e.getMessage() + " für Zeile: " + line);
            }
        }
        
        System.out.println("Drawdown-Daten verarbeitet: " + processedLines + " gültige Einträge");
    }
    
    /**
     * Gibt das JFreeChart-Objekt zurück
     */
    public JFreeChart getChart() {
        return chart;
    }
    
    /**
     * Setzt den Titel des Charts
     */
    public void setTitle(String title) {
        chart.setTitle(title);
    }
}