package charts;

import java.awt.BorderLayout;
import java.awt.Color;
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
       plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
       plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
       
       // Lots Dataset hinzufügen
       plot.setDataset(1, lotsDataset);
       
       // Renderer für Trades
       XYLineAndShapeRenderer renderer1 = new XYLineAndShapeRenderer();
       renderer1.setDefaultShapesVisible(false);
       renderer1.setSeriesPaint(0, Color.BLUE);
       plot.setRenderer(0, renderer1);
       
       // Renderer für Lots
       XYLineAndShapeRenderer renderer2 = new XYLineAndShapeRenderer();
       renderer2.setDefaultShapesVisible(false);
       renderer2.setSeriesPaint(0, Color.RED);
       plot.setRenderer(1, renderer2);
       
       // Datumsachse (X-Achse) anpassen 
       DateAxis dateAxis = (DateAxis) plot.getDomainAxis();
       dateAxis.setDateFormatOverride(new SimpleDateFormat("yyyy-MM"));
       dateAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
       dateAxis.setLabelAngle(Math.PI / 2.0);
       dateAxis.setVerticalTickLabels(true);
       dateAxis.setLowerMargin(0.05);
       dateAxis.setUpperMargin(0.05);
       dateAxis.setTickMarkPosition(DateTickMarkPosition.MIDDLE);
       dateAxis.setTickUnit(new DateTickUnit(DateTickUnitType.MONTH, 1));

       // Erste Y-Achse (Trades)
       NumberAxis rangeAxis1 = (NumberAxis) plot.getRangeAxis();
       int maxValue = getMaxTradeValue(trades);
       rangeAxis1.setRange(0, maxValue * 1.2);
       rangeAxis1.setTickUnit(new NumberTickUnit(1));
       rangeAxis1.setVerticalTickLabels(false);
       
       // Zweite Y-Achse (Lots)
       NumberAxis rangeAxis2 = new NumberAxis("Lots");
       rangeAxis2.setAutoRange(true);
       plot.setRangeAxis(1, rangeAxis2);
       plot.mapDatasetToRangeAxis(1, 1);
       
       // Mehr Platz für Labels
       chart.setPadding(new RectangleInsets(5, 5, 50, 50));
       
       // Legende oben platzieren
       chart.getLegend().setPosition(RectangleEdge.TOP);
       
       // ChartPanel erstellen
       ChartPanel chartPanel = new ChartPanel(chart) {
           @Override
           public Dimension getPreferredSize() {
               return new Dimension(800, 400);
           }
           
           @Override
           public Dimension getMinimumSize() {
               return new Dimension(600, 400);
           }
       };
       
       setLayout(new BorderLayout());
       add(chartPanel, BorderLayout.CENTER);
       
       // Daten hinzufügen
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
                   Date.from(trade.getOpenTime().atZone(ZoneId.systemDefault()).toInstant())
               ),
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
                   Date.from(trade.getOpenTime().atZone(ZoneId.systemDefault()).toInstant())
               ),
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