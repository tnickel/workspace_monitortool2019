package components;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import data.DataManager;
import data.ProviderStats;
import models.FilterCriteria;
import models.HighlightTableModel;
import ui.LoadingDialog;

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
        refreshFilteredDataWithProgress();
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

    /**
     * Diese neue Methode führt die Filterung mit einer Fortschrittsanzeige durch
     */
    public void refreshFilteredDataWithProgress() {
        if (currentFilter == null) {
            tableModel.populateData(dataManager.getStats());
            mainTable.updateStatus();
            return;
        }
        
        LoadingDialog progressDialog = new LoadingDialog(
        	    (Frame)SwingUtilities.getWindowAncestor(mainTable),
        	    "Filter anwenden",
        	    "Filtere Daten..."
        	);
        
        // Starte die Filterung in einem Hintergrund-Thread
        SwingWorker<Map<String, ProviderStats>, Integer> worker = 
            new SwingWorker<Map<String, ProviderStats>, Integer>() {
                
            @Override
            protected Map<String, ProviderStats> doInBackground() throws Exception {
                Map<String, ProviderStats> stats = dataManager.getStats();
                List<Map.Entry<String, ProviderStats>> entries = new ArrayList<>(stats.entrySet());
                Map<String, ProviderStats> result = new java.util.HashMap<>();
                
                int total = entries.size();
                for (int i = 0; i < total; i++) {
                    Map.Entry<String, ProviderStats> entry = entries.get(i);
                    
                    // Für den Fortschritt
                    int progress = (i * 100) / total;
                    publish(progress);
                    
                    // Hole die tatsächlichen Werte durch temporäres Befüllen der Tabelle
                    tableModel.setRowCount(0);
                    tableModel.populateData(Map.of(entry.getKey(), entry.getValue()));
                    
                    // Extrahiere die Werte aus der ersten (und einzigen) Zeile
                    Object[] rowData = new Object[tableModel.getColumnCount()];
                    for (int j = 0; j < tableModel.getColumnCount(); j++) {
                        rowData[j] = tableModel.getValueAt(0, j);
                    }
                    
                    // Prüfe ob die Werte dem Filter entsprechen
                    boolean matches = currentFilter.matches(entry.getValue(), rowData);
                    if (matches) {
                        result.put(entry.getKey(), entry.getValue());
                    }
                    
                    final int currentIndex = i; // Neue finale Variable für den Lambda-Ausdruck
                    SwingUtilities.invokeLater(() -> {
                        progressDialog.setStatus("Verarbeite Provider: " + entry.getKey() + 
                                               " (" + (currentIndex+1) + "/" + total + ")");
                    });
                }
                
                return result;
            }
            
            @Override
            protected void process(List<Integer> chunks) {
                // Update progress bar with the latest value
                if (!chunks.isEmpty()) {
                    int latestProgress = chunks.get(chunks.size() - 1);
                    progressDialog.setProgress(latestProgress);
                }
            }
            
            @Override
            protected void done() {
                try {
                    Map<String, ProviderStats> filteredStats = get();
                    // Zeige die gefilterten Daten an
                    tableModel.populateData(filteredStats);
                    mainTable.updateStatus();
                    progressDialog.complete();
                } catch (Exception e) {
                    e.printStackTrace();
                    progressDialog.dispose();
                }
            }
        };
        
        // Starte den Worker und zeige den Dialog
        worker.execute();
        progressDialog.setVisible(true);
    }

    public void refreshFilteredData() {
        // Für kleine Datenmengen oder programmatische Aufrufe ohne Fortschrittsanzeige
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