package components;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.JTable;
import javax.swing.table.TableRowSorter;

import data.DataManager;
import data.ProviderStats;
import models.FilterCriteria;
import models.HighlightTableModel;
import renderers.HighlightRenderer;
import renderers.RiskScoreRenderer;
import ui.DetailFrame;

public class MainTable extends JTable {
    private final HighlightTableModel model;
    private final HighlightRenderer renderer;
    private final RiskScoreRenderer riskRenderer;
    private final DataManager dataManager;
    private final DateTimeFormatter dateFormatter;
    private FilterCriteria currentFilter;
    private Consumer<String> statusUpdateCallback;
    
    public MainTable(DataManager dataManager) {
        this.dataManager = dataManager;
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        this.model = new HighlightTableModel(dateFormatter);
        this.renderer = new HighlightRenderer();
        this.riskRenderer = new RiskScoreRenderer();
        initialize();
        setupMouseListener();
        setupModelListener();
    }
    
    private void initialize() {
        setModel(model);
        setRowSorter(new TableRowSorter<>(model));
        
        // Set renderer for all columns
        for (int i = 0; i < getColumnCount(); i++) {
            if (i == 6) { // Max Drawdown column
                getColumnModel().getColumn(i).setCellRenderer(riskRenderer);
            } else if (i == 7) { // Profit Factor column
                getColumnModel().getColumn(i).setCellRenderer(riskRenderer);
            } else {
                getColumnModel().getColumn(i).setCellRenderer(renderer);
            }
        }
        
        // Set column widths
        getColumnModel().getColumn(0).setPreferredWidth(50);   // No.
        getColumnModel().getColumn(1).setPreferredWidth(150);  // Signal Provider
        getColumnModel().getColumn(2).setPreferredWidth(80);   // Trades
        getColumnModel().getColumn(3).setPreferredWidth(100);  // Win Rate
        getColumnModel().getColumn(4).setPreferredWidth(100);  // Total Profit
        getColumnModel().getColumn(5).setPreferredWidth(120);  // Avg Profit/Trade
        getColumnModel().getColumn(6).setPreferredWidth(120);  // Max Drawdown
        getColumnModel().getColumn(7).setPreferredWidth(100);  // Profit Factor
        getColumnModel().getColumn(8).setPreferredWidth(150);  // Max Concurrent Trades
        getColumnModel().getColumn(9).setPreferredWidth(150);  // Max Concurrent Lots
        getColumnModel().getColumn(10).setPreferredWidth(100); // Start Date
        getColumnModel().getColumn(11).setPreferredWidth(100); // End Date
        getColumnModel().getColumn(12).setPreferredWidth(80);  // Users
        getColumnModel().getColumn(13).setPreferredWidth(120); // Invested Capital
        
        // Initial data population
        refreshTableData();
    }
    
    private void setupMouseListener() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = getSelectedRow();
                    if (row != -1) {
                        row = convertRowIndexToModel(row);
                        String providerName = (String) model.getValueAt(row, 1);
                        ProviderStats stats = dataManager.getStats().get(providerName);
                        String providerId = providerName.substring(providerName.lastIndexOf("_") + 1).replace(".csv", "");
                        
                        if (stats != null) {
                            DetailFrame detailFrame = new DetailFrame(providerName, stats, providerId);
                            detailFrame.setVisible(true);
                        }
                    }
                }
            }
        });
    }
    
    private void setupModelListener() {
        model.addTableModelListener(e -> updateStatus());
    }
    
    public void setStatusUpdateCallback(Consumer<String> callback) {
        this.statusUpdateCallback = callback;
        updateStatus();
    }
    
    public void updateStatus() {
        if (statusUpdateCallback != null) {
            int totalProviders = dataManager.getStats().size();
            int visibleProviders = model.getRowCount();
            
            StringBuilder status = new StringBuilder()
                .append(String.format("%d/%d Signal Providers", visibleProviders, totalProviders));
                
            if (currentFilter != null) {
                status.append(" (filtered)");
            }

            statusUpdateCallback.accept(status.toString());
        }
    }
    
    public void highlightSearchText(String text) {
        renderer.setSearchText(text);
        repaint();
    }
    
    public void clearHighlight() {
        renderer.setSearchText("");
        repaint();
    }
    
    public boolean findAndSelectNext(String searchText, int[] currentIndex) {
        int startRow = currentIndex[0] + 1;
        
        for (int row = startRow; row < getRowCount(); row++) {
            for (int col = 0; col < getColumnCount(); col++) {
                Object value = getValueAt(row, col);
                if (value != null && value.toString().toLowerCase().contains(searchText)) {
                    currentIndex[0] = row;
                    scrollRectToVisible(getCellRect(row, 0, true));
                    setRowSelectionInterval(row, row);
                    return true;
                }
            }
        }
        return false;
    }
    
    public void applyFilter(FilterCriteria criteria) {
        this.currentFilter = criteria;
        refreshTableData();
    }
    
    private void refreshTableData() {
        model.clearData();
        Map<String, ProviderStats> statsToShow;
        
        if (currentFilter == null) {
            statsToShow = dataManager.getStats();
        } else {
            statsToShow = dataManager.getStats().entrySet().stream()
                .filter(entry -> currentFilter.matches(entry.getValue()))
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
                ));
        }
        
        for (Map.Entry<String, ProviderStats> entry : statsToShow.entrySet()) {
            String providerName = entry.getKey();
            model.addRow(providerName, entry.getValue());
        }
        
        updateStatus();
    }
    
    public Map<String, ProviderStats> getCurrentProviderStats() {
        if (currentFilter == null) {
            return dataManager.getStats();
        }
        return dataManager.getStats().entrySet().stream()
            .filter(entry -> currentFilter.matches(entry.getValue()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue
            ));
    }
}