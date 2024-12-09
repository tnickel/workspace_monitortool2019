package ui;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.*;

import data.ProviderStats;
import data.Trade;
import data.FavoritesManager;

public class CompareDialog extends JDialog {
    private final Map<String, ProviderStats> providerStats;
    private final JPanel detailPanel;
    private final String rootPath;
    private final FavoritesManager favoritesManager;

    public CompareDialog(JFrame parent, Map<String, ProviderStats> stats, String rootPath) {
        super(parent, "Compare Equity Curves", true);
        this.providerStats = stats;
        this.rootPath = rootPath;
        this.favoritesManager = new FavoritesManager(rootPath);
        this.detailPanel = new JPanel();
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        for (Map.Entry<String, ProviderStats> entry : providerStats.entrySet()) {
            setupProviderPanel(entry.getKey(), entry.getValue(), mainPanel, gbc);
            gbc.gridy++;
        }

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        setSize(1000, 800);
        setLocationRelativeTo(parent);
    }

    private void setupProviderPanel(String providerName, ProviderStats stats, JPanel mainPanel, GridBagConstraints gbc) {
        JPanel providerPanel = new JPanel(new BorderLayout());
        
        // Header Panel mit Titel und Favorite Toggle
        JPanel headerPanel = new JPanel(new BorderLayout());
        JToggleButton favoriteToggle = createFavoriteToggle(providerName);
        
        // Title Panel
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel titleLabel = new JLabel(providerName);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16));
        titlePanel.add(titleLabel);
        
        headerPanel.add(titlePanel, BorderLayout.CENTER);
        headerPanel.add(favoriteToggle, BorderLayout.EAST);
        
        // Chart
        JFreeChart chart = createChart(providerName, stats);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(900, 300));
        
        providerPanel.add(headerPanel, BorderLayout.NORTH);
        providerPanel.add(chartPanel, BorderLayout.CENTER);
        providerPanel.setBorder(BorderFactory.createEtchedBorder());
        
        mainPanel.add(providerPanel, gbc);
    }

    private JToggleButton createFavoriteToggle(String providerName) {
        String providerId = providerName.substring(providerName.lastIndexOf("_") + 1).replace(".csv", "");
        JToggleButton toggle = new JToggleButton("★");
        toggle.setSelected(favoritesManager.isFavorite(providerId));
        toggle.setToolTipText("Add to Favorites");
        toggle.setFocusPainted(false);
        toggle.addActionListener(e -> {
            favoritesManager.toggleFavorite(providerId);
            toggle.setSelected(favoritesManager.isFavorite(providerId));
        });
        return toggle;
    }

    private JFreeChart createChart(String providerName, ProviderStats stats) {
        TimeSeries series = new TimeSeries(providerName);
        TimeSeriesCollection dataset = new TimeSeriesCollection(series);
        
        List<Trade> trades = stats.getTrades();
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

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setDefaultShapesVisible(false);
        plot.setRenderer(renderer);

        return chart;
    }
}