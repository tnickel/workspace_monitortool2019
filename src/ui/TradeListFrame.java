package ui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDateTime;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import components.TradeChartPanel;
import components.TradeListTable;
import components.WebViewPanel;
import data.ProviderStats;
import data.Trade;
import utils.TradeUtils;

public class TradeListFrame extends JFrame {
    private final TradeListTable tradeTable;
    private final JPanel detailPanel;
    private final TradeChartPanel chartPanel;
    private final WebViewPanel webViewPanel;
    private final ProviderStats stats;
    private JSplitPane mainSplitPane;
    private JSplitPane rightSplitPane;

    public TradeListFrame(String providerName, ProviderStats stats) {
        super("Trade List: " + providerName);
        this.stats = stats;
        this.tradeTable = new TradeListTable(stats);
        this.detailPanel = new JPanel();
        this.chartPanel = new TradeChartPanel();
        this.webViewPanel = new WebViewPanel();
        
        initializeUI();
        setupSelectionListener();
        
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        // Escape-Taste zum Schließen
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
        setSize(1600, 900);
        setLayout(new BorderLayout(5, 0));

        // Hauptbereich mit Tabelle
        JScrollPane scrollPane = new JScrollPane(tradeTable);
        
        // Rechtes Panel mit Chart, Details und WebView
        rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        
        // Oberes Panel für Chart und Details
        JPanel upperPanel = new JPanel(new BorderLayout(0, 5));
        
        // Chart Panel im oberen Bereich
        JPanel chartContainer = new JPanel(new BorderLayout());
        chartContainer.setBorder(BorderFactory.createTitledBorder("Open Trades Timeline"));
        chartContainer.add(chartPanel, BorderLayout.CENTER);
        
        // Detail Panel für konkurrierende Trades
        detailPanel.setBorder(BorderFactory.createTitledBorder("Concurrent Trades"));
        detailPanel.setLayout(new BorderLayout());

        // Zusammenführen der oberen Panels
        upperPanel.add(chartContainer, BorderLayout.NORTH);
        upperPanel.add(detailPanel, BorderLayout.CENTER);
        
        // WebView Panel
        JPanel webViewContainer = new JPanel(new BorderLayout());
        webViewContainer.setBorder(BorderFactory.createTitledBorder("Signal Provider Details"));
        webViewContainer.add(webViewPanel, BorderLayout.CENTER);
        
        //// Panels zum Split Pane hinzufügen
        rightSplitPane.setTopComponent(upperPanel);
        rightSplitPane.setBottomComponent(webViewContainer);
        rightSplitPane.setResizeWeight(0.5);
        
        // Haupt-Split Pane
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, rightSplitPane);
        mainSplitPane.setResizeWeight(0.8); // Geändert von 0.6 auf 0.8 für 80% Breite

        // Toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        
        JButton exportButton = new JButton("Export");
        exportButton.addActionListener(e -> exportTrades());
        toolBar.add(exportButton);

        // Layout zusammenbauen
        add(toolBar, BorderLayout.NORTH);
        add(mainSplitPane, BorderLayout.CENTER);
        setLocationRelativeTo(null);
        
        // Explizites Setzen der initialen Divider-Position
        SwingUtilities.invokeLater(() -> {
            mainSplitPane.setDividerLocation(0.8);
        });
    }
    private void setupSelectionListener() {
        tradeTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int row = tradeTable.getSelectedRow();
                    if (row >= 0) {
                        Trade selectedTrade = tradeTable.getTradeAt(row);
                        List<Trade> concurrentTrades = TradeUtils.getActiveTradesAt(
                            stats.getTrades(), 
                            selectedTrade.getOpenTime()
                        );
                        updateDetailPanel(selectedTrade, concurrentTrades);
                        // Provider URL laden wenn verfügbar
                        if (selectedTrade.getSignalProviderURL() != null) {
                            webViewPanel.loadURL(selectedTrade.getSignalProviderURL());
                        }
                    }
                }
            }
        });
    }

    private void updateDetailPanel(Trade selectedTrade, List<Trade> concurrentTrades) {
        detailPanel.removeAll();
        
        // Chart aktualisieren
        chartPanel.updateTrades(concurrentTrades, selectedTrade.getOpenTime());
        
        // Details Panel
        JPanel content = new JPanel();
        content.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Übersichtsinformationen
        JPanel summaryPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        summaryPanel.setBorder(BorderFactory.createTitledBorder("Summary"));
        
        summaryPanel.add(new JLabel("Time: " + selectedTrade.getOpenTime()));
        summaryPanel.add(new JLabel("Concurrent Trades: " + concurrentTrades.size()));
        
        double totalLots = concurrentTrades.stream().mapToDouble(Trade::getLots).sum();
        summaryPanel.add(new JLabel("Total Lots: " + String.format("%.3f", totalLots)));
        
        content.add(summaryPanel, gbc);
        gbc.gridy++;

        // Details für jeden konkurrierenden Trade
        for (Trade trade : concurrentTrades) {
            JPanel tradePanel = createTradePanel(trade);
            content.add(tradePanel, gbc);
            gbc.gridy++;
        }

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);
        
        detailPanel.add(scrollPane, BorderLayout.CENTER);
        detailPanel.revalidate();
        detailPanel.repaint();
    }

    private JPanel createTradePanel(Trade trade) {
        JPanel tradePanel = new JPanel(new GridBagLayout());
        tradePanel.setBorder(BorderFactory.createTitledBorder("Trade"));
        
        GridBagConstraints tgbc = new GridBagConstraints();
        tgbc.gridx = 0;
        tgbc.gridy = 0;
        tgbc.anchor = GridBagConstraints.WEST;
        tgbc.insets = new Insets(2, 5, 2, 5);
        tgbc.fill = GridBagConstraints.HORIZONTAL;
        tgbc.weightx = 1.0;

        addTradeDetail(tradePanel, "Symbol:", trade.getSymbol(), tgbc);
        addTradeDetail(tradePanel, "Type:", trade.getType(), tgbc);
        addTradeDetail(tradePanel, "Lots:", String.format("%.2f", trade.getLots()), tgbc);
        addTradeDetail(tradePanel, "Open Price:", String.format("%.5f", trade.getOpenPrice()), tgbc);
        if (trade.getStopLoss() > 0) {
            addTradeDetail(tradePanel, "Stop Loss:", String.format("%.5f", trade.getStopLoss()), tgbc);
        }
        if (trade.getTakeProfit() > 0) {
            addTradeDetail(tradePanel, "Take Profit:", String.format("%.5f", trade.getTakeProfit()), tgbc);
        }
        
        return tradePanel;
    }

    private void addTradeDetail(JPanel panel, String label, String value, GridBagConstraints gbc) {
        JPanel rowPanel = new JPanel(new BorderLayout(10, 0));
        rowPanel.add(new JLabel(label), BorderLayout.WEST);
        rowPanel.add(new JLabel(value), BorderLayout.CENTER);
        panel.add(rowPanel, gbc);
        gbc.gridy++;
    }

    private void exportTrades() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Trades");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setSelectedFile(new File("trades_export.csv"));
        
        FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV Files (*.csv)", "csv");
        fileChooser.setFileFilter(filter);
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getAbsolutePath() + ".csv");
            }
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.println("Open Time,Close Time,Type,Symbol,Lots,Open Price,Close Price," +
                             "Profit/Loss,Commission,Swap,Total");
                
                for (int i = 0; i < tradeTable.getModel().getRowCount(); i++) {
                    Trade trade = tradeTable.getTradeAt(i);
                    writer.printf("%s,%s,%s,%s,%.2f,%.5f,%.5f,%.2f,%.2f,%.2f,%.2f%n",
                        trade.getOpenTime(),
                        trade.getCloseTime(),
                        trade.getType(),
                        trade.getSymbol(),
                        trade.getLots(),
                        trade.getOpenPrice(),
                        trade.getClosePrice(),
                        trade.getProfit(),
                        trade.getCommission(),
                        trade.getSwap(),
                        trade.getTotalProfit()
                    );
                }
                
                JOptionPane.showMessageDialog(this, 
                    "Export successful: " + file.getAbsolutePath(), 
                    "Export Complete", 
                    JOptionPane.INFORMATION_MESSAGE);
                    
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                    "Error exporting file: " + e.getMessage(),
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
}