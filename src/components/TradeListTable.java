package components;

import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import data.ProviderStats;
import data.Trade;
import data.TradeComparator;
import utils.TradeUtils;

public class TradeListTable extends JTable {
    private final DecimalFormat df = new DecimalFormat("#,##0.00");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DefaultTableModel model;
    private List<Trade> sortedTrades;

    public TradeListTable(ProviderStats stats) {
        this.model = createTableModel(stats);
        initializeTable();
        setupRenderers();
    }

    public Trade getTradeAt(int row) {
        return sortedTrades.get(row);
    }

    private DefaultTableModel createTableModel(ProviderStats stats) {
        String[] columnNames = { 
            "No.", "Open Time", "Close Time", "Type", "Symbol", "Lots", 
            "Open Price", "Close Price", "Profit/Loss", "Commission", "Swap", 
            "Total", "Running Profit", "Open Trades", "Open Lots", "Open Equity" 
        };

        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 0:
                        return Integer.class;
                    case 5: // Lots
                    case 6: // Open Price
                    case 7: // Close Price
                    case 8: // Profit/Loss
                    case 9: // Commission
                    case 10: // Swap
                    case 11: // Total
                    case 12: // Running Profit
                    case 13: // Open Trades
                    case 14: // Open Lots
                    case 15: // Open Equity
                        return Double.class;
                    default:
                        return String.class;
                }
            }
        };

        populateData(model, stats);
        return model;
    }

    private void populateData(DefaultTableModel model, ProviderStats stats) {
        this.sortedTrades = new ArrayList<>(stats.getTrades());
        sortedTrades.sort(new TradeComparator());

        double runningProfit = 0.0;

        for (int i = 0; i < sortedTrades.size(); i++) {
            Trade trade = sortedTrades.get(i);
            double totalProfit = trade.getTotalProfit();
            runningProfit += totalProfit;

            List<Trade> activeTrades = TradeUtils.getActiveTradesAt(sortedTrades, trade.getOpenTime());
            int openTradesCount = activeTrades.size();
            double openLotsCount = activeTrades.stream().mapToDouble(Trade::getLots).sum();
            
            // Open Equity Berechnung
            double openEquity = estimateOpenEquity(activeTrades, trade.getOpenTime());

            model.addRow(new Object[] {
                i + 1,
                trade.getOpenTime().format(dateFormatter),
                trade.getCloseTime().format(dateFormatter),
                trade.getType(),
                trade.getSymbol(),
                trade.getLots(),
                trade.getOpenPrice(),
                trade.getClosePrice(),
                trade.getProfit(),
                trade.getCommission(),
                trade.getSwap(),
                totalProfit,
                runningProfit,
                openTradesCount,
                openLotsCount,
                openEquity
            });
        }
    }

    private void initializeTable() {
        setModel(model);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);

        Comparator<String> dateComparator = new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                try {
                    LocalDateTime dt1 = LocalDateTime.parse(s1, dateFormatter);
                    LocalDateTime dt2 = LocalDateTime.parse(s2, dateFormatter);
                    return dt1.compareTo(dt2);
                } catch (Exception e) {
                    return s1.compareTo(s2);
                }
            }
        };

        sorter.setComparator(1, dateComparator); // Open Time
        sorter.setComparator(2, dateComparator); // Close Time

        setRowSorter(sorter);
        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        int[] preferredWidths = { 
            50, 140, 140, 60, 80, 70, 90, 90, 90, 90, 90, 
            90, 100, 90, 90, 90  // Letzte 90 für Open Equity
        };
        for (int i = 0; i < preferredWidths.length; i++) {
            getColumnModel().getColumn(i).setPreferredWidth(preferredWidths[i]);
        }
    }

    private void setupRenderers() {
        TableCellRenderer moneyRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof Double) {
                    double amount = (Double) value;
                    if (!isSelected) {
                        c.setForeground(amount >= 0 ? new Color(0, 150, 0) : Color.RED);
                    }
                    setText(df.format(amount));
                }
                setHorizontalAlignment(SwingConstants.RIGHT);
                return c;
            }
        };

        // Money Columns
        int[] moneyColumns = {8, 9, 10, 11, 12, 13, 14, 15}; // 15 für Open Equity
        for (int column : moneyColumns) {
            getColumnModel().getColumn(column).setCellRenderer(moneyRenderer);
        }
    }

    private double estimateOpenEquity(List<Trade> activeTrades, LocalDateTime currentTime) {
        double totalEquity = 0.0;
        
        for (Trade trade : activeTrades) {
            double progress = calculateTradeProgress(trade, currentTime);
            double estimatedProfit = interpolateProfit(trade, progress);
            
            // Aggressivere Abschätzung:
            // 1. Wenn der Trade am Ende Verlust macht, nehmen wir an dass der Verlust früher eintritt
            // 2. Wenn der Trade am Ende Gewinn macht, nehmen wir an dass der Gewinn später eintritt
            if (trade.getProfit() < 0) {
                // Bei Verlust-Trades: Verlust schneller eintreten lassen
                estimatedProfit *= 1.5; // 50% mehr Verlust
                
                // Progress beschleunigen für Verlust-Trades
                double acceleratedProgress = Math.pow(progress, 0.7); // Verluste treten früher ein
                estimatedProfit = trade.getProfit() * acceleratedProgress;
            } else {
                // Bei Gewinn-Trades: Gewinne verzögern
                double delayedProgress = Math.pow(progress, 1.3); // Gewinne treten später ein
                estimatedProfit = trade.getProfit() * delayedProgress;
            }
            
            // Zusätzlicher Risiko-Faktor basierend auf der Lot-Größe
            double lotRiskFactor = 1.0 + (trade.getLots() * 0.2); // Größere Positionen = mehr Risiko
            if (estimatedProfit < 0) {
                estimatedProfit *= lotRiskFactor;
            }
            
            totalEquity += estimatedProfit;
        }
        
        // Gesamtrisiko-Faktor basierend auf Anzahl offener Trades
        double totalRiskFactor = 1.0 + (activeTrades.size() * 0.1); // Mehr offene Trades = mehr Risiko
        if (totalEquity < 0) {
            totalEquity *= totalRiskFactor;
        }
        
        return totalEquity;
    }

    private double calculateTradeProgress(Trade trade, LocalDateTime currentTime) {
        long totalDuration = java.time.Duration.between(trade.getOpenTime(), trade.getCloseTime()).toMillis();
        long currentDuration = java.time.Duration.between(trade.getOpenTime(), currentTime).toMillis();
        return Math.min(1.0, Math.max(0.0, (double) currentDuration / totalDuration));
    }

    private double interpolateProfit(Trade trade, double progress) {
        return trade.getProfit() * progress;
    }
}