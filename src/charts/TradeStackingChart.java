package charts;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickMarkPosition;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import data.Trade;



public class TradeStackingChart extends JPanel {
 private final JFreeChart chart;
 private final TimeSeriesCollection tradeDataset;
 private final TimeSeriesCollection lotsDataset;

 public TradeStackingChart(List<Trade> trades) {
     tradeDataset = new TimeSeriesCollection();
     lotsDataset = new TimeSeriesCollection();
     
     chart = ChartFactory.createTimeSeriesChart(
         "Verlauf Der Gleichzeitig Geöffneten Trades Über Die Zeit",
         "Zeit",
         "Anzahl geöffneter Trades",
         tradeDataset,
         true,
         true,
         false
     );
     
     // Plot-Grundeinstellungen
     XYPlot plot = (XYPlot) chart.getPlot();
     plot.setBackgroundPaint(Color.WHITE);
     plot.setDomainGridlinePaint(new Color(220, 220, 220));
     plot.setRangeGridlinePaint(new Color(220, 220, 220));
     
     // Lots Dataset hinzufügen
     plot.setDataset(1, lotsDataset);
     
     // Renderer für Trades
     XYLineAndShapeRenderer renderer1 = new XYLineAndShapeRenderer();
     renderer1.setDefaultShapesVisible(false);
     renderer1.setSeriesPaint(0, Color.BLUE);
     renderer1.setSeriesStroke(0, new BasicStroke(2.0f));
     plot.setRenderer(0, renderer1);
     
     // Renderer für Lots
     XYLineAndShapeRenderer renderer2 = new XYLineAndShapeRenderer();
     renderer2.setDefaultShapesVisible(false);
     renderer2.setSeriesPaint(0, Color.RED);
     renderer2.setSeriesStroke(0, new BasicStroke(2.0f));
     plot.setRenderer(1, renderer2);
     
     // Datumsachse (X-Achse)
     DateAxis dateAxis = (DateAxis) plot.getDomainAxis();
     dateAxis.setDateFormatOverride(new SimpleDateFormat("yyyy-MM"));
     dateAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));
     dateAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 14));
     dateAxis.setVerticalTickLabels(true);
     dateAxis.setTickMarkPosition(DateTickMarkPosition.MIDDLE);
     dateAxis.setTickUnit(new DateTickUnit(DateTickUnitType.MONTH, 1));

     // Y-Achse (Trades)
     NumberAxis rangeAxis1 = (NumberAxis) plot.getRangeAxis();
     rangeAxis1.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));
     rangeAxis1.setLabelFont(new Font("SansSerif", Font.BOLD, 14));
     rangeAxis1.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
     
     // Zweite Y-Achse (Lots)
     NumberAxis rangeAxis2 = new NumberAxis("Lots");
     rangeAxis2.setAutoRange(true);
     rangeAxis2.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));
     rangeAxis2.setLabelFont(new Font("SansSerif", Font.BOLD, 14));
     plot.setRangeAxis(1, rangeAxis2);
     plot.mapDatasetToRangeAxis(1, 1);
     
     // Chart-Titel
     chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 16));
     
     // Legende
     chart.getLegend().setPosition(RectangleEdge.TOP);
     chart.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 12));
     
     // Hier wird die Höhe um Faktor 3 gestreckt
     ChartPanel chartPanel = new ChartPanel(chart) {
         @Override
         public Dimension getPreferredSize() {
             // Normale Breite (950), aber dreifache Höhe (300 * 3 = 900)
             return new Dimension(950, 900);
         }
         
         @Override
         public Dimension getMinimumSize() {
             // Minimale Breite (600), aber dreifache Höhe (400 * 3 = 1200)
             return new Dimension(600, 1200);
         }
     };
     
     // Feste Größe setzen
     chartPanel.setPreferredSize(new Dimension(950, 1200));
     
     setLayout(new BorderLayout());
     add(chartPanel, BorderLayout.CENTER);
     
     addTrades(trades);
     addLots(trades);
 }
 

   
    private int getMaxTradeValue(List<Trade> trades) {
        List<Trade> activeTrades = new ArrayList<>();
        List<Trade> sortedTrades = new ArrayList<>(trades);
        sortedTrades.sort((t1, t2) -> t1.getOpenTime().compareTo(t2.getOpenTime()));
        
        int maxValue = 0;
        for (Trade trade : sortedTrades) {
            activeTrades.removeIf(t -> t.getCloseTime().compareTo(trade.getOpenTime()) <= 0);
            activeTrades.add(trade);
            maxValue = Math.max(maxValue, activeTrades.size());
        }
        return maxValue;
    }

    private double getMaxLotsValue(List<Trade> trades) {
        List<Trade> activeTrades = new ArrayList<>();
        List<Trade> sortedTrades = new ArrayList<>(trades);
        sortedTrades.sort((t1, t2) -> t1.getOpenTime().compareTo(t2.getOpenTime()));
        
        double maxLots = 0.0;
        for (Trade trade : sortedTrades) {
            activeTrades.removeIf(t -> t.getCloseTime().compareTo(trade.getOpenTime()) <= 0);
            activeTrades.add(trade);
            double totalLots = activeTrades.stream()
                                      .mapToDouble(Trade::getLots)
                                      .sum();
            maxLots = Math.max(maxLots, totalLots);
        }
        return maxLots;
    }
   
    private void addTrades(List<Trade> trades) {
        TimeSeries series = new TimeSeries("Trades");
        
        List<Trade> activeTrades = new ArrayList<>();
        List<Trade> sortedTrades = new ArrayList<>(trades);
        sortedTrades.sort((t1, t2) -> t1.getOpenTime().compareTo(t2.getOpenTime()));
        
        for (Trade trade : sortedTrades) {
            activeTrades.removeIf(t -> t.getCloseTime().compareTo(trade.getOpenTime()) <= 0);
            activeTrades.add(trade);
            
            series.addOrUpdate(
                new Millisecond(
                    Date.from(trade.getOpenTime()
                        .atZone(ZoneId.systemDefault())
                        .toInstant())),
                activeTrades.size()
            );
        }
        
        tradeDataset.addSeries(series);
    }
   
    private void addLots(List<Trade> trades) {
        TimeSeries series = new TimeSeries("Lots");
        
        List<Trade> activeTrades = new ArrayList<>();
        List<Trade> sortedTrades = new ArrayList<>(trades);
        sortedTrades.sort((t1, t2) -> t1.getOpenTime().compareTo(t2.getOpenTime()));
        
        for (Trade trade : sortedTrades) {
            activeTrades.removeIf(t -> t.getCloseTime().compareTo(trade.getOpenTime()) <= 0);
            activeTrades.add(trade);
            
            double totalLots = activeTrades.stream()
                                       .mapToDouble(Trade::getLots)
                                       .sum();
            
            series.addOrUpdate(
                new Millisecond(
                    Date.from(trade.getOpenTime()
                        .atZone(ZoneId.systemDefault())
                        .toInstant())),
                totalLots
            );
        }
        
        lotsDataset.addSeries(series);
    }
   
    public void clear() {
        tradeDataset.removeAllSeries();
        lotsDataset.removeAllSeries();
    }
   
    public JFreeChart getChart() {
        return chart;
    }
   
    public void setTitle(String title) {
        chart.setTitle(title);
    }
}