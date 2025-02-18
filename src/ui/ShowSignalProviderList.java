package ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import data.ProviderStats;
import data.Trade;
import utils.HtmlDatabase;
import components.WebViewPanel;

public class ShowSignalProviderList extends JDialog {
    private final JTable providerTable;
    private final JPanel chartPanel;
    private final WebViewPanel webViewPanel;
    private final Map<String, ProviderStats> providers;
    private final HtmlDatabase htmlDatabase;
    private final String rootPath;
    
    public ShowSignalProviderList(Window owner, Map<String, ProviderStats> providers, 
                            HtmlDatabase htmlDatabase, String rootPath) {
        super(owner, "Signal Provider Overview", Dialog.ModalityType.MODELESS);
        this.providers = providers;
        this.htmlDatabase = htmlDatabase;
        this.rootPath = rootPath;
        
        setSize(1200, 800);
        setLocationRelativeTo(owner);
        
        // Create main split pane
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setResizeWeight(0.7);
        
        // Create left panel with table and charts
        JSplitPane leftPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        leftPane.setResizeWeight(0.5);
        
        // Initialize table
        providerTable = createProviderTable();
        JScrollPane tableScrollPane = new JScrollPane(providerTable);
        leftPane.setLeftComponent(tableScrollPane);
        
        // Initialize chart panel
        chartPanel = new JPanel(new BorderLayout());
        leftPane.setRightComponent(chartPanel);
        
        mainSplitPane.setLeftComponent(leftPane);
        
        // Initialize web browser panel
        webViewPanel = new WebViewPanel();
        mainSplitPane.setRightComponent(webViewPanel);
        
        add(mainSplitPane);
        
        // Setup selection listener
        providerTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSelectedProvider();
            }
        });
        
        // Select first row by default
        if (providerTable.getRowCount() > 0) {
            providerTable.setRowSelectionInterval(0, 0);
            updateSelectedProvider();
        }
    }
    
    private JTable createProviderTable() {
        String[] columnNames = {"Provider Info", "Equity Curve"};
        Object[][] data = new Object[providers.size()][2];
        int row = 0;

        for (Map.Entry<String, ProviderStats> entry : providers.entrySet()) {
            ProviderStats stats = entry.getValue();
            
            // Column 1: Provider information
            StringBuilder info = new StringBuilder();
            info.append("Name: ").append(entry.getKey()).append("\n");
            info.append("Total Trades: ").append(stats.getTradeCount()).append("\n");
            info.append("Win Rate: ").append(String.format("%.2f%%", stats.getWinRate())).append("\n");
            info.append("Profit Factor: ").append(String.format("%.2f", stats.getProfitFactor())).append("\n");
            info.append("Max Drawdown: ").append(String.format("%.2f%%", stats.getMaxDrawdown()));
            
            data[row][0] = info.toString();
            
            // Column 2: Create equity curve chart
            JFreeChart chart = createEquityChart(entry.getKey(), stats);
            ChartPanel equityPanel = new ChartPanel(chart);
            equityPanel.setPreferredSize(new Dimension(300, 150));
            data[row][1] = equityPanel;
            
            row++;
        }
        
        DefaultTableModel model = new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 1) return ChartPanel.class;
                return Object.class;
            }
        };
        
        JTable table = new JTable(model);
        table.setRowHeight(150);
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(300);
        
        // Custom renderer for the chart column
        table.getColumnModel().getColumn(1).setCellRenderer((table1, value, isSelected, hasFocus, row1, column) -> 
            (ChartPanel) value);
        
        return table;
    }
    
    private void updateSelectedProvider() {
        int selectedRow = providerTable.getSelectedRow();
        if (selectedRow >= 0) {
            String providerInfo = (String) providerTable.getValueAt(selectedRow, 0);
            String providerName = providerInfo.substring(
                providerInfo.indexOf(": ") + 2, 
                providerInfo.indexOf("\n")
            );
            
            // Update chart panel with larger equity curve
            ProviderStats stats = providers.get(providerName);
            if (stats != null) {
                JFreeChart largeChart = createEquityChart(providerName, stats);
                ChartPanel largeChartPanel = new ChartPanel(largeChart);
                chartPanel.removeAll();
                chartPanel.add(largeChartPanel, BorderLayout.CENTER);
                chartPanel.revalidate();
                chartPanel.repaint();
                
                // Update web browser with provider URL
                String providerUrl = stats.getSignalProviderURL();
                if (providerUrl != null && !providerUrl.isEmpty()) {
                    webViewPanel.loadURL(providerUrl);
                }
            }
        }
    }

    private JFreeChart createEquityChart(String providerName, ProviderStats stats) {
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