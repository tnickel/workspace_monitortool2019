package ui;



import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;

import components.MainTable;
import data.DataManager;
import models.FilterCriteria;
import services.ProviderHistoryService;
import utils.MqlAnalyserConf;


public class MainFrame extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(MainFrame.class.getName());
    private final MainTable mainTable;
    private final JLabel statusLabel;
    private final JTextField searchField;
    private final DataManager dataManager;
    private final MqlAnalyserConf config;
    private final ProviderHistoryService historyService;
    private int[] currentSearchIndex = {-1};
    private String rootPath_glob = null;
    
    // Definierte Farben für das neue Design
    private static final Color PRIMARY_COLOR = new Color(26, 45, 90); // #1A2D5A - Dunkelblau
    private static final Color SECONDARY_COLOR = new Color(62, 125, 204); // #3E7DCC - Helleres Blau
    private static final Color ACCENT_COLOR = new Color(255, 209, 102); // #FFD166 - Gold/Gelb
    private static final Color BG_COLOR = new Color(245, 247, 250); // #F5F7FA - Sehr helles Grau
    private static final Color TEXT_COLOR = new Color(51, 51, 51); // #333333 - Dunkelgrau
    private static final Color TEXT_SECONDARY_COLOR = new Color(85, 85, 85); // #555555 - Helleres Grau

    public MainFrame(DataManager dataManager, String rootPath, MqlAnalyserConf config) {
        super("Signal Providers Performance Analysis");
        this.dataManager = dataManager;
        this.config = config;
        this.statusLabel = createStyledLabel("");
        this.searchField = createStyledTextField(20);
        rootPath_glob = rootPath;
        
        // Provider History Service initialisieren
        this.historyService = ProviderHistoryService.getInstance();
        this.historyService.initialize(rootPath);
        
        // UI-Komponenten mit dem neuen Stil
        setUpUIDefaults();
        
        mainTable = new MainTable(dataManager, rootPath_glob);
        mainTable.setStatusUpdateCallback(text -> updateStatusBar());
        
        setupUI();
        setupSearch();
        setupStatusBar();
        
        // Hinzufügen eines WindowListeners für sauberes Herunterfahren
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                LOGGER.info("Fenster wird geschlossen, Ressourcen werden freigegeben...");
                historyService.shutdown();
            }
        });
    }
    
    private void setUpUIDefaults() {
        // Globale UI-Einstellungen
        UIManager.put("Panel.background", BG_COLOR);
        UIManager.put("OptionPane.background", BG_COLOR);
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("TextField.foreground", TEXT_COLOR);
        UIManager.put("TextField.caretForeground", PRIMARY_COLOR);
        UIManager.put("Button.background", SECONDARY_COLOR);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.font", new Font("SansSerif", Font.BOLD, 12));
        UIManager.put("Label.foreground", TEXT_COLOR);
        UIManager.put("MenuBar.background", PRIMARY_COLOR);
        UIManager.put("MenuBar.foreground", Color.WHITE);
        UIManager.put("Menu.background", PRIMARY_COLOR);
        UIManager.put("Menu.foreground", Color.WHITE);
        UIManager.put("Menu.selectionBackground", SECONDARY_COLOR);
        UIManager.put("Menu.selectionForeground", Color.WHITE);
        UIManager.put("MenuItem.background", BG_COLOR);
        UIManager.put("MenuItem.foreground", TEXT_COLOR);
        UIManager.put("MenuItem.selectionBackground", SECONDARY_COLOR);
        UIManager.put("MenuItem.selectionForeground", Color.WHITE);
        UIManager.put("Table.background", Color.WHITE);
        UIManager.put("Table.foreground", TEXT_COLOR);
        UIManager.put("Table.selectionBackground", SECONDARY_COLOR);
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("Table.gridColor", new Color(230, 230, 230));
        UIManager.put("ScrollPane.background", BG_COLOR);
        UIManager.put("ToolBar.background", PRIMARY_COLOR);
        UIManager.put("ToolBar.foreground", Color.WHITE);
    }
    
    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_COLOR);
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        return label;
    }
    
    private JTextField createStyledTextField(int columns) {
        JTextField textField = new JTextField(columns);
        textField.setBackground(Color.WHITE);
        textField.setForeground(TEXT_COLOR);
        textField.setBorder(new CompoundBorder(
            new LineBorder(SECONDARY_COLOR, 1),
            new EmptyBorder(4, 6, 4, 6)
        ));
        return textField;
    }

    private void updateStatusBar() {
        String providerCount = mainTable.getStatusText();
        String downloadPath = "Download Path: " + config.getDownloadPath();
        statusLabel.setText(providerCount + " | " + downloadPath);
    }

    private void setDownloadPath() {
        String currentPath = config.getDownloadPath();
        JFileChooser chooser = new JFileChooser(currentPath);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        if (chooser.showDialog(this, "Select Download Directory") == JFileChooser.APPROVE_OPTION) {
            String newPath = chooser.getSelectedFile().getAbsolutePath();
            config.setDownloadPath(newPath);
            rootPath_glob = newPath;
            
            // History Service mit neuem Pfad aktualisieren
            historyService.initialize(newPath);
            
            if (JOptionPane.showConfirmDialog(this, 
                "Download path updated. Reload data?", 
                "Reload", 
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                reloadData(newPath);
            }
        }
    }
  
    private void resetAll() {
        searchField.setText("");
        mainTable.clearHighlight();
        currentSearchIndex[0] = -1;
        mainTable.resetFilter();
        updateStatusBar();
    }
    
    private void setupUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setupMenuBar();
        
        // Hintergrund für das Hauptpanel
        JPanel contentPane = new GradientPanel();
        contentPane.setLayout(new BorderLayout(5, 5));
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        
        createToolBar();
        
        // Tabelle mit schönem Rahmen
        JScrollPane scrollPane = new JScrollPane(mainTable);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SECONDARY_COLOR, 1),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        contentPane.add(scrollPane, BorderLayout.CENTER);
        
        setSize(1200, 800);
        setLocationRelativeTo(null);
    }
    
    // Neues JPanel mit Farbverlauf für den Hintergrund
    private class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            
            // Heller Farbverlauf als Hintergrund
            GradientPaint gradient = new GradientPaint(
                0, 0, new Color(245, 247, 250), 
                0, getHeight(), new Color(230, 238, 245)
            );
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(PRIMARY_COLOR);
        menuBar.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        
        // Config-Menü
        JMenu configMenu = new JMenu("Config");
        configMenu.setForeground(Color.WHITE);
        
        JMenuItem pathMenuItem = new JMenuItem("Set Download Path");
        pathMenuItem.addActionListener(e -> setDownloadPath());
        
        JMenuItem columnVisibilityMenuItem = new JMenuItem("Tabellenspalten konfigurieren");
        columnVisibilityMenuItem.addActionListener(e -> showColumnConfigDialog());
        
        configMenu.add(pathMenuItem);
        configMenu.add(columnVisibilityMenuItem);
        
        // Visual-Menü
        JMenu visualMenu = new JMenu("Visual");
        visualMenu.setForeground(Color.WHITE);
        
        JMenuItem tableConfigMenuItem = new JMenuItem("Tabellenspalten anzeigen/verstecken");
        tableConfigMenuItem.addActionListener(e -> showColumnConfigDialog());
        
        visualMenu.add(tableConfigMenuItem);
        
        // Stats-Menü hinzufügen
        JMenu statsMenu = new JMenu("Statistik");
        statsMenu.setForeground(Color.WHITE);
        
        JMenuItem historyMenuItem = new JMenuItem("3MPDD Verlauf anzeigen");
        historyMenuItem.addActionListener(e -> showMpddHistoryDialog());
        
        statsMenu.add(historyMenuItem);
        
        menuBar.add(configMenu);
        menuBar.add(visualMenu);
        menuBar.add(statsMenu);
        
        setJMenuBar(menuBar);
    }
    
    private void showMpddHistoryDialog() {
        // Wenn Provider ausgewählt sind, zeige deren Historie, sonst zeige eine Meldung
        List<String> selectedProviders = mainTable.getSelectedProviders();
        if (selectedProviders.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Bitte wählen Sie mindestens einen Provider aus.",
                "Keine Auswahl",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Hier könnte ein neuer Dialog geöffnet werden, der die Historie der ausgewählten Provider anzeigt
        // Für jetzt zeigen wir einfach eine Meldung
        JOptionPane.showMessageDialog(this,
            "Die 3MPDD-Historien für die ausgewählten Provider können im Performance Analysis Dialog angezeigt werden.\n" +
            "Doppelklicken Sie auf einen Provider in der Tabelle, um den Dialog zu öffnen.",
            "3MPDD Historie",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showColumnConfigDialog() {
        TableColumnConfigDialog dialog = new TableColumnConfigDialog(this, mainTable);
        dialog.showDialog();
    }

    private void reloadData(String newPath) {
        try {
            dataManager.loadData(newPath);
            mainTable.refreshTableData();
            updateStatusBar();
        } catch (Exception e) {
            LOGGER.severe("Error reloading data: " + e.getMessage());
            JOptionPane.showMessageDialog(this, 
                "Error loading data from new path", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBackground(PRIMARY_COLOR);
        toolBar.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setOpaque(false);
        
        JLabel searchLabel = new JLabel("Search: ");
        searchLabel.setForeground(Color.WHITE);
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        
        JButton searchButton = createStyledButton("Search");
        searchButton.addActionListener(e -> performSearch());
        searchPanel.add(searchButton);
        
        // Neuer Delete Selected Button
        JButton deleteSelectedButton = createStyledButton("Delete Selected");
        deleteSelectedButton.addActionListener(e -> deleteSelectedProviders());
        searchPanel.add(deleteSelectedButton);
        
        toolBar.add(searchPanel);
        toolBar.addSeparator();
        
        JButton resetButton = createStyledButton("Reset");
        resetButton.addActionListener(e -> resetAll());
        toolBar.add(resetButton);
        
        JButton filterButton = createStyledButton("Filter");
        filterButton.addActionListener(e -> showFilterDialog());
        toolBar.add(filterButton);
        
        JButton showFavoritesButton = createStyledButton("Show Favorites");
        showFavoritesButton.addActionListener(e -> {
            mainTable.filterFavorites();
        });
        toolBar.add(showFavoritesButton);
        
        JButton compareButton = createStyledButton("Compare Equity Curves");
        compareButton.addActionListener(e -> showCompareDialog());
        toolBar.add(compareButton);
        
        JButton showSignalProvidersButton = createStyledButton("Show Signal Providers");
        showSignalProvidersButton.addActionListener(e -> {
            ShowSignalProviderList dialog = new ShowSignalProviderList(
                this,
                mainTable.getCurrentProviderStats(),
                mainTable.getHtmlDatabase(),
                config.getDownloadPath()
            );
            dialog.setVisible(true);
        });
        toolBar.add(showSignalProvidersButton);
        
        JButton compareOpenTradesButton = createStyledButton("Compare Open Trades");
        compareOpenTradesButton.addActionListener(e -> {
            CompareOpenTradesDialog dialog = new CompareOpenTradesDialog(this, mainTable.getCurrentProviderStats());
            dialog.setVisible(true);
        });
        toolBar.add(compareOpenTradesButton);

        JButton riskScoreButton = createStyledButton("Risk Score Explanation");
        riskScoreButton.addActionListener(e -> {
            RiskScoreExplanationDialog dialog = new RiskScoreExplanationDialog(this);
            dialog.setVisible(true);
        });
        toolBar.add(riskScoreButton);
       
        JButton dbViewerButton = createStyledButton("DB Einträge");
        dbViewerButton.addActionListener(e -> {
            DatabaseViewerDialog dialog = new DatabaseViewerDialog(this, historyService);
            dialog.setVisible(true);
        });
        toolBar.add(dbViewerButton);
        
        JButton dbForceSaveButton = createStyledButton("DB Speicherung erzwingen");
        dbForceSaveButton.addActionListener(e -> {
            // Prüfen, ob bereits Einträge in der DB vorhanden sind
            if (!historyService.hasDatabaseEntries()) {
                int answer = JOptionPane.showConfirmDialog(
                    this,
                    "Es wurden keine Einträge in der Datenbank gefunden. Möchten Sie eine initiale Speicherung für alle Provider erzwingen?",
                    "Datenbank leer",
                    JOptionPane.YES_NO_OPTION
                );
                
                if (answer == JOptionPane.YES_OPTION) {
                    // Initiale Speicherung im Hintergrund ausführen
                    new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            historyService.forceInitialSave();
                            return null;
                        }
                        
                        @Override
                        protected void done() {
                            JOptionPane.showMessageDialog(
                                MainFrame.this,
                                "Initiale Speicherung abgeschlossen. Die Datenbank enthält nun Einträge für alle Provider.",
                                "Speicherung abgeschlossen",
                                JOptionPane.INFORMATION_MESSAGE
                            );
                        }
                    }.execute();
                }
            } else {
                // Detaillierten Dialog zum Erzwingen der Speicherung öffnen
                // Hier rootPath übergeben
                ForceDbSaveDialog dialog = new ForceDbSaveDialog(
                    this,
                    historyService,
                    dataManager.getStats(),
                    config.getDownloadPath() // Hier wird der Download-Pfad aus der Konfiguration übergeben
                );
                dialog.setVisible(true);
            }
        });
        toolBar.add(dbForceSaveButton);
        
        add(toolBar, BorderLayout.NORTH);
    }
    
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(SECONDARY_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 90, 150), 1),
            BorderFactory.createEmptyBorder(4, 12, 4, 12)
        ));
        return button;
    }
    
    private void deleteSelectedProviders() {
        List<String> selectedProviders = mainTable.getSelectedProviders();
        if (selectedProviders.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Bitte wählen Sie mindestens einen Provider aus.",
                "Keine Auswahl",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        int result = JOptionPane.showConfirmDialog(this,
            "Möchten Sie die " + selectedProviders.size() + " ausgewählten Signal Provider löschen?",
            "Provider löschen",
            JOptionPane.YES_NO_OPTION);
            
        if (result == JOptionPane.YES_OPTION) {
            DeleteProviderDialog dialog = new DeleteProviderDialog(
                this,
                rootPath_glob,
                dataManager,
                mainTable.getSelectedProvidersMap(),
                () -> {
                    reloadData(config.getDownloadPath());
                    mainTable.refreshTableData();
                    updateStatusBar();
                }
            );
            dialog.setVisible(true);
        }
    }
    
    private void setupStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(SECONDARY_COLOR);
        statusBar.setBorder(BorderFactory.createEmptyBorder(4, 5, 4, 5));
        
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        
        statusBar.add(statusLabel, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
    }
    
    private void setupSearch() {
        searchField.addActionListener(e -> performSearch());
        
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    searchField.setText("");
                    mainTable.clearHighlight();
                    currentSearchIndex[0] = -1;
                }
            }
        });
    }
    
    private void performSearch() {
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
                JOptionPane.showMessageDialog(this,
                    "No matches found for: " + searchText,
                    "Search Result",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
    
    private void showFilterDialog() {
        FilterDialog dialog = new FilterDialog(this, mainTable.getCurrentFilter()); // Filterkriterien übergeben
        FilterCriteria criteria = dialog.showDialog();
        if (criteria != null) {
            mainTable.applyFilter(criteria);
        }
    }
    
    private void showCompareDialog() {
        CompareEquityCurvesDialog dialog = new CompareEquityCurvesDialog(this, mainTable.getCurrentProviderStats(), rootPath_glob);
        dialog.setVisible(true);
    }
    
    public void display() {
        SwingUtilities.invokeLater(() -> {
            setVisible(true);
            updateStatusBar();
            
            // Prüfen, ob wöchentliche Speicherung erforderlich ist
            historyService.checkAndPerformWeeklySave();
        });
    }
    
    // Main-Methode (falls sie Teil dieser Klasse ist)
    public static void main(String[] args) {
        // ... (bestehender Code)
        
        // Registriere Shutdown-Hook für sauberes Beenden
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Anwendung wird beendet, Ressourcen werden freigegeben...");
            ProviderHistoryService.getInstance().shutdown();
        }));
        
        // ... (weiterer Code)
    }
}