
package components;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import data.Trade;
import java.time.LocalDateTime;
import java.util.List;

public class MonthlyReturnsTable extends JPanel {
    private final JTable table;
    private final String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                                   "Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "Year"};
    
    public MonthlyReturnsTable() {
        setLayout(new BorderLayout());
        
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        model.addColumn("Year");
        for (String month : months) {
            model.addColumn(month);
        }
        
        table = new JTable(model);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (value != null && !value.toString().isEmpty()) {
                    try {
                        double val = Double.parseDouble(value.toString().replace("%", ""));
                        if (val > 0) {
                            c.setForeground(new Color(0, 100, 0));
                        } else if (val < 0) {
                            c.setForeground(new Color(150, 0, 0));
                        } else {
                            c.setForeground(Color.BLACK);
                        }
                    } catch (NumberFormatException e) {
                        c.setForeground(Color.BLACK);
                    }
                }
                
                return c;
            }
        });
        
        table.setGridColor(Color.LIGHT_GRAY);
        table.setShowGrid(true);
        table.getTableHeader().setReorderingAllowed(false);
        
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
        
        JLabel linkLabel = new JLabel("<html><u>Wie wird der Zuwachs in Signalen gerechnet?</u></html>");
        linkLabel.setForeground(Color.BLUE);
        linkLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        add(linkLabel, BorderLayout.SOUTH);
    }
    
    public void updateData(List<Trade> trades, double initialBalance) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        
        TreeMap<Integer, Map<Integer, Double>> yearMonthReturns = calculateMonthlyReturns(trades, initialBalance);
        
        double totalReturn = 0.0;
        
        for (Map.Entry<Integer, Map<Integer, Double>> yearEntry : yearMonthReturns.entrySet()) {
            Object[] rowData = new Object[14];
            rowData[0] = yearEntry.getKey();
            
            double yearTotal = 0.0;
            Map<Integer, Double> monthReturns = yearEntry.getValue();
            
            for (int month = 1; month <= 12; month++) {
                Double monthReturn = monthReturns.get(month);
                if (monthReturn != null) {
                    rowData[month] = String.format("%.2f", monthReturn);
                    yearTotal += monthReturn;
                } else {
                    rowData[month] = "";
                }
            }
            
            rowData[13] = String.format("%.2f%%", yearTotal);
            totalReturn += yearTotal;
            
            model.addRow(rowData);
        }
        
        model.addRow(new Object[]{"Total:", "", "", "", "", "", "", "", "", "", "", "", "", 
                                String.format("%.2f%%", totalReturn)});
    }
    
    private TreeMap<Integer, Map<Integer, Double>> calculateMonthlyReturns(List<Trade> trades, double initialBalance) {
        TreeMap<Integer, Map<Integer, Double>> returns = new TreeMap<>();
        double balance = initialBalance;
        
        for (Trade trade : trades) {
            LocalDateTime closeTime = trade.getCloseTime();
            int year = closeTime.getYear();
            int month = closeTime.getMonthValue();
            
            returns.putIfAbsent(year, new TreeMap<>());
            Map<Integer, Double> monthReturns = returns.get(year);
            
            double monthReturn = monthReturns.getOrDefault(month, 0.0);
            double profitPercent = (trade.getTotalProfit() / balance) * 100;
            monthReturns.put(month, monthReturn + profitPercent);
            
            balance += trade.getTotalProfit();
        }
        
        return returns;
    }
}

