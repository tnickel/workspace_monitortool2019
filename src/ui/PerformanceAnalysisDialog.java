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
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import charts.CurrencyPairTradesChart;
import charts.DurationProfitChart;
import charts.EfficiencyChart;
import charts.MonthlyTradeCountChart;
import charts.ProviderStatHistoryChart;
import charts.SymbolDistributionChart;
import charts.ThreeMonthProfitChart;
import charts.TradeStackingChart;
import charts.WeeklyLotsizeChart;
import data.FavoritesManager;
import data.ProviderStats;
import db.HistoryDatabaseManager.HistoryEntry;
import services.ProviderHistoryService;
import utils.ChartFactoryUtil;
import utils.HtmlDatabase;

public class PerformanceAnalysisDialog extends JFrame {
    private final ProviderStats stats;
    private final String providerId;
    private final String providerName;
    private final DecimalFormat df = new DecimalFormat("#,##0.00");
    private final DecimalFormat pf = new DecimalFormat("#,##0.00'%'");
    private final ChartFactoryUtil chartFactory;
    private final HtmlDatabase htmlDatabase;
    private final String rootPath;
    
    // Definierte Farben für das neue Design
    private static final Color PRIMARY_COLOR = new Color(26, 45, 90); // #1A2D5A - Dunkelblau
    private static final Color SECONDARY_COLOR = new Color(62, 125, 204); // #3E7DCC - Helleres Blau
    private static final Color ACCENT_COLOR = new Color(255, 209, 102); // #FFD166 - Gold/Gelb
    private static final Color BG_COLOR = new Color(245, 247, 250); // #F5F7FA - Sehr helles Grau
    private static final Color TEXT_COLOR = new Color(51, 51, 51); // #333333 - Dunkelgrau
    private static final Color TEXT_SECONDARY_COLOR = new Color(85, 85, 85); // #555555 - Helleres Grau
    private static final Color POSITIVE_COLOR = new Color(46, 139, 87); // #2E8B57 - Grün
    private static final Color NEGATIVE_COLOR = new Color(204, 59, 59); // #CC3B3B - Rot
    
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
        
        // Fensterbreite auf 85% der Bildschirmbreite
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int) (screenSize.width * 0.85);
        int height = (int) (screenSize.height * 0.8);
        setSize(width, height);
        
        setLocationRelativeTo(null);
        
        KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        Action escapeAction = new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispose();
            }
        };
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "ESCAPE");
        getRootPane().getActionMap().put("ESCAPE", escapeAction);
    }
    
    private void initializeUI() {
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(BG_COLOR);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Kompaktes Stats-Panel erstellen
        JPanel compactStatsPanel = createCompactStatsPanel();
        compactStatsPanel.setAlignmentX(LEFT_ALIGNMENT);
        mainPanel.add(compactStatsPanel);
        mainPanel.add(javax.swing.Box.createRigidArea(new Dimension(0, 15)));
        
        // Kleinere Diagrammgröße für bessere Darstellung auf dem Bildschirm
        // Reduzierung um 30% in der Breite
        Dimension chartSize = new Dimension(665, 240); // 950 * 0.7 = 665, 300 * 0.8 = 240
        
        // Charts hinzufügen
        addChartToPanel(mainPanel, chartFactory.createEquityCurveChart(stats), "Equity Curve", chartSize);
        addChartToPanel(mainPanel, chartFactory.createMonthlyProfitChart(stats), "Monthly Performance Overview", chartSize);
        
        // MPDD Chart
        Map<String, Double> monthlyProfits = htmlDatabase.getMonthlyProfitPercentages(providerName + ".csv");
        double equityDrawdown = htmlDatabase.getEquityDrawdown(providerName);
        ThreeMonthProfitChart profitChart = new ThreeMonthProfitChart(monthlyProfits, equityDrawdown);
        addChartToPanel(mainPanel, profitChart, "3-Month Profit & Drawdown Analysis", chartSize);
        
        // MPDD History Chart
        ProviderStatHistoryChart statHistoryChart = new ProviderStatHistoryChart();
        statHistoryChart.loadProviderHistory(providerName);
        addChartToPanel(mainPanel, statHistoryChart, "3MPDD History", chartSize);
        
        // Stacking Chart
        TradeStackingChart stackingChart = new TradeStackingChart(stats.getTrades());
        addChartToPanel(mainPanel, stackingChart, "Trade Stacking Analysis", chartSize);
        
        // Currency Pair Chart (etwas höher, da es zwei Diagramme enthält)
        CurrencyPairTradesChart currencyPairTradesChart = new CurrencyPairTradesChart(stats.getTrades());
        addChartToPanel(mainPanel, currencyPairTradesChart, "Currency Pair Analysis", new Dimension(665, 920));
        
        // Weitere Charts
        addChartToPanel(mainPanel, new DurationProfitChart(stats.getTrades()), "Duration vs Profit Analysis", chartSize);
        addChartToPanel(mainPanel, new EfficiencyChart(stats.getTrades()), "Trading Efficiency Analysis", chartSize);
        addChartToPanel(mainPanel, new WeeklyLotsizeChart(stats.getTrades()), "Weekly Lot Size Analysis", chartSize);
        addChartToPanel(mainPanel, new MonthlyTradeCountChart(stats.getTrades()), "Monthly Trade Count", chartSize);
        addChartToPanel(mainPanel, chartFactory.createWeekdayProfitChart(stats), "Profit by Weekday", chartSize);
        addChartToPanel(mainPanel, chartFactory.createMartingaleVisualizationChart(stats), "Martingale Strategy Detection", chartSize);
        addChartToPanel(mainPanel, new SymbolDistributionChart(stats.getTrades()), "Symbol Distribution", chartSize);
        
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);
        scrollPane.setBackground(BG_COLOR);
        add(scrollPane);
    }
    
    // Hilfsmethode zum Hinzufügen eines Charts zum Panel
    private void addChartToPanel(JPanel panel, Component chartComponent, String title, Dimension size) {
        // Besondere Größe für Duration vs Profit Chart
        if (title.contains("Duration vs Profit")) {
            // Größeres Panel für die Duration-Grafik
            size = new Dimension(size.width, 500); // Höhe von 240 auf 500 erhöht
        }
        
        // Chart-Größe anpassen
        chartComponent.setPreferredSize(size);
        chartComponent.setMaximumSize(size);
        chartComponent.setMinimumSize(size);
        
        // Chart in Panel mit Rahmen verpacken
        JPanel chartPanel = new JPanel(new BorderLayout());
        chartPanel.setOpaque(false);
        chartPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(SECONDARY_COLOR, 1),
                title,
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 12),
                SECONDARY_COLOR
            ),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        chartPanel.add(chartComponent, BorderLayout.CENTER);
        chartPanel.setAlignmentX(LEFT_ALIGNMENT);
        
        // Panel hinzufügen und Abstand einfügen
        panel.add(chartPanel);
        panel.add(javax.swing.Box.createRigidArea(new Dimension(0, 15)));
    }
    
    // Neues kompakteres Stats-Panel ohne Lücke links
    private JPanel createCompactStatsPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new CompoundBorder(
            new LineBorder(SECONDARY_COLOR, 1),
            new EmptyBorder(10, 10, 10, 10)
        ));
        
        // Obere Zeile mit Provider-Name und URL
        JPanel headerPanel = new JPanel(new BorderLayout(5, 0));
        headerPanel.setOpaque(false);
        
        // Provider-Name
        JLabel titleLabel = new JLabel(providerName);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(PRIMARY_COLOR);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        // URL als klickbarer Link
        String urlText = String.format(
                "<html><u>https://www.mql5.com/de/signals/%s?source=Site+Signals+Subscriptions#!tab=account</u></html>",
                providerId);
        JLabel urlLabel = new JLabel(urlText);
        urlLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        urlLabel.setForeground(SECONDARY_COLOR);
        urlLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        urlLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                try {
                    Desktop.getDesktop().browse(new URI(String.format(
                            "https://www.mql5.com/de/signals/%s?source=Site+Signals+Subscriptions#!tab=account",
                            providerId)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        
        JPanel urlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        urlPanel.setOpaque(false);
        urlPanel.add(urlLabel);
        headerPanel.add(urlPanel, BorderLayout.EAST);
        
        // Statistik-Werte in einem Raster anordnen
        JPanel statsGrid = new JPanel(new GridLayout(2, 7, 10, 5));
        statsGrid.setOpaque(false);
        
        String csvFileName = providerName + ".csv";
        double equityDrawdown = htmlDatabase.getEquityDrawdown(csvFileName);
        double threeMonthProfit = htmlDatabase.getAverageMonthlyProfit(csvFileName, 3);
        double sixMonthProfit = htmlDatabase.getAverageMonthlyProfit(csvFileName, 6);
        double nineMonthProfit = htmlDatabase.getAverageMonthlyProfit(csvFileName, 9);
        
        double mpdd3 = calculateMPDD(threeMonthProfit, equityDrawdown);
        double mpdd6 = calculateMPDD(sixMonthProfit, equityDrawdown);
        double mpdd9 = calculateMPDD(nineMonthProfit, equityDrawdown);
        
        // Steigungswert von der HtmlDatabase holen statt zu berechnen
        //double steigungswert = htmlDatabase.getSteigungswert(csvFileName);
        
        // Erste Zeile mit Statistiken
        addStatField(statsGrid, "Total Trades: ", String.format("%d", stats.getTrades().size()));
        addStatField(statsGrid, "Win Rate: ", pf.format(stats.getWinRate()));
        addStatField(statsGrid, "Total Profit: ", df.format(stats.getTotalProfit()));
        addStatField(statsGrid, "Profit Factor: ", df.format(stats.getProfitFactor()));
        addStatField(statsGrid, "Max Concurrent Lots: ", df.format(stats.getMaxConcurrentLots()));
        addStatField(statsGrid, "Stability: ", df.format(htmlDatabase.getStabilitaetswert(csvFileName)));
        addStatField(statsGrid, "Days: ", String.format("%d", calculateDaysBetween(stats)));
        
        // Zweite Zeile mit Statistiken
        addStatField(statsGrid, "Avg Profit/Trade: ", df.format(stats.getAverageProfit()));
        addStatField(statsGrid, "Equity Drawdown: ", pf.format(equityDrawdown));
        addStatField(statsGrid, "3MPDD: ", df.format(mpdd3));
        addStatField(statsGrid, "6MPDD: ", df.format(mpdd6));
        addStatField(statsGrid, "9MPDD: ", df.format(mpdd9));
        //addStatField(statsGrid, "Steigung: ", df.format(steigungswert));
        
        // Buttons für Aktionen
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setOpaque(false);
        
        // Den FavoritesManager initialisieren
        FavoritesManager favoritesManager = new FavoritesManager(rootPath);
        boolean isFavorite = favoritesManager.isFavorite(providerId);
        
        JButton favButton = createStyledButton(isFavorite ? "Remove Favorite" : "Set Favorite");
        if (isFavorite) {
            favButton.setBackground(ACCENT_COLOR);
            favButton.setForeground(TEXT_COLOR);
        }
        
        JButton showTradesButton = createStyledButton("Show Trade List");
        JButton showDbInfoButton = createStyledButton("Show DB Info");
        
        favButton.addActionListener(e -> {
            favoritesManager.toggleFavorite(providerId);
            boolean isNowFavorite = favoritesManager.isFavorite(providerId);
            favButton.setText(isNowFavorite ? "Remove Favorite" : "Set Favorite");
            
            if (isNowFavorite) {
                favButton.setBackground(ACCENT_COLOR);
                favButton.setForeground(TEXT_COLOR);
            } else {
                favButton.setBackground(SECONDARY_COLOR);
                favButton.setForeground(Color.WHITE);
            }
            
            System.out.println("Favorit-Status für Provider " + providerId + " geändert: " + isNowFavorite);
        });
        
        showTradesButton.addActionListener(e -> {
            TradeListFrame tradeListFrame = new TradeListFrame(getTitle(), stats);
            tradeListFrame.setVisible(true);
        });
        
        showDbInfoButton.addActionListener(e -> showDatabaseInfo());
        
        buttonPanel.add(favButton);
        buttonPanel.add(showTradesButton);
        buttonPanel.add(showDbInfoButton);
        
        // Alles zusammenfügen
        JPanel contentPanel = new JPanel(new BorderLayout(0, 10));
        contentPanel.setOpaque(false);
        contentPanel.add(headerPanel, BorderLayout.NORTH);
        contentPanel.add(statsGrid, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        
        return mainPanel;
    }
    
    // Hilfsmethode zum Erstellen eines Buttons
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(SECONDARY_COLOR);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 90, 150), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        return button;
    }
    
    private long calculateDaysBetween(ProviderStats stats) {
        return Math.abs(ChronoUnit.DAYS.between(stats.getStartDate(), stats.getEndDate())) + 1;
    }
    
    private double calculateMPDD(double monthlyProfitPercent, double maxEquityDrawdown) {
        if (maxEquityDrawdown == 0.0) {
            return 0.0; // Verhindert Division durch Null
        }
        return monthlyProfitPercent / maxEquityDrawdown;
    }
    
    private void addStatField(JPanel panel, String label, String value) {
        JPanel fieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        fieldPanel.setOpaque(false);
        
        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(new Font("SansSerif", Font.BOLD, 12));
        labelComponent.setForeground(TEXT_COLOR);
        fieldPanel.add(labelComponent);
        
        JLabel valueComponent = new JLabel(value);
        valueComponent.setFont(new Font("SansSerif", Font.BOLD, 13));
        
        // Farbliche Formatierung je nach Wert
        if (label.contains("Profit") || label.contains("Win Rate") || label.contains("MPDD") || label.contains("Stability")) {
            try {
                double numValue = Double.parseDouble(value.replace("%", "").replace(",", "."));
                if (numValue > 0) {
                    valueComponent.setForeground(POSITIVE_COLOR);
                } else if (numValue < 0) {
                    valueComponent.setForeground(NEGATIVE_COLOR);
                } else {
                    valueComponent.setForeground(TEXT_COLOR);
                }
            } catch (NumberFormatException e) {
                valueComponent.setForeground(TEXT_COLOR);
            }
        } else {
            valueComponent.setForeground(TEXT_SECONDARY_COLOR);
        }
        
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
        dbInfoDialog.getContentPane().setBackground(BG_COLOR);
        
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
        table.setRowHeight(25);
        table.setBackground(Color.WHITE);
        table.setForeground(TEXT_COLOR);
        table.setGridColor(new Color(230, 230, 230));
        table.getTableHeader().setBackground(SECONDARY_COLOR);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        
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
                            c.setForeground(POSITIVE_COLOR);
                        } else {
                            c.setForeground(NEGATIVE_COLOR);
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
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Informationstext hinzufügen, wenn keine Daten vorhanden
        if (mpddHistory.isEmpty()) {
            JLabel noDataLabel = new JLabel("Keine Datenbank-Einträge für diesen Provider vorhanden.");
            noDataLabel.setHorizontalAlignment(JLabel.CENTER);
            noDataLabel.setForeground(TEXT_COLOR);
            noDataLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));
            dbInfoDialog.add(noDataLabel, BorderLayout.CENTER);
        } else {
            dbInfoDialog.add(scrollPane, BorderLayout.CENTER);
        }
        
        // Schließen-Button hinzufügen
        JButton closeButton = createStyledButton("Schließen");
        closeButton.addActionListener(e -> dbInfoDialog.dispose());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        buttonPanel.add(closeButton);
        dbInfoDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        // Dialog anzeigen
        dbInfoDialog.pack();
        dbInfoDialog.setLocationRelativeTo(this);
        dbInfoDialog.setVisible(true);
    }
}