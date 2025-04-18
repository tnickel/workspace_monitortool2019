package components;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import data.DataManager;
import data.FavoritesManager;
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
    private final HighlightTableModel model;
    private final HighlightRenderer renderer;
    private final RiskScoreRenderer riskRenderer;
    private final DataManager dataManager;
    private String rootPath;
    private String downloadPath;
    private FilterCriteria currentFilter;
    private Consumer<String> statusUpdateCallback;
    private final HtmlDatabase htmlDatabase;
    private Map<Integer, Integer> originalColumnWidths = new HashMap<>();
    private final ProviderHistoryService historyService;

    
    public MainTable(DataManager dataManager, String rootPathStr) {
        this.dataManager = dataManager;
        
        // Extrahiere Hauptpfad und Download-Pfad korrekt
        this.rootPath = rootPathStr;

        
        MqlAnalyserConf config = new MqlAnalyserConf(rootPathStr);
        this.downloadPath = config.getDownloadPath();
      
        
        this.model = new HighlightTableModel(downloadPath);
        this.renderer = new HighlightRenderer();
        this.riskRenderer = new RiskScoreRenderer();
        this.htmlDatabase = new HtmlDatabase(downloadPath);
        this.currentFilter = new FilterCriteria();
        
        // Provider History Service initialisieren
        this.historyService = ProviderHistoryService.getInstance();
        this.historyService.initialize(rootPath);

        loadSavedFilter(); // Filter beim Start laden
        initialize();
        setupMouseListener();
        setupModelListener();
        
        // Spalten-Sichtbarkeit laden NACH der Initialisierung der Tabelle
        loadColumnVisibilitySettings();
        
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
           System.out.println("Error=xxx");
        }
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
        
        // Setze die Renderer für die Spalten
        for (int i = 0; i < getColumnCount(); i++) {
            String columnName = getColumnModel().getColumn(i).getHeaderValue().toString();
            // Finde die Risk Score Spalte basierend auf dem Index
            if (i == 19) { // Risk Score ist an Position 19 im COLUMN_NAMES Array
                getColumnModel().getColumn(i).setCellRenderer(riskRenderer);
            } else {
                getColumnModel().getColumn(i).setCellRenderer(renderer);
            }
        }
        getColumnModel().getColumn(1).setPreferredWidth(300);  // Signal Provider ist Spalte 1
        getColumnModel().getColumn(1).setMinWidth(250);      
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
                            // Verwende den korrekten rootPath für den PerformanceAnalysisDialog
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
            
        if (currentFilter != null) {
            status.append(" (filtered)");
        }

        status.append(" | Download Path: " + downloadPath);
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
        if (currentFilter == null) {
            model.populateData(dataManager.getStats());
        } else {
            Map<String, ProviderStats> filteredStats = dataManager.getStats().entrySet().stream()
                .filter(entry -> {
                    // Hole die tatsächlichen Werte durch temporäres Befüllen der Tabelle
                    model.setRowCount(0);
                    model.populateData(Map.of(entry.getKey(), entry.getValue()));
                    
                    // Extrahiere die Werte aus der ersten (und einzigen) Zeile
                    Object[] rowData = new Object[model.getColumnCount()];
                    for (int i = 0; i < model.getColumnCount(); i++) {
                        rowData[i] = model.getValueAt(0, i);
                    }
                    
                    // Prüfe ob die Werte dem Filter entsprechen
                    boolean matches = currentFilter.matches(entry.getValue(), rowData);
                    return matches;
                })
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
                ));
                
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
            
            // Zeige die gefilterten Daten an
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
    
    public HtmlDatabase getHtmlDatabase() {
        return htmlDatabase;
    }
    
    public void filterFavorites() {
        if (dataManager == null) return;
        
        FavoritesManager favoritesManager = new FavoritesManager(rootPath);
        System.out.println("Filterung nach Favoriten mit rootPath: " + rootPath);
        
        Map<String, ProviderStats> filteredStats = new HashMap<>();
        
        for (Map.Entry<String, ProviderStats> entry : dataManager.getStats().entrySet()) {
            String providerName = entry.getKey();
            String providerId = extractProviderId(providerName);
            
            System.out.println("Prüfe Provider: " + providerName + " mit ID: " + providerId);
            
            if (favoritesManager.isFavorite(providerId)) {
                System.out.println("  -> Ist ein Favorit!");
                filteredStats.put(providerName, entry.getValue());
            }
        }
        
        model.populateData(filteredStats);
        updateStatus();
    }
    
    // Hilfsmethode, um die Provider-ID aus dem Dateinamen zu extrahieren
    private String extractProviderId(String providerName) {
        // Variante 1: Name_123456.csv -> 123456
        int underscoreIndex = providerName.lastIndexOf("_");
        int dotIndex = providerName.lastIndexOf(".");
        
        if (underscoreIndex > 0 && dotIndex > underscoreIndex) {
            return providerName.substring(underscoreIndex + 1, dotIndex);
        }
        
        // Variante 2: Falls das Format anders ist, versuche Zahlen zu extrahieren
        StringBuilder digits = new StringBuilder();
        for (char c : providerName.toCharArray()) {
            if (Character.isDigit(c)) {
                digits.append(c);
            }
        }
        
        if (digits.length() > 0) {
            return digits.toString();
        }
        
        // Fallback
        return providerName;
    }
    
    public void loadColumnVisibilitySettings() {
        File configFile = new File("column_config.properties");
        Properties props = new Properties();
        
        if (configFile.exists()) {
            try (FileInputStream in = new FileInputStream(configFile)) {
                props.load(in);
                
                // Prüfen, ob überhaupt Spalteneinstellungen existieren
                boolean hasSettings = false;
                for (int i = 0; i < getColumnCount(); i++) {
                    String key = "column_visible_" + i;
                    if (props.getProperty(key) != null) {
                        hasSettings = true;
                        break;
                    }
                }
                
                if (hasSettings) {
                    // Bestehende Einstellungen laden
                    for (int i = 0; i < getColumnCount(); i++) {
                        // Erste 2 Spalten sind immer sichtbar
                        if (i <= 1) continue;
                        
                        String columnName = getColumnName(i);
                        
                        // MaxDrawdown-Spalte immer ausblenden
                        if (columnName.equals("Max Drawdown %")) {
                            TableColumn column = getColumnModel().getColumn(i);
                            int originalWidth = column.getPreferredWidth();
                            originalColumnWidths.put(i, originalWidth);
                            column.setMinWidth(0);
                            column.setPreferredWidth(0);
                            column.setMaxWidth(0);
                            continue;
                        }
                        
                        String key = "column_visible_" + i;
                        boolean visible = Boolean.parseBoolean(props.getProperty(key, "true"));
                        
                        // Prüfe, ob die Spalte sichtbar sein soll
                        if (!visible) {
                            // Spalte verstecken
                            TableColumn column = getColumnModel().getColumn(i);
                            int originalWidth = column.getPreferredWidth();
                            originalColumnWidths.put(i, originalWidth);
                            column.setMinWidth(0);
                            column.setPreferredWidth(0);
                            column.setMaxWidth(0);
                        }
                    }
                } else {
                    // Wenn keine Einstellungen existieren, Standardwerte anwenden
                    setDefaultColumnVisibility();
                }
                
                System.out.println("Spalteneinstellungen wurden geladen aus: " + configFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Fehler beim Laden der Spalteneinstellungen: " + e.getMessage());
                e.printStackTrace();
                
                // Bei Fehler: Standardeinstellungen verwenden
                setDefaultColumnVisibility();
            }
        } else {
            // Wenn keine Konfigurationsdatei existiert, Standardeinstellungen verwenden
            setDefaultColumnVisibility();
        }
    }

    private void setDefaultColumnVisibility() {
        // Standardeinstellungen: Nur wichtige Spalten anzeigen, andere ausblenden
        // Beispiel: Spalten 0, 1, 3, 4, 8, 11, 14, 19 sind sichtbar (Index-basiert)
        for (int i = 0; i < getColumnCount(); i++) {
            String columnName = getColumnName(i);
            
            // MaxDrawdown-Spalte immer ausblenden
            if (columnName.equals("Max Drawdown %")) {
                TableColumn column = getColumnModel().getColumn(i);
                int originalWidth = column.getPreferredWidth();
                originalColumnWidths.put(i, originalWidth);
                column.setMinWidth(0);
                column.setPreferredWidth(0);
                column.setMaxWidth(0);
                continue;
            }
            
            // Wichtige Spalten standardmäßig sichtbar lassen
            boolean isStandardVisible = (i <= 1) || (i == 3) || (i == 4) || (i == 8) || 
                                       (i == 11) || (i == 14) || (i == 19);
            
            if (!isStandardVisible) {
                // Spalte verstecken
                TableColumn column = getColumnModel().getColumn(i);
                int originalWidth = column.getPreferredWidth();
                originalColumnWidths.put(i, originalWidth);
                column.setMinWidth(0);
                column.setPreferredWidth(0);
                column.setMaxWidth(0);
            }
        }
    }

    /**
     * Prüft, ob eine Spalte sichtbar ist
     * 
     * @param columnIndex Index der Spalte
     * @return true wenn die Spalte sichtbar ist, false sonst
     */
    public boolean isColumnVisible(int columnIndex) {
        if (columnIndex < 0 || columnIndex >= getColumnCount()) {
            return false;
        }
        
        return !originalColumnWidths.containsKey(columnIndex);
    }
   
    public void setColumnVisible(int columnIndex, boolean visible) {
        if (columnIndex < 0 || columnIndex >= getColumnCount()) {
            return;
        }
        
        // Verhindere, dass die ersten beiden Spalten jemals ausgeblendet werden
        if (columnIndex <= 1 && !visible) {
            return;
        }
        
        // Verhindere, dass die MaxDrawdown-Spalte jemals eingeblendet wird
        String columnName = getColumnName(columnIndex);
        if (columnName.equals("Max Drawdown %") && visible) {
            return;
        }
        
        try {
            TableColumn column = getColumnModel().getColumn(columnIndex);
            
            if (visible) {
                // Spalte wieder sichtbar machen
                if (originalColumnWidths.containsKey(columnIndex)) {
                    // Originale Breite wiederherstellen
                    column.setMinWidth(0);
                    column.setMaxWidth(Integer.MAX_VALUE);
                    column.setPreferredWidth(originalColumnWidths.get(columnIndex));
                    originalColumnWidths.remove(columnIndex);
                }
            } else {
                // Spalte unsichtbar machen
                if (!originalColumnWidths.containsKey(columnIndex)) {
                    // Originale Breite speichern
                    originalColumnWidths.put(columnIndex, column.getPreferredWidth());
                    
                    // Spalte auf minimale Breite setzen
                    column.setMinWidth(0);
                    column.setPreferredWidth(0);
                    column.setMaxWidth(0);
                }
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Ändern der Spaltensichtbarkeit: " + e.getMessage());
        }
    }
}