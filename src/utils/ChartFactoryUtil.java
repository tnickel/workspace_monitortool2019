package utils;

import java.awt.Color;
import java.awt.Paint;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Day;
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
        
        plot.setRenderer(renderer);

        // Rotiere die X-Achsen-Beschriftungen für bessere Lesbarkeit
        plot.getDomainAxis().setCategoryLabelPositions(
            org.jfree.chart.axis.CategoryLabelPositions.UP_45);

        ChartPanel chartPanel = new ChartPanel(chart);
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
}
