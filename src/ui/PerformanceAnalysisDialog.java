package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.net.URI;
import java.text.DecimalFormat;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
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

import charts.DrawdownChart;
import charts.DurationProfitChart;
import charts.SymbolDistributionChart;
import charts.ThreeMonthProfitChart;
import charts.TradeStackingChart;
import data.ProviderStats;
import utils.ChartFactoryUtil;
import utils.HtmlDatabase;

public class PerformanceAnalysisDialog extends JFrame
{
	private final ProviderStats stats;
	private final String providerId;
	private final String providerName;
	private final DecimalFormat df = new DecimalFormat("#,##0.00");
	private final DecimalFormat pf = new DecimalFormat("#,##0.00'%'");
	private final ChartFactoryUtil chartFactory;
	private final HtmlDatabase htmlDatabase;
	
	public PerformanceAnalysisDialog(String providerName, ProviderStats stats, String providerId,
			HtmlDatabase htmlDatabase)
	{
		super("Performance Analysis: " + providerName);
		this.stats = stats;
		this.providerId = providerId;
		this.providerName = providerName;
		this.htmlDatabase = htmlDatabase;
		this.chartFactory = new ChartFactoryUtil();
		
		// DEBUG: Ausgabe des Dateinamens
		System.out.println("Reading data for file: " + providerName + ".csv");
		
		// DEBUG: Ausgabe der gelesenen Daten
		Map<String, Double> monthlyProfits = htmlDatabase.getMonthlyProfitPercentages(providerName + ".csv");
		System.out.println("Monthly profits read: " + monthlyProfits);
		
		stats.setMonthlyProfits(monthlyProfits);
		
		initializeUI();
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		// Breite um 30% erhöhen und Höhe um 10% reduzieren
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = (int) (screenSize.width * 0.75); // 75% der Bildschirmbreite
		int height = (int) (screenSize.height * 0.8); // 80% der Bildschirmhöhe
		setSize(width, height);
		
		setLocationRelativeTo(null);
		
		KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
		Action escapeAction = new AbstractAction() {
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				dispose();
			}
		};
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "ESCAPE");
		getRootPane().getActionMap().put("ESCAPE", escapeAction);
	}
	
	private void initializeUI()
	{
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		
		JPanel statsPanel = createStatsPanel();
		statsPanel.setAlignmentX(LEFT_ALIGNMENT);
		mainPanel.add(statsPanel);
		mainPanel.add(javax.swing.Box.createRigidArea(new Dimension(0, 20)));
		
		Dimension chartSize = new Dimension(950, 300);
		
		ChartPanel equityChart = chartFactory.createEquityCurveChart(stats);
		equityChart.setPreferredSize(chartSize);
		equityChart.setAlignmentX(LEFT_ALIGNMENT);
		mainPanel.add(equityChart);
		mainPanel.add(javax.swing.Box.createRigidArea(new Dimension(0, 20)));
		
		ChartPanel monthlyChart = chartFactory.createMonthlyProfitChart(stats);
		monthlyChart.setPreferredSize(chartSize);
		monthlyChart.setAlignmentX(LEFT_ALIGNMENT);
		mainPanel.add(monthlyChart);
		mainPanel.add(javax.swing.Box.createRigidArea(new Dimension(0, 20)));
		
		// Neue ThreeMonthProfitChart anstelle der Risk Exposure Chart
		Map<String, Double> monthlyProfits = htmlDatabase.getMonthlyProfitPercentages(providerName + ".csv");
		double equityDrawdown = htmlDatabase.getEquityDrawdown(providerName);
		ThreeMonthProfitChart profitChart = new ThreeMonthProfitChart(monthlyProfits, equityDrawdown);
		profitChart.setPreferredSize(chartSize);
		profitChart.setAlignmentX(LEFT_ALIGNMENT);
		mainPanel.add(profitChart);
		mainPanel.add(javax.swing.Box.createRigidArea(new Dimension(0, 20)));
		
		TradeStackingChart stackingChart = new TradeStackingChart(stats.getTrades());
		stackingChart.setPreferredSize(chartSize);
		stackingChart.setAlignmentX(LEFT_ALIGNMENT);
		mainPanel.add(stackingChart);
		mainPanel.add(javax.swing.Box.createRigidArea(new Dimension(0, 20)));
		
		DurationProfitChart durationChart = new DurationProfitChart(stats.getTrades());
		durationChart.setPreferredSize(chartSize);
		durationChart.setAlignmentX(LEFT_ALIGNMENT);
		mainPanel.add(durationChart);
		mainPanel.add(javax.swing.Box.createRigidArea(new Dimension(0, 20)));
		
		DrawdownChart drawdownChart = new DrawdownChart(stats.getTrades(), stats.getInitialBalance());
		drawdownChart.setPreferredSize(chartSize);
		drawdownChart.setAlignmentX(LEFT_ALIGNMENT);
		mainPanel.add(drawdownChart);
		mainPanel.add(javax.swing.Box.createRigidArea(new Dimension(0, 20)));
		
		SymbolDistributionChart symbolChart = new SymbolDistributionChart(stats.getTrades());
		symbolChart.setPreferredSize(chartSize);
		symbolChart.setAlignmentX(LEFT_ALIGNMENT);
		mainPanel.add(symbolChart);
		
		JScrollPane scrollPane = new JScrollPane(mainPanel);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		scrollPane.setBorder(null);
		add(scrollPane);
	}
	
	private JPanel createStatsPanel()
	{
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
		
		// Ändere das Grid-Layout auf 2 Zeilen und 7 Spalten für mehr Platz
		JPanel statsGrid = new JPanel(new GridLayout(2, 7, 15, 5));
		
		String csvFileName = providerName + ".csv";
		double equityDrawdown = htmlDatabase.getEquityDrawdown(csvFileName);
		double threeMonthProfit = htmlDatabase.getAverageMonthlyProfit(csvFileName, 3);
		double sixMonthProfit = htmlDatabase.getAverageMonthlyProfit(csvFileName, 6);
		double nineMonthProfit = htmlDatabase.getAverageMonthlyProfit(csvFileName, 9);
		double twelveMonthProfit = htmlDatabase.getAverageMonthlyProfit(csvFileName, 12);
		
		double mpdd3 = calculateMPDD(threeMonthProfit, equityDrawdown);
		double mpdd6 = calculateMPDD(sixMonthProfit, equityDrawdown);
		double mpdd9 = calculateMPDD(nineMonthProfit, equityDrawdown);
		
		// Erste Zeile
		addStatField(statsGrid, "Total Trades: ", String.format("%d", stats.getTrades().size()));
		addStatField(statsGrid, "Win Rate: ", pf.format(stats.getWinRate()));
		addStatField(statsGrid, "Total Profit: ", df.format(stats.getTotalProfit()));
		addStatField(statsGrid, "Profit Factor: ", df.format(stats.getProfitFactor()));
		addStatField(statsGrid, "Max Concurrent Lots: ", df.format(stats.getMaxConcurrentLots()));
		addStatField(statsGrid, "Stability: ", df.format(htmlDatabase.getStabilitaetswert(csvFileName)));
		addStatField(statsGrid, "Days: ", String.format("%d", calculateDaysBetween(stats)));
		
		// Zweite Zeile
		addStatField(statsGrid, "Avg Profit/Trade: ", df.format(stats.getAverageProfit()));
		addStatField(statsGrid, "Max Drawdown: ", pf.format(stats.getMaxDrawdown()));
		addStatField(statsGrid, "Equity Drawdown: ", pf.format(equityDrawdown));
		addStatField(statsGrid, "3MPDD: ", df.format(mpdd3));
		addStatField(statsGrid, "6MPDD: ", df.format(mpdd6));
		addStatField(statsGrid, "9MPDD: ", df.format(mpdd9));
		addStatField(statsGrid, "Steigung: ", df.format(htmlDatabase.getSteigungswert(csvFileName)));
		
		JPanel urlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		String urlText = String.format(
				"<html><u>https://www.mql5.com/de/signals/%s?source=Site+Signals+Subscriptions#!tab=account</u></html>",
				providerId);
		JLabel urlLabel = new JLabel(urlText);
		urlLabel.setForeground(Color.BLUE);
		urlLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		
		urlLabel.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent evt)
			{
				try
				{
					Desktop.getDesktop()
							.browse(new URI(String.format(
									"https://www.mql5.com/de/signals/%s?source=Site+Signals+Subscriptions#!tab=account",
									providerId)));
				} catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
		
		urlPanel.add(urlLabel);
		
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton favButton = new JButton("Set Favorite");
		JButton showTradesButton = new JButton("Show Trade List");
		
		favButton.setBackground(Color.WHITE);
		favButton.addActionListener(e ->
		{
			if (favButton.getBackground() == Color.WHITE)
			{
				favButton.setBackground(Color.YELLOW);
				favButton.setText("Remove Favorite");
			} else
			{
				favButton.setBackground(Color.WHITE);
				favButton.setText("Set Favorite");
			}
		});
		
		showTradesButton.addActionListener(e ->
		{
			TradeListFrame tradeListFrame = new TradeListFrame(getTitle(), stats);
			tradeListFrame.setVisible(true);
		});
		
		buttonPanel.add(favButton);
		buttonPanel.add(showTradesButton);
		
		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.add(statsGrid, BorderLayout.CENTER);
		topPanel.add(buttonPanel, BorderLayout.EAST);
		
		mainPanel.add(topPanel, BorderLayout.NORTH);
		mainPanel.add(urlPanel, BorderLayout.CENTER);
		
		return mainPanel;
	}
	
	private long calculateDaysBetween(ProviderStats stats)
	{
		return Math.abs(ChronoUnit.DAYS.between(stats.getStartDate(), stats.getEndDate())) + 1;
	}
	
	private double calculateMPDD(double monthlyProfitPercent, double maxEquityDrawdown)
	{
		if (maxEquityDrawdown == 0.0)
		{
			return 0.0; // Verhindert Division durch Null
		}
		return monthlyProfitPercent / maxEquityDrawdown;
	}
	
	private void addStatField(JPanel panel, String label, String value)
	{
		JPanel fieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		
		JLabel labelComponent = new JLabel(label);
		labelComponent.setFont(new Font("SansSerif", Font.PLAIN, 12));
		fieldPanel.add(labelComponent);
		
		JLabel valueComponent = new JLabel(value);
		valueComponent.setFont(new Font("SansSerif", Font.BOLD, 12));
		valueComponent.setForeground(new Color(0, 100, 0));
		fieldPanel.add(valueComponent);
		
		panel.add(fieldPanel);
	}
}