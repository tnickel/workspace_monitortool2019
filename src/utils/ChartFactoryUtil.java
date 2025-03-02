package utils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Paint;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import data.ProviderStats;
import data.Trade;

public class ChartFactoryUtil {

    public ChartPanel createEquityCurveChart(ProviderStats stats) {
        TimeSeries series = new TimeSeries("Equity");
        TimeSeriesCollection dataset = new TimeSeriesCollection(series);

        List<Trade> trades = new ArrayList<>(stats.getTrades());
        trades.sort((t1, t2) -> t1.getCloseTime().compareTo(t2.getCloseTime()));

        double equity = stats.getInitialBalance();

        for (Trade trade : trades) {
            if (trade.getCloseTime() != null && !trade.getCloseTime().isAfter(LocalDateTime.now())) {
                equity += trade.getTotalProfit();
                series.addOrUpdate(
                    new Day(Date.from(trade.getCloseTime().atZone(ZoneId.systemDefault()).toInstant())),
                    equity
                );
            }
        }

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            "Equity Curve",
            "Time",
            "Equity",
            dataset,
            true,
            true,
            false
        );

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        DateAxis dateAxis = (DateAxis) plot.getDomainAxis();
        dateAxis.setDateFormatOverride(new java.text.SimpleDateFormat("yyyy-MM-dd"));

        return new ChartPanel(chart);
    }

    public ChartPanel createMonthlyProfitChart(ProviderStats stats) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        Map<YearMonth, Double> monthlyProfits = stats.getMonthlyProfitPercentages();

        // Sortiere die Monate
        TreeMap<YearMonth, Double> sortedMap = new TreeMap<>(monthlyProfits);
        
        for (Map.Entry<YearMonth, Double> entry : sortedMap.entrySet()) {
            YearMonth month = entry.getKey();
            double profitPercentage = entry.getValue();
            String monthStr = month.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            dataset.addValue(profitPercentage, "Performance %", monthStr);
        }

        JFreeChart chart = ChartFactory.createBarChart(
            "Monthly Performance Overview",
            "Month",
            "Performance %",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.GRAY);

        // Verwenden des benutzerdefinierten Renderers
        BarRenderer renderer = new BarRenderer() {
            @Override
            public Paint getItemPaint(int row, int column) {
                Number value = dataset.getValue(row, column);
                if (value != null && value.doubleValue() < 0) {
                    return Color.RED; // Rot für negative Werte
                } else {
                    return Color.GREEN; // Grün für positive Werte
                }
            }
        };
        
        // Labels für die Balken hinzufügen
        renderer.setDefaultItemLabelGenerator(new org.jfree.chart.labels.StandardCategoryItemLabelGenerator("{2}%", new java.text.DecimalFormat("0.00")));
        renderer.setDefaultItemLabelsVisible(true);
        
        // Schriftart für bessere Lesbarkeit
        renderer.setDefaultItemLabelFont(new Font("SansSerif", Font.BOLD, 12));
        
        // Vergrößern der Margins zwischen den Balken
        renderer.setItemMargin(0.2);  // Mehr Platz zwischen den Balken
        
        // Verringern der Balkenbreite
        renderer.setMaximumBarWidth(0.05);  // Schmälere Balken
        
        plot.setRenderer(renderer);

        // Rotiere die X-Achsen-Beschriftungen für bessere Lesbarkeit
        plot.getDomainAxis().setCategoryLabelPositions(
            org.jfree.chart.axis.CategoryLabelPositions.UP_45);

        // Erstelle das ChartPanel mit größerer Breite
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(1200, 200)); 

        return chartPanel;
    }
  
    
    private TreeMap<YearMonth, Double> calculateMonthlyProfits(List<Trade> trades) {
        TreeMap<YearMonth, Double> monthlyProfits = new TreeMap<>();

        for (Trade trade : trades) {
            LocalDateTime closeTime = trade.getCloseTime();
            if (closeTime != null) {
                YearMonth month = YearMonth.from(closeTime);
                monthlyProfits.merge(month, trade.getTotalProfit(), Double::sum);
            }
        }

        return monthlyProfits;
    }
    public ChartPanel createWeekdayProfitChart(ProviderStats stats) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        List<Trade> trades = stats.getTrades();
        
        // Summiere Gewinne/Verluste pro Wochentag
        Map<String, Double> profitByWeekday = new HashMap<>();
        String[] weekdays = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String weekday : weekdays) {
            profitByWeekday.put(weekday, 0.0);
        }
        
        for (Trade trade : trades) {
            LocalDateTime closeTime = trade.getCloseTime();
            String weekday = weekdays[closeTime.getDayOfWeek().getValue() % 7]; // Umwandlung in 0-6 Index (Sonntag = 0)
            double profit = trade.getTotalProfit();
            profitByWeekday.put(weekday, profitByWeekday.get(weekday) + profit);
        }
        
        // Füge die Daten zum Dataset hinzu
        for (String weekday : weekdays) {
            dataset.addValue(profitByWeekday.get(weekday), "P/L", weekday);
        }
        
        JFreeChart chart = ChartFactory.createBarChart(
            "P/L by weekday",
            "Weekday",
            "Profit/Loss",
            dataset,
            PlotOrientation.VERTICAL,
            false,  // keine Legende nötig
            true,
            false
        );
        
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // Benutzerdefinierten Renderer für rot/grün Färbung
        BarRenderer renderer = new BarRenderer() {
            @Override
            public Paint getItemPaint(int row, int column) {
                Number value = dataset.getValue(row, column);
                if (value != null) {
                    if (value.doubleValue() < 0) {
                        return new Color(220, 50, 50); // Rot für Verluste
                    } else if (value.doubleValue() > 0) {
                        return new Color(0, 120, 0);   // Grün für Gewinne
                    }
                }
                return Color.GRAY; // Neutral für 0-Werte
            }
        };
        
        // Anpassungen für den Renderer
        renderer.setItemMargin(0.1);
        renderer.setMaximumBarWidth(0.1);
        renderer.setDefaultItemLabelsVisible(false); // Keine Labels direkt auf den Balken
        
        plot.setRenderer(renderer);
        
        // Anpassung der Achsen
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        
        // Erstelle das ChartPanel
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(1200, 300));
        
        return chartPanel;
    }
    public ChartPanel createMartingaleVisualizationChart(ProviderStats stats) {
        List<Trade> trades = stats.getTrades();
        MartingaleAnalyzer analyzer = new MartingaleAnalyzer(trades);
        Map<String, List<MartingaleAnalyzer.MartingaleSequence>> sequences = analyzer.findMartingaleSequences();
        
        // Erstelle ein Dataset für die Lotgrößen im Zeitverlauf
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        
        // Gruppiere nach Symbol für verschiedene Linien
        Map<String, TimeSeries> timeSeriesMap = new HashMap<>();
        
        for (Trade trade : trades) {
            String symbol = trade.getSymbol();
            if (!timeSeriesMap.containsKey(symbol)) {
                timeSeriesMap.put(symbol, new TimeSeries(symbol));
            }
            
            Date date = Date.from(trade.getOpenTime().atZone(ZoneId.systemDefault()).toInstant());
            timeSeriesMap.get(symbol).addOrUpdate(new Millisecond(date), trade.getLots());
        }
        
        // Füge die Zeitreihen zum Dataset hinzu
        for (TimeSeries series : timeSeriesMap.values()) {
            dataset.addSeries(series);
        }
        
        // Erstelle das Chart
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            "Martingale Analysis - Lot Sizes Over Time",
            "Time",
            "Lot Size",
            dataset,
            true,
            true,
            false
        );
        
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // Füge Marker für identifizierte Martingale-Sequenzen hinzu
        for (Map.Entry<String, List<MartingaleAnalyzer.MartingaleSequence>> entry : sequences.entrySet()) {
            String symbol = entry.getKey();
            List<MartingaleAnalyzer.MartingaleSequence> symbolSequences = entry.getValue();
            
            for (MartingaleAnalyzer.MartingaleSequence sequence : symbolSequences) {
                List<Trade> sequenceTrades = sequence.getTrades();
                
                // Füge Markierung für jede Sequenz hinzu
                if (!sequenceTrades.isEmpty()) {
                    Trade firstTrade = sequenceTrades.get(0);
                    Trade lastTrade = sequenceTrades.get(sequenceTrades.size() - 1);
                    
                    // Erstelle Bereich-Marker für die Zeitspanne der Sequenz
                    Date startDate = Date.from(firstTrade.getOpenTime().atZone(ZoneId.systemDefault()).toInstant());
                    Date endDate = Date.from(lastTrade.getCloseTime().atZone(ZoneId.systemDefault()).toInstant());
                    
                    org.jfree.chart.plot.IntervalMarker marker = new org.jfree.chart.plot.IntervalMarker(
                        startDate.getTime(), endDate.getTime(), 
                        new Color(255, 0, 0, 50)); // Halbtransparentes Rot
                    
                    marker.setLabel("Martingale: " + symbol);
                    marker.setLabelFont(new Font("SansSerif", Font.BOLD, 9));
                    marker.setLabelPaint(Color.RED);
                    marker.setLabelAnchor(org.jfree.chart.ui.RectangleAnchor.TOP_LEFT);
                    marker.setLabelTextAnchor(org.jfree.chart.ui.TextAnchor.TOP_LEFT);
                    
                    plot.addDomainMarker(marker);
                }
            }
        }
        
        // Martingale-Score anzeigen
        double martingaleScore = analyzer.calculateMartingaleScore();
        chart.setTitle(String.format("Martingale Analysis - Score: %.1f%%", martingaleScore));
        
        // Chart Panel erstellen
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(1200, 400));
        
        return chartPanel;
    }
    
}
