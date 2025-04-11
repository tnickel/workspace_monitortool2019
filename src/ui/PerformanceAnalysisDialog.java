package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.jfree.chart.ChartPanel;

import charts.CurrencyPairTradesChart;
import charts.DurationProfitChart;
import charts.EfficiencyChart;
import charts.MonthlyTradeCountChart;
import charts.ProviderStatHistoryChart;
import charts.SymbolDistributionChart;
import charts.ThreeMonthProfitChart;
import charts.TradeStackingChart;
import data.FavoritesManager;
import data.ProviderStats;
import db.HistoryDatabaseManager.HistoryEntry;
import services.ProviderHistoryService;
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
    private final String rootPath;
	
    public PerformanceAnalysisDialog(String providerName, ProviderStats stats, String providerId,
            HtmlDatabase htmlDatabase, String rootPath) {
        super("Performance Analysis: " + providerName);
        this.stats = stats;
        this.providerId = providerId;
        this.providerName = providerName;
        this.htmlDatabase = htmlDatabase;
        this.rootPath = rootPath;
        this.chartFactory = new ChartFactoryUtil();
        
        // DEBUG: Ausgabe des Dateinamens
        System.out.println("Reading data for file: " + providerName + ".csv");
        
        // DEBUG: Ausgabe der gelesenen Daten
        Map<String, Double> monthlyProfits = htmlDatabase.getMonthlyProfitPercentages(providerName + ".csv");
        System.out.println("Monthly profits read: " + monthlyProfits);
        
        stats.setMonthlyProfits(monthlyProfits);
        
        initializeUI();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        // Fensterbreite auf 85% statt 75% und Höhe unverändert bei 80%
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int) (screenSize.width * 0.85); // Erhöht von 75% auf 85%
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
		
		// 3MPDD History Chart hinzufügen
		ProviderStatHistoryChart statHistoryChart = new ProviderStatHistoryChart();
		statHistoryChart.setPreferredSize(chartSize);
		statHistoryChart.setAlignmentX(LEFT_ALIGNMENT);
		statHistoryChart.loadProviderHistory(providerName);
		mainPanel.add(statHistoryChart);
		mainPanel.add(javax.swing.Box.createRigidArea(new Dimension(0, 20)));
		
		TradeStackingChart stackingChart = new TradeStackingChart(stats.getTrades());
		stackingChart.setPreferredSize(chartSize);
		stackingChart.setAlignmentX(LEFT_ALIGNMENT);
		mainPanel.add(stackingChart);
		mainPanel.add(javax.swing.Box.createRigidArea(new Dimension(0, 20)));
		
		CurrencyPairTradesChart currencyPairTradesChart = new CurrencyPairTradesChart(stats.getTrades());
		currencyPairTradesChart.setPreferredSize(new Dimension(950, 650)); // Höher für beide Diagramme
		currencyPairTradesChart.setAlignmentX(LEFT_ALIGNMENT);
		mainPanel.add(currencyPairTradesChart);
		mainPanel.add(javax.swing.Box.createRigidArea(new Dimension(0, 20)));
		
		
		DurationProfitChart durationChart = new DurationProfitChart(stats.getTrades());
		durationChart.setPreferredSize(chartSize);
		durationChart.setAlignmentX(LEFT_ALIGNMENT);
		mainPanel.add(durationChart);
		mainPanel.add(javax.swing.Box.createRigidArea(new Dimension(0, 20)));
		
		// Neue EfficiencyChart hinzufügen
		EfficiencyChart efficiencyChart = new EfficiencyChart(stats.getTrades());
		efficiencyChart.setPreferredSize(chartSize);
		efficiencyChart.setAlignmentX(LEFT_ALIGNMENT);
		mainPanel.add(efficiencyChart);
		mainPanel.add(javax.swing.Box.createRigidArea(new Dimension(0, 20)));
		
		// Neue MonthlyTradeCountChart hinzufügen
		MonthlyTradeCountChart tradeCountChart = new MonthlyTradeCountChart(stats.getTrades());
		tradeCountChart.setPreferredSize(chartSize);
		tradeCountChart.setAlignmentX(LEFT_ALIGNMENT);
		mainPanel.add(tradeCountChart);
		mainPanel.add(javax.swing.Box.createRigidArea(new Dimension(0, 22)));
		
		ChartPanel weekdayProfitChart = chartFactory.createWeekdayProfitChart(stats);
		weekdayProfitChart.setPreferredSize(chartSize);
		weekdayProfitChart.setAlignmentX(LEFT_ALIGNMENT);
		mainPanel.add(weekdayProfitChart);
		mainPanel.add(javax.swing.Box.createRigidArea(new Dimension(0, 20)));
		
		// In der initializeUI()-Methode, nach einem anderen Diagramm
		ChartPanel martingaleChart = chartFactory.createMartingaleVisualizationChart(stats);
		martingaleChart.setPreferredSize(chartSize);
		martingaleChart.setAlignmentX(LEFT_ALIGNMENT);
		mainPanel.add(martingaleChart);
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
	
	private JPanel createStatsPanel() {
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
	        public void mouseClicked(java.awt.event.MouseEvent evt) {
	            try {
	                Desktop.getDesktop()
	                        .browse(new URI(String.format(
	                                "https://www.mql5.com/de/signals/%s?source=Site+Signals+Subscriptions#!tab=account",
	                                providerId)));
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        }
	    });
	    
	    urlPanel.add(urlLabel);
	    
	    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
	    
	    // Den FavoritesManager initialisieren
	    FavoritesManager favoritesManager = new FavoritesManager(rootPath);
	    
	    // Überprüfen, ob der aktuelle Provider bereits ein Favorit ist
	    boolean isFavorite = favoritesManager.isFavorite(providerId);
	    
	    JButton favButton = new JButton(isFavorite ? "Remove Favorite" : "Set Favorite");
	    favButton.setBackground(isFavorite ? Color.YELLOW : Color.WHITE);
	    
	    JButton showTradesButton = new JButton("Show Trade List");
	    
	    // Neuer Button für Datenbank-Informationen
	    JButton showDbInfoButton = new JButton("Show DB Info");
	    showDbInfoButton.addActionListener(e -> showDatabaseInfo());
	    
	    favButton.addActionListener(e -> {
	        // Die Favoriten umschalten
	        favoritesManager.toggleFavorite(providerId);
	        
	        // Aktualisiere die Button-Anzeige
	        boolean isNowFavorite = favoritesManager.isFavorite(providerId);
	        if (isNowFavorite) {
	            favButton.setText("Remove Favorite");
	            favButton.setBackground(Color.YELLOW);
	        } else {
	            favButton.setText("Set Favorite");
	            favButton.setBackground(Color.WHITE);
	        }
	        
	        System.out.println("Favorit-Status für Provider " + providerId + " geändert: " + isNowFavorite);
	    });
	    
	    showTradesButton.addActionListener(e -> {
	        TradeListFrame tradeListFrame = new TradeListFrame(getTitle(), stats);
	        tradeListFrame.setVisible(true);
	    });
	    
	    buttonPanel.add(favButton);
	    buttonPanel.add(showTradesButton);
	    buttonPanel.add(showDbInfoButton); // Neuen Button hinzufügen
	    
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
	private void showDatabaseInfo() {
	    // ProviderHistoryService und die Datenbank-Informationen abrufen
	    ProviderHistoryService historyService = ProviderHistoryService.getInstance();
	    
	    // Alle verfügbaren Statistiktypen abrufen
	    List<HistoryEntry> mpddHistory = historyService.get3MpddHistory(providerName);
	    
	    // Dialog erstellen
	    JDialog dbInfoDialog = new JDialog(this, "Datenbank-Informationen für " + providerName, true);
	    dbInfoDialog.setLayout(new BorderLayout(10, 10));
	    
	    // Erstelle ein Modell für die Tabelle mit den Datenbankinformationen
	    DefaultTableModel model = new DefaultTableModel();
	    model.addColumn("Datum");
	    model.addColumn("Statistiktyp");
	    model.addColumn("Wert");
	    
	    // Füge die MPDD-Werte hinzu
	    for (HistoryEntry entry : mpddHistory) {
	        model.addRow(new Object[] {
	            entry.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
	            "3MPDD",
	            String.format("%.4f", entry.getValue())
	        });
	    }
	    
	    // Erstelle die Tabelle und füge sie zum Dialog hinzu
	    JTable table = new JTable(model);
	    table.getColumnModel().getColumn(0).setPreferredWidth(200);
	    table.getColumnModel().getColumn(1).setPreferredWidth(100);
	    table.getColumnModel().getColumn(2).setPreferredWidth(100);
	    
	    // Optional: Bessere Formatierung für Werte
	    table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
	        @Override
	        public Component getTableCellRendererComponent(JTable table, Object value,
	                                                     boolean isSelected, boolean hasFocus, int row, int column) {
	            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	            if (column == 2 && value != null) {
	                try {
	                    double val = Double.parseDouble(value.toString().replace(",", "."));
	                    if (val > 0) {
	                        c.setForeground(new Color(0, 150, 0));
	                    } else {
	                        c.setForeground(Color.RED);
	                    }
	                } catch (Exception e) {
	                    // Ignorieren, falls kein gültiger Zahlenwert
	                }
	            }
	            return c;
	        }
	    });
	    
	    JScrollPane scrollPane = new JScrollPane(table);
	    scrollPane.setPreferredSize(new Dimension(500, 300));
	    
	    // Informationstext hinzufügen, wenn keine Daten vorhanden
	    if (mpddHistory.isEmpty()) {
	        JLabel noDataLabel = new JLabel("Keine Datenbank-Einträge für diesen Provider vorhanden.");
	        noDataLabel.setHorizontalAlignment(JLabel.CENTER);
	        dbInfoDialog.add(noDataLabel, BorderLayout.CENTER);
	    } else {
	        dbInfoDialog.add(scrollPane, BorderLayout.CENTER);
	    }
	    
	    // Schließen-Button hinzufügen
	    JButton closeButton = new JButton("Schließen");
	    closeButton.addActionListener(e -> dbInfoDialog.dispose());
	    
	    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
	    buttonPanel.add(closeButton);
	    dbInfoDialog.add(buttonPanel, BorderLayout.SOUTH);
	    
	    // Dialog anzeigen
	    dbInfoDialog.pack();
	    dbInfoDialog.setLocationRelativeTo(this);
	    dbInfoDialog.setVisible(true);
	}
}