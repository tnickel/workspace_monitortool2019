package components;



import data.ProviderStats;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public class TradeListTable extends JTable {
    private final DefaultTableModel model;
    
    public TradeListTable(ProviderStats stats) {
        this.model = createTableModel(stats);
        initializeTable();
        setupRenderers();
    }
    
    private DefaultTableModel createTableModel(ProviderStats stats) {
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
        
        populateData(model, stats);
        return model;
    }
    
    private void populateData(DefaultTableModel model, ProviderStats stats) {
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
    }
    
    private void initializeTable() {
        setModel(model);
        setRowSorter(new TableRowSorter<>(model));
        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    }
    
    private void setupRenderers() {
        // Profit/Loss column renderer
        getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
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

        // Running balance column renderer
        getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
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
    }
}