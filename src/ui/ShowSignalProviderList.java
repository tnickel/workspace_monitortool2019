package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import components.WebViewPanel;
import data.ProviderStats;
import data.Trade;
import utils.HtmlDatabase;

public class ShowSignalProviderList extends JDialog {
    private static final Logger LOGGER = Logger.getLogger(ShowSignalProviderList.class.getName());
    private final JTable providerTable;
    private final WebViewPanel webViewPanel;
    private final Map<String, ProviderStats> providers;
    private final HtmlDatabase htmlDatabase;
    private final String rootPath;
    private final JLabel statusLabel;
    
    public ShowSignalProviderList(Window owner, Map<String, ProviderStats> providers, 
            HtmlDatabase htmlDatabase, String rootPath) {
super(owner, "Signal Provider Overview", Dialog.ModalityType.MODELESS);
this.providers = providers;
this.htmlDatabase = htmlDatabase;
this.rootPath = rootPath;

setSize(1560, 800);
setLocationRelativeTo(owner);

// Status Label erstellen
statusLabel = new JLabel(" ");
statusLabel.setBorder(BorderFactory.createCompoundBorder(
BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY),
BorderFactory.createEmptyBorder(5, 5, 5, 5)
));

JPanel statusPanel = new JPanel(new BorderLayout());
statusPanel.add(statusLabel, BorderLayout.CENTER);
statusPanel.setBackground(new Color(238, 238, 238));

// Main Split Pane mit angepasster Gewichtung (30% Tabelle, 70% Browser)
JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
mainSplitPane.setResizeWeight(0.3);  // Browser bekommt mehr Platz

// Table Setup
providerTable = createProviderTable();
JScrollPane tableScrollPane = new JScrollPane(providerTable);
mainSplitPane.setLeftComponent(tableScrollPane);

// Browser Setup
webViewPanel = new WebViewPanel();
mainSplitPane.setRightComponent(webViewPanel);

// Layout zusammenbauen
JPanel mainPanel = new JPanel(new BorderLayout());
mainPanel.add(mainSplitPane, BorderLayout.CENTER);
mainPanel.add(statusPanel, BorderLayout.SOUTH);

setContentPane(mainPanel);

setupMouseListener();

// Initial selection
if (providerTable.getRowCount() > 0) {
providerTable.setRowSelectionInterval(0, 0);
updateSelectedProvider();
}
}

    
        
    private void setupMouseListener() {
        providerTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                LOGGER.info("Mouse clicked in table");
                int row = providerTable.rowAtPoint(e.getPoint());
                int col = providerTable.columnAtPoint(e.getPoint());
                if (row >= 0 && col >= 0) {
                    LOGGER.info("Clicked on row: " + row + ", column: " + col);
                    updateSelectedProvider();
                }
            }
        });
    }
    
    private JTable createProviderTable() {
        String[] columnNames = {"Provider Info", "Equity Curve"};
        Object[][] data = new Object[providers.size()][2];
        int row = 0;

        for (Map.Entry<String, ProviderStats> entry : providers.entrySet()) {
            ProviderStats stats = entry.getValue();
            
            // Column 1: Provider information
            StringBuilder info = new StringBuilder();
            info.append("<html>");
            info.append("<div style='margin: 5px;'>");
            info.append("Name: ").append(entry.getKey()).append("<br>");
            info.append("<b>Total Trades:</b> ").append(stats.getTradeCount()).append("<br>");
            info.append("<b>Win Rate:</b> ").append(String.format("%.2f%%", stats.getWinRate())).append("<br>");
            info.append("<b>Profit Factor:</b> ").append(String.format("%.2f", stats.getProfitFactor())).append("<br>");
            info.append("<b>Max Drawdown:</b> ").append(String.format("%.2f%%", stats.getMaxDrawdown()));
            info.append("</div>");
            info.append("</html>");
            
            data[row][0] = info.toString();
            
            // Column 2: Create equity curve chart
            JFreeChart chart = createEquityChart(entry.getKey(), stats);
            ChartPanel equityPanel = new ChartPanel(chart);
            equityPanel.setMinimumDrawWidth(10);
            equityPanel.setMinimumDrawHeight(10);
            equityPanel.setMaximumDrawWidth(2000);
            equityPanel.setMaximumDrawHeight(2000);
            
            // Fixed size setzen
            Dimension chartSize = new Dimension(280, 150);
            equityPanel.setPreferredSize(chartSize);
            equityPanel.setMinimumSize(chartSize);
            equityPanel.setMaximumSize(chartSize);
            
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
        
        // Custom renderer for text column to handle HTML
        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setVerticalAlignment(SwingConstants.TOP);
                setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                return this;
            }
        });
        
        // Custom renderer for the chart column
        table.getColumnModel().getColumn(1).setCellRenderer(new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                return (ChartPanel) value;
            }
        });

        // Ensure proper column sizes
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setWidth(280);
        table.getColumnModel().getColumn(1).setMinWidth(280);
        table.getColumnModel().getColumn(1).setMaxWidth(280);
        
        return table;
    }
    
    private void updateSelectedProvider() {
        int selectedRow = providerTable.getSelectedRow();
        if (selectedRow >= 0) {
            String providerInfo = (String) providerTable.getValueAt(selectedRow, 0);
            LOGGER.info("Raw provider info: " + providerInfo);
            
            try {
                // Name zwischen "Name: " und "<br>" extrahieren
                int startIndex = providerInfo.indexOf("Name: ") + 6;
                int endIndex = providerInfo.indexOf("<br>");
                if (startIndex < 0 || endIndex < 0) {
                    throw new IllegalStateException("Could not find provider name markers in text");
                }
                
                String providerName = providerInfo.substring(startIndex, endIndex);
                LOGGER.info("Extracted provider name: " + providerName);
                
                ProviderStats stats = providers.get(providerName);
                if (stats != null) {
                    String providerId = providerName.substring(providerName.lastIndexOf("_") + 1).replace(".csv", "");
                    String fullUrl = "https://www.mql5.com/de/signals/" + providerId + "?source=Site+Signals+Subscriptions#!tab=account";
                    
                    LOGGER.info("Loading URL: " + fullUrl);
                    statusLabel.setText("Loading URL: " + fullUrl);
                    webViewPanel.loadURL(fullUrl);
                } else {
                    LOGGER.warning("No stats found for provider: " + providerName);
                    statusLabel.setText("Error: No stats found for " + providerName);
                }
            } catch (Exception e) {
                LOGGER.severe("Error processing provider info: " + e.getMessage());
                e.printStackTrace();
                statusLabel.setText("Error processing provider info");
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
            null,  // Kein Titel
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