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
import data.TradeTracker;

public class TradeListTable extends JTable
{
    private final DecimalFormat df = new DecimalFormat("#,##0.00");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DefaultTableModel model;

    public TradeListTable(ProviderStats stats)
    {
        this.model = createTableModel(stats);
        initializeTable();
        setupRenderers();
    }

    private DefaultTableModel createTableModel(ProviderStats stats)
    {
        String[] columnNames =
        { "No.", "Open Time", "Close Time", "Type", "Symbol", "Lots", "Open Price", "Close Price", "Profit/Loss",
                "Commission", "Swap", "Total", "Running Profit", "Open Trades", "Open Lots" };

        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex)
            {
                switch (columnIndex)
                {
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
        List<Trade> sortedTrades = new ArrayList<>(stats.getTrades());
        sortedTrades.sort(new TradeComparator());

        double runningProfit = 0.0;

        for (int i = 0; i < sortedTrades.size(); i++) {
            Trade trade = sortedTrades.get(i);
            double totalProfit = trade.getTotalProfit();
            runningProfit += totalProfit;

            int openTradesCount = TradeTracker.calculateOpenTradesAt(sortedTrades, trade.getCloseTime());
            double openLotsCount = TradeTracker.calculateOpenLotsAt(sortedTrades, trade.getCloseTime());

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
                openLotsCount
            });
        }
    
        
    
    }

    private void initializeTable()
    {
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

        int[] preferredWidths = { 50, 140, 140, 60, 80, 70, 90, 90, 90, 90, 90, 90, 100, 90, 90 };
        for (int i = 0; i < preferredWidths.length; i++) {
            getColumnModel().getColumn(i).setPreferredWidth(preferredWidths[i]);
        }
    }

    private void setupRenderers()
    {
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

        getColumnModel().getColumn(8).setCellRenderer(moneyRenderer); // Profit/Loss
        getColumnModel().getColumn(9).setCellRenderer(moneyRenderer); // Commission
        getColumnModel().getColumn(10).setCellRenderer(moneyRenderer); // Swap
        getColumnModel().getColumn(11).setCellRenderer(moneyRenderer); // Total
        getColumnModel().getColumn(12).setCellRenderer(moneyRenderer); // Running Profit
        getColumnModel().getColumn(13).setCellRenderer(moneyRenderer); // Open Trades
        getColumnModel().getColumn(14).setCellRenderer(moneyRenderer); // Open Lots
    }
}
