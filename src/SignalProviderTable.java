import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class SignalProviderTable {
    public static void main(String[] args) {
        String rootPath = "c:\\tmp\\mql5";
        String downloadPath = rootPath + "\\download";

        Map<String, ProviderStats> signalProviderStats = new HashMap<>();
        File downloadDirectory = new File(downloadPath);

        if (downloadDirectory.exists() && downloadDirectory.isDirectory()) {
            File[] files = downloadDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
            if (files != null) {
                for (File file : files) {
                    processFile(file, signalProviderStats);
                }
            }
        }

        SwingUtilities.invokeLater(() -> createAndShowGUI(signalProviderStats));
    }

    private static void processFile(File file, Map<String, ProviderStats> signalProviderStats) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isHeader = true;
            ProviderStats stats = new ProviderStats();
            
            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                
                if (line.trim().isEmpty()) continue;
                
                String[] data = line.split(";", -1);
                if (line.contains("Balance")) {
                    double balance = Double.parseDouble(data[data.length - 1]);
                    stats.setInitialBalance(balance);
                    continue;
                }
                
                try {
                    LocalDate tradeDate = LocalDate.parse(data[0].substring(0, 10), 
                        DateTimeFormatter.ofPattern("yyyy.MM.dd"));
                    double profit = Double.parseDouble(data[data.length - 1]);
                    stats.addTrade(profit, tradeDate);
                } catch (Exception e) {
                    continue;
                }
            }
            
            if (!stats.getProfits().isEmpty()) {
                signalProviderStats.put(file.getName(), stats);
                System.out.println("\nAnalysis for " + file.getName() + ":\n" + stats.getAnalysisString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createAndShowGUI(Map<String, ProviderStats> signalProviderStats) {
        JFrame frame = new JFrame("Signal Providers Performance Analysis");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 600);

        // Main panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Table panel
        JPanel tablePanel = createTablePanel(signalProviderStats);
        mainPanel.add(tablePanel, BorderLayout.CENTER);

        // Button panel on the right
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton compareButton = new JButton("Compare Equity Curves");
        compareButton.addActionListener(e -> showEquityCurvesComparison(signalProviderStats));
        buttonPanel.add(compareButton);
        mainPanel.add(buttonPanel, BorderLayout.EAST);

        frame.add(mainPanel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static JPanel createTablePanel(Map<String, ProviderStats> signalProviderStats) {
        String[] columnNames = {
            "No.", "Signal Provider", "Trades", "Win Rate %", "Total Profit", 
            "Avg Profit/Trade", "Max Drawdown %", "Profit Factor", "Start Date", "End Date"
        };
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 0:
                    case 2:
                        return Integer.class;
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                        return Double.class;
                    case 8:
                    case 9:
                        return LocalDate.class;
                    default:
                        return String.class;
                }
            }
            
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        populateTableModel(model, signalProviderStats);
        
        JTable table = new JTable(model);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row != -1) {
                        row = table.convertRowIndexToModel(row);
                        String providerName = (String) model.getValueAt(row, 1);
                        showDetailedAnalysis(providerName, signalProviderStats.get(providerName));
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }
    private static void showEquityCurvesComparison(Map<String, ProviderStats> signalProviderStats) {
        JFrame comparisonFrame = new JFrame("Equity Curves Comparison");
        comparisonFrame.setSize(1200, 800);

        List<Map.Entry<String, ProviderStats>> sortedProviders = signalProviderStats.entrySet()
            .stream()
            .sorted((e1, e2) -> Double.compare(e2.getValue().getTotalProfit(), e1.getValue().getTotalProfit()))
            .collect(Collectors.toList());

        JPanel curvesPanel = new JPanel();
        curvesPanel.setLayout(new BoxLayout(curvesPanel, BoxLayout.Y_AXIS));

        for (Map.Entry<String, ProviderStats> entry : sortedProviders) {
            String providerName = entry.getKey();
            ProviderStats stats = entry.getValue();
            
            JPanel curvePanel = new JPanel(new BorderLayout());
            curvePanel.setBorder(BorderFactory.createTitledBorder(
                String.format("%s (Profit: %.2f)", providerName, stats.getTotalProfit())
            ));
            
            ChartPanel chartPanel = createEquityCurveChart(stats);
            chartPanel.setPreferredSize(new Dimension(1100, 300));
            curvePanel.add(chartPanel, BorderLayout.CENTER);
            
            curvesPanel.add(curvePanel);
        }

        JScrollPane scrollPane = new JScrollPane(curvesPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        comparisonFrame.add(scrollPane);

        comparisonFrame.setLocationRelativeTo(null);
        comparisonFrame.setVisible(true);
    }
    
   

   

   
    
    private static void showDetailedAnalysis(String providerName, ProviderStats stats) {
        JFrame detailFrame = new JFrame("Detailed Performance Analysis: " + providerName);
        detailFrame.setSize(1000, 800);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Stats Panel with button
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel statsPanel = createStatsPanel(stats);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton showTradesButton = new JButton("Show Trade List");
        showTradesButton.addActionListener(e -> showTradeList(providerName, stats));
        buttonPanel.add(showTradesButton);
        
        topPanel.add(statsPanel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.EAST);
        
        mainPanel.add(topPanel, BorderLayout.NORTH);
        
        // Charts
        JPanel chartsPanel = new JPanel(new GridLayout(2, 1));
        chartsPanel.add(createEquityCurveChart(stats));
        chartsPanel.add(createMonthlyProfitChart(stats));
        mainPanel.add(chartsPanel, BorderLayout.CENTER);
        
        detailFrame.add(mainPanel);
        detailFrame.setLocationRelativeTo(null);
        detailFrame.setVisible(true);
    }

    private static void showTradeList(String providerName, ProviderStats stats) {
        JFrame tradeListFrame = new JFrame("Trade List: " + providerName);
        tradeListFrame.setSize(800, 600);

        String[] columnNames = {
            "No.", "Date", "Profit/Loss", "Running Balance"
        };
        
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 0: return Integer.class;
                    case 2: 
                    case 3: return Double.class;
                    default: return Object.class;
                }
            }
        };

        double runningBalance = stats.getInitialBalance();
        List<Double> profits = stats.getProfits();
        List<LocalDate> dates = stats.getTradeDates();
        
        for (int i = 0; i < profits.size(); i++) {
            runningBalance += profits.get(i);
            model.addRow(new Object[]{
                i + 1,
                dates.get(i),
                profits.get(i),
                runningBalance
            });
        }

        JTable table = new JTable(model);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // Custom cell renderer for profit/loss coloring
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value,
                        isSelected, hasFocus, row, column);
                if (value instanceof Double) {
                    double profit = (Double) value;
                    if (!isSelected) {
                        c.setForeground(profit >= 0 ? new Color(0, 150, 0) : Color.RED);
                    }
                    setText(String.format("%,.2f", profit));
                }
                return c;
            }
        });

        // Format running balance column
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value,
                        isSelected, hasFocus, row, column);
                if (value instanceof Double) {
                    setText(String.format("%,.2f", (Double) value));
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        tradeListFrame.add(scrollPane);
        tradeListFrame.setLocationRelativeTo(null);
        tradeListFrame.setVisible(true);
    }

    private static JPanel createStatsPanel(ProviderStats stats) {
        JPanel panel = new JPanel(new GridLayout(0, 4, 10, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        DecimalFormat df = new DecimalFormat("#,##0.00");
        DecimalFormat pf = new DecimalFormat("#,##0.00'%'");
        
        addStatRow(panel, "Total Trades:", String.valueOf(stats.getTradeCount()));
        addStatRow(panel, "Win Rate:", pf.format(stats.getWinRate()));
        addStatRow(panel, "Total Profit:", df.format(stats.getTotalProfit()));
        addStatRow(panel, "Profit Factor:", df.format(stats.getProfitFactor()));
        addStatRow(panel, "Avg Profit/Trade:", df.format(stats.getAverageProfit()));
        addStatRow(panel, "Max Drawdown:", pf.format(stats.getMaxDrawdown()));
        addStatRow(panel, "Largest Win:", df.format(stats.getMaxProfit()));
        addStatRow(panel, "Largest Loss:", df.format(stats.getMaxLoss()));
        
        return panel;
    }
    private static void addStatRow(JPanel panel, String label, String value) {
        panel.add(new JLabel(label, SwingConstants.RIGHT));
        panel.add(new JLabel(value, SwingConstants.LEFT));
    }

    private static ChartPanel createEquityCurveChart(ProviderStats stats) {
        XYSeries series = new XYSeries("Equity");
        double equity = stats.getInitialBalance();
        List<Double> profits = stats.getProfits();
        
        for (int i = 0; i < profits.size(); i++) {
            equity += profits.get(i);
            series.add(i + 1, equity);
        }
        
        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(
            "Equity Curve Performance",
            "Trade Number",
            "Account Balance",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );
        
        XYPlot plot = (XYPlot) chart.getPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.RED);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        plot.setRenderer(renderer);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.GRAY);
        plot.setDomainGridlinePaint(Color.GRAY);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setAutoRangeIncludesZero(false);
        rangeAxis.setNumberFormatOverride(new DecimalFormat("#,##0.00"));
        
        return new ChartPanel(chart);
    }

    private static ChartPanel createMonthlyProfitChart(ProviderStats stats) {
        DefaultCategoryDataset dataset = stats.getMonthlyProfitData();
        JFreeChart chart = ChartFactory.createBarChart(
            "Monthly Performance Overview",
            "Month",
            "Profit/Loss",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );
        
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.GRAY);
        
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, Color.RED);
        
        return new ChartPanel(chart);
    }

    private static void populateTableModel(DefaultTableModel model, 
                                         Map<String, ProviderStats> signalProviderStats) {
        int index = 1;
        for (Map.Entry<String, ProviderStats> entry : signalProviderStats.entrySet()) {
            ProviderStats stats = entry.getValue();
            model.addRow(new Object[]{
                index++,
                entry.getKey(),
                stats.getTradeCount(),
                stats.getWinRate(),
                stats.getTotalProfit(),
                stats.getAverageProfit(),
                stats.getMaxDrawdown(),
                stats.getProfitFactor(),
                stats.getStartDate(),
                stats.getEndDate()
            });
        }
    }
}


