package charts;

import java.awt.Color;
import java.awt.Dimension;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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

public class EfficiencyChart extends JPanel {
    
    private final List<Trade> trades;
    private TreeMap<LocalDate, List<Trade>> weeklyTrades;
    private DefaultCategoryDataset dataset;
    
    // Tooltip Text
    private static final String TOOLTIP_TEXT = 
        "<html><div style='width:400px;'>" +
        "<h3>Effizienzwert - Erklärung</h3>" +
        "<p>Der Effizienzwert ist ein Maß für die Effizienz deines Tradings. Er wird berechnet als:</p>" +
        "<p style='text-align:center'><b>Effizienzwert = Gewinnsumme / Lotsize-Summe</b></p>" +
        "<p><b>Anwendung im Martingale-System:</b></p>" +
        "<p>Bei einem Martingale-System werden Positionen oft mit größeren Lotsizes geöffnet, " +
        "wenn vorherige Trades Verluste erzielt haben. Dies führt zu einem schlechten Effizienzwert.</p>" +
        "<p>Eine hohe Effizienz bedeutet, dass du viel Gewinn mit wenig Risiko (kleine Lots) erzielst. " +
        "Dies ist die optimale Phase, um deine Handelsgrößen zu erhöhen.</p>" +
        "<p><b>Nutzen des Effizienzwerts:</b></p>" +
        "<ul>" +
        "<li>Identifiziere profitable Handelsphasen</li>" +
        "<li>Erkenne, wann du mit minimaler Lotsize maximal profitieren kannst</li>" +
        "<li>Vermeide das Erhöhen der Handelsgrößen in risikoreichen Phasen</li>" +
        "</ul>" +
        "</div></html>";
    
    public EfficiencyChart(List<Trade> trades) {
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
            "Effizienzwert pro Woche (Gewinnsumme / Lotsize-Summe)",  // Titel
            "",                                                      // X-Achse (kein Label)
            "Effizienzwert",                                         // Y-Achse
            dataset,                                                 // Daten
            PlotOrientation.VERTICAL,                                // Orientierung
            true,                                                    // Legende
            true,                                                    // Tooltips
            false                                                    // URLs
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
        
        // Farben für positive/negative Effizienz
        renderer.setSeriesPaint(0, new Color(0, 120, 0));  // Grün für positive Effizienz
        renderer.setSeriesPaint(1, new Color(220, 0, 0));  // Rot für negative Effizienz
        
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
        
        // Effizienzwert für jede Woche berechnen
        for (LocalDate weekStart : weeklyTrades.keySet()) {
            List<Trade> weekTrades = weeklyTrades.get(weekStart);
            
            // Summen berechnen
            double profitSum = 0.0;
            double lotsizeSum = 0.0;
            
            for (Trade trade : weekTrades) {
                profitSum += trade.getTotalProfit();
                lotsizeSum += trade.getLots();
            }
            
            // Effizienzwert berechnen (mit Schutz vor Division durch Null)
            double efficiency = 0.0;
            if (lotsizeSum > 0) {
                efficiency = profitSum / lotsizeSum;
            }
            
            // Wochen-Label erstellen: Format KW-JJ (z.B. "KW01-23" für erste Woche 2023)
            LocalDate weekEnd = weekStart.plusDays(6);
            String weekLabel = createWeekLabel(weekStart, weekEnd);
            
            // Wert zur Kategorie hinzufügen (positive oder negative Effizienz)
            String category = efficiency >= 0 ? "Positive Effizienz" : "Negative Effizienz";
            dataset.addValue(Math.abs(efficiency), category, weekLabel);
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