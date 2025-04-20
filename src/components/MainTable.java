package components;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.table.TableRowSorter;

import data.DataManager;
import data.ProviderStats;
import models.FilterCriteria;
import models.HighlightTableModel;
import renderers.HighlightRenderer;
import renderers.RiskScoreRenderer;
import services.ProviderHistoryService;
import ui.PerformanceAnalysisDialog;
import ui.ShowSignalProviderList;
import utils.HtmlDatabase;
import utils.MqlAnalyserConf;

public class MainTable extends JTable {
    private static final Logger LOGGER = Logger.getLogger(MainTable.class.getName());
    private final HighlightTableModel model;
    private final HighlightRenderer renderer;
    private final RiskScoreRenderer riskRenderer;
    private final DataManager dataManager;
    private final String rootPath;
    private Consumer<String> statusUpdateCallback;
    private final HtmlDatabase htmlDatabase;
    private final ProviderHistoryService historyService;
    
    // Manager für verschiedene Funktionalitäten
    private final TableFilterManager filterManager;
    private final TableColumnManager columnManager;
    private final FavoritesFilterManager favoritesManager;

    public MainTable(DataManager dataManager, String rootPathStr) {
        this.dataManager = dataManager;
        this.rootPath = rootPathStr;
        
        // Konfiguration aus dem Root-Pfad laden
        MqlAnalyserConf config = new MqlAnalyserConf(rootPathStr);
        String downloadPath = config.getDownloadPath();
        
        // Model und Renderer initialisieren
        this.model = new HighlightTableModel(downloadPath);
        this.renderer = new HighlightRenderer();
        this.riskRenderer = new RiskScoreRenderer();
        this.htmlDatabase = new HtmlDatabase(downloadPath);
        
        // Provider History Service initialisieren
        this.historyService = ProviderHistoryService.getInstance();
        this.historyService.initialize(rootPath);
        
        // Manager initialisieren
        this.filterManager = new TableFilterManager(this, model, dataManager);
        this.columnManager = new TableColumnManager(this);
        this.favoritesManager = new FavoritesFilterManager(this, model, dataManager.getStats(), rootPath);
        
        // Tabelle initialisieren
        initialize();
        setupMouseListener();
        setupModelListener();
        
        // Spalten-Sichtbarkeit laden NACH der Initialisierung der Tabelle
        columnManager.loadColumnVisibilitySettings();
        
        // Stellen Sie sicher, dass beim Beenden der Anwendung die Ressourcen freigegeben werden
        try {
            JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            if (parentFrame != null) {
                parentFrame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        historyService.shutdown();
                    }
                });
            }
        } catch (Exception e) {
            LOGGER.warning("Konnte WindowListener nicht hinzufügen: " + e.getMessage());
        }
    }

    public FilterCriteria getCurrentFilter() {
        return filterManager.getCurrentFilter();
    }

    private void initialize() {
        setModel(model);
        TableRowSorter<HighlightTableModel> sorter = new TableRowSorter<>(model);
        
        // Konfiguriere den Sorter
        for (int i = 0; i < model.getColumnCount(); i++) {
            final int column = i;
            sorter.setComparator(column, (java.util.Comparator<Object>) (o1, o2) -> {
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
        
        // Tooltip-Setup
        ToolTipManager.sharedInstance().setInitialDelay(200);
        ToolTipManager.sharedInstance().setDismissDelay(8000);
        ToolTipManager.sharedInstance().registerComponent(this);
        
        // Setze die Renderer für die Spalten
        for (int i = 0; i < getColumnCount(); i++) {
            // Finde die Risk Score Spalte basierend auf dem Index
            if (i == 19) { // Risk Score ist an Position 19 im COLUMN_NAMES Array
                getColumnModel().getColumn(i).setCellRenderer(riskRenderer);
            } else {
                getColumnModel().getColumn(i).setCellRenderer(renderer);
            }
        }
        
        // Konfiguriere die Spaltenbreiten
        getColumnModel().getColumn(1).setPreferredWidth(300);  // Signal Provider ist Spalte 1
        getColumnModel().getColumn(1).setMinWidth(250);
        
        // Daten laden
        model.populateData(dataManager.getStats());
    }
    
    public JButton createShowSignalProviderButton() {
        JButton showProvidersButton = new JButton("Show Signal Providers");
        showProvidersButton.addActionListener(e -> {
            Map<String, ProviderStats> currentStats = getCurrentProviderStats();
            ShowSignalProviderList dialog = new ShowSignalProviderList(
                SwingUtilities.getWindowAncestor(this),
                currentStats,
                htmlDatabase,
                rootPath
            );
            dialog.setVisible(true);
        });
        return showProvidersButton;
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
                            PerformanceAnalysisDialog detailFrame = new PerformanceAnalysisDialog(
                                providerName, stats, providerId, htmlDatabase, rootPath);
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
    
    public String getStatusText() {
        int totalProviders = dataManager.getStats().size();
        int visibleProviders = model.getRowCount();
        
        StringBuilder status = new StringBuilder()
            .append(String.format("%d/%d Signal Providers", visibleProviders, totalProviders));
            
        if (filterManager.getCurrentFilter() != null) {
            status.append(" (filtered)");
        }

        status.append(" | Download Path: " + htmlDatabase.getRootPath());
        status.append(" | Root Path: " + rootPath);

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
        filterManager.refreshFilteredData();
        
        // Prüfen, ob wöchentliche Speicherung erforderlich ist
        historyService.checkAndPerformWeeklySave();
        
        // Bei jeder Aktualisierung auf geänderte 3MPDD-Werte prüfen und ggf. speichern
        for (Map.Entry<String, ProviderStats> entry : dataManager.getStats().entrySet()) {
            String providerName = entry.getKey();
            
            // Die Berechnung der 3MPDD-Werte erfolgt jetzt direkt hier
            double threeMonthProfit = htmlDatabase.getAverageMonthlyProfit(providerName, 3);
            double equityDrawdown = htmlDatabase.getEquityDrawdown(providerName);
            double mpdd3 = model.calculateMPDD(threeMonthProfit, equityDrawdown);
            
            // Speichere den Wert in der Datenbank
            historyService.store3MpddValue(providerName, mpdd3);
        }
    }
    
    public Map<String, ProviderStats> getCurrentProviderStats() {
        return filterManager.getFilteredProviderStats();
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
        filterManager.applyFilter(criteria);
    }

    public void resetFilter() {
        filterManager.resetFilter();
    }
    
    public HtmlDatabase getHtmlDatabase() {
        return htmlDatabase;
    }
    
    public void filterFavorites() {
        favoritesManager.filterByFavorites();
    }

    public boolean isColumnVisible(int columnIndex) {
        return columnManager.isColumnVisible(columnIndex);
    }
   
    public void setColumnVisible(int columnIndex, boolean visible) {
        columnManager.setColumnVisible(columnIndex, visible);
    }
}