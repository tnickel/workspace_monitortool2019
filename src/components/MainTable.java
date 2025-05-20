package components;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
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
import utils.ApplicationConstants;
import utils.HtmlDatabase;
import utils.MqlAnalyserConf;
import reports.ReportGenerator;
import javax.swing.JOptionPane;
import java.io.File;
import java.awt.Desktop;
import java.util.Map;
import java.util.HashMap;
import data.FavoritesManager;
import renderers.NumberFormatRenderer;

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
        // Validiere den Pfad und korrigiere ihn, falls nötig
        rootPathStr = ApplicationConstants.validateRootPath(rootPathStr, "MainTable.constructor");
        
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
        
        // Manager initialisieren - Verwenden der Singleton-Instanz vom FavoritesManager
        this.filterManager = new TableFilterManager(this, model, dataManager);
        this.columnManager = new TableColumnManager(this);
        this.favoritesManager = new FavoritesFilterManager(this, model, dataManager.getStats(), rootPath);
        
        // Tabelle initialisieren
        initialize();
        setupMouseListener();
        setupModelListener();
        setupKeyBindings(); // Neue Methode für Delete-Taste
        
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
    
    // Neue Methoden für die Löschfunktion
    
    /**
     * Setzt Key-Bindings für Tastaturaktionen auf der Tabelle
     */
    private void setupKeyBindings() {
        // Key-Binding für die Delete-Taste
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
            "deleteSelectedProviders"
        );
        
        getActionMap().put("deleteSelectedProviders", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedProviders();
            }
        });
    }
    
    /**
     * Löscht die ausgewählten Signal-Provider aus dem Downloadbereich
     */
    private void deleteSelectedProviders() {
        List<String> selectedProviders = getSelectedProviders();
        
        if (selectedProviders.isEmpty()) {
            return;
        }
        
        // Bestätigung vom Benutzer einholen
        int result = JOptionPane.showConfirmDialog(
            this,
            "Möchten Sie die ausgewählten " + selectedProviders.size() + " Signal Provider wirklich löschen?\n" +
            "Diese Aktion kann nicht rückgängig gemacht werden.",
            "Signal Provider löschen",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        
        MqlAnalyserConf config = new MqlAnalyserConf(rootPath);
        String downloadPath = config.getDownloadPath();
        
        boolean anyDeleted = false;
        List<String> deletedProvidersList = new ArrayList<>();
        
        // Speichern der Namen der zu löschenden Provider, um sie später aus dem Model zu entfernen
        List<String> providersToRemove = new ArrayList<>();
        
        for (String providerName : selectedProviders) {
            boolean providerDeleted = false;
            
            LOGGER.info("Versuche Provider zu löschen: " + providerName);
            
            // Lösche die CSV-Datei
            File csvFile = new File(downloadPath, providerName);
            if (csvFile.exists()) {
                if (csvFile.delete()) {
                    providerDeleted = true;
                    anyDeleted = true;
                    LOGGER.info("CSV-Datei gelöscht: " + csvFile.getAbsolutePath());
                } else {
                    LOGGER.warning("Konnte CSV-Datei nicht löschen: " + csvFile.getAbsolutePath());
                }
            } else {
                LOGGER.warning("CSV-Datei existiert nicht: " + csvFile.getAbsolutePath());
            }
            
            // Lösche die TXT-Datei
            String txtFileName = providerName.replace(".csv", "") + "_root.txt";
            File txtFile = new File(downloadPath, txtFileName);
            if (txtFile.exists()) {
                if (txtFile.delete()) {
                    providerDeleted = true;
                    anyDeleted = true;
                    LOGGER.info("TXT-Datei gelöscht: " + txtFile.getAbsolutePath());
                } else {
                    LOGGER.warning("Konnte TXT-Datei nicht löschen: " + txtFile.getAbsolutePath());
                }
            } else {
                LOGGER.warning("TXT-Datei existiert nicht: " + txtFile.getAbsolutePath());
            }
            
            // Lösche die HTML-Datei - mit dem korrekten "_root.html" Suffix
            String baseProviderName = providerName.replace(".csv", "");
            String htmlFileName = baseProviderName + "_root.html";
            File htmlFile = new File(downloadPath, htmlFileName);
            
            if (htmlFile.exists()) {
                if (htmlFile.delete()) {
                    providerDeleted = true;
                    anyDeleted = true;
                    LOGGER.info("HTML-Datei gelöscht: " + htmlFile.getAbsolutePath());
                } else {
                    LOGGER.warning("Konnte HTML-Datei nicht löschen: " + htmlFile.getAbsolutePath());
                }
            } else {
                LOGGER.warning("HTML-Datei mit _root.html existiert nicht: " + htmlFile.getAbsolutePath());
                
                // Alternative HTML-Dateiformate versuchen
                String[] alternativeSuffixes = {".html", "_index.html", "_history.html"};
                for (String suffix : alternativeSuffixes) {
                    String altHtmlFileName = baseProviderName + suffix;
                    File altHtmlFile = new File(downloadPath, altHtmlFileName);
                    
                    if (altHtmlFile.exists()) {
                        if (altHtmlFile.delete()) {
                            providerDeleted = true;
                            anyDeleted = true;
                            LOGGER.info("Alternative HTML-Datei gelöscht: " + altHtmlFile.getAbsolutePath());
                        } else {
                            LOGGER.warning("Konnte alternative HTML-Datei nicht löschen: " + altHtmlFile.getAbsolutePath());
                        }
                    }
                }
            }
            
            if (providerDeleted) {
                deletedProvidersList.add(providerName);
                providersToRemove.add(providerName);
            }
        }
        
        if (anyDeleted) {
            // Entferne die Provider aus dem DataManager-Cache
            Map<String, ProviderStats> stats = dataManager.getStats();
            for (String provider : providersToRemove) {
                stats.remove(provider);
            }
            
            // Cache im Renderer leeren
            if (renderer != null) {
                renderer.clearCache();
            }
            
            // Tabelle vollständig neu laden
            SwingUtilities.invokeLater(() -> {
                // Das Model komplett neu befüllen
                model.populateData(dataManager.getStats());
                
                // Neu filtern, falls ein Filter aktiv ist
                if (filterManager.getCurrentFilter() != null) {
                    filterManager.applyFilter(filterManager.getCurrentFilter());
                }
                
                // Die Tabelle aktualisieren
                updateUI();
                repaint();
                
                // Statusmeldung aktualisieren
                if (statusUpdateCallback != null) {
                    statusUpdateCallback.accept(getStatusText());
                }
            });
            
            // Erstelle die Nachricht mit Begrenzung auf 20 Provider
            StringBuilder message = new StringBuilder("Gelöschte Provider:\n");
            int maxProvidersToShow = Math.min(20, deletedProvidersList.size());
            
            for (int i = 0; i < maxProvidersToShow; i++) {
                message.append("- ").append(deletedProvidersList.get(i)).append("\n");
            }
            
            // Falls mehr als 20 Provider gelöscht wurden, zeige einen Hinweis
            if (deletedProvidersList.size() > 20) {
                int remaining = deletedProvidersList.size() - 20;
                message.append("\n... und ").append(remaining).append(" weitere");
            }
            
            // Info-Dialog mit gelöschten Providern anzeigen
            JOptionPane.showMessageDialog(
                this,
                message.toString(),
                "Provider gelöscht",
                JOptionPane.INFORMATION_MESSAGE
            );
        } else {
            JOptionPane.showMessageDialog(
                this,
                "Es konnten keine Provider gelöscht werden.",
                "Keine Änderungen",
                JOptionPane.INFORMATION_MESSAGE
            );
        }
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
        
        // Erstelle einen NumberFormatRenderer, der den HighlightRenderer verwendet
        NumberFormatRenderer numberRenderer = new NumberFormatRenderer(renderer);
        
        // Setze die Renderer für die Spalten
        for (int i = 0; i < getColumnCount(); i++) {
            // Risk Score Spalte (Spalte 20) verwendet den speziellen RiskScoreRenderer
            if (i == 20) {
                getColumnModel().getColumn(i).setCellRenderer(riskRenderer);
            }
            // Spalte 0 (No) und 1 (Signal Provider) verwenden den Standard-Renderer
            else if (i == 0 || i == 1) {
                getColumnModel().getColumn(i).setCellRenderer(renderer);
            }
            // Alle anderen Spalten sind numerisch und verwenden den NumberFormatRenderer
            else {
                getColumnModel().getColumn(i).setCellRenderer(numberRenderer);
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
    public JButton createReportButton() {
        JButton reportButton = new JButton("Favoriten-Report erstellen");
        reportButton.addActionListener(e -> {
            // Dialoge und Fortschrittsanzeigen hier in einem separaten Thread, um die UI nicht zu blockieren
            new Thread(() -> {
                try {
                    // Alle Favoriten ermitteln
                    FavoritesManager favManager = new FavoritesManager(rootPath);
                    Map<String, ProviderStats> favorites = new HashMap<>();
                    
                    // Über alle verfügbaren Provider iterieren
                    for (Map.Entry<String, ProviderStats> entry : dataManager.getStats().entrySet()) {
                        String providerName = entry.getKey();
                        ProviderStats stats = entry.getValue();
                        
                        // Provider-ID extrahieren
                        String providerId = "";
                        if (providerName.contains("_")) {
                            providerId = providerName.substring(providerName.lastIndexOf("_") + 1).replace(".csv", "");
                        } else {
                            // Fallback für unerwartetes Format
                            StringBuilder digits = new StringBuilder();
                            for (char ch : providerName.toCharArray()) {
                                if (Character.isDigit(ch)) {
                                    digits.append(ch);
                                }
                            }
                            if (digits.length() > 0) {
                                providerId = digits.toString();
                            }
                        }
                        
                        // Prüfen, ob dieser Provider ein Favorit ist
                        if (!providerId.isEmpty() && favManager.isFavorite(providerId)) {
                            favorites.put(providerName, stats);
                        }
                    }
                    
                    // Prüfen, ob Favoriten gefunden wurden
                    if (favorites.isEmpty()) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(MainTable.this,
                                    "Es wurden keine Favoriten gefunden.\nBitte markieren Sie zuerst einige Signal Provider als Favoriten.",
                                    "Keine Favoriten",
                                    JOptionPane.INFORMATION_MESSAGE);
                        });
                        return;
                    }
                    
                    // Fortschrittsanzeige während der Report-Generierung
                    final int totalProviders = favorites.size();
                    SwingUtilities.invokeLater(() -> {
                        statusUpdateCallback.accept("Generiere Report für " + totalProviders + " Favoriten...");
                    });
                    
                    // Report-Generator initialisieren und Report erstellen
                    ReportGenerator reportGenerator = new ReportGenerator(rootPath, htmlDatabase);
                    String reportPath = reportGenerator.generateReport(favorites);
                    
                    if (reportPath != null) {
                        final String finalReportPath = reportPath;
                        SwingUtilities.invokeLater(() -> {
                            // Statusmeldung aktualisieren
                            statusUpdateCallback.accept("Report erfolgreich erstellt: " + finalReportPath);
                            
                            // Erfolgsmeldung anzeigen
                            int choice = JOptionPane.showConfirmDialog(MainTable.this,
                                    "Report wurde erfolgreich erstellt in:\n" + finalReportPath + "\n\nMöchten Sie den Report jetzt öffnen?",
                                    "Report erstellt",
                                    JOptionPane.YES_NO_OPTION,
                                    JOptionPane.INFORMATION_MESSAGE);
                            
                            // Bei Bedarf den Report im Standardbrowser öffnen
                            if (choice == JOptionPane.YES_OPTION) {
                                try {
                                    File htmlFile = new File(finalReportPath);
                                    Desktop.getDesktop().browse(htmlFile.toURI());
                                } catch (Exception ex) {
                                    JOptionPane.showMessageDialog(MainTable.this,
                                            "Fehler beim Öffnen des Reports: " + ex.getMessage(),
                                            "Fehler",
                                            JOptionPane.ERROR_MESSAGE);
                                }
                            }
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(MainTable.this,
                                    "Fehler beim Erstellen des Reports. Bitte prüfen Sie die Logs.",
                                    "Fehler",
                                    JOptionPane.ERROR_MESSAGE);
                        });
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(MainTable.this,
                                "Unerwarteter Fehler: " + ex.getMessage(),
                                "Fehler",
                                JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
        });
        
        return reportButton;
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
        MqlAnalyserConf config = new MqlAnalyserConf(ApplicationConstants.ROOT_PATH);
    
        status.append(" | Download Path: " + config.getDownloadPath());
       

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
        // Cache im Renderer leeren, damit Favoriten und Bad Provider korrekt angezeigt werden
        if (renderer != null) {
            renderer.clearCache();
        }
        
        filterManager.refreshFilteredData();
        
        // Prüfen, ob wöchentliche Speicherung erforderlich ist
        historyService.checkAndPerformWeeklySave();
        
        // Tabelle neu zeichnen nach Aktualisierung
        repaint();
        
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
        // Debug-Ausgabe hinzufügen, um das Problem zu identifizieren
        LOGGER.info("getCurrentProviderStats wird aufgerufen");
        
        // Versuche die gefilterten Daten vom FilterManager zu bekommen
        Map<String, ProviderStats> filteredStats = filterManager.getFilteredProviderStats();
        
        // Überprüfe, ob die gefilterten Daten gültig sind
        if (filteredStats == null || filteredStats.isEmpty()) {
            LOGGER.warning("Gefilterte Provider-Statistiken sind leer oder null");
            
            // Als Fallback verwenden wir alle verfügbaren Statistiken vom DataManager
            Map<String, ProviderStats> allStats = dataManager.getStats();
            
            LOGGER.info("Verwende alle verfügbaren Provider als Fallback: " + 
                       (allStats != null ? allStats.size() : "null") + " Provider gefunden");
            
            return allStats != null ? allStats : new HashMap<>();
        }
        
        LOGGER.info("Gefilterte Provider-Statistiken enthalten " + filteredStats.size() + " Provider");
        return filteredStats;
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
    /**
     * Filtert die Tabelle, um nur Favoriten einer bestimmten Kategorie anzuzeigen
     * @param category Die Kategorie (0-10), wobei 0 bedeutet, keine Filterung anwenden
     */
    public void filterByFavoriteCategory(int category) {
        favoritesManager.filterByCategory(category);
        updateStatus();
    }
    public void manageFavoriteCategory(String providerId, int currentCategory) {
        Object[] options = new Object[11];
        options[0] = "Kein Favorit";
        for (int i = 1; i <= 10; i++) {
            options[i] = "Kategorie " + i;
        }
        
        int selectedOption = JOptionPane.showOptionDialog(
            SwingUtilities.getWindowAncestor(this),
            "Bitte wählen Sie die Favoriten-Kategorie für Provider " + providerId,
            "Favoriten-Kategorie wählen",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[currentCategory]
        );
        
        if (selectedOption >= 0) {
            favoritesManager.getFavoritesManager().setFavoriteCategory(providerId, selectedOption);
            
            // Wenn die aktuelle Kategorie angezeigt wird, aktualisiere die Anzeige
            if (favoritesManager.getCurrentCategory() > 0) {
                favoritesManager.filterByCategory(favoritesManager.getCurrentCategory());
            }
            
            repaint();
        }
    }

    /**
     * Gibt den FavoritesFilterManager zurück
     * @return Der FavoritesFilterManager
     */
    public FavoritesFilterManager getFavoritesManager() {
        return favoritesManager;
    }

    /**
     * Gibt die Favoriten-Kategorie eines Signal Providers zurück
     * @param providerName Der Name des Signal Providers
     * @return Die Kategorie des Providers (0 = kein Favorit)
     */
    public int getFavoriteCategory(String providerName) {
        String providerId = extractProviderId(providerName);
        return favoritesManager.getFavoritesManager().getFavoriteCategory(providerId);
    }

    /**
     * Extrahiert die Provider-ID aus dem Providernamen
     * @param providerName Name des Providers (normalerweise ein Dateipfad)
     * @return Die extrahierte Provider-ID
     */
    private String extractProviderId(String providerName) {
        // Provider-ID aus dem Namen extrahieren
        String providerId = "";
        if (providerName.contains("_")) {
            providerId = providerName.substring(providerName.lastIndexOf("_") + 1).replace(".csv", "");
        } else {
            // Fallback für unerwartetes Format
            StringBuilder digits = new StringBuilder();
            for (char ch : providerName.toCharArray()) {
                if (Character.isDigit(ch)) {
                    digits.append(ch);
                }
            }
            if (digits.length() > 0) {
                providerId = digits.toString();
            }
        }
        return providerId;
    }
}