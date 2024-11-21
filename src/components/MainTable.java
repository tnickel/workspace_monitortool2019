package components;



import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;           // Diese Zeile fehlt
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
import utils.LoggerUtil;

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
        initializeTable();
        setupMouseListener();
        setupModelListener();
    }
    
    private void initializeTable() {
        setModel(model);
        setRowSorter(new TableRowSorter<>(model));
        
        // Set renderer for all columns
        for (int i = 0; i < getColumnCount(); i++) {
            getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
        
        LoggerUtil.debug("Loading data into table...");
        LoggerUtil.debug("Number of providers: " + dataManager.getStats().size());
        
        // Populate data
        model.populateData(dataManager.getStats());
        
        LoggerUtil.debug("Table rows after population: " + model.getRowCount());
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
    
    private void updateStatus() {
        if (statusUpdateCallback != null) {
            int totalProviders = dataManager.getStats().size();
            int visibleProviders = model.getRowCount();
            
            StringBuilder status = new StringBuilder()
                .append(String.format("Loaded %d providers (showing %d)", totalProviders, visibleProviders));
            
            // Wenn Filter aktiv ist, füge die Filterkriterien hinzu
            if (currentFilter != null) {
                status.append(" | Filter: ");
                List<String> activeFilters = new ArrayList<String>();
                
             
                
                if (currentFilter.getMinTradeDays() > 0) {
                    activeFilters.add(String.format("Min Days: %d", currentFilter.getMinTradeDays()));
                }
                if (currentFilter.getMinProfit() > 0) {
                    activeFilters.add(String.format("Min Profit: %.2f", currentFilter.getMinProfit()));
                }
                if (currentFilter.getMinProfitFactor() > 0) {
                    activeFilters.add(String.format("Min PF: %.2f", currentFilter.getMinProfitFactor()));
                }
                if (currentFilter.getMinWinRate() > 0) {
                    activeFilters.add(String.format("Min WinRate: %.1f%%", currentFilter.getMinWinRate()));
                }
                if (currentFilter.getMaxDrawdown() < 100) {
                    activeFilters.add(String.format("Max DD: %.1f%%", currentFilter.getMaxDrawdown()));
                }
                
                status.append(String.join(", ", activeFilters));
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
        LoggerUtil.info("Applying filter: " + criteria);
        this.currentFilter = criteria;
        refreshTableData();
    }
    
    public void clearFilter() {
        LoggerUtil.info("Clearing filter");
        this.currentFilter = null;
        refreshTableData();
    }
    
    private void refreshTableData() {
        if (currentFilter == null) {
            LoggerUtil.debug("No filter active, showing all data");
            model.populateData(dataManager.getStats());
        } else {
            LoggerUtil.debug("Filtering data with criteria");
            Map<String, ProviderStats> filteredStats = dataManager.getStats().entrySet().stream()
                .filter(entry -> currentFilter.matches(entry.getValue()))
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
                ));
            model.populateData(filteredStats);
            LoggerUtil.info(String.format("Filter applied: %d of %d providers match criteria",
                filteredStats.size(), dataManager.getStats().size()));
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