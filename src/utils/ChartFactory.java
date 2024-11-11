package utils;


import java.awt.BasicStroke;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.List;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import data.ProviderStats;

public class ChartFactory {
    
    public ChartPanel createEquityCurveChart(ProviderStats stats) {
        XYSeries series = new XYSeries("Equity");
        double equity = stats.getInitialBalance();
        List<Double> profits = stats.getProfits();
        
        for (int i = 0; i < profits.size(); i++) {
            equity += profits.get(i);
            series.add(i + 1, equity);
        }
        
        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = org.jfree.chart.ChartFactory.createXYLineChart(
            "Equity Curve Performance",
            "Trade Number",
            "Account Balance",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );
        
        customizeXYPlot((XYPlot) chart.getPlot());
        return new ChartPanel(chart);
    }
    
    public ChartPanel createMonthlyProfitChart(ProviderStats stats) {
        DefaultCategoryDataset dataset = stats.getMonthlyProfitData();
        JFreeChart chart = org.jfree.chart.ChartFactory.createBarChart(
            "Monthly Performance Overview",
            "Month",
            "Profit/Loss",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );
        
        customizeCategoryPlot((CategoryPlot) chart.getPlot());
        return new ChartPanel(chart);
    }
    
    private void customizeXYPlot(XYPlot plot) {
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.RED);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        plot.setRenderer(renderer);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.GRAY);
        plot.setDomainGridlinePaint(Color.GRAY);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setAutoRangeIncludesZero(false);
        rangeAxis.setNumberFormatOverride(new DecimalFormat("#,##0.00"));
    }
    
    private void customizeCategoryPlot(CategoryPlot plot) {
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.GRAY);
        
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, Color.RED);
    }
}