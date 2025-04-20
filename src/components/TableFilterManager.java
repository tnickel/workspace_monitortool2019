package components;

import models.FilterCriteria;
import data.ProviderStats;
import data.DataManager;
import models.HighlightTableModel;

import java.util.Map;
import java.util.stream.Collectors;

public class TableFilterManager {
    private final MainTable mainTable;
    private final HighlightTableModel tableModel;
    private final DataManager dataManager;
    private FilterCriteria currentFilter;

    public TableFilterManager(MainTable mainTable, HighlightTableModel tableModel, DataManager dataManager) {
        this.mainTable = mainTable;
        this.tableModel = tableModel;
        this.dataManager = dataManager;
        this.currentFilter = new FilterCriteria();
        loadSavedFilter();
    }

    public FilterCriteria getCurrentFilter() {
        return currentFilter != null ? currentFilter : new FilterCriteria();
    }

    public void applyFilter(FilterCriteria criteria) {
        this.currentFilter = criteria;
        currentFilter.saveFilters(); // Speichert die Filterwerte nach Anwendung
        refreshFilteredData();
    }

    public void resetFilter() {
        this.currentFilter = new FilterCriteria();
        currentFilter.saveFilters(); // Speichert den leeren Filter
        refreshFilteredData();
    }

    public void loadSavedFilter() {
        if (currentFilter == null) {
            currentFilter = new FilterCriteria();
        }
        currentFilter.loadFilters();
    }

    public void refreshFilteredData() {
        if (currentFilter == null) {
            tableModel.populateData(dataManager.getStats());
        } else {
            Map<String, ProviderStats> filteredStats = dataManager.getStats().entrySet().stream()
                .filter(entry -> {
                    // Hole die tatsächlichen Werte durch temporäres Befüllen der Tabelle
                    tableModel.setRowCount(0);
                    tableModel.populateData(Map.of(entry.getKey(), entry.getValue()));
                    
                    // Extrahiere die Werte aus der ersten (und einzigen) Zeile
                    Object[] rowData = new Object[tableModel.getColumnCount()];
                    for (int i = 0; i < tableModel.getColumnCount(); i++) {
                        rowData[i] = tableModel.getValueAt(0, i);
                    }
                    
                    // Prüfe ob die Werte dem Filter entsprechen
                    boolean matches = currentFilter.matches(entry.getValue(), rowData);
                    return matches;
                })
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
                ));
                
            // Zeige die gefilterten Daten an
            tableModel.populateData(filteredStats);
        }
        mainTable.updateStatus();
    }

    public Map<String, ProviderStats> getFilteredProviderStats() {
        if (currentFilter == null) {
            return dataManager.getStats();
        }
        return dataManager.getStats().entrySet().stream()
            .filter(entry -> currentFilter.matches(
                entry.getValue(),
                tableModel.createRowDataForProvider(entry.getKey(), entry.getValue())
            ))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue
            ));
    }
}