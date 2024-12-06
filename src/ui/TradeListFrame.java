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
import data.ProviderStats;
import data.Trade;
import utils.TradeUtils;

public class TradeListFrame extends JFrame {
    private final TradeListTable tradeTable;
    private final JPanel detailPanel;
    private final ProviderStats stats;

    public TradeListFrame(String providerName, ProviderStats stats) {
        super("Trade List: " + providerName);
        this.stats = stats;
        this.tradeTable = new TradeListTable(stats);
        this.detailPanel = new JPanel();
        initializeUI();
        setupSelectionListener();
        
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
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
        setSize(1100, 600);
        setLayout(new BorderLayout(5, 0));

        // Hauptbereich mit Tabelle
        JScrollPane scrollPane = new JScrollPane(tradeTable);
        
        // Detail Panel für konkurrierende Trades
        detailPanel.setPreferredSize(new Dimension(300, 0));
        detailPanel.setBorder(BorderFactory.createTitledBorder("Concurrent Trades"));
        detailPanel.setLayout(new BorderLayout());

        // Toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        JButton exportButton = new JButton("Export");
        exportButton.addActionListener(e -> exportTrades());
        toolBar.add(exportButton);

        add(toolBar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(detailPanel, BorderLayout.EAST);
        setLocationRelativeTo(null);
    }

    private void setupSelectionListener() {
        tradeTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int row = tradeTable.getSelectedRow();
                    if (row >= 0) {
                        Trade selectedTrade = tradeTable.getTradeAt(row);  // Hier die Änderung
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
        
        JPanel content = new JPanel();
        content.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Übersicht
        content.add(new JLabel("Zeitpunkt: " + selectedTrade.getOpenTime()), gbc);
        gbc.gridy++;
        content.add(new JLabel("Anzahl konkurrierender Trades: " + concurrentTrades.size()), gbc);
        gbc.gridy++;
        
        double totalLots = concurrentTrades.stream().mapToDouble(Trade::getLots).sum();
        content.add(new JLabel("Gesamte Lots: " + String.format("%.3f", totalLots)), gbc);
        gbc.gridy++;
        
        // Separator
        content.add(new JLabel(" "), gbc);
        gbc.gridy++;

        // Details für jeden konkurrierenden Trade
        for (Trade trade : concurrentTrades) {
            JPanel tradePanel = new JPanel(new GridBagLayout());
            tradePanel.setBorder(BorderFactory.createTitledBorder("Trade"));
            
            GridBagConstraints tgbc = new GridBagConstraints();
            tgbc.gridx = 0;
            tgbc.gridy = 0;
            tgbc.anchor = GridBagConstraints.WEST;
            tgbc.insets = new Insets(2, 2, 2, 2);
            
            tradePanel.add(new JLabel("Symbol: " + trade.getSymbol()), tgbc);
            tgbc.gridy++;
            tradePanel.add(new JLabel("Type: " + trade.getType()), tgbc);
            tgbc.gridy++;
            tradePanel.add(new JLabel("Lots: " + trade.getLots()), tgbc);
            tgbc.gridy++;
            tradePanel.add(new JLabel("Open Time: " + trade.getOpenTime()), tgbc);
            tgbc.gridy++;
            tradePanel.add(new JLabel("Close Time: " + trade.getCloseTime()), tgbc);
            
            content.add(tradePanel, gbc);
            gbc.gridy++;
        }

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        detailPanel.add(scrollPane, BorderLayout.CENTER);
        detailPanel.revalidate();
        detailPanel.repaint();
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
                writer.println("Date,Profit/Loss");
                
                for (int i = 0; i < tradeTable.getRowCount(); i++) {
                    String date = tradeTable.getValueAt(i, 0).toString();
                    String profit = tradeTable.getValueAt(i, 1).toString();
                    writer.println(date + "," + profit);
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