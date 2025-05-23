package charts;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import data.ProviderStats;
import data.Trade;
import utils.HtmlDatabase;

/**
 * Chart-Komponente zur Anzeige des Equity Drawdowns
 * Allgemeine Lösung, die sich dynamisch an alle Drawdown-Bereiche anpasst
 */
public class EquityDrawdownChart extends JPanel {
    private final JFreeChart chart;
    private final TimeSeriesCollection dataset;
    private final ProviderStats stats;
    private final double maxDrawdownGraphic;
    private final HtmlDatabase htmlDatabase;
    
    // Liste der Zeitpunkte mit Extrempunkten für spezielle Markierung
    private final List<Date> extremePoints = new ArrayList<>();
    private final double extremeThreshold = 5.0; // Schwellenwert für Extrempunkte (5% oder höher)
    
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
        setBorder(new EmptyBorder(0, 0, 0, 0));
        
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
        chartPanel.setBorder(null);
        chartPanel.setSize(new Dimension(950, 300));
        
        add(chartPanel, BorderLayout.CENTER);
        
        setPreferredSize(new Dimension(950, 300));
        setMinimumSize(new Dimension(400, 200));
        
        System.out.println("EquityDrawdownChart Panel Größe: " + getPreferredSize());
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
            true,
            true,
            false
        );
        
        return chart;
    }
    
    /**
     * Passt das Chart-Design an
     */
    private void customizeChart() {
        chart.setBackgroundPaint(Color.WHITE);
        
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // Spezieller Linienrenderer für Extrempunkt-Markierungen
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer() {
            @Override
            public java.awt.Shape getItemShape(int series, int item) {
                // Hole den Zeitpunkt für diesen Datenpunkt
                Date itemDate = new Date(dataset.getSeries(series).getTimePeriod(item).getFirstMillisecond());
                
                // Prüfe, ob dieser Zeitpunkt ein Extrempunkt ist
                boolean isExtreme = extremePoints.stream()
                    .anyMatch(extremeDate -> Math.abs(extremeDate.getTime() - itemDate.getTime()) < 60000); // 1 Minute Toleranz
                
                if (isExtreme) {
                    // Kleinerer roter Kreis für Extrempunkte (6x6 statt 12x12)
                    return new java.awt.geom.Ellipse2D.Double(-3, -3, 6, 6);
                } else {
                    // Normale kleine Punkte (meist nicht sichtbar)
                    return new java.awt.geom.Ellipse2D.Double(-1, -1, 2, 2);
                }
            }
            
            @Override
            public java.awt.Paint getItemPaint(int series, int item) {
                // Hole den Zeitpunkt für diesen Datenpunkt
                Date itemDate = new Date(dataset.getSeries(series).getTimePeriod(item).getFirstMillisecond());
                
                // Prüfe, ob dieser Zeitpunkt ein Extrempunkt ist
                boolean isExtreme = extremePoints.stream()
                    .anyMatch(extremeDate -> Math.abs(extremeDate.getTime() - itemDate.getTime()) < 60000); // 1 Minute Toleranz
                
                if (isExtreme) {
                    return new Color(255, 0, 0, 200); // Leuchtend rot für Extrempunkte
                } else {
                    return new Color(220, 20, 60); // Normale Linienfarbe
                }
            }
            
            @Override
            public boolean getItemShapeVisible(int series, int item) {
                // Hole den Zeitpunkt für diesen Datenpunkt
                Date itemDate = new Date(dataset.getSeries(series).getTimePeriod(item).getFirstMillisecond());
                
                // Zeige Shapes nur für Extrempunkte
                return extremePoints.stream()
                    .anyMatch(extremeDate -> Math.abs(extremeDate.getTime() - itemDate.getTime()) < 60000); // 1 Minute Toleranz
            }
        };
        
        renderer.setSeriesPaint(0, new Color(220, 20, 60)); // Kräftiges Rot für die Linie
        renderer.setSeriesStroke(0, new java.awt.BasicStroke(2.0f));
        renderer.setDefaultShapesVisible(false); // Standardmäßig keine Shapes
        plot.setRenderer(renderer);
        
        // X-Achse (Zeit) anpassen
        DateAxis dateAxis = (DateAxis) plot.getDomainAxis();
        dateAxis.setDateFormatOverride(new SimpleDateFormat("yyyy-MM-dd"));
        dateAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        dateAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 12));
        
        // Y-Achse (Drawdown) anpassen - Invertieren für traditionelle Drawdown-Darstellung
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());
        rangeAxis.setInverted(true); // Drawdown geht nach unten
        rangeAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 12));
        rangeAxis.setTickLabelFont(new Font("SansSerif", Font.BOLD, 11)); // Größere, fettere Schrift
        rangeAxis.setLabelPaint(Color.BLACK); // Schwarze Beschriftung für bessere Lesbarkeit
        rangeAxis.setTickLabelPaint(Color.BLACK); // Schwarze Tick-Labels für bessere Lesbarkeit
        
        // Titel anpassen
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 14));
    }
    
    /**
     * Dynamische Erstellung von horizontalen Linien und Farbmarkierungen 
     * basierend auf dem tatsächlichen Drawdown-Bereich
     */
    private void addDynamicHorizontalLines(XYPlot plot, double maxValue) {
        // Bestimme einen sinnvollen Skalierungsbereich
        double effectiveMax = Math.max(maxValue, 1.0); // Mindestens 1% für sehr kleine Drawdowns
        
        // Bestimme die Anzahl der Zonen basierend auf dem Maximum
        int numberOfZones;
        double zoneSize;
        
        if (effectiveMax <= 5.0) {
            // Kleine Drawdowns: 5 Zonen à 1%
            numberOfZones = 5;
            zoneSize = 1.0;
        } else if (effectiveMax <= 20.0) {
            // Mittlere Drawdowns: 4 Zonen à 5%
            numberOfZones = 4;
            zoneSize = 5.0;
        } else if (effectiveMax <= 50.0) {
            // Große Drawdowns: 5 Zonen à 10%
            numberOfZones = 5;
            zoneSize = 10.0;
        } else {
            // Sehr große Drawdowns: 4 Zonen à 25%
            numberOfZones = 4;
            zoneSize = 25.0;
        }
        
        // Berechne die obere Grenze für die Zonendarstellung
        double upperBound = Math.ceil(effectiveMax / zoneSize) * zoneSize;
        
        // Farben für die Zonen (von gut zu schlecht)
        Color[] zoneColors = {
            new Color(144, 238, 144, 60), // Hellgrün - geringer Drawdown
            new Color(255, 255, 0, 60),   // Gelb - mäßiger Drawdown
            new Color(255, 165, 0, 60),   // Orange - erhöhter Drawdown
            new Color(255, 99, 71, 60),   // Rot-Orange - hoher Drawdown
            new Color(220, 20, 60, 60),   // Kräftiges Rot - sehr hoher Drawdown
        };
        
        // Erstelle Farbzonen
        for (int i = 0; i < numberOfZones && i < zoneColors.length; i++) {
            double lowerBound = i * zoneSize;
            double zoneBound = Math.min((i + 1) * zoneSize, upperBound);
            
            if (lowerBound < upperBound) {
                IntervalMarker marker = new IntervalMarker(lowerBound, zoneBound);
                marker.setPaint(zoneColors[i]);
                plot.addRangeMarker(marker, Layer.BACKGROUND);
            }
        }
        
        // Erstelle horizontale Linien an sinnvollen Intervallen
        double lineInterval = determineLineInterval(effectiveMax);
        
        for (double value = lineInterval; value <= upperBound; value += lineInterval) {
            ValueMarker marker = new ValueMarker(value);
            marker.setPaint(Color.GRAY);
            marker.setStroke(new java.awt.BasicStroke(1.0f, 
                                                    java.awt.BasicStroke.CAP_BUTT, 
                                                    java.awt.BasicStroke.JOIN_MITER, 
                                                    1.0f, 
                                                    new float[] {3.0f, 3.0f}, 
                                                    0.0f));
            
            // Formatiere das Label basierend auf der Größe des Wertes
            String label = formatDrawdownLabel(value);
            marker.setLabel(label);
            marker.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
            marker.setLabelTextAnchor(TextAnchor.TOP_RIGHT);
            marker.setLabelFont(new Font("SansSerif", Font.PLAIN, 9));
            marker.setLabelPaint(Color.DARK_GRAY);
            
            plot.addRangeMarker(marker);
        }
        
        System.out.println("Dynamische Skalierung erstellt: Max=" + effectiveMax + 
                          "%, Zonen=" + numberOfZones + ", ZonenGröße=" + zoneSize + 
                          "%, LinienIntervall=" + lineInterval + "%");
    }
    
    /**
     * Bestimmt das passende Intervall für horizontale Linien basierend auf dem Maximum
     */
    private double determineLineInterval(double maxValue) {
        if (maxValue <= 2.0) return 0.5;      // 0.5% Intervalle für sehr kleine Drawdowns
        else if (maxValue <= 5.0) return 1.0;  // 1% Intervalle
        else if (maxValue <= 10.0) return 2.0; // 2% Intervalle
        else if (maxValue <= 25.0) return 5.0; // 5% Intervalle
        else if (maxValue <= 50.0) return 10.0; // 10% Intervalle
        else return 20.0;                       // 20% Intervalle für sehr große Drawdowns
    }
    
    /**
     * Formatiert die Labels für Drawdown-Werte
     */
    private String formatDrawdownLabel(double value) {
        if (value < 1.0) {
            return String.format("%.1f%%", value); // Eine Dezimalstelle für kleine Werte
        } else {
            return String.format("%.0f%%", value); // Ganze Zahlen für größere Werte
        }
    }
    
    /**
     * Berechnet die optimale obere Grenze für die Y-Achse
     */
    private double calculateOptimalUpperBound(double actualMax) {
        if (actualMax <= 0.0) return 5.0; // Mindestbereich für leere Daten
        
        // Füge 10-20% Puffer hinzu, abhängig von der Größe
        double buffer = actualMax <= 10.0 ? 1.2 : 1.1; // 20% Puffer für kleine, 10% für große Werte
        double withBuffer = actualMax * buffer;
        
        // Runde auf sinnvolle Werte auf
        if (withBuffer <= 2.0) return Math.ceil(withBuffer * 2) / 2.0;     // Auf 0.5% runden
        else if (withBuffer <= 10.0) return Math.ceil(withBuffer);         // Auf 1% runden
        else if (withBuffer <= 50.0) return Math.ceil(withBuffer / 5.0) * 5.0; // Auf 5% runden
        else return Math.ceil(withBuffer / 10.0) * 10.0;                   // Auf 10% runden
    }
    
    /**
     * Berechnet die Drawdown-Daten und füllt das Chart
     */
    private void populateChart() {
        TimeSeries drawdownSeries = new TimeSeries("Drawdown");
        
        boolean dataFound = false;
        double actualMaxDrawdown = 0.0;
        
        if (htmlDatabase != null && stats.getSignalProvider() != null) {
            String txtFileName = stats.getSignalProvider() + "_root.txt";
            String csvFileName = stats.getSignalProvider() + ".csv";
            
            System.out.println("Versuche Drawdown-Daten zu laden für: " + txtFileName);
            
            String drawdownData = htmlDatabase.getDrawdownChartData(txtFileName);
            if (drawdownData == null || drawdownData.isEmpty()) {
                drawdownData = htmlDatabase.getDrawdownChartData(csvFileName);
            }
            
            if (drawdownData != null && !drawdownData.isEmpty()) {
                System.out.println("Drawdown-Daten gefunden! Länge: " + drawdownData.length() + " Zeichen");
                actualMaxDrawdown = processDrawdownData(drawdownData, drawdownSeries);
                dataFound = true;
            } else {
                System.err.println("Keine Drawdown-Daten in der HTML-Datenbank gefunden für: " + txtFileName);
            }
        }
        
        // Fallback: Berechne aus Trades
        if (!dataFound || drawdownSeries.isEmpty()) {
            System.out.println("Berechne Drawdown-Daten aus den Trades...");
            actualMaxDrawdown = calculateDrawdownFromTrades(drawdownSeries);
        }
        
        // Dummy-Daten falls immer noch leer
        if (drawdownSeries.isEmpty()) {
            System.out.println("Keine Daten gefunden. Füge Dummy-Daten hinzu...");
            addDummyData(drawdownSeries);
            actualMaxDrawdown = 5.0; // Konservativer Dummy-Wert
        } else {
            System.out.println("Chart enthält " + drawdownSeries.getItemCount() + " Datenpunkte");
            System.out.println("Tatsächlicher maximaler Drawdown: " + actualMaxDrawdown + "%");
        }
        
        dataset.addSeries(drawdownSeries);
        
        // Y-Achsen-Skalierung dynamisch basierend auf den tatsächlichen Daten setzen
        XYPlot plot = (XYPlot) chart.getPlot();
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        
        // Berechne optimale obere Grenze
        double optimalUpperBound = calculateOptimalUpperBound(actualMaxDrawdown);
        
        // Setze die Y-Achsen-Grenzen
        rangeAxis.setLowerBound(0.0);
        rangeAxis.setUpperBound(optimalUpperBound);
        
        // Formatierung der Y-Achse basierend auf der Größe der Werte
        String formatPattern = optimalUpperBound <= 10.0 ? "0.0" : "0";
        rangeAxis.setNumberFormatOverride(new java.text.DecimalFormat(formatPattern));
        
        // Verbesserte Lesbarkeit für Y-Achsen-Labels
        rangeAxis.setTickLabelInsets(new org.jfree.chart.ui.RectangleInsets(5, 5, 5, 5));
        
        // Zusätzliche Y-Achse auf der rechten Seite für bessere Lesbarkeit
        NumberAxis rightAxis = new NumberAxis("Drawdown (%)");
        rightAxis.setLowerBound(0.0);
        rightAxis.setUpperBound(optimalUpperBound);
        rightAxis.setInverted(true); // Auch die rechte Achse invertieren
        rightAxis.setNumberFormatOverride(new java.text.DecimalFormat(formatPattern));
        rightAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 12));
        rightAxis.setTickLabelFont(new Font("SansSerif", Font.BOLD, 11));
        rightAxis.setLabelPaint(Color.BLACK);
        rightAxis.setTickLabelPaint(Color.BLACK);
        plot.setRangeAxis(1, rightAxis);
        
        // Dynamische horizontale Linien und Farbmarkierungen hinzufügen
        addDynamicHorizontalLines(plot, actualMaxDrawdown);
        
        // Debug-Zusammenfassung der Extrempunkte
        System.out.println("=== EXTREMPUNKTE ZUSAMMENFASSUNG ===");
        System.out.println("Schwellenwert: " + extremeThreshold + "%");
        System.out.println("Gefundene Extrempunkte: " + extremePoints.size());
        if (!extremePoints.isEmpty()) {
            System.out.println("Extrempunkte werden mit großen roten Kreisen markiert!");
            // Zeige die ersten paar Extrempunkte als Beispiel
            for (int i = 0; i < Math.min(5, extremePoints.size()); i++) {
                System.out.println("Extrempunkt " + (i+1) + ": " + extremePoints.get(i));
            }
            if (extremePoints.size() > 5) {
                System.out.println("... und " + (extremePoints.size() - 5) + " weitere");
            }
        }
        System.out.println("=====================================");
        
        System.out.println("Y-Achse gesetzt: 0.0 bis " + optimalUpperBound + " (basierend auf max=" + actualMaxDrawdown + "%)");
    }
    
    /**
     * Verarbeitet die Drawdown-Daten aus dem Datenfile und gibt den maximalen Drawdown zurück
     * Pro Datum werden alle Werte aufgenommen: der maximale Wert wird 5x mit 10-Minuten-Versatz eingefügt,
     * die restlichen Werte werden flexibel über den Tag verteilt für optimale Sichtbarkeit
     */
    private double processDrawdownData(String drawdownData, TimeSeries drawdownSeries) {
        String[] lines = drawdownData.split("\n");
        
        System.out.println("Verarbeite " + lines.length + " Zeilen mit Drawdown-Daten");
        
        // Map um für jedes Datum alle Drawdown-Werte zu sammeln
        Map<LocalDate, List<Double>> drawdownPerDate = new HashMap<>();
        double overallMaxDrawdown = 0.0;
        int processedLines = 0;
        
        // Erste Phase: Sammle alle Werte pro Datum
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            String[] parts = line.split(":");
            if (parts.length != 2) {
                continue;
            }
            
            String dateStr = parts[0].trim();
            String valueStr = parts[1].trim().replace("%", "").replace(",", ".");
            
            try {
                LocalDate date = LocalDate.parse(dateStr);
                double value = Double.parseDouble(valueStr);
                
                // Sammle alle Werte für dieses Datum
                drawdownPerDate.computeIfAbsent(date, k -> new ArrayList<>()).add(value);
                
                // Verfolge den globalen maximalen Drawdown
                overallMaxDrawdown = Math.max(overallMaxDrawdown, value);
                
                processedLines++;
            } catch (Exception e) {
                System.err.println("Fehler beim Parsen: " + line + " - " + e.getMessage());
            }
        }
        
        System.out.println("Drawdown-Daten verarbeitet: " + processedLines + " gültige Einträge");
        System.out.println("Eindeutige Tage gefunden: " + drawdownPerDate.size());
        System.out.println("Maximaler Drawdown aus Daten: " + overallMaxDrawdown + "%");
        
        // Zweite Phase: Verteile alle Werte flexibel über den Tag
        int totalAddedPoints = 0;
        
        for (Map.Entry<LocalDate, List<Double>> entry : drawdownPerDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<Double> values = entry.getValue();
            
            // Entferne Duplikate und sortiere absteigend (höchste Werte zuerst)
            List<Double> uniqueValues = values.stream()
                .distinct()
                .sorted((a, b) -> Double.compare(b, a)) // Absteigend sortieren
                .collect(Collectors.toList());
            
            int pointsAdded = addFlexibleDailyPoints(drawdownSeries, date, uniqueValues);
            totalAddedPoints += pointsAdded;
            
            // Debug: Zeige Details für Tage mit hohem Drawdown
            if (uniqueValues.get(0) > 10.0) { // Höchster Wert (Index 0)
                System.out.println("Hoher Drawdown Tag: " + date + 
                                 " - Max: " + uniqueValues.get(0) + "%" +
                                 " (" + pointsAdded + " Punkte eingefügt aus " + values.size() + " original/" + 
                                 uniqueValues.size() + " unique)");
            }
        }
        
        System.out.println("TimeSeries befüllt mit " + totalAddedPoints + " Datenpunkten");
        System.out.println("Durchschnittlich " + (totalAddedPoints / (double)drawdownPerDate.size()) + " Punkte pro Tag");
        
        return overallMaxDrawdown;
    }
    
    /**
     * Fügt alle Drawdown-Werte eines Tages flexibel über 24 Stunden verteilt hinzu
     * Das Maximum wird 5x mit 10-Minuten-Versatz eingefügt für bessere Sichtbarkeit
     * Extrempunkte über dem Schwellenwert werden für spezielle Markierung gespeichert
     */
    private int addFlexibleDailyPoints(TimeSeries drawdownSeries, LocalDate date, List<Double> sortedValues) {
        if (sortedValues.isEmpty()) {
            return 0;
        }
        
        int pointsAdded = 0;
        double maxValue = sortedValues.get(0); // Höchster Wert ist Index 0
        
        // Phase 1: Maximum 5x mit 10-Minuten-Versatz einfügen (00:00, 00:10, 00:20, 00:30, 00:40)
        for (int i = 0; i < 5; i++) {
            LocalDateTime maxTime = date.atTime(0, i * 10); // 0:00, 0:10, 0:20, 0:30, 0:40
            Date javaDate = Date.from(maxTime.atZone(ZoneId.systemDefault()).toInstant());
            drawdownSeries.addOrUpdate(new Millisecond(javaDate), maxValue);
            pointsAdded++;
            
            // Prüfe, ob dies ein Extrempunkt ist und speichere ihn
            if (maxValue >= extremeThreshold) {
                extremePoints.add(javaDate);
            }
        }
        
        // Phase 2: Restliche Werte über den restlichen Tag verteilen (ab 01:00 bis 23:59)
        if (sortedValues.size() > 1) {
            List<Double> remainingValues = sortedValues.subList(1, sortedValues.size()); // Alle außer dem Maximum
            
            // Berechne Zeitintervalle für die restlichen Werte
            int availableMinutes = 23 * 60 + 59; // 23:59 - 01:00 = 1379 Minuten verfügbar
            int numberOfPoints = remainingValues.size();
            
            if (numberOfPoints > 0) {
                // Gleichmäßige Verteilung über den verfügbaren Zeitraum
                double minuteStep = availableMinutes / (double) numberOfPoints;
                
                for (int i = 0; i < remainingValues.size(); i++) {
                    double value = remainingValues.get(i);
                    
                    // Berechne die Zeit: Start um 01:00 + i * Schritt
                    int totalMinutesFromMidnight = 60 + (int) Math.round(i * minuteStep); // Start um 01:00 (60 Min)
                    
                    // Stelle sicher, dass wir nicht über 23:59 hinausgehen
                    totalMinutesFromMidnight = Math.min(totalMinutesFromMidnight, 23 * 60 + 59);
                    
                    int hours = totalMinutesFromMidnight / 60;
                    int minutes = totalMinutesFromMidnight % 60;
                    
                    LocalDateTime valueTime = date.atTime(hours, minutes);
                    Date javaDate = Date.from(valueTime.atZone(ZoneId.systemDefault()).toInstant());
                    
                    drawdownSeries.addOrUpdate(new Millisecond(javaDate), value);
                    pointsAdded++;
                    
                    // Prüfe, ob dies ein Extrempunkt ist und speichere ihn
                    if (value >= extremeThreshold) {
                        extremePoints.add(javaDate);
                    }
                }
            }
        }
        
        // Debug für Tage mit vielen Datenpunkten oder Extrempunkten
        long extremePointsToday = sortedValues.stream()
            .mapToLong(v -> v >= extremeThreshold ? 1 : 0)
            .sum();
        
        if (extremePointsToday > 0) {
            System.out.println("EXTREMPUNKTE für " + date + ": " + extremePointsToday + 
                             " Werte über " + extremeThreshold + "%, Max: " + maxValue + "%");
        }
        
        if (sortedValues.size() > 50) {
            System.out.println("Viele Datenpunkte für " + date + ": " + sortedValues.size() + 
                             " unique Werte, " + pointsAdded + " Punkte eingefügt");
        }
        
        return pointsAdded;
    }
    
    /**
     * Berechnet Drawdown-Daten aus den Trades und gibt den maximalen Drawdown zurück
     */
    private double calculateDrawdownFromTrades(TimeSeries drawdownSeries) {
        if (stats.getTrades().isEmpty()) {
            System.out.println("Keine Trades zum Berechnen des Drawdowns vorhanden");
            return 0.0;
        }
        
        List<Trade> sortedTrades = new ArrayList<>(stats.getTrades());
        sortedTrades.sort((t1, t2) -> t1.getCloseTime().compareTo(t2.getCloseTime()));
        
        double currentBalance = stats.getInitialBalance();
        double peak = currentBalance;
        double maxDrawdown = 0.0;
        
        System.out.println("Berechne Drawdown aus " + sortedTrades.size() + " Trades, Startguthaben: " + currentBalance);
        
        for (Trade trade : sortedTrades) {
            currentBalance += trade.getTotalProfit();
            
            if (currentBalance > peak) {
                peak = currentBalance;
            }
            
            if (peak > 0) {
                double drawdownPercent = ((peak - currentBalance) / peak) * 100.0;
                maxDrawdown = Math.max(maxDrawdown, drawdownPercent);
                
                Date date = Date.from(trade.getCloseTime()
                    .atZone(ZoneId.systemDefault())
                    .toInstant());
                
                drawdownSeries.addOrUpdate(new Millisecond(date), drawdownPercent);
            }
        }
        
        System.out.println("Drawdown-Berechnung abgeschlossen. Datenpunkte: " + drawdownSeries.getItemCount());
        System.out.println("Maximaler berechneter Drawdown: " + maxDrawdown + "%");
        
        return maxDrawdown;
    }
    
    /**
     * Fügt Dummy-Daten hinzu (nur für Debug-Zwecke)
     */
    private void addDummyData(TimeSeries drawdownSeries) {
        LocalDate now = LocalDate.now();
        for (int i = 0; i < 10; i++) {
            LocalDateTime dateTime = now.minusDays(i * 30).atTime(0, 0);
            Date date = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
            drawdownSeries.addOrUpdate(new Millisecond(date), i * 0.5); // Konservative Dummy-Werte: 0%, 0.5%, 1%, etc.
        }
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