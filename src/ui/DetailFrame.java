package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.text.DecimalFormat;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

import org.jfree.chart.ChartPanel;

import charts.TradeStackingChart;
import data.FavoritesManager;
import data.ProviderStats;
import utils.ChartFactoryUtil;

public class DetailFrame extends JFrame {
    private final ProviderStats stats;
    private final String providerId;
    private final DecimalFormat df = new DecimalFormat("#,##0.00");
    private final DecimalFormat pf = new DecimalFormat("#,##0.00'%'");
    private final ChartFactoryUtil chartFactory;
    private final FavoritesManager favoritesManager;
    private final JToggleButton favoriteButton;

    public DetailFrame(String providerName, ProviderStats stats, String providerId, String rootPath) {
        super("Performance Analysis: " + providerName);
        this.stats = stats;
        this.providerId = providerId;
        this.chartFactory = new ChartFactoryUtil();
        this.favoritesManager = new FavoritesManager(rootPath);
        this.favoriteButton = createFavoriteButton();
        
        initializeUI();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 3500);
        setLocationRelativeTo(null);

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
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        
        // Stats Panel oben
        JPanel statsPanel = createStatsPanel();
        mainPanel.add(statsPanel);
        
        // Favorite Button hinzufügen in Position 4
        statsPanel.add(favoriteButton);
        
        // Equity Curve Chart
        ChartPanel equityChart = chartFactory.createEquityCurveChart(stats);
        equityChart.setPreferredSize(new Dimension(950, 300));
        mainPanel.add(equityChart);
        
        // Monthly Profit Chart
        ChartPanel monthlyChart = chartFactory.createMonthlyProfitChart(stats);
        monthlyChart.setPreferredSize(new Dimension(950, 300));
        mainPanel.add(monthlyChart);
        
        // Trade Stacking Chart - deutlich größer für bessere übersicht
        TradeStackingChart stackingChart = new TradeStackingChart(stats.getTrades());
        stackingChart.setPreferredSize(new Dimension(950, 2800));
        mainPanel.add(stackingChart);
        
        // Scrollpane für das gesamte Panel
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane);
    }

    private JToggleButton createFavoriteButton() {
        JToggleButton button = new JToggleButton("Set Favorite");
        button.setSelected(favoritesManager.isFavorite(providerId));
        button.addActionListener(e -> {
            favoritesManager.toggleFavorite(providerId);
            button.setSelected(favoritesManager.isFavorite(providerId));
        });
        return button;
    }

    private JPanel createStatsPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Stats Grid
        JPanel statsPanel = new JPanel(new GridLayout(4, 4, 10, 5));
        
        // Left statistics
        addStatField(statsPanel, "Total Trades:", String.format("%d", stats.getTrades().size()));
        addStatField(statsPanel, "Total Profit:", df.format(stats.getTotalProfit()));
        addStatField(statsPanel, "Avg Profit/Trade:", df.format(stats.getAverageProfit()));
        addStatField(statsPanel, "Max Concurrent Trades:", String.format("%d", stats.getMaxConcurrentTrades()));
        
        // Right statistics
        addStatField(statsPanel, "Win Rate:", pf.format(stats.getWinRate()));
        addStatField(statsPanel, "Profit Factor:", df.format(stats.getProfitFactor()));
        addStatField(statsPanel, "Max Drawdown:", pf.format(stats.getMaxDrawdown()));
        addStatField(statsPanel, "Max Concurrent Lots:", df.format(stats.getMaxConcurrentLots()));

        // Button Panel für Trade List
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton showTradesButton = new JButton("Show Trade List");
        showTradesButton.addActionListener(e -> {
            TradeListFrame tradeListFrame = new TradeListFrame(getTitle(), stats);
            tradeListFrame.setVisible(true);
        });
        buttonPanel.add(showTradesButton);

        // Link Panel für mql5.com
        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel urlLabel = new JLabel("<html><u>https://www.mql5.com/de/signals/" + 
            providerId + "?source=Site+Signals+Subscriptions#!tab=account</u></html>");
        urlLabel.setForeground(Color.BLUE);
        urlLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        urlLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://www.mql5.com/de/signals/" + 
                        providerId + "?source=Site+Signals+Subscriptions#!tab=account"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        
        linkPanel.add(urlLabel);

        // Layout zusammenbauen
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(statsPanel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.EAST);
        
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(linkPanel, BorderLayout.CENTER);

        return mainPanel;
    }

    private void addStatField(JPanel panel, String label, String value) {
        JPanel fieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        fieldPanel.add(new JLabel(label));
        JLabel valueLabel = new JLabel(value);
        valueLabel.setForeground(new Color(0, 100, 0));  // Dunkelgrün
        fieldPanel.add(valueLabel);
        panel.add(fieldPanel);
    }
}