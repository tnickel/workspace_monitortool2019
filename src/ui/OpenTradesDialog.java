package ui;

import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import data.ProviderStats;
import data.Trade;
import utils.ChartFactoryUtil;

public class OpenTradesDialog extends JFrame {
   private final Map<String, ProviderStats> providerStats;
   private final ChartFactoryUtil chartFactory;

   public OpenTradesDialog(JFrame parent, Map<String, ProviderStats> stats) {
       super("Compare Open Trades");
       this.providerStats = stats;
       this.chartFactory = new ChartFactoryUtil();
       
       initializeUI();
       setSize(1500, 800);
       setLocationRelativeTo(parent);
       
       // ESC zum Schließen
       KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
       Action escapeAction = new AbstractAction() {
           public void actionPerformed(ActionEvent e) {
               dispose();
           }
       };
       getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "ESCAPE");
       getRootPane().getActionMap().put("ESCAPE", escapeAction);
   }
   
   private void initializeUI() {
       JPanel mainPanel = new JPanel();
       mainPanel.setLayout(new GridLayout(0, 1, 0, 10));
       mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
       
       for (Map.Entry<String, ProviderStats> entry : providerStats.entrySet()) {
           String providerName = entry.getKey();
           ProviderStats stats = entry.getValue();
           
           JPanel providerPanel = new JPanel(new BorderLayout());
           providerPanel.setBorder(BorderFactory.createTitledBorder(providerName));
           
           // Header Panel mit Link und Button
           JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
           
           String providerId = providerName.substring(providerName.lastIndexOf("_") + 1).replace(".csv", "");
           JLabel linkLabel = new JLabel("<html><u>https://www.mql5.com/de/signals/" + providerId + 
               "?source=Site+Signals+Subscriptions#!tab=account</u></html>");
           linkLabel.setForeground(Color.BLUE);
           linkLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
           linkLabel.addMouseListener(new MouseAdapter() {
               @Override
               public void mouseClicked(MouseEvent e) {
                   try {
                       Desktop.getDesktop().browse(new URI("https://www.mql5.com/de/signals/" + providerId + 
                           "?source=Site+Signals+Subscriptions#!tab=account"));
                   } catch (Exception ex) {
                       ex.printStackTrace();
                   }
               }
           });
           headerPanel.add(linkLabel);
           
           JButton showTradeListButton = new JButton("Show Trade List");
           showTradeListButton.addActionListener(e -> {
               TradeListFrame tradeListFrame = new TradeListFrame(providerName, stats);
               tradeListFrame.setVisible(true);
           });
           headerPanel.add(showTradeListButton);
           
           // Charts Panel
           JPanel chartsPanel = new JPanel(new GridLayout(1, 3, 10, 0));
           
           // 1. Equity Curve Chart
           ChartPanel equityChart = chartFactory.createEquityCurveChart(stats);
           equityChart.setPreferredSize(new Dimension(450, 300));
           chartsPanel.add(equityChart);
           
           // 2. Open Trades Chart (nur Trades, ohne Lots)
           TimeSeriesCollection tradeDataset = new TimeSeriesCollection();
           TimeSeries tradeSeries = new TimeSeries("Trades");
           fillTradesSeries(tradeSeries, stats.getTrades());
           tradeDataset.addSeries(tradeSeries);
           JFreeChart tradesChart = createTimeSeriesChart("Open Trades", "Trades", tradeDataset);
           ChartPanel tradesPanel = new ChartPanel(tradesChart);
           tradesPanel.setPreferredSize(new Dimension(450, 300));
           chartsPanel.add(tradesPanel);
           
           // 3. Open Lots Chart
           TimeSeriesCollection lotsDataset = new TimeSeriesCollection();
           TimeSeries lotsSeries = new TimeSeries("Lots");
           fillLotsSeries(lotsSeries, stats.getTrades());
           lotsDataset.addSeries(lotsSeries);
           JFreeChart lotsChart = createTimeSeriesChart("Open Lots", "Lots", lotsDataset);
           ChartPanel lotsPanel = new ChartPanel(lotsChart);
           lotsPanel.setPreferredSize(new Dimension(450, 300));
           chartsPanel.add(lotsPanel);
           
           providerPanel.add(headerPanel, BorderLayout.NORTH);
           providerPanel.add(chartsPanel, BorderLayout.CENTER);
           mainPanel.add(providerPanel);
       }
       
       JScrollPane scrollPane = new JScrollPane(mainPanel);
       scrollPane.getVerticalScrollBar().setUnitIncrement(16);
       add(scrollPane);
   }
   
   private JFreeChart createTimeSeriesChart(String title, String yAxisLabel, TimeSeriesCollection dataset) {
       JFreeChart chart = ChartFactory.createTimeSeriesChart(
           title,
           "Zeit",
           yAxisLabel,
           dataset,
           true,
           true,
           false
       );
       
       XYPlot plot = (XYPlot) chart.getPlot();
       plot.setBackgroundPaint(Color.WHITE);
       plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
       plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
       
       XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
       renderer.setDefaultShapesVisible(false);
       plot.setRenderer(renderer);
       
       DateAxis dateAxis = (DateAxis) plot.getDomainAxis();
       dateAxis.setDateFormatOverride(new SimpleDateFormat("yyyy-MM"));
       dateAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
       dateAxis.setLabelAngle(Math.PI / 2.0);
       
       return chart;
   }
   
   private void fillTradesSeries(TimeSeries series, List<Trade> trades) {
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
   }
   
   private void fillLotsSeries(TimeSeries series, List<Trade> trades) {
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
   }
}