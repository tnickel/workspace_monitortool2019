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
    private final TimeSeriesCollection dataset;
    
    public TradeStackingChart(List<Trade> trades) {
        dataset = new TimeSeriesCollection();
        
        chart = ChartFactory.createTimeSeriesChart(
            "Verlauf Der Gleichzeitig Geöffneten Trades Über Die Zeit",
            "Zeit",
            "Anzahl geöffneter Trades",
            dataset,
            true,
            true,
            false
        );
        
        // Plot-Grundeinstellungen
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // Renderer anpassen
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setDefaultShapesVisible(false);
        renderer.setSeriesPaint(0, Color.BLUE);  // Trades in Blau
        renderer.setSeriesPaint(1, Color.RED);   // Lots in Rot
        plot.setRenderer(renderer);
        
        // Datumsachse (X-Achse) anpassen
        DateAxis dateAxis = (DateAxis) plot.getDomainAxis();
        dateAxis.setDateFormatOverride(new SimpleDateFormat("yyyy-MM-dd HH:mm"));
        dateAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        dateAxis.setLabelAngle(Math.PI / 2.0);  // Vertikale Ausrichtung (90 Grad)
        dateAxis.setVerticalTickLabels(true);
        dateAxis.setLowerMargin(0.05);
        dateAxis.setUpperMargin(0.05);
        
        // Mehr Platz für Datumsbeschriftungen
        chart.setPadding(new RectangleInsets(5, 5, 50, 5));  // oben, links, unten, rechts
        plot.setAxisOffset(new RectangleInsets(5, 5, 50, 5));
        dateAxis.setTickMarkPosition(DateTickMarkPosition.MIDDLE);
        dateAxis.setLabelInsets(new RectangleInsets(5, 5, 20, 5));
        
        // Y-Achse (Range Axis) anpassen
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        int maxValue = getMaxTradeValue(trades);
        rangeAxis.setRange(0, maxValue * 1.2); // 20% mehr als Maximum für Puffer
        rangeAxis.setTickUnit(new NumberTickUnit(1)); // Ganze Zahlen als Ticks
        rangeAxis.setVerticalTickLabels(true);
        
        // Legende oben platzieren für mehr Platz unten
        chart.getLegend().setPosition(RectangleEdge.TOP);
        
        // ChartPanel erstellen und konfigurieren
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
        
        // Layout setzen
        setLayout(new BorderLayout());
        add(chartPanel, BorderLayout.CENTER);
        
        // Daten hinzufügen
        addProvider("Trades", trades);
        addLotsProvider("Lots", trades);
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
    
    public void addProvider(String providerName, List<Trade> trades) {
        TimeSeries series = new TimeSeries(providerName);
        
        // Liste der aktiven Trades
        List<Trade> activeTrades = new ArrayList<>();
        
        // Sortiere alle Trades nach OpenTime
        List<Trade> sortedTrades = new ArrayList<>(trades);
        sortedTrades.sort((t1, t2) -> t1.getOpenTime().compareTo(t2.getOpenTime()));
        
        for (Trade trade : sortedTrades) {
            // Entferne zuerst alle Trades die vor diesem Trade geschlossen wurden
            activeTrades.removeIf(t -> t.getCloseTime().compareTo(trade.getOpenTime()) <= 0);
            
            // Füge den neuen Trade hinzu
            activeTrades.add(trade);
            
            // Füge nur einen Datenpunkt zum Zeitpunkt des Trade-Opens hinzu
            series.addOrUpdate(
                new Millisecond(
                    Date.from(trade.getOpenTime().atZone(ZoneId.systemDefault()).toInstant())
                ),
                activeTrades.size()
            );
        }
        
        dataset.addSeries(series);
    }
    
    public void addLotsProvider(String providerName, List<Trade> trades) {
        TimeSeries series = new TimeSeries(providerName);
        
        // Liste der aktiven Trades
        List<Trade> activeTrades = new ArrayList<>();
        
        // Sortiere alle Trades nach OpenTime
        List<Trade> sortedTrades = new ArrayList<>(trades);
        sortedTrades.sort((t1, t2) -> t1.getOpenTime().compareTo(t2.getOpenTime()));
        
        for (Trade trade : sortedTrades) {
            // Entferne zuerst alle Trades die vor diesem Trade geschlossen wurden
            activeTrades.removeIf(t -> t.getCloseTime().compareTo(trade.getOpenTime()) <= 0);
            
            // Füge den neuen Trade hinzu
            activeTrades.add(trade);
            
            // Berechne Gesamt-Lots
            double totalLots = activeTrades.stream()
                                         .mapToDouble(Trade::getLots)
                                         .sum();
            
            // Füge nur einen Datenpunkt zum Zeitpunkt des Trade-Opens hinzu
            series.addOrUpdate(
                new Millisecond(
                    Date.from(trade.getOpenTime().atZone(ZoneId.systemDefault()).toInstant())
                ),
                totalLots
            );
        }
        
        dataset.addSeries(series);
    }
    
    public void clear() {
        dataset.removeAllSeries();
    }
    
    public JFreeChart getChart() {
        return chart;
    }
    
    public void setTitle(String title) {
        chart.setTitle(title);
    }
}