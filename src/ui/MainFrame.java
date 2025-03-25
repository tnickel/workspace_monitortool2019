package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.logging.Logger;

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
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;

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

    public MainFrame(DataManager dataManager, String rootPath, MqlAnalyserConf config) {
        super("Signal Providers Performance Analysis");
        this.dataManager = dataManager;
        this.config = config;
        this.statusLabel = new JLabel();
        this.searchField = new JTextField(20);
        rootPath_glob = rootPath;
        
        // Provider History Service initialisieren
        this.historyService = ProviderHistoryService.getInstance();
        this.historyService.initialize(rootPath);
        
        mainTable = new MainTable(dataManager, config.getDownloadPath());
        mainTable.setStatusUpdateCallback(text -> updateStatusBar());
        
        mainTable.loadColumnVisibilitySettings();
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
        
        JPanel contentPane = new JPanel(new BorderLayout(5, 5));
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        
        createToolBar();
        
        JScrollPane scrollPane = new JScrollPane(mainTable);
        contentPane.add(scrollPane, BorderLayout.CENTER);
        
        setSize(1200, 800);
        setLocationRelativeTo(null);
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // Config-Menü
        JMenu configMenu = new JMenu("Config");
        
        JMenuItem pathMenuItem = new JMenuItem("Set Download Path");
        pathMenuItem.addActionListener(e -> setDownloadPath());
        
        JMenuItem columnVisibilityMenuItem = new JMenuItem("Tabellenspalten konfigurieren");
        columnVisibilityMenuItem.addActionListener(e -> showColumnConfigDialog());
        
        configMenu.add(pathMenuItem);
        configMenu.add(columnVisibilityMenuItem);
        
        // Visual-Menü
        JMenu visualMenu = new JMenu("Visual");
        
        JMenuItem tableConfigMenuItem = new JMenuItem("Tabellenspalten anzeigen/verstecken");
        tableConfigMenuItem.addActionListener(e -> showColumnConfigDialog());
        
        visualMenu.add(tableConfigMenuItem);
        
        // Stats-Menü hinzufügen
        JMenu statsMenu = new JMenu("Statistik");
        
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
        
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search: "));
        searchPanel.add(searchField);
        
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> performSearch());
        searchPanel.add(searchButton);
        
        // Neuer Delete Selected Button
        JButton deleteSelectedButton = new JButton("Delete Selected");
        deleteSelectedButton.addActionListener(e -> deleteSelectedProviders());
        searchPanel.add(deleteSelectedButton);
        
        toolBar.add(searchPanel);
        toolBar.addSeparator();
        
        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> resetAll());
        toolBar.add(resetButton);
        
        JButton filterButton = new JButton("Filter");
        filterButton.addActionListener(e -> showFilterDialog());
        toolBar.add(filterButton);
        
        JButton showFavoritesButton = new JButton("Show Favorites");
        showFavoritesButton.addActionListener(e -> {
            mainTable.filterFavorites();
        });
        toolBar.add(showFavoritesButton);
        
        JButton compareButton = new JButton("Compare Equity Curves");
        compareButton.addActionListener(e -> showCompareDialog());
        toolBar.add(compareButton);
        
        JButton showSignalProvidersButton = new JButton("Show Signal Providers");
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
        
        JButton compareOpenTradesButton = new JButton("Compare Open Trades");
        compareOpenTradesButton.addActionListener(e -> {
            CompareOpenTradesDialog dialog = new CompareOpenTradesDialog(this, mainTable.getCurrentProviderStats());
            dialog.setVisible(true);
        });
        toolBar.add(compareOpenTradesButton);

        JButton riskScoreButton = new JButton("Risk Score Explanation");
        riskScoreButton.addActionListener(e -> {
            RiskScoreExplanationDialog dialog = new RiskScoreExplanationDialog(this);
            dialog.setVisible(true);
        });
        toolBar.add(riskScoreButton);
       
        JButton dbViewerButton = new JButton("DB Einträge");
        dbViewerButton.addActionListener(e -> {
            DatabaseViewerDialog dialog = new DatabaseViewerDialog(this, historyService);
            dialog.setVisible(true);
        });
        toolBar.add(dbViewerButton);
        
        JButton dbForceSaveButton = new JButton("DB Speicherung erzwingen");
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
        statusBar.setBorder(new BevelBorder(BevelBorder.LOWERED));
        
        statusLabel.setBorder(new EmptyBorder(2, 5, 2, 5));
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(240, 240, 240));
        
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