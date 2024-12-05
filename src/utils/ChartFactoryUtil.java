package utils;



import java.awt.Color;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
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

	    JFreeChart chart = org.jfree.chart.ChartFactory.createTimeSeriesChart(
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

	    return new ChartPanel(chart);
	}

	private void customizeXYPlot(XYPlot plot) {
	    plot.setBackgroundPaint(Color.WHITE);
	    plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
	    plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

	    XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
	    renderer.setDefaultShapesVisible(false);
	    plot.setRenderer(renderer);
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
	    
	    CategoryPlot plot = chart.getCategoryPlot();
	    plot.setBackgroundPaint(Color.WHITE);
	    plot.setRangeGridlinePaint(Color.GRAY);
	    
	    BarRenderer renderer = (BarRenderer) plot.getRenderer();
	    renderer.setSeriesPaint(0, Color.GREEN);
	    renderer.setSeriesPaint(1, Color.RED);
	    
	    return new ChartPanel(chart);
	}
    
  
    
    private void customizeCategoryPlot(CategoryPlot plot) {
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.GRAY);
        
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, Color.RED);
    }
}