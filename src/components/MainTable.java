package components;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import ui.DetailFrame;

public class MainTable extends JTable {
    private final HighlightTableModel model;
    private final HighlightRenderer renderer;
    private final DataManager dataManager;
    private FilterCriteria currentFilter;
    private Consumer<String> statusUpdateCallback;
    
    public MainTable(DataManager dataManager) {
        this.dataManager = dataManager;
        this.model = new HighlightTableModel();
        this.renderer = new HighlightRenderer();
        initialize();
        setupMouseListener();
        setupModelListener();
    }
    
    private void initialize() {
        setModel(model);
        setRowSorter(new TableRowSorter<>(model));
        
        // Set renderer for all columns
        for (int i = 0; i < getColumnCount(); i++) {
            getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
        
        // Populate data
        model.populateData(dataManager.getStats());
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
                        String providerId = providerName.substring(providerName.lastIndexOf("_") + 1).replace(".csv", "");
                        new DetailFrame(providerName,
                                      dataManager.getStats().get(providerName),
                                      providerId).setVisible(true);
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
        if (currentFilter == null) {
            model.populateData(dataManager.getStats());
        } else {
            Map<String, ProviderStats> filteredStats = dataManager.getStats().entrySet().stream()
                .filter(entry -> currentFilter.matches(entry.getValue()))
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
                ));
            model.populateData(filteredStats);
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