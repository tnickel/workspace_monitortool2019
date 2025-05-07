package ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import components.MainTable;
import models.FilterCriteria;
import services.ProviderHistoryService;
import ui.FilterDialog;
import utils.UIStyle;

/**
 * Verwaltet die Toolbar-Komponenten des Hauptfensters
 */
public class ToolbarManager {
    private final JFrame parentFrame;
    private final MainTable mainTable;
    private final JTextField searchField;
    private final int[] currentSearchIndex;
    private final ProviderHistoryService historyService;
    private final String rootPath;
    
    // UI-Komponenten
    private JPanel toolBar;
    private JPanel reportPanel; // Panel für den Report-Button
    
    /**
     * Konstruktor für den ToolbarManager
     */
    public ToolbarManager(JFrame parentFrame, MainTable mainTable, JTextField searchField, 
            int[] currentSearchIndex, ProviderHistoryService historyService, String rootPath) {
        this.parentFrame = parentFrame;
        this.mainTable = mainTable;
        this.searchField = searchField;
        this.currentSearchIndex = currentSearchIndex;
        this.historyService = historyService;
        this.rootPath = rootPath;
        
        createToolbar();
        createReportPanel(); // Neues Report-Panel erstellen
    }
    
    /**
     * Erstellt die Toolbar mit den Filter-Buttons
     */
    private void createToolbar() {
        toolBar = new JPanel(new BorderLayout(10, 5));
        toolBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        //toolBar.setBackground(UIStyle.BACKGROUND_COLOR);
        
        // Filter-Buttons in einem Panel links
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        filterPanel.setOpaque(false);
        
        // Verschiedene Buttons erstellen...
        JButton filterButton = UIStyle.createStyledButton("Filter");
        filterButton.addActionListener(e -> showFilterDialog());
        
        JButton resetButton = UIStyle.createStyledButton("Reset Filter");
        resetButton.addActionListener(e -> resetFilter());
        
        JButton favoritesButton = UIStyle.createStyledButton("Nur Favoriten");
        favoritesButton.addActionListener(e -> filterFavorites());
        
        JButton showProvidersButton = mainTable.createShowSignalProviderButton();
        //UIStyle.applyButtonStyle(showProvidersButton);
        
        // Buttons zum Panel hinzufügen
        filterPanel.add(filterButton);
        filterPanel.add(resetButton);
        filterPanel.add(favoritesButton);
        filterPanel.add(showProvidersButton);
        
        // Suchfeld-Panel auf der rechten Seite
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        searchPanel.setOpaque(false);
        
        JButton searchButton = UIStyle.createStyledButton("Suchen");
        searchButton.addActionListener(e -> performSearch());
        
        JButton clearButton = UIStyle.createStyledButton("Löschen");
        clearButton.addActionListener(e -> clearSearch());
        
        // Komponenten dem Suchpanel hinzufügen
        searchPanel.add(UIStyle.createStyledLabel("Suche:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(clearButton);
        
        // Panels zur Toolbar hinzufügen
        toolBar.add(filterPanel, BorderLayout.WEST);
        toolBar.add(searchPanel, BorderLayout.EAST);
    }
    
    /**
     * Erstellt das gelb markierte Panel mit dem Report-Button
     */
    private void createReportPanel() {
        reportPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        reportPanel.setBackground(new Color(255, 255, 210)); // Hellgelber Hintergrund für Hervorhebung
        reportPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Report-Button erstellen
        JButton reportButton = mainTable.createReportButton();
        reportButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        reportButton.setToolTipText("Erstellt einen HTML-Report für alle als Favoriten markierten Signal Provider");
        
        // Button zum Panel hinzufügen
        reportPanel.add(reportButton);
    }
    
    // Verschiedene Hilfsmethoden für Button-Aktionen
    
    private void showFilterDialog() {
        FilterDialog filterDialog = new FilterDialog(parentFrame, mainTable.getCurrentFilter());
        FilterCriteria criteria = filterDialog.showDialog(); // Diese Methode sollte den Filter zurückgeben oder null
        if (criteria != null) {
            mainTable.applyFilter(criteria);
        }
    }
    
    private void resetFilter() {
        mainTable.resetFilter();
    }
    
    private void filterFavorites() {
        mainTable.filterFavorites();
    }
    
    private void performSearch() {
        String searchText = searchField.getText().toLowerCase().trim();
        if (!searchText.isEmpty()) {
            mainTable.highlightSearchText(searchText);
            mainTable.findAndSelectNext(searchText, currentSearchIndex);
        }
    }
    
    private void clearSearch() {
        searchField.setText("");
        mainTable.clearHighlight();
        currentSearchIndex[0] = -1;
    }
    
    /**
     * Gibt die Toolbar zurück
     */
    public JPanel getToolBar() {
        return toolBar;
    }
    
    /**
     * Gibt das Report-Panel zurück
     */
    public JPanel getReportPanel() {
        return reportPanel;
    }
}