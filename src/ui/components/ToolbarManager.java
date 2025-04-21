package ui.components;

import java.awt.Color;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import components.FavoritesFilterManager;
import components.MainTable;
import models.FilterCriteria;
import services.ProviderHistoryService;
import ui.DatabaseViewerDialog;
import ui.FilterDialog;
import ui.ForceDbSaveDialog;
import ui.MainFrame;
import ui.RiskScoreExplanationDialog;
import utils.UIStyle;

public class ToolbarManager {
    private final MainFrame parentFrame;
    private final MainTable mainTable;
    private final JTextField searchField;
    private final JToolBar toolBar;
    private final int[] currentSearchIndex;
    private final ProviderHistoryService historyService;
    private final String rootPath;

    public ToolbarManager(MainFrame parentFrame, MainTable mainTable, JTextField searchField, 
                         int[] searchIndex, ProviderHistoryService historyService, String rootPath) {
        this.parentFrame = parentFrame;
        this.mainTable = mainTable;
        this.searchField = searchField;
        this.currentSearchIndex = searchIndex;
        this.historyService = historyService;
        this.rootPath = rootPath;
        
        this.toolBar = new JToolBar();
        
        createToolBar();
    }
    
    public JToolBar getToolBar() {
        return toolBar;
    }
    
    private void createToolBar() {
        toolBar.setFloatable(false);
        toolBar.setBackground(UIStyle.PRIMARY_COLOR);
        toolBar.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setOpaque(false);
        
        JLabel searchLabel = new JLabel("Search: ");
        searchLabel.setForeground(Color.WHITE);
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        
        JButton searchButton = UIStyle.createStyledButton("Search");
        searchButton.addActionListener(e -> performSearch());
        searchPanel.add(searchButton);
        
        // Neuer Delete Selected Button
        JButton deleteSelectedButton = UIStyle.createStyledButton("Delete Selected");
        deleteSelectedButton.addActionListener(e -> parentFrame.deleteSelectedProviders());
        searchPanel.add(deleteSelectedButton);
        
        toolBar.add(searchPanel);
        toolBar.addSeparator();
        
        JButton resetButton = UIStyle.createStyledButton("Reset");
        resetButton.addActionListener(e -> parentFrame.resetAll());
        toolBar.add(resetButton);
        
        JButton filterButton = UIStyle.createStyledButton("Filter");
        filterButton.addActionListener(e -> showFilterDialog());
        toolBar.add(filterButton);
        
        JButton showFavoritesButton = UIStyle.createStyledButton("Show Favorites");
        showFavoritesButton.addActionListener(e -> {
            mainTable.filterFavorites();
        });
        toolBar.add(showFavoritesButton);
        
        // "Show Bad Providers" Button wurde entfernt
        
        JButton riskScoreButton = UIStyle.createStyledButton("Risk Score Explanation");
        riskScoreButton.addActionListener(e -> {
            RiskScoreExplanationDialog dialog = new RiskScoreExplanationDialog(parentFrame);
            dialog.setVisible(true);
        });
        toolBar.add(riskScoreButton);
       
        JButton dbViewerButton = UIStyle.createStyledButton("DB Einträge");
        dbViewerButton.addActionListener(e -> {
            DatabaseViewerDialog dialog = new DatabaseViewerDialog(parentFrame, historyService);
            dialog.setVisible(true);
        });
        toolBar.add(dbViewerButton);
        
        JButton dbForceSaveButton = UIStyle.createStyledButton("DB Speicherung erzwingen");
        dbForceSaveButton.addActionListener(e -> {
            // Prüfen, ob bereits Einträge in der DB vorhanden sind
            if (!historyService.hasDatabaseEntries()) {
                int answer = JOptionPane.showConfirmDialog(
                    parentFrame,
                    "Es wurden keine Einträge in der Datenbank gefunden. Möchten Sie eine initiale Speicherung für alle Provider erzwingen?",
                    "Datenbank leer",
                    JOptionPane.YES_NO_OPTION
                );
                
                if (answer == JOptionPane.YES_OPTION) {
                    // Initiale Speicherung im Hintergrund ausführen
                    new javax.swing.SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            historyService.forceInitialSave();
                            return null;
                        }
                        
                        @Override
                        protected void done() {
                            JOptionPane.showMessageDialog(
                                parentFrame,
                                "Initiale Speicherung abgeschlossen. Die Datenbank enthält nun Einträge für alle Provider.",
                                "Speicherung abgeschlossen",
                                JOptionPane.INFORMATION_MESSAGE
                            );
                        }
                    }.execute();
                }
            } else {
                // Detaillierten Dialog zum Erzwingen der Speicherung öffnen
                ForceDbSaveDialog dialog = new ForceDbSaveDialog(
                    parentFrame,
                    historyService,
                    mainTable.getCurrentProviderStats(),
                    rootPath
                );
                dialog.setVisible(true);
            }
        });
        toolBar.add(dbForceSaveButton);
    }
    
    private void showFilterDialog() {
        FilterDialog dialog = new FilterDialog(parentFrame, mainTable.getCurrentFilter());
        FilterCriteria criteria = dialog.showDialog();
        if (criteria != null) {
            mainTable.applyFilter(criteria);
        }
    }
    
    public void performSearch() {
        String searchText = searchField.getText().toLowerCase().trim();
        if (searchText.isEmpty()) {
            mainTable.clearHighlight();
            currentSearchIndex[0] = -1;
            return;
        }
        
        mainTable.highlightSearchText(searchText);
        
        if (!mainTable.findAndSelectNext(searchText, currentSearchIndex)) {
            currentSearchIndex[0] = -1;
            if (!mainTable.findAndSelectNext(searchText, currentSearchIndex)) {
                JOptionPane.showMessageDialog(parentFrame,
                    "No matches found for: " + searchText,
                    "Search Result",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
}