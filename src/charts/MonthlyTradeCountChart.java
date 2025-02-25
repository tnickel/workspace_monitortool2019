package charts;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Paint;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.category.DefaultCategoryDataset;

import data.Trade;

public class MonthlyTradeCountChart extends JPanel {
    
    public MonthlyTradeCountChart(List<Trade> trades) {
        DefaultCategoryDataset dataset = createDataset(trades);
        JFreeChart chart = createChart(dataset);
        
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(950, 300));
        
        setLayout(new java.awt.BorderLayout());
        add(chartPanel);
    }
    
    private DefaultCategoryDataset createDataset(List<Trade> trades) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        // Gruppiere Trades nach Monat (YearMonth)
        Map<YearMonth, Integer> tradeCountByMonth = new HashMap<>();
        
        for (Trade trade : trades) {
            LocalDateTime openTime = trade.getOpenTime();
            YearMonth yearMonth = YearMonth.from(openTime);
            
            // Erhöhe den Zähler für diesen Monat
            tradeCountByMonth.merge(yearMonth, 1, Integer::sum);
        }
        
        // Sortiere nach Datum
        TreeMap<YearMonth, Integer> sortedCounts = new TreeMap<>(tradeCountByMonth);
        
        // Füge die Daten zum Dataset hinzu
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        for (Map.Entry<YearMonth, Integer> entry : sortedCounts.entrySet()) {
            String month = entry.getKey().format(formatter);
            dataset.addValue(entry.getValue(), "Trades", month);
        }
        
        return dataset;
    }
    
    private JFreeChart createChart(DefaultCategoryDataset dataset) {
        JFreeChart chart = ChartFactory.createBarChart(
            "Anzahl der Trades pro Monat",  // Titel
            "Monat",                        // x-Achse
            "Anzahl der Trades",            // y-Achse
            dataset,                        // Daten
            PlotOrientation.VERTICAL,       // Orientierung
            true,                           // Legende anzeigen
            true,                           // Tooltips
            false                           // URLs
        );
        
        // Anpassung des Charts
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // X-Achsen-Labels rotieren für bessere Lesbarkeit
        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        domainAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        
        // Y-Achse anpassen
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        
        // Berechne das Y-Achsen-Maximum basierend auf den Daten
        double maxValue = 0;
        for (int i = 0; i < dataset.getColumnCount(); i++) {
            for (int j = 0; j < dataset.getRowCount(); j++) {
                Number value = dataset.getValue(j, i);
                if (value != null && value.doubleValue() > maxValue) {
                    maxValue = value.doubleValue();
                }
            }
        }
        
        // Setze das Maximum mit etwas Abstand nach oben (ca. 15%)
        rangeAxis.setUpperBound(maxValue * 1.15);
        // Stelle sicher, dass die Y-Achse bei 0 beginnt
        rangeAxis.setLowerBound(0);
        // Schrittgröße anpassen
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setVerticalTickLabels(false);
        
        // Benutzerdefinierter Renderer für farbige Balken
        final double finalMaxValue = maxValue;
        BarRenderer renderer = new BarRenderer() {
            @Override
            public Paint getItemPaint(int row, int column) {
                Number value = dataset.getValue(row, column);
                if (value != null && value.doubleValue() > finalMaxValue * 0.8) {
                    return Color.YELLOW; // Gelbe Farbe für hohe Werte
                } else {
                    return new Color(65, 105, 225); // Royal Blue für normale Balken
                }
            }
        };
        
        // Renderer-Einstellungen
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setMaximumBarWidth(0.05);
        renderer.setItemMargin(0.1);
        
        // Labels für die Balken hinzufügen
        renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        renderer.setDefaultItemLabelsVisible(true);
        
        // Positioniere die Labels über den Balken
        renderer.setDefaultPositiveItemLabelPosition(
            new ItemLabelPosition(
                ItemLabelAnchor.OUTSIDE12, 
                TextAnchor.BOTTOM_CENTER
            )
        );
        
        // Setze die Schriftart für die Labels
        renderer.setDefaultItemLabelFont(new Font("SansSerif", Font.BOLD, 11));
        
        // Setze den benutzerdefinierten Renderer für das Plot
        plot.setRenderer(renderer);
        
        return chart;
    }
}