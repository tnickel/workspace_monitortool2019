package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import data.ProviderStats;
import data.Trade;

public class OpenTradesDialog extends JDialog {
    private final Map<String, ProviderStats> providerStats;
    
    public OpenTradesDialog(JFrame parent, Map<String, ProviderStats> stats) {
        super(parent, "Compare Open Trades", true);
        this.providerStats = stats;
        
        // Hauptpanel für die Charts
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Erstelle für jeden Provider zwei Charts nebeneinander
        for (Map.Entry<String, ProviderStats> entry : providerStats.entrySet()) {
            String providerName = entry.getKey();
            ProviderStats currentStats = entry.getValue();
            
            // Panel für Provider mit vergrößertem Titel
            JPanel providerPanel = new JPanel(new BorderLayout());
            Font currentFont = UIManager.getFont("TitledBorder.font");
            Font largerFont = currentFont.deriveFont(currentFont.getSize() * 2.0f);
            TitledBorder titledBorder = BorderFactory.createTitledBorder(providerName);
            titledBorder.setTitleFont(largerFont);
            providerPanel.setBorder(titledBorder);
            
            // Panel für die zwei Charts nebeneinander
            JPanel chartsPanel = new JPanel(new GridLayout(1, 2, 5, 0));
            
            // Trades Count Chart
            JFreeChart tradesChart = createTradesChart("Anzahl Offener Trades", currentStats.getTrades());
            ChartPanel tradesChartPanel = new ChartPanel(tradesChart);
            tradesChartPanel.setPreferredSize(new Dimension(400, 300));
            
            // Lots Sum Chart
            JFreeChart lotsChart = createLotsChart("Summe Offener Lots", currentStats.getTrades());
            ChartPanel lotsChartPanel = new ChartPanel(lotsChart);
            lotsChartPanel.setPreferredSize(new Dimension(400, 300));
            
            chartsPanel.add(tradesChartPanel);
            chartsPanel.add(lotsChartPanel);
            
            providerPanel.add(chartsPanel, BorderLayout.CENTER);
            mainPanel.add(providerPanel, gbc);
            gbc.gridy++;
        }

        // Scrollpane für alle Charts
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        // Dialog-Einstellungen
        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        setSize(1200, 800);  // Breiter für zwei Charts
        setLocationRelativeTo(parent);
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
            series.addOrUpdate(
                new Millisecond(
                    Date.from(entry.getKey().atZone(java.time.ZoneId.systemDefault()).toInstant())
                ),
                currentOpen
            );
        }
        
        dataset.addSeries(series);
        return createBaseChart(title, "Anzahl offener Trades", dataset);
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
            series.addOrUpdate(
                new Millisecond(
                    Date.from(entry.getKey().atZone(java.time.ZoneId.systemDefault()).toInstant())
                ),
                currentLots
            );
        }
        
        dataset.addSeries(series);
        return createBaseChart(title, "Summe der Lots", dataset);
    }
    
    private JFreeChart createBaseChart(String title, String yAxisLabel, TimeSeriesCollection dataset) {
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
        
        return chart;
    }
}