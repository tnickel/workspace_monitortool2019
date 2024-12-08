package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import data.ProviderStats;
import data.Trade;
import utils.ChartFactoryUtil;

public class OpenTradesDialog extends JDialog {
    private final Map<String, ProviderStats> providerStats;
    private JPanel detailPanel;
    private final ChartFactoryUtil chartFactory;
    
    public OpenTradesDialog(JFrame parent, Map<String, ProviderStats> stats) {
        super(parent, "Compare Open Trades", true);
        this.providerStats = stats;
        this.chartFactory = new ChartFactoryUtil();
        
        setLayout(new BorderLayout(5, 0));
        
        // Hauptpanel f�r die Charts
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Detail Panel f�r concurrent Trades
        detailPanel = new JPanel();
        detailPanel.setLayout(new BorderLayout());
        detailPanel.setPreferredSize(new Dimension(300, 0));
        detailPanel.setBorder(BorderFactory.createTitledBorder("Concurrent Trades"));
        
        // Erstelle f�r jeden Provider drei Charts nebeneinander
        for (Map.Entry<String, ProviderStats> entry : providerStats.entrySet()) {
            String providerName = entry.getKey();
            ProviderStats currentStats = entry.getValue();
            
            // Panel f�r Provider mit vergr��ertem Titel
            JPanel providerPanel = new JPanel(new BorderLayout());
            Font currentFont = UIManager.getFont("TitledBorder.font");
            Font largerFont = currentFont.deriveFont(currentFont.getSize() * 2.0f);
            TitledBorder titledBorder = BorderFactory.createTitledBorder(providerName);
            titledBorder.setTitleFont(largerFont);
            providerPanel.setBorder(titledBorder);
            
            // Panel f�r die drei Charts nebeneinander
            JPanel chartsPanel = new JPanel(new GridLayout(1, 3, 5, 0));
            
            // Equity Kurve (links)
            ChartPanel equityChart = chartFactory.createEquityCurveChart(currentStats);
            equityChart.setPreferredSize(new Dimension(300, 300));
            chartsPanel.add(equityChart);
            
            // Trades Count Chart (mitte)
            JFreeChart tradesChart = createTradesChart("Anzahl Offener Trades", currentStats.getTrades());
            ChartPanel tradesChartPanel = new ChartPanel(tradesChart);
            tradesChartPanel.setPreferredSize(new Dimension(300, 300));
            
            // Mouse Listener f�r Trade Details
            tradesChartPanel.addChartMouseListener(new ChartMouseListener() {
                @Override
                public void chartMouseClicked(ChartMouseEvent event) {
                    XYPlot plot = (XYPlot) tradesChart.getPlot();
                    double domainValue = plot.getDomainCrosshairValue();
                    LocalDateTime clickTime = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli((long) domainValue), 
                        ZoneId.systemDefault()
                    );
                    
                    List<Trade> activeTrades = findActiveTradesAt(currentStats.getTrades(), clickTime);
                    updateDetailPanel(activeTrades, clickTime);
                }

                @Override
                public void chartMouseMoved(ChartMouseEvent event) {
                    // Nicht ben�tigt
                }
            });
            chartsPanel.add(tradesChartPanel);
            
            // Lots Chart (rechts)
            JFreeChart lotsChart = createLotsChart("Summe Offener Lots", currentStats.getTrades());
            ChartPanel lotsChartPanel = new ChartPanel(lotsChart);
            lotsChartPanel.setPreferredSize(new Dimension(300, 300));
            chartsPanel.add(lotsChartPanel);
            
            providerPanel.add(chartsPanel, BorderLayout.CENTER);
            mainPanel.add(providerPanel, gbc);
            gbc.gridy++;
        }

        // Scrollpane f�r alle Charts
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        // Layout zusammenbauen
        add(scrollPane, BorderLayout.CENTER);
        add(detailPanel, BorderLayout.EAST);
        
        setSize(1500, 800);  // Breiter f�r drei Charts nebeneinander
        setLocationRelativeTo(null);
    }
    
    private List<Trade> findActiveTradesAt(List<Trade> trades, LocalDateTime time) {
        List<Trade> activeTrades = new ArrayList<>();
        
        for (Trade trade : trades) {
            if (trade.getOpenTime().compareTo(time) <= 0 && 
                trade.getCloseTime().compareTo(time) > 0) {
                activeTrades.add(trade);
            }
        }
        
        return activeTrades;
    }
    
    private void updateDetailPanel(List<Trade> trades, LocalDateTime time) {
        detailPanel.removeAll();
        
        JPanel content = new JPanel();
        content.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Zeitpunkt anzeigen
        content.add(new JLabel("Zeitpunkt: " + time.toString()), gbc);
        gbc.gridy++;
        
        // Anzahl Trades
        content.add(new JLabel("Anzahl aktiver Trades: " + trades.size()), gbc);
        gbc.gridy++;
        
        // Summe Lots
        double totalLots = trades.stream().mapToDouble(Trade::getLots).sum();
        content.add(new JLabel("Summe Lots: " + String.format("%.2f", totalLots)), gbc);
        gbc.gridy++;
        
        // Separator
        content.add(new JLabel(" "), gbc);
        gbc.gridy++;
        
        // Details f�r jeden Trade
        for (Trade trade : trades) {
            JPanel tradePanel = new JPanel(new GridBagLayout());
            tradePanel.setBorder(BorderFactory.createTitledBorder("Trade"));
            
            GridBagConstraints tgbc = new GridBagConstraints();
            tgbc.gridx = 0;
            tgbc.gridy = 0;
            tgbc.anchor = GridBagConstraints.WEST;
            tgbc.insets = new Insets(2, 2, 2, 2);
            
            tradePanel.add(new JLabel("Symbol: " + trade.getSymbol()), tgbc);
            tgbc.gridy++;
            tradePanel.add(new JLabel("Type: " + trade.getType()), tgbc);
            tgbc.gridy++;
            tradePanel.add(new JLabel("Lots: " + trade.getLots()), tgbc);
            tgbc.gridy++;
            tradePanel.add(new JLabel("Open Time: " + trade.getOpenTime()), tgbc);
            tgbc.gridy++;
            tradePanel.add(new JLabel("Close Time: " + trade.getCloseTime()), tgbc);
            
            content.add(tradePanel, gbc);
            gbc.gridy++;
        }

        detailPanel.add(new JScrollPane(content), BorderLayout.CENTER);
        detailPanel.revalidate();
        detailPanel.repaint();
    }
    
    private JFreeChart createTradesChart(String title, List<Trade> trades) {
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        TimeSeries series = new TimeSeries("Anzahl Trades");
        
        TreeMap<LocalDateTime, Integer> changes = new TreeMap<>();
        for (Trade trade : trades) {
            changes.merge(trade.getOpenTime(), 1, Integer::sum);
            changes.merge(trade.getCloseTime(), -1, Integer::sum);
        }
        
        int currentOpen = 0;
        for (Map.Entry<LocalDateTime, Integer> entry : changes.entrySet()) {
            currentOpen += entry.getValue();
            series.addOrUpdate(  // hier ge�ndert von add zu addOrUpdate
                new Day(
                    Date.from(entry.getKey().atZone(ZoneId.systemDefault()).toInstant())
                ),
                currentOpen
            );
        }
        
        dataset.addSeries(series);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            title,
            "Zeit",
            "Anzahl offener Trades",
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

        return chart;
    }
    
    private JFreeChart createLotsChart(String title, List<Trade> trades) {
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        TimeSeries series = new TimeSeries("Lots Summe");
        
        TreeMap<LocalDateTime, Double> changes = new TreeMap<>();
        for (Trade trade : trades) {
            changes.merge(trade.getOpenTime(), trade.getLots(), Double::sum);
            changes.merge(trade.getCloseTime(), -trade.getLots(), Double::sum);
        }
        
        double currentLots = 0.0;
        for (Map.Entry<LocalDateTime, Double> entry : changes.entrySet()) {
            currentLots += entry.getValue();
            series.addOrUpdate(  // hier ge�ndert von add zu addOrUpdate
                new Day(
                    Date.from(entry.getKey().atZone(ZoneId.systemDefault()).toInstant())
                ),
                currentLots
            );
        }
        
        dataset.addSeries(series);
        return ChartFactory.createTimeSeriesChart(
            title,
            "Zeit",
            "Summe der Lots",
            dataset,
            true,
            true,
            false
        );
    }
}