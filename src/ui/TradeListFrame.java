package ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.time.LocalDateTime;
import java.util.List;

import components.TradeListTable;
import components.OpenTradesChartPanel;
import data.ProviderStats;
import data.Trade;
import utils.TradeUtils;

public class TradeListFrame extends JFrame {
    private final TradeListTable tradeTable;
    private final JPanel detailPanel;
    private final OpenTradesChartPanel chartPanel;
    private final ProviderStats stats;

    public TradeListFrame(String providerName, ProviderStats stats) {
        super("Trade List: " + providerName);
        this.stats = stats;
        this.tradeTable = new TradeListTable(stats);
        this.detailPanel = new JPanel();
        this.chartPanel = new OpenTradesChartPanel();
        initializeUI();
        setupSelectionListener();
        
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        // Escape-Taste zum Schlie�en
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
        setSize(1430, 780);
        setLayout(new BorderLayout(5, 0));

        // Hauptbereich mit Tabelle
        JScrollPane scrollPane = new JScrollPane(tradeTable);
        
        // Rechtes Panel mit Chart und Details
        JPanel rightPanel = new JPanel(new BorderLayout(0, 5));
        rightPanel.setPreferredSize(new Dimension(300, 0));
        
        // Chart Panel im oberen Bereich
        JPanel chartContainer = new JPanel(new BorderLayout());
        chartContainer.setBorder(BorderFactory.createTitledBorder("Open Trades Timeline"));
        chartContainer.add(chartPanel, BorderLayout.CENTER);
        
        // Detail Panel f�r konkurrierende Trades
        detailPanel.setBorder(BorderFactory.createTitledBorder("Concurrent Trades"));
        detailPanel.setLayout(new BorderLayout());

        // Zusammenf�hren der Panels
        rightPanel.add(chartContainer, BorderLayout.NORTH);
        rightPanel.add(detailPanel, BorderLayout.CENTER);

        // Toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        
        JButton exportButton = new JButton("Export");
        exportButton.addActionListener(e -> exportTrades());
        toolBar.add(exportButton);

        // Layout zusammenbauen
        add(toolBar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
        setLocationRelativeTo(null);
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

        // �bersichtsinformationen
        JPanel summaryPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        summaryPanel.setBorder(BorderFactory.createTitledBorder("Summary"));
        
        summaryPanel.add(new JLabel("Time: " + selectedTrade.getOpenTime()));
        summaryPanel.add(new JLabel("Concurrent Trades: " + concurrentTrades.size()));
        
        double totalLots = concurrentTrades.stream().mapToDouble(Trade::getLots).sum();
        summaryPanel.add(new JLabel("Total Lots: " + String.format("%.3f", totalLots)));
        
        content.add(summaryPanel, gbc);
        gbc.gridy++;

        // Details f�r jeden konkurrierenden Trade
        for (Trade trade : concurrentTrades) {
            JPanel tradePanel = createTradePanel(trade);
            content.add(tradePanel, gbc);
            gbc.gridy++;
        }

        // Scroll-Bereich f�r die Details
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);  // Entfernt den Border des ScrollPane
        
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

        // Trade Details
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
                // CSV Header
                writer.println("Open Time,Close Time,Type,Symbol,Lots,Open Price,Close Price," +
                             "Profit/Loss,Commission,Swap,Total");
                
                // Daten schreiben
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