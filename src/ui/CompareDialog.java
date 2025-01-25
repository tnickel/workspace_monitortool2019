package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import data.FavoritesManager;
import data.ProviderStats;
import data.Trade;

public class CompareDialog extends JFrame {
    private final Map<String, ProviderStats> providerStats;
    private final JPanel detailPanel;
    private final String rootPath;
    private final FavoritesManager favoritesManager;

    public CompareDialog(JFrame parent, Map<String, ProviderStats> stats, String rootPath) {
        super("Compare Equity Curves");
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
        
        // ESC zum Schließen
        KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        Action escapeAction = new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispose();
            }
        };
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "ESCAPE");
        getRootPane().getActionMap().put("ESCAPE", escapeAction);
    }

    private void setupProviderPanel(String providerName, ProviderStats stats, JPanel mainPanel, GridBagConstraints gbc) {
        JPanel providerPanel = new JPanel(new BorderLayout());
        providerPanel.setBorder(BorderFactory.createEtchedBorder());
        
        JPanel headerPanel = new JPanel(new BorderLayout());
        JToggleButton favoriteToggle = createFavoriteToggle(providerName);
        
        // Title Panel mit Link und Button
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel titleLabel = new JLabel(providerName);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16));
        titlePanel.add(titleLabel);
        
        // MQL5 Link
        String providerId = providerName.substring(providerName.lastIndexOf("_") + 1).replace(".csv", "");
        JLabel linkLabel = new JLabel("<html><u>https://www.mql5.com/de/signals/" + providerId + "?source=Site+Signals+Subscriptions#!tab=account</u></html>");
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
        titlePanel.add(linkLabel);
        
        // Show Trade List Button
        JButton showTradeListButton = new JButton("Show Trade List");
        showTradeListButton.addActionListener(e -> {
            TradeListFrame tradeListFrame = new TradeListFrame(providerName, stats);
            tradeListFrame.setVisible(true);
        });
        titlePanel.add(showTradeListButton);
        
        headerPanel.add(titlePanel, BorderLayout.CENTER);
        headerPanel.add(favoriteToggle, BorderLayout.EAST);
        
        // Chart
        JFreeChart chart = createChart(providerName, stats);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(900, 300));
        
        providerPanel.add(headerPanel, BorderLayout.NORTH);
        providerPanel.add(chartPanel, BorderLayout.CENTER);
        
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