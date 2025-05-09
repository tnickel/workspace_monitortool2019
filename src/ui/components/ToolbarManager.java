package ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import components.MainTable;
import data.ProviderStats;
import models.FilterCriteria;
import services.ProviderHistoryService;
import ui.FilterDialog;
import utils.HtmlDatabase;

/**
 * Manager für die Toolbar und Reporting-Funktionen
 */
public class ToolbarManager {
    private static final Logger LOGGER = Logger.getLogger(ToolbarManager.class.getName());
    
    private final JFrame parentFrame;
    private final MainTable mainTable;
    private final JTextField searchField;
    private final int[] currentSearchIndex;
    private final ProviderHistoryService historyService;
    private final String rootPath;
    
    private final JToolBar toolBar;
    private final JPanel reportPanel;
    private JComboBox<String> categoryComboBox;
    
    /**
     * Konstruktor für den ToolbarManager
     */
    public ToolbarManager(JFrame parentFrame, MainTable mainTable, JTextField searchField, 
                         int[] currentSearchIndex, ProviderHistoryService historyService, 
                         String rootPath) {
        this.parentFrame = parentFrame;
        this.mainTable = mainTable;
        this.searchField = searchField;
        this.currentSearchIndex = currentSearchIndex;
        this.historyService = historyService;
        this.rootPath = rootPath;
        
        // UI-Komponenten erstellen
        this.toolBar = createToolBar();
        this.reportPanel = createReportPanel();
    }
    
    /**
     * Erstellt die Toolbar mit allen Buttons
     */
    private JToolBar createToolBar() {
        // Anpassen des Aussehens der Toolbar selbst
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBorderPainted(false);
        toolbar.setOpaque(true);
        toolbar.setBackground(AppUIStyle.PRIMARY_COLOR);
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        
        // Filter-Button
        JButton filterButton = AppUIStyle.createStyledButton("Filter");
        filterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showFilterDialog();
            }
        });
        toolbar.add(filterButton);
        
        // Reset-Filter-Button
        JButton resetFilterButton = AppUIStyle.createStyledButton("Reset Filter");
        resetFilterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetFilter();
            }
        });
        toolbar.add(resetFilterButton);
        
        // Separator
        toolbar.add(Box.createHorizontalStrut(5));
        
        // Nur Favoriten Button
        JButton favoritesButton = AppUIStyle.createStyledButton("Nur Favoriten");
        favoritesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainTable.filterFavorites();
                // Setze die Kategorie-Auswahl auf 1
                if (categoryComboBox != null) {
                    categoryComboBox.setSelectedIndex(1);
                }
            }
        });
        toolbar.add(favoritesButton);
        
        // Show Signal Providers Button
        JButton showProvidersButton = AppUIStyle.createStyledButton("Show Signal Providers");
        showProvidersButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Diese Funktion ist bereits im MainTable implementiert
                mainTable.createShowSignalProviderButton().doClick();
            }
        });
        toolbar.add(showProvidersButton);
        
        // Abstand hinzufügen
        toolbar.add(Box.createHorizontalGlue());
        
        // Kategorie-Selektor hinzufügen
        JPanel categoryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        categoryPanel.setOpaque(false);
        
        JLabel categoryLabel = new JLabel("Favoriten-Kategorie:");
        categoryLabel.setForeground(Color.WHITE);
        categoryLabel.setFont(AppUIStyle.BOLD_FONT);
        categoryPanel.add(categoryLabel);
        
        // Kategorien als Array
        String[] categories = new String[11];
        categories[0] = "Alle anzeigen";
        for (int i = 1; i <= 10; i++) {
            categories[i] = "Favoriten " + i;
        }
        
        categoryComboBox = new JComboBox<>(categories);
        categoryComboBox.setPreferredSize(new Dimension(150, 25));
        
        // Stil für ComboBox
        categoryComboBox.setBackground(AppUIStyle.TEXT_FIELD_BG_COLOR);
        categoryComboBox.setForeground(AppUIStyle.TEXT_COLOR);
        categoryComboBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppUIStyle.SECONDARY_COLOR, 1),
            BorderFactory.createEmptyBorder(2, 5, 2, 5)
        ));
        categoryComboBox.setOpaque(true);
        categoryComboBox.setFont(AppUIStyle.REGULAR_FONT);
        
        // Sicherstellen, dass der Text gut lesbar ist
        categoryComboBox.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, 
                    int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                
                if (isSelected) {
                    c.setBackground(AppUIStyle.SECONDARY_COLOR);
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(AppUIStyle.TEXT_FIELD_BG_COLOR);
                    c.setForeground(AppUIStyle.TEXT_COLOR);
                }
                return c;
            }
        });
        
        categoryComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedCategory = categoryComboBox.getSelectedIndex();
                LOGGER.info("Favoriten-Kategorie ausgewählt: " + selectedCategory);
                mainTable.filterByFavoriteCategory(selectedCategory);
            }
        });
        
        categoryPanel.add(categoryComboBox);
        toolbar.add(categoryPanel);
        
        // Abstand hinzufügen
        toolbar.add(Box.createHorizontalGlue());
        
        // Suchfeld und Buttons
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchPanel.setOpaque(false);
        
        JLabel searchLabel = new JLabel("Suche:");
        searchLabel.setForeground(Color.WHITE);
        searchLabel.setFont(AppUIStyle.BOLD_FONT);
        searchPanel.add(searchLabel);
        
        // Suchfeld Stil
        searchField.setBackground(AppUIStyle.TEXT_FIELD_BG_COLOR);
        searchField.setForeground(AppUIStyle.TEXT_COLOR);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppUIStyle.SECONDARY_COLOR, 1),
            BorderFactory.createEmptyBorder(2, 5, 2, 5)
        ));
        searchField.setOpaque(true);
        searchField.setFont(AppUIStyle.REGULAR_FONT);
        searchPanel.add(searchField);
        
        JButton searchButton = AppUIStyle.createStyledButton("Suchen");
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                search();
            }
        });
        searchPanel.add(searchButton);
        
        JButton clearButton = AppUIStyle.createStyledButton("Löschen");
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearSearch();
            }
        });
        searchPanel.add(clearButton);
        
        // Enter-Taste im Suchfeld
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    search();
                }
            }
        });
        
        toolbar.add(searchPanel);
        
        return toolbar;
    }
    
    /**
     * Erstellt das Panel für die Report-Buttons
     */
    private JPanel createReportPanel() {
        // Panel für den Report-Button mit blauer Hervorhebung statt gelb
        JPanel panel = AppUIStyle.createReportPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 5));
        
        // Favoriten-Report-Button
        JButton favoritesReportButton = mainTable.createReportButton();
        // Stil anpassen
        favoritesReportButton.setBackground(AppUIStyle.SECONDARY_COLOR);
        favoritesReportButton.setForeground(Color.WHITE);
        favoritesReportButton.setText("Favoriten-Report erstellen");
        favoritesReportButton.setPreferredSize(new Dimension(200, 30));
        favoritesReportButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppUIStyle.PRIMARY_COLOR),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        favoritesReportButton.setOpaque(true);
        favoritesReportButton.setFocusPainted(false);
        panel.add(favoritesReportButton);
        
        // Kategorie-Report-Button
        JButton categoryReportButton = AppUIStyle.createStyledButton("Kategorie-Report erstellen");
        categoryReportButton.setPreferredSize(new Dimension(200, 30));
        categoryReportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                generateCategoryReport();
            }
        });
        panel.add(categoryReportButton);
        
        // Report für alle angezeigten Provider
        JButton customReportButton = AppUIStyle.createStyledButton("Report für angezeigte Provider");
        customReportButton.setPreferredSize(new Dimension(200, 30));
        customReportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                generateCustomReport();
            }
        });
        panel.add(customReportButton);
        
        return panel;
    }
    
    /**
     * Generiert einen Report für die ausgewählte Kategorie
     */
    private void generateCategoryReport() {
        int category = categoryComboBox.getSelectedIndex();
        
        if (category <= 0) {
            JOptionPane.showMessageDialog(
                parentFrame,
                "Bitte wählen Sie eine Favoriten-Kategorie (1-10) aus.",
                "Keine Kategorie ausgewählt",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        
        try {
            // Report erstellen
            HtmlDatabase htmlDatabase = mainTable.getHtmlDatabase();
            reports.ReportGenerator reportGenerator = 
                new reports.ReportGenerator(rootPath, htmlDatabase);
            
            String reportPath = reportGenerator.generateReport(
                mainTable.getCurrentProviderStats(), 
                category
            );
            
            if (reportPath != null) {
                JOptionPane.showMessageDialog(
                    parentFrame,
                    "Kategorie-Report wurde erfolgreich erstellt:\n" + reportPath,
                    "Report erstellt",
                    JOptionPane.INFORMATION_MESSAGE
                );
                
                // Fragen, ob der Report geöffnet werden soll
                int result = JOptionPane.showConfirmDialog(
                    parentFrame,
                    "Möchten Sie den Report jetzt öffnen?",
                    "Report öffnen",
                    JOptionPane.YES_NO_OPTION
                );
                
                if (result == JOptionPane.YES_OPTION) {
                    try {
                        java.awt.Desktop.getDesktop().browse(
                            new File(reportPath).toURI()
                        );
                    } catch (Exception ex) {
                        LOGGER.warning("Fehler beim Öffnen des Reports: " + ex.getMessage());
                        JOptionPane.showMessageDialog(
                            parentFrame,
                            "Der Report konnte nicht automatisch geöffnet werden.",
                            "Fehler",
                            JOptionPane.WARNING_MESSAGE
                        );
                    }
                }
            } else {
                JOptionPane.showMessageDialog(
                    parentFrame,
                    "Es konnten keine Provider in der ausgewählten Kategorie gefunden werden.",
                    "Keine Provider",
                    JOptionPane.WARNING_MESSAGE
                );
            }
        } catch (Exception e) {
            LOGGER.severe("Fehler beim Erstellen des Kategorie-Reports: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                parentFrame,
                "Fehler beim Erstellen des Reports: " + e.getMessage(),
                "Fehler",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
    
    /**
     * Erstellt einen benutzerdefinierten Report für alle angezeigten Provider
     */
    private void generateCustomReport() {
        try {
            // Aktuell angezeigte Provider holen
            Map<String, ProviderStats> currentStats = mainTable.getCurrentProviderStats();
            
            if (currentStats.isEmpty()) {
                JOptionPane.showMessageDialog(
                    parentFrame,
                    "Es sind keine Provider angezeigt, für die ein Report erstellt werden könnte.",
                    "Keine Provider",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            
            // Titel für den Report abfragen
            String reportTitle = JOptionPane.showInputDialog(
                parentFrame,
                "Bitte geben Sie einen Titel für den Report ein:",
                "Signal Provider Report - " + java.time.LocalDate.now().toString()
            );
            
            if (reportTitle == null || reportTitle.trim().isEmpty()) {
                return; // Abbruch
            }
            
            // Ausgabepfad erstellen
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            String defaultPath = rootPath + File.separator + "report" + File.separator +
                                "custom_report_" + timestamp + ".html";
            
            // Datei-Dialog für den Speicherort anzeigen
            javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
            fileChooser.setDialogTitle("Report speichern unter");
            fileChooser.setSelectedFile(new File(defaultPath));
            fileChooser.setFileSelectionMode(javax.swing.JFileChooser.FILES_ONLY);
            
            if (fileChooser.showSaveDialog(parentFrame) != javax.swing.JFileChooser.APPROVE_OPTION) {
                return; // Abbruch
            }
            
            File selectedFile = fileChooser.getSelectedFile();
            String outputPath = selectedFile.getAbsolutePath();
            if (!outputPath.toLowerCase().endsWith(".html")) {
                outputPath += ".html";
            }
            
            // Report erstellen
            HtmlDatabase htmlDatabase = mainTable.getHtmlDatabase();
            reports.ReportGenerator reportGenerator = 
                new reports.ReportGenerator(rootPath, htmlDatabase);
            
            String reportPath = reportGenerator.generateReport(
                currentStats, 
                reportTitle,
                outputPath
            );
            
            if (reportPath != null) {
                JOptionPane.showMessageDialog(
                    parentFrame,
                    "Der Report wurde erfolgreich erstellt:\n" + reportPath,
                    "Report erstellt",
                    JOptionPane.INFORMATION_MESSAGE
                );
                
                // Fragen, ob der Report geöffnet werden soll
                int result = JOptionPane.showConfirmDialog(
                    parentFrame,
                    "Möchten Sie den Report jetzt öffnen?",
                    "Report öffnen",
                    JOptionPane.YES_NO_OPTION
                );
                
                if (result == JOptionPane.YES_OPTION) {
                    try {
                        java.awt.Desktop.getDesktop().browse(
                            new File(reportPath).toURI()
                        );
                    } catch (Exception e) {
                        LOGGER.warning("Fehler beim Öffnen des Reports: " + e.getMessage());
                        JOptionPane.showMessageDialog(
                            parentFrame,
                            "Der Report konnte nicht automatisch geöffnet werden.",
                            "Fehler",
                            JOptionPane.WARNING_MESSAGE
                        );
                    }
                }
            } else {
                JOptionPane.showMessageDialog(
                    parentFrame,
                    "Fehler beim Erstellen des Reports.",
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        } catch (Exception e) {
            LOGGER.severe("Fehler beim Erstellen des benutzerdefinierten Reports: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                parentFrame,
                "Fehler beim Erstellen des Reports: " + e.getMessage(),
                "Fehler",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
    
    /**
     * Zeigt den FilterDialog an
     */
    private void showFilterDialog() {
        // Die aktuelle FilterCriteria vom MainTable abrufen
        FilterCriteria currentFilter = mainTable.getCurrentFilter();
        
        // FilterDialog mit der aktuellen FilterCriteria erstellen
        FilterDialog dialog = new FilterDialog(parentFrame, currentFilter);
        
        // Dialog anzeigen und neuen Filter bekommen
        FilterCriteria newFilter = dialog.showDialog();
        
        // Neuen Filter anwenden, wenn er sich geändert hat
        if (newFilter != null && newFilter != currentFilter) {
            mainTable.applyFilter(newFilter);
        }
    }
    
    /**
     * Setzt alle Filter zurück
     */
    private void resetFilter() {
        // Wenn der MainFrame bekannt ist, rufen wir seine resetAll-Methode auf
        try {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(mainTable);
            if (frame != null && frame.getClass().getSimpleName().equals("MainFrame")) {
                java.lang.reflect.Method resetAllMethod = frame.getClass().getMethod("resetAll");
                resetAllMethod.invoke(frame);
                
                // Kategorie-Selektor zurücksetzen
                categoryComboBox.setSelectedIndex(0);
            }
        } catch (Exception e) {
            LOGGER.warning("Fehler beim Zurücksetzen des Filters: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback: Nur die Tabelle zurücksetzen
            mainTable.resetFilter();
            categoryComboBox.setSelectedIndex(0);
        }
    }
    
    /**
     * Sucht nach dem eingegebenen Text
     */
    private void search() {
        String searchText = searchField.getText().toLowerCase();
        if (searchText.isEmpty()) {
            clearSearch();
            return;
        }
        
        mainTable.highlightSearchText(searchText);
        currentSearchIndex[0] = -1;  // Setze den Index zurück, damit von Anfang an gesucht wird
        
        boolean found = mainTable.findAndSelectNext(searchText, currentSearchIndex);
        if (!found && currentSearchIndex[0] >= 0) {
            // Wenn am Ende angekommen, von vorne beginnen
            currentSearchIndex[0] = -1;
            found = mainTable.findAndSelectNext(searchText, currentSearchIndex);
        }
    }
    
    /**
     * Leert das Suchfeld und hebt die Hervorhebung auf
     */
    private void clearSearch() {
        searchField.setText("");
        mainTable.clearHighlight();
        currentSearchIndex[0] = -1;
    }
    
    /**
     * Gibt die Toolbar zurück
     */
    public JToolBar getToolBar() {
        return toolBar;
    }
    
    /**
     * Gibt das Report-Panel zurück
     */
    public JPanel getReportPanel() {
        return reportPanel;
    }
    
    /**
     * Setzt die Auswahl im Kategorie-Selektor
     */
    public void setCategorySelection(int category) {
        if (categoryComboBox != null && category >= 0 && category <= 10) {
            categoryComboBox.setSelectedIndex(category);
        }
    }
    
    /**
     * Gibt die aktuell ausgewählte Kategorie zurück
     */
    public int getSelectedCategory() {
        return categoryComboBox != null ? categoryComboBox.getSelectedIndex() : 0;
    }
}