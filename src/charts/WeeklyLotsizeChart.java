package charts;

import java.awt.Color;
import java.awt.Dimension;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.swing.JPanel;
import javax.swing.ToolTipManager;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.category.DefaultCategoryDataset;

import data.Trade;

public class WeeklyLotsizeChart extends JPanel {
    
    private final List<Trade> trades;
    private TreeMap<LocalDate, List<Trade>> weeklyTrades;
    private DefaultCategoryDataset dataset;
    
    // Tooltip Text
    private static final String TOOLTIP_TEXT = 
        "<html><div style='width:400px;'>" +
        "<h3>Wöchentliche Lot-Größe - Erklärung</h3>" +
        "<p>Dieses Diagramm zeigt die Summe aller Lots, die in jeder Woche gehandelt wurden.</p>" +
        "<p><b>Wichtige Merkmale:</b></p>" +
        "<ul>" +
        "<li>Jeder Balken repräsentiert die Gesamtanzahl der Lots einer Woche</li>" +
        "<li>Ein höherer Balken bedeutet mehr Risiko in dieser Woche</li>" +
        "<li>Bei einem Martingale-System können hohe Lot-Größen auf eine Phase mit vielen Verlusten hindeuten</li>" +
        "</ul>" +
        "<p><b>Anwendung:</b></p>" +
        "<p>Dieses Diagramm sollte in Verbindung mit dem Effizienzwert-Diagramm betrachtet werden:</p>" +
        "<ul>" +
        "<li>Wochen mit hoher Effizienz und niedrigen Lots sind optimal</li>" +
        "<li>Wochen mit hohen Lots und niedriger Effizienz deuten auf problematische Handelsphasen hin</li>" +
        "<li>Der Verlauf der Lot-Größen kann Martingale-Phasen identifizieren</li>" +
        "</ul>" +
        "<p>Ziel sollte sein, die Handelsgrößen in Phasen mit hoher Effizienz zu erhöhen, " +
        "nicht in Phasen, wo bereits viele Lots getradet werden.</p>" +
        "</div></html>";
    
    public WeeklyLotsizeChart(List<Trade> trades) {
        this.trades = trades;
        setLayout(new java.awt.BorderLayout());
        
        // Interne Berechnung
        this.weeklyTrades = groupTradesByWeek();
        this.dataset = createDataset();
        
        // Chart erstellen
        JFreeChart chart = createChart();
        
        // ChartPanel mit Tooltip-Unterstützung
        ChartPanel chartPanel = new ChartPanel(chart) {
            // Tooltip für den Chart-Titel anzeigen
            @Override
            public String getToolTipText(java.awt.event.MouseEvent e) {
                // Bereich für den Chart-Titel definieren (oberer Bereich)
                int titleY = 40; // Ungefähre Position des Titels von oben
                
                if (e.getY() < titleY) {
                    return TOOLTIP_TEXT;
                }
                return super.getToolTipText(e);
            }
        };
        
        chartPanel.setPreferredSize(new Dimension(950, 300));
        chartPanel.setInitialDelay(0); // Sofort anzeigen
        chartPanel.setDismissDelay(60000); // Länger anzeigen (1 Minute)
        ToolTipManager.sharedInstance().registerComponent(chartPanel);
        
        add(chartPanel);
    }
    
    private JFreeChart createChart() {
        JFreeChart chart = ChartFactory.createBarChart(
            "Wöchentliche Lot-Größe",                               // Titel
            "",                                                     // X-Achse (kein Label)
            "Lot-Größe",                                            // Y-Achse
            dataset,                                                // Daten
            PlotOrientation.VERTICAL,                               // Orientierung
            true,                                                   // Legende
            true,                                                   // Tooltips
            false                                                   // URLs
        );
        
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // Erhöhe den Abstand zum unteren Rand für die Datumsanzeige
        plot.setAxisOffset(new RectangleInsets(5, 5, 20, 5));
        
        // Renderer für die Balkendarstellung anpassen
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        
        // Entferne die Labels über den Balken
        renderer.setDefaultItemLabelsVisible(false);
        
        // Farbe für die Lots
        renderer.setSeriesPaint(0, new Color(65, 105, 225));  // Royal Blue für Lots
        
        // Anpassen der Balkenbreite
        renderer.setMaximumBarWidth(0.05);  // Schmälere Balken für bessere Übersichtlichkeit
        
        // X-Achse anpassen (die Wochen)
        CategoryAxis domainAxis = plot.getDomainAxis();
        
        // Verbesserte X-Achsen-Beschriftung
        domainAxis.setTickLabelFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 8));
        domainAxis.setMaximumCategoryLabelLines(3); // Mehrzeilige Labels erlauben
        
        // Zeige nur jede 4. Woche auf der X-Achse
        domainAxis.setTickLabelsVisible(true);
        domainAxis.setCategoryMargin(0.01); // Verringere den Abstand zwischen Kategorien
        
        // Wir verstecken die meisten Labels, jedoch nicht alle
        int categoryCount = dataset.getColumnCount();
        for (int i = 0; i < categoryCount; i++) {
            if (i % 4 != 0) { // Zeige nur jede 4. Woche an
                domainAxis.setTickLabelPaint(dataset.getColumnKey(i), new Color(255, 255, 255, 0)); // Transparent
            }
        }
        
        // Y-Achse anpassen
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());
        
        return chart;
    }
    
    private DefaultCategoryDataset createDataset() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        // Lotsize für jede Woche berechnen
        for (LocalDate weekStart : weeklyTrades.keySet()) {
            List<Trade> weekTrades = weeklyTrades.get(weekStart);
            
            // Summe der Lots in dieser Woche
            double totalLotsize = 0.0;
            
            for (Trade trade : weekTrades) {
                totalLotsize += trade.getLots();
            }
            
            // Wochen-Label erstellen: Format KW-JJ (z.B. "KW01-23" für erste Woche 2023)
            LocalDate weekEnd = weekStart.plusDays(6);
            String weekLabel = createWeekLabel(weekStart, weekEnd);
            
            // Wert zur Kategorie hinzufügen
            dataset.addValue(totalLotsize, "Wöchentliche Lots", weekLabel);
        }
        
        return dataset;
    }
    
    /**
     * Erstellt ein übersichtliches Label für eine Woche
     */
    private String createWeekLabel(LocalDate weekStart, LocalDate weekEnd) {
        // Format: "KW[WeekOfYear] (MM/YY)"
        int weekNumber = weekStart.get(java.time.temporal.WeekFields.ISO.weekOfYear());
        String monthYear = weekStart.format(DateTimeFormatter.ofPattern("MM/yy"));
        
        return String.format("KW%02d-%s", weekNumber, monthYear);
    }
    
    /**
     * Gruppiert die Trades nach Kalenderwochen
     * Jede Woche beginnt am Montag und endet am Sonntag
     */
    private TreeMap<LocalDate, List<Trade>> groupTradesByWeek() {
        TreeMap<LocalDate, List<Trade>> weeklyTrades = new TreeMap<>();
        
        for (Trade trade : trades) {
            // Benutze CloseTime für die Zuordnung
            LocalDateTime closeTime = trade.getCloseTime();
            LocalDate tradeDate = closeTime.toLocalDate();
            
            // Finde den Montag der aktuellen Woche
            LocalDate weekStart = tradeDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            
            // Füge den Trade zur entsprechenden Woche hinzu
            weeklyTrades.computeIfAbsent(weekStart, k -> new ArrayList<>()).add(trade);
        }
        
        return weeklyTrades;
    }
}