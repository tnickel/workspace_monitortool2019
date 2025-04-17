package charts;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;

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

/**
 * Chart-Komponente zur Anzeige der offenen Trades und Lots pro Währungspaar im Zeitverlauf
 */
public class CurrencyPairTradesChart extends JPanel {
    private final JFreeChart tradesChart;
    private final JFreeChart lotsChart;
    private final ChartPanel tradesChartPanel;
    private final ChartPanel lotsChartPanel;
    private final TimeSeriesCollection tradesDataset;
    private final TimeSeriesCollection lotsDataset;
    private final Map<String, JCheckBox> currencyPairCheckboxes;
    private final Map<String, TimeSeries> tradesSeries;
    private final Map<String, TimeSeries> lotsSeries;
    private final List<Trade> allTrades;
    
    /**
     * Konstruktor für die CurrencyPairTradesChart-Komponente
     * 
     * @param trades Liste aller Trades
     */
    public CurrencyPairTradesChart(List<Trade> trades) {
        this.allTrades = trades;
        this.tradesDataset = new TimeSeriesCollection();
        this.lotsDataset = new TimeSeriesCollection();
        this.currencyPairCheckboxes = new HashMap<>();
        this.tradesSeries = new HashMap<>();
        this.lotsSeries = new HashMap<>();
        
        setLayout(new BorderLayout(0, 10));
        
        // Erstelle die Charts
        tradesChart = createTimeSeriesChart("Offene Trades pro Währungspaar", "Anzahl Trades");
        lotsChart = createTimeSeriesChart("Offene Lots pro Währungspaar", "Anzahl Lots");
        
        tradesChartPanel = new ChartPanel(tradesChart);
        lotsChartPanel = new ChartPanel(lotsChart);
        
        tradesChartPanel.setPreferredSize(new Dimension(950, 300));
        // Erhöhe die Höhe des unteren Charts um 30%
        lotsChartPanel.setPreferredSize(new Dimension(950, 390)); // Ursprünglich 300, jetzt 30% höher
        
        // Panel für die Checkboxen erstellen
        JPanel checkboxPanel = createCheckboxPanel();
        
        // Layout zusammensetzen
        JPanel chartsPanel = new JPanel(new BorderLayout(0, 10));
        chartsPanel.add(tradesChartPanel, BorderLayout.NORTH);
        chartsPanel.add(lotsChartPanel, BorderLayout.CENTER);
        
        add(checkboxPanel, BorderLayout.NORTH);
        add(chartsPanel, BorderLayout.CENTER);
        
        // Daten hinzufügen
        populateCharts();
    }
    
    /**
     * Erstellt ein leeres Zeitreihen-Chart mit den grundlegenden Einstellungen
     * 
     * @param title Titel des Charts
     * @param yAxisLabel Beschriftung der Y-Achse
     * @return Das erstellte JFreeChart-Objekt
     */
    private JFreeChart createTimeSeriesChart(String title, String yAxisLabel) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            title,
            "Zeit",
            yAxisLabel,
            new TimeSeriesCollection(),
            true,  // Legende anzeigen
            true,  // Tooltips anzeigen
            false  // URLs nicht anzeigen
        );
        
        // Plot-Grundeinstellungen
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // Renderer anpassen
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setDefaultShapesVisible(false); // Keine Punkte anzeigen
        renderer.setDefaultItemLabelsVisible(false);
        plot.setRenderer(renderer);
        
        // Datumsachse (X-Achse) anpassen
        DateAxis dateAxis = (DateAxis) plot.getDomainAxis();
        dateAxis.setDateFormatOverride(new SimpleDateFormat("yyyy-MM-dd"));
        dateAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        dateAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 12));
        
        // Y-Achse anpassen
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setAutoRangeIncludesZero(true);
        rangeAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 12));
        rangeAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        
        // Sicherstellen, dass die Y-Achsenbeschriftung sichtbar ist
        rangeAxis.setVisible(true);
        rangeAxis.setLabel(yAxisLabel);
        
        return chart;
    }
    
    /**
     * Erstellt das Panel mit den Checkboxen für die Währungspaare
     * 
     * @return Das erstellte JPanel mit den Checkboxen
     */
    private JPanel createCheckboxPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Währungspaare"));
        
        // Alle verwendeten Währungspaare ermitteln
        Map<String, Boolean> uniqueSymbols = new HashMap<>();
        for (Trade trade : allTrades) {
            uniqueSymbols.put(trade.getSymbol(), true);
        }
        
        // Panel für alle Checkboxen
        JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        checkboxPanel.setPreferredSize(new Dimension(800, 100)); // Größeres Panel für mehr Platz
        
        // "Alles ausschalten" Checkbox am Anfang
        JCheckBox toggleAllCheckbox = new JCheckBox("Alles ausschalten");
        toggleAllCheckbox.addActionListener(e -> {
            boolean newState = !toggleAllCheckbox.isSelected();
            for (JCheckBox cb : currencyPairCheckboxes.values()) {
                cb.setSelected(newState);
                // Entsprechende Serien aktualisieren
                String symbol = cb.getText();
                updateVisibility(symbol, newState);
            }
            toggleAllCheckbox.setSelected(!newState);
            toggleAllCheckbox.setText(newState ? "Alles ausschalten" : "Alles einschalten");
        });
        
        // Panel für den Toggle-All Button
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(toggleAllCheckbox);
        panel.add(topPanel, BorderLayout.NORTH);
        
        // Checkboxen für jedes Währungspaar erstellen
        for (String symbol : uniqueSymbols.keySet()) {
            JCheckBox checkbox = new JCheckBox(symbol);
            checkbox.setSelected(true);  // Standardmäßig alle ausgewählt
            checkbox.addActionListener(e -> updateVisibility(symbol, checkbox.isSelected()));
            currencyPairCheckboxes.put(symbol, checkbox);
            checkboxPanel.add(checkbox);
        }
        
        // Scrollpane für die Checkboxen, falls es zu viele werden
        JScrollPane scrollPane = new JScrollPane(checkboxPanel);
        scrollPane.setPreferredSize(new Dimension(800, 60));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Aktualisiert die Sichtbarkeit der Serien für ein bestimmtes Währungspaar
     * 
     * @param symbol Das Währungspaar
     * @param visible True, wenn die Serien sichtbar sein sollen
     */
    private void updateVisibility(String symbol, boolean visible) {
        TimeSeries tradeSeries = tradesSeries.get(symbol);
        TimeSeries lotSeries = lotsSeries.get(symbol);
        
        if (tradeSeries != null) {
            if (visible && !tradesDataset.getSeries().contains(tradeSeries)) {
                tradesDataset.addSeries(tradeSeries);
            } else if (!visible && tradesDataset.getSeries().contains(tradeSeries)) {
                tradesDataset.removeSeries(tradeSeries);
            }
        }
        
        if (lotSeries != null) {
            if (visible && !lotsDataset.getSeries().contains(lotSeries)) {
                lotsDataset.addSeries(lotSeries);
            } else if (!visible && lotsDataset.getSeries().contains(lotSeries)) {
                lotsDataset.removeSeries(lotSeries);
            }
        }
    }
    
    /**
     * Befüllt die Charts mit den Daten aus den übergebenen Trades
     */
    private void populateCharts() {
        // Trades nach Währungspaar gruppieren
        Map<String, List<Trade>> tradesBySymbol = new HashMap<>();
        for (Trade trade : allTrades) {
            tradesBySymbol.computeIfAbsent(trade.getSymbol(), k -> new ArrayList<>()).add(trade);
        }
        
        // Für jedes Währungspaar eine Serie erstellen
        for (Map.Entry<String, List<Trade>> entry : tradesBySymbol.entrySet()) {
            String symbol = entry.getKey();
            List<Trade> symbolTrades = entry.getValue();
            
            // TimeSeries für Trades und Lots erstellen
            TimeSeries tradeSeries = new TimeSeries(symbol + " (Trades)");
            TimeSeries lotSeries = new TimeSeries(symbol + " (Lots)");
            
            // Map für alle Zeitpunkte mit Änderungen (Eröffnung oder Schließung eines Trades)
            TreeMap<LocalDateTime, Integer> tradeChanges = new TreeMap<>();
            TreeMap<LocalDateTime, Double> lotChanges = new TreeMap<>();
            
            // Alle Trade-Änderungen erfassen
            for (Trade trade : symbolTrades) {
                // Bei Eröffnung: +1 Trade und +Lots
                tradeChanges.merge(trade.getOpenTime(), 1, Integer::sum);
                lotChanges.merge(trade.getOpenTime(), trade.getLots(), Double::sum);
                
                // Bei Schließung: -1 Trade und -Lots
                tradeChanges.merge(trade.getCloseTime(), -1, Integer::sum);
                lotChanges.merge(trade.getCloseTime(), -trade.getLots(), Double::sum);
            }
            
            // Trades-Serie mit kumulierten Werten füllen
            int openTrades = 0;
            for (Map.Entry<LocalDateTime, Integer> change : tradeChanges.entrySet()) {
                openTrades += change.getValue();
                
                Date date = Date.from(change.getKey()
                    .atZone(ZoneId.systemDefault())
                    .toInstant());
                tradeSeries.addOrUpdate(new Millisecond(date), openTrades);
            }
            
            // Lots-Serie mit kumulierten Werten füllen
            double openLots = 0.0;
            for (Map.Entry<LocalDateTime, Double> change : lotChanges.entrySet()) {
                openLots += change.getValue();
                
                Date date = Date.from(change.getKey()
                    .atZone(ZoneId.systemDefault())
                    .toInstant());
                lotSeries.addOrUpdate(new Millisecond(date), openLots);
            }
            
            // Serien speichern und zu Datasets hinzufügen
            tradesSeries.put(symbol, tradeSeries);
            lotsSeries.put(symbol, lotSeries);
            
            tradesDataset.addSeries(tradeSeries);
            lotsDataset.addSeries(lotSeries);
        }
        
        // Datasets zu Charts hinzufügen
        XYPlot tradesPlot = tradesChart.getXYPlot();
        tradesPlot.setDataset(tradesDataset);
        
        XYPlot lotsPlot = lotsChart.getXYPlot();
        lotsPlot.setDataset(lotsDataset);
        
        // Farben für Serien zuweisen
        assignColors(tradesPlot);
        assignColors(lotsPlot);
        
        // Sicherstellen, dass die Y-Achsen-Labels korrekt angezeigt werden
        NumberAxis tradesAxis = (NumberAxis) tradesPlot.getRangeAxis();
        tradesAxis.setLabel("Anzahl Trades");
        tradesAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 12));
        
        NumberAxis lotsAxis = (NumberAxis) lotsPlot.getRangeAxis();
        lotsAxis.setLabel("Anzahl Lots");
        lotsAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 12));
    }
    
    /**
     * Weist den Serien im Plot verschiedene Farben zu
     * 
     * @param plot Der XYPlot, dessen Serien Farben zugewiesen werden sollen
     */
    private void assignColors(XYPlot plot) {
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        
        // Vorgegebene Farben für häufigste Währungspaare
        Map<String, Color> colorMap = new HashMap<>();
        colorMap.put("EURUSD", new Color(0, 102, 204));   // Blau
        colorMap.put("GBPUSD", new Color(204, 0, 0));     // Rot
        colorMap.put("USDJPY", new Color(0, 153, 0));     // Grün
        colorMap.put("USDCHF", new Color(153, 0, 153));   // Lila
        colorMap.put("AUDUSD", new Color(255, 153, 0));   // Orange
        colorMap.put("NZDUSD", new Color(0, 153, 153));   // Türkis
        colorMap.put("USDCAD", new Color(153, 51, 0));    // Braun
        
        // Farben für die Serien zuweisen
        int seriesCount = plot.getDataset().getSeriesCount();
        for (int i = 0; i < seriesCount; i++) {
            String seriesKey = plot.getDataset().getSeriesKey(i).toString();
            String symbol = seriesKey.split(" ")[0]; // Entferne " (Trades)" oder " (Lots)"
            
            Color color = colorMap.getOrDefault(symbol, new Color(
                (int) (Math.random() * 200), 
                (int) (Math.random() * 200), 
                (int) (Math.random() * 200)
            ));
            
            renderer.setSeriesPaint(i, color);
            renderer.setSeriesStroke(i, new java.awt.BasicStroke(2.0f));
            renderer.setSeriesShapesVisible(i, false); // Keine Punkte für diese Serie anzeigen
        }
    }
    
    /**
     * Aktualisiert die Daten in den Charts
     * 
     * @param trades Neue Liste von Trades
     */
    public void updateData(List<Trade> trades) {
        // Datasets leeren
        tradesDataset.removeAllSeries();
        lotsDataset.removeAllSeries();
        tradesSeries.clear();
        lotsSeries.clear();
        currencyPairCheckboxes.clear(); // Checkboxen-Map leeren
        
        // Checkboxen entfernen
        removeAll();
        
        // Alles neu aufbauen
        this.allTrades.clear();
        this.allTrades.addAll(trades);
        
        setLayout(new BorderLayout(0, 10));
        
        // Panel für die Checkboxen erstellen
        JPanel checkboxPanel = createCheckboxPanel();
        
        // Layout zusammensetzen
        JPanel chartsPanel = new JPanel(new BorderLayout(0, 10));
        chartsPanel.add(tradesChartPanel, BorderLayout.NORTH);
        chartsPanel.add(lotsChartPanel, BorderLayout.CENTER);
        
        add(checkboxPanel, BorderLayout.NORTH);
        add(chartsPanel, BorderLayout.CENTER);
        
        // Daten hinzufügen
        populateCharts();
        
        revalidate();
        repaint();
    }
}