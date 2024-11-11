package components;

import models.HighlightTableModel;
import models.FilterCriteria;
import renderers.HighlightRenderer;
import data.DataManager;
import data.ProviderStats;
import ui.DetailFrame;
import utils.LoggerUtil;
import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.stream.Collectors;

public class MainTable extends JTable {
    private final HighlightTableModel model;
    private final HighlightRenderer renderer;
    private final DataManager dataManager;
    private FilterCriteria currentFilter;
    
    public MainTable(DataManager dataManager) {
        this.dataManager = dataManager;
        this.model = new HighlightTableModel();
        this.renderer = new HighlightRenderer();
        initializeTable();
        setupMouseListener();
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
                        new DetailFrame(providerName, 
                            dataManager.getStats().get(providerName)).setVisible(true);
                    }
                }
            }
        });
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
    
    // Neue Methoden für die Filterung
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
    }
}