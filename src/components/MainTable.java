package components;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.JTable;
import javax.swing.ToolTipManager;
import javax.swing.table.TableRowSorter;

import data.DataManager;
import data.ProviderStats;
import models.FilterCriteria;
import models.HighlightTableModel;
import renderers.HighlightRenderer;
import renderers.RiskScoreRenderer;
import ui.DetailFrame;
import utils.HtmlParser;
import utils.StabilityResult;

public class MainTable extends JTable {
    private final HighlightTableModel model;
    private final HighlightRenderer renderer;
    private final RiskScoreRenderer riskRenderer;
    private final DataManager dataManager;
    private String rootPath;
    private FilterCriteria currentFilter;
    private Consumer<String> statusUpdateCallback;
    private final HtmlParser htmlParser;
    
    public MainTable(DataManager dataManager, String downloadPath) {
        this.dataManager = dataManager;
        this.rootPath = downloadPath;
        this.model = new HighlightTableModel(rootPath);
        this.renderer = new HighlightRenderer();
        this.riskRenderer = new RiskScoreRenderer();
        this.htmlParser = new HtmlParser(rootPath);
        this.currentFilter = new FilterCriteria();

        loadSavedFilter(); // Filter beim Start laden

        initialize();
        setupMouseListener();
        setupModelListener();
    }

    public FilterCriteria getCurrentFilter() {
        return currentFilter != null ? currentFilter : new FilterCriteria();
    }

    	private void initialize() {
    	    setModel(model);
    	    TableRowSorter<HighlightTableModel> sorter = new TableRowSorter<>(model);
    	    
    	    // Setze spezifische Comparatoren für jede Spalte
    	    for (int i = 0; i < model.getColumnCount(); i++) {
    	        final int column = i;
    	        sorter.setComparator(column, (Comparator<Object>) (o1, o2) -> {
    	            if (o1 == null && o2 == null) return 0;
    	            if (o1 == null) return -1;
    	            if (o2 == null) return 1;
    	            
    	            // Behandle numerische Spalten
    	            if (o1 instanceof Number && o2 instanceof Number) {
    	                double d1 = ((Number) o1).doubleValue();
    	                double d2 = ((Number) o2).doubleValue();
    	                return Double.compare(d1, d2);
    	            }
    	            
    	            // String Vergleich für alle anderen Fälle
    	            return o1.toString().compareTo(o2.toString());
    	        });
    	    }
    	    setRowSorter(sorter);
    	    
    	    ToolTipManager.sharedInstance().setInitialDelay(200);
    	    ToolTipManager.sharedInstance().setDismissDelay(8000);
    	    ToolTipManager.sharedInstance().registerComponent(this);
    	    
    	    for (int i = 0; i < getColumnCount(); i++) {
    	        if (i == 13) {
    	            getColumnModel().getColumn(i).setCellRenderer(riskRenderer);
    	        } else {
    	            getColumnModel().getColumn(i).setCellRenderer(renderer);
    	        }
    	    }
    	    
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
                        ProviderStats stats = dataManager.getStats().get(providerName);
                        String providerId = providerName.substring(providerName.lastIndexOf("_") + 1).replace(".csv", "");
                        
                        if (stats != null) {
                            DetailFrame detailFrame = new DetailFrame(providerName, stats, providerId, htmlParser);
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
    }
    
    @Override
    public String getToolTipText(MouseEvent e) {
        int row = rowAtPoint(e.getPoint());
        int col = columnAtPoint(e.getPoint());
        
        if (row >= 0 && col >= 0) {
            row = convertRowIndexToModel(row);
            col = convertColumnIndexToModel(col);
            
            if (col == 4) { // 3MProfProz column
                String providerName = (String) model.getValueAt(row, 1);
                return getThreeMonthProfitCalculationToolTip(providerName);
            } else if (col == 21) { // Stabilitaet column - Index korrigiert von 20 auf 21
                String providerName = (String) model.getValueAt(row, 1);
                if (providerName != null && htmlParser != null) {
                    StabilityResult stability = htmlParser.getStabilitaetswertDetails(providerName);
                    return "<html>" + stability.getDetails() + "</html>";
                }
            }
        }
        return null;
    }
    
    private String getThreeMonthProfitCalculationToolTip(String providerName) {
        List<String> profits = htmlParser.getLastThreeMonthsDetails(providerName);
        
        if (profits.isEmpty()) {
            return "Keine Profitdaten verfügbar";
        }

        double sum = profits.stream()
            .mapToDouble(s -> {
                String valueStr = s.split(":")[1].trim()
                                 .replace("%", "")
                                 .replace(",", ".");
                return Double.parseDouble(valueStr);
            })
            .sum();
        
        StringBuilder tooltip = new StringBuilder("<html><b>3-Monats Profit Berechnung:</b><br><br>");
        
        profits.forEach(profit -> tooltip.append(profit).append("<br>"));
        
        tooltip.append("<br>Summe: ").append(String.format("%.2f%%", sum))
               .append("<br>Durchschnitt: ").append(String.format("%.2f%%", sum / profits.size()))
               .append(" (").append(profits.size()).append(" Monate)")
               .append("</html>");
        
        return tooltip.toString();
    }
    
    public String getStatusText() {
        int totalProviders = dataManager.getStats().size();
        int visibleProviders = model.getRowCount();
        
        StringBuilder status = new StringBuilder()
            .append(String.format("%d/%d Signal Providers", visibleProviders, totalProviders));
            
        if (currentFilter != null) {
            status.append(" (filtered)");
        }

        status.append(" | Download Path: " + rootPath);

        return status.toString();
    }
    
    public void updateStatus() {
        if (statusUpdateCallback != null) {
            statusUpdateCallback.accept(getStatusText());
        }
        repaint();
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
    
    public void refreshTableData() {
        if (currentFilter == null) {
            model.populateData(dataManager.getStats());
        } else {
            Map<String, ProviderStats> filteredStats = dataManager.getStats().entrySet().stream()
                .filter(entry -> {
                    Object[] rowData = model.createRowDataForProvider(
                        entry.getKey(), 
                        entry.getValue()
                    );
                    return currentFilter.matches(entry.getValue(), rowData);
                })
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
            .filter(entry -> currentFilter.matches(
                entry.getValue(),
                model.createRowDataForProvider(entry.getKey(), entry.getValue())
            ))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue
            ));
    }

    public List<String> getSelectedProviders() {
        int[] selectedRows = getSelectedRows();
        List<String> providers = new ArrayList<>();
        for (int row : selectedRows) {
            int modelRow = convertRowIndexToModel(row);
            String provider = (String) model.getValueAt(modelRow, 1);
            providers.add(provider);
        }
        return providers;
    }

    public Map<String, ProviderStats> getSelectedProvidersMap() {
        List<String> selectedProviders = getSelectedProviders();
        Map<String, ProviderStats> selectedStats = new HashMap<>();
        
        Map<String, ProviderStats> allStats = dataManager.getStats();
        for (String provider : selectedProviders) {
            if (allStats.containsKey(provider)) {
                selectedStats.put(provider, allStats.get(provider));
            }
        }
        return selectedStats;
    }

    public void applyFilter(FilterCriteria criteria) {
        this.currentFilter = criteria;
        currentFilter.saveFilters(); // Speichert die Filterwerte nach Anwendung
        refreshTableData();
    }

    public void resetFilter() {
        this.currentFilter = new FilterCriteria();
        currentFilter.saveFilters(); // Speichert den leeren Filter
        refreshTableData();
    }

    public void loadSavedFilter() {
        if (currentFilter == null) {
            currentFilter = new FilterCriteria();
        }
        currentFilter.loadFilters();
    }
}