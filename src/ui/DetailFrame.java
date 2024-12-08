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
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

import org.jfree.chart.ChartPanel;

import charts.OpenTradesChart;
import data.ProviderStats;
import utils.ChartFactoryUtil;

public class DetailFrame extends JFrame {
    private final ProviderStats stats;
    private final String providerId;
    private final DecimalFormat df = new DecimalFormat("#,##0.00");
    private final DecimalFormat pf = new DecimalFormat("#,##0.00'%'");
    private final ChartFactoryUtil chartFactory;

    public DetailFrame(String providerName, ProviderStats stats, String providerId) {
        super("Performance Analysis: " + providerName);
        this.stats = stats;
        this.providerId = providerId;
        this.chartFactory = new ChartFactoryUtil();
        
        initializeUI();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1800, 1200);  // Höhe angepasst für drei Zeilen Charts
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
        statsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        mainPanel.add(statsPanel);
        
        // Erste Zeile: Equity Kurve
        ChartPanel equityChart = chartFactory.createEquityCurveChart(stats);
        equityChart.setPreferredSize(new Dimension(1700, 300));
        JPanel equityPanel = new JPanel(new BorderLayout());
        equityPanel.add(equityChart, BorderLayout.CENTER);
        equityPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));
        mainPanel.add(equityPanel);
        
        // Zweite Zeile: Offene Trades und Lots nebeneinander
        JPanel secondRowPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        
        // Offene Trades Chart (links)
        OpenTradesChart openTradesChart = new OpenTradesChart();
        openTradesChart.addProvider("Anzahl Trades", stats.getTrades());
        openTradesChart.setPreferredSize(new Dimension(840, 300));
        secondRowPanel.add(openTradesChart);
        
        // Offene Lots Chart (rechts)
        OpenTradesChart lotsChart = new OpenTradesChart();
        lotsChart.addLotsProvider("Lots Summe", stats.getTrades());
        lotsChart.setPreferredSize(new Dimension(840, 300));
        secondRowPanel.add(lotsChart);
        
        // Panel für zweite Zeile mit maximaler Höhe
        JPanel secondRowContainer = new JPanel(new BorderLayout());
        secondRowContainer.add(secondRowPanel, BorderLayout.CENTER);
        secondRowContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));
        mainPanel.add(secondRowContainer);
        
        // Dritte Zeile: Monatsübersicht
        ChartPanel monthlyProfitChart = chartFactory.createMonthlyProfitChart(stats);
        monthlyProfitChart.setPreferredSize(new Dimension(1700, 300));
        JPanel monthlyPanel = new JPanel(new BorderLayout());
        monthlyPanel.add(monthlyProfitChart, BorderLayout.CENTER);
        monthlyPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));
        mainPanel.add(monthlyPanel);
        
        // Scrollpane für das gesamte Panel
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane);
    }

    private JPanel createStatsPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Stats Grid
        JPanel statsGrid = new JPanel(new GridLayout(2, 4, 10, 5));
        statsGrid.setBorder(new EmptyBorder(5, 10, 5, 10));

        // Zeile 1
        addStatField(statsGrid, "Total Trades:", String.format("%d", stats.getTradeCount()));
        addStatField(statsGrid, "Win Rate:", pf.format(stats.getWinRate()));
        addStatField(statsGrid, "Total Profit:", df.format(stats.getTotalProfit()));
        addStatField(statsGrid, "Max Concurrent Trades:", String.format("%d", stats.getMaxConcurrentTrades()));
        
        // Zeile 2
        addStatField(statsGrid, "Avg Profit:", df.format(stats.getAverageProfit()));
        addStatField(statsGrid, "Profit Factor:", df.format(stats.getProfitFactor()));
        addStatField(statsGrid, "Max Drawdown:", pf.format(stats.getMaxDrawdown()));
        addStatField(statsGrid, "Max Concurrent Lots:", df.format(stats.getMaxConcurrentLots()));

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

        mainPanel.add(statsGrid, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.EAST);
        mainPanel.add(linkPanel, BorderLayout.SOUTH);

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