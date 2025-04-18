package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
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
import javax.swing.border.EmptyBorder;

import components.GradientPanel;
import components.LoadingPanel;
import components.MainTable;
import data.DataManager;
import models.FilterCriteria;
import renderers.StyledTableCellRenderer;
import renderers.StyledTableHeaderRenderer;
import services.ProviderHistoryService;
import utils.ChartUtils;
import utils.HtmlDatabase;
import utils.IconManager;
import utils.MqlAnalyserConf;
import utils.UIStyleManager;

/**
 * Hauptfenster der Anwendung mit modernisiertem UI-Design
 */
public class MainFrame extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(MainFrame.class.getName());
    private final MainTable mainTable;
    private final JLabel statusLabel;
    private final JTextField searchField;
    private final DataManager dataManager;
    private final MqlAnalyserConf config;
    private final ProviderHistoryService historyService;
    private final LoadingPanel loadingPanel;
    private int[] currentSearchIndex = {-1};
    private String rootPath_glob = null;
    
    public MainFrame(DataManager dataManager, String rootPath, MqlAnalyserConf config) {
        super("Signal Providers Performance Analysis");
        this.dataManager = dataManager;
        this.config = config;
        this.statusLabel = UIStyleManager.createRegularLabel("");
        this.searchField = UIStyleManager.createStyledTextField(20);
        this.loadingPanel = new LoadingPanel("Initialisiere...");
        rootPath_glob = rootPath;
        
        // Provider History Service initialisieren
        this.historyService = ProviderHistoryService.getInstance();
        this.historyService.initialize(rootPath);
        
        // UI-Komponenten mit dem neuen Stil
        UIStyleManager.setupGlobalUI();
        
        mainTable = new MainTable(dataManager, rootPath_glob);
        mainTable.setStatusUpdateCallback(text -> updateStatusBar());
        
        // Rendern der Tabelle mit erweiterten Renderern verbessern
        mainTable.setDefaultRenderer(Object.class, new StyledTableCellRenderer());
        mainTable.getTableHeader().setDefaultRenderer(new StyledTableHeaderRenderer());
        
        setupUI();
        setupSearch();
        setupStatusBar();
        
        // Prüfe Dateisystemzugriff
        SwingUtilities.invokeLater(() -> verifyFileSystemAccess());
        
        // Hinzufügen eines WindowListeners für sauberes Herunterfahren
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                LOGGER.info("Fenster wird geschlossen, Ressourcen werden freigegeben...");
                historyService.shutdown();
            }
        });
    }
    
    private void setupUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setupMenuBar();
        
        // Hintergrund für das Hauptpanel
        GradientPanel contentPane = new GradientPanel();
        contentPane.setLayout(new BorderLayout(10, 10));
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(contentPane);
        
        // Dashboard-Layout erstellen
        setupDashboardUI();
        
        setSize(1200, 800);
        setLocationRelativeTo(null);
    }
    
    private void setupDashboardUI() {
        // Toolbar erstellen
        createModernToolBar();
        
        // Dashboard-Übersicht erstellen
        JPanel dashboardPanel = new JPanel();
        dashboardPanel.setOpaque(false);
        dashboardPanel.setLayout(new BorderLayout(0, 10));
        
        // Statistik-Kacheln
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        statsPanel.setOpaque(false);
        
        // Kachel 1: Anzahl der Provider
        JPanel providerCountPanel = ChartUtils.createStatTile(
            "Signal Providers", 
            String.valueOf(dataManager.getStats().size()), 
            UIStyleManager.SECONDARY_COLOR
        );
        
        // Kachel 2: Erfolgreiche Provider (Win Rate > 65%)
        long successfulProviders = dataManager.getStats().values().stream()
                .filter(stats -> stats.getWinRate() >= 65.0)
                .count();
        JPanel successfulPanel = ChartUtils.createStatTile(
            "Erfolgreiche Provider", 
            String.valueOf(successfulProviders), 
            UIStyleManager.POSITIVE_COLOR
        );
        
        // Kachel 3: Risikoreiche Provider (MaxDrawdown > 30%)
        long riskyProviders = dataManager.getStats().values().stream()
                .filter(stats -> stats.getMaxDrawdown() > 30.0)
                .count();
        JPanel riskyPanel = ChartUtils.createStatTile(
            "Risikoreiche Provider", 
            String.valueOf(riskyProviders), 
            UIStyleManager.NEGATIVE_COLOR
        );
        
        // Kachel 4: Gesamt-Handelstage (Durchschnitt)
        double avgTradeDays = dataManager.getStats().values().stream()
                .mapToInt(stats -> stats.getTradeDays())
                .average()
                .orElse(0.0);
        JPanel tradeDaysPanel = ChartUtils.createStatTile(
            "Ø Handelstage", 
            String.format("%.0f", avgTradeDays), 
            UIStyleManager.ACCENT_COLOR
        );
        
        statsPanel.add(providerCountPanel);
        statsPanel.add(successfulPanel);
        statsPanel.add(riskyPanel);
        statsPanel.add(tradeDaysPanel);
        
        dashboardPanel.add(statsPanel, BorderLayout.NORTH);
        
        // Tabelle mit Rahmen
        JScrollPane scrollPane = new JScrollPane(mainTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIStyleManager.SECONDARY_COLOR, 1));
        
        dashboardPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Haupt-Layout
        add(dashboardPanel, BorderLayout.CENTER);
        
        // Loading Panel (zunächst unsichtbar)
        loadingPanel.setVisible(false);
        add(loadingPanel, BorderLayout.SOUTH);
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(UIStyleManager.PRIMARY_COLOR);
        menuBar.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        
        // Config-Menü
        JMenu configMenu = new JMenu("Config");
        configMenu.setForeground(Color.WHITE);
        
        JMenuItem pathMenuItem = new JMenuItem("Set Download Path");
        pathMenuItem.setIcon(IconManager.getSettingsIcon(IconManager.ICON_SMALL));
        pathMenuItem.addActionListener(e -> setDownloadPath());
        
        JMenuItem columnVisibilityMenuItem = new JMenuItem("Tabellenspalten konfigurieren");
        columnVisibilityMenuItem.setIcon(IconManager.getSettingsIcon(IconManager.ICON_SMALL));
        columnVisibilityMenuItem.addActionListener(e -> showColumnConfigDialog());
        
        configMenu.add(pathMenuItem);
        configMenu.add(columnVisibilityMenuItem);
        
        // Visual-Menü
        JMenu visualMenu = new JMenu("Visual");
        visualMenu.setForeground(Color.WHITE);
        
        JMenuItem tableConfigMenuItem = new JMenuItem("Tabellenspalten anzeigen/verstecken");
        tableConfigMenuItem.setIcon(IconManager.getSettingsIcon(IconManager.ICON_SMALL));
        tableConfigMenuItem.addActionListener(e -> showColumnConfigDialog());
        
        visualMenu.add(tableConfigMenuItem);
        
        // Stats-Menü hinzufügen
        JMenu statsMenu = new JMenu("Statistik");
        statsMenu.setForeground(Color.WHITE);
        
        JMenuItem historyMenuItem = new JMenuItem("3MPDD Verlauf anzeigen");
        historyMenuItem.setIcon(IconManager.getChartIcon(IconManager.ICON_SMALL));
        historyMenuItem.addActionListener(e -> showMpddHistoryDialog());
        
        statsMenu.add(historyMenuItem);
        
        menuBar.add(configMenu);
        menuBar.add(visualMenu);
        menuBar.add(statsMenu);
        
        setJMenuBar(menuBar);
    }
    
    private void showMpddHistoryDialog() {
        // Wenn Provider ausgewählt sind, zeige deren Historie, sonst zeige eine Meldung
        java.util.List<String> selectedProviders = mainTable.getSelectedProviders();
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

    private void createModernToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBackground(UIStyleManager.PRIMARY_COLOR);
        toolBar.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        
        // Suche mit Icon
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchPanel.setOpaque(false);
        
        JLabel searchIcon = new JLabel();
        searchIcon.setIcon(IconManager.getSearchIcon(IconManager.ICON_SMALL));
        searchIcon.setForeground(Color.WHITE);
        
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(0, 0, 0, 0),
            BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));
        
        JPanel searchFieldPanel = new JPanel(new BorderLayout());
        searchFieldPanel.setBackground(Color.WHITE);
        searchFieldPanel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
        searchFieldPanel.add(searchIcon, BorderLayout.WEST);
        searchFieldPanel.add(searchField, BorderLayout.CENTER);
        
        searchPanel.add(searchFieldPanel, BorderLayout.CENTER);
        
        // Button-Gruppe mit Abstand
        JPanel buttonGroup1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonGroup1.setOpaque(false);
        
        JButton resetButton = createToolbarButton("Reset", IconManager.getResetIcon(IconManager.ICON_MEDIUM));
        resetButton.addActionListener(e -> resetAll());
        
        JButton filterButton = createToolbarButton("Filter", IconManager.getFilterIcon(IconManager.ICON_MEDIUM));
        filterButton.addActionListener(e -> showFilterDialog());
        
        buttonGroup1.add(resetButton);
        buttonGroup1.add(filterButton);
        
        // Zweite Button-Gruppe
        JPanel buttonGroup2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonGroup2.setOpaque(false);
        
        JButton favoritesButton = createToolbarButton("Favorites", IconManager.getFavoriteIcon(IconManager.ICON_MEDIUM, true));
        favoritesButton.addActionListener(e -> {
            mainTable.filterFavorites();
        });
        
        JButton compareButton = createToolbarButton("Compare", IconManager.getCompareIcon(IconManager.ICON_MEDIUM));
        compareButton.addActionListener(e -> showCompareDialog());
        
        buttonGroup2.add(favoritesButton);
        buttonGroup2.add(compareButton);
        
        // Tool-Verzeichnis
        JPanel buttonGroup3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonGroup3.setOpaque(false);
        
        JButton signalProvidersButton = createToolbarButton("Signal Providers", IconManager.getListIcon(IconManager.ICON_MEDIUM));
        signalProvidersButton.addActionListener(e -> {
            ShowSignalProviderList dialog = new ShowSignalProviderList(
                this,
                mainTable.getCurrentProviderStats(),
                mainTable.getHtmlDatabase(),
                config.getDownloadPath()
            );
            dialog.setVisible(true);
        });
        
        JButton dbButton = createToolbarButton("Database", IconManager.getDatabaseIcon(IconManager.ICON_MEDIUM));
        dbButton.addActionListener(e -> {
            DatabaseViewerDialog dialog = new DatabaseViewerDialog(this, historyService);
            dialog.setVisible(true);
        });
        
        buttonGroup3.add(signalProvidersButton);
        buttonGroup3.add(dbButton);
        
        // Alles zusammenfügen
        toolBar.add(searchPanel);
        toolBar.addSeparator(new Dimension(20, 0));
        toolBar.add(buttonGroup1);
        toolBar.addSeparator(new Dimension(20, 0));
        toolBar.add(buttonGroup2);
        toolBar.addSeparator(new Dimension(20, 0));
        toolBar.add(buttonGroup3);
        
        add(toolBar, BorderLayout.NORTH);
    }
    
    private JButton createToolbarButton(String text, javax.swing.ImageIcon icon) {
        JButton button = new JButton(text);
        button.setIcon(icon);
        button.setForeground(Color.WHITE);
        button.setBackground(UIStyleManager.PRIMARY_COLOR);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setFont(UIStyleManager.REGULAR_FONT);
        button.setIconTextGap(8);
        button.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        
        // Hover-Effekt
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(UIStyleManager.SECONDARY_COLOR);
                button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(UIStyleManager.PRIMARY_COLOR);
            }
        });
        
        return button;
    }
    
    private void updateStatusBar() {
        String providerCount = mainTable.getStatusText();
        
        // Extrahiere den relevanten Teil aus dem Text (enthält bereits den Pfad)
        String statusText = providerCount;
        
        // Entferne doppelte Pfadanzeigen, behalte nur den letzten (korrekten) Pfad
        if (statusText.contains("Download Path:")) {
            int lastOccurrence = statusText.lastIndexOf("Download Path:");
            if (lastOccurrence > 0) {
                // Behalte nur den Text vor dem ersten "Download Path:" und den letzten Pfad
                String prefix = statusText.substring(0, statusText.indexOf("Download Path:"));
                String lastPath = statusText.substring(lastOccurrence);
                statusText = prefix + " | " + lastPath;
            }
        }
        
        // Füge Root Path hinzu, falls nicht enthalten
        if (!statusText.contains("Root Path:")) {
            statusText += " | Root Path: " + config.getDownloadPath();
        }
        
        statusLabel.setText(statusText);
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
            
            // Statusleiste aktualisieren, um den neuen Pfad anzuzeigen
            updateStatusBar();
            
            if (JOptionPane.showConfirmDialog(this, 
                "Download path updated. Reload data?", 
                "Reload", 
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                reloadData(newPath);
            }
            
            // Nach der Änderung des Pfades erneut überprüfen
            verifyFileSystemAccess();
        }
    }
  
    private void resetAll() {
        searchField.setText("");
        mainTable.clearHighlight();
        currentSearchIndex[0] = -1;
        mainTable.resetFilter();
        updateStatusBar();
    }
    
    private void setupStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(UIStyleManager.SECONDARY_COLOR);
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
        FilterDialog dialog = new FilterDialog(this, mainTable.getCurrentFilter());
        FilterCriteria criteria = dialog.showDialog();
        if (criteria != null) {
            // Zeige LoadingPanel während des Filtervorgangs
            loadingPanel.setMessage("Filtere Daten...");
            loadingPanel.setVisible(true);
            loadingPanel.start();
            
            // Filter in einem separaten Thread anwenden
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    mainTable.applyFilter(criteria);
                    return null;
                }
                
                @Override
                protected void done() {
                    loadingPanel.stop();
                    loadingPanel.setVisible(false);
                }
            };
            
            worker.execute();
        }
    }
    
    private void showCompareDialog() {
        // Zeige LoadingPanel während der Erstellung des Vergleichsdialogs
        loadingPanel.setMessage("Lade Vergleichsdaten...");
        loadingPanel.setVisible(true);
        loadingPanel.start();
        
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Kurze Pause für bessere Benutzererfahrung
                Thread.sleep(300);
                return null;
            }
            
            @Override
            protected void done() {
                CompareEquityCurvesDialog dialog = new CompareEquityCurvesDialog(MainFrame.this, mainTable.getCurrentProviderStats(), rootPath_glob);
                loadingPanel.stop();
                loadingPanel.setVisible(false);
                dialog.setVisible(true);
            }
        };
        
        worker.execute();
    }
    
    // Methode zur Überprüfung des Dateisystemzugriffs
    private void verifyFileSystemAccess() {
        HtmlDatabase htmlDb = mainTable.getHtmlDatabase();
        if (htmlDb != null) {
            boolean accessOk = htmlDb.checkFileAccess();
            if (!accessOk) {
                JOptionPane.showMessageDialog(
                    this,
                    "WARNUNG: Es gibt Probleme beim Zugriff auf das Datenverzeichnis:\n" +
                    htmlDb.getRootPath() + "\n\n" +
                    "Dies kann zu fehlenden Daten wie 3MPDD-Werten führen.\n" +
                    "Bitte überprüfen Sie den Pfad in den Einstellungen.",
                    "Dateizugriffsproblem",
                    JOptionPane.WARNING_MESSAGE
                );
            } else {
                // Prüfe auf _root.txt Dateien
                File dir = new File(htmlDb.getRootPath());
                File[] rootTxtFiles = dir.listFiles((d, name) -> name.endsWith("_root.txt"));
                if (rootTxtFiles == null || rootTxtFiles.length == 0) {
                    JOptionPane.showMessageDialog(
                        this,
                        "WARNUNG: Im Datenverzeichnis wurden keine _root.txt Dateien gefunden:\n" +
                        htmlDb.getRootPath() + "\n\n" +
                        "Dies führt dazu, dass 3MPDD-Werte und andere Statistiken nicht berechnet werden können.\n" +
                        "Bitte überprüfen Sie den Pfad in den Einstellungen.",
                        "Fehlende Datendateien",
                        JOptionPane.WARNING_MESSAGE
                    );
                } else {
                    LOGGER.info("Gefunden " + rootTxtFiles.length + " _root.txt Dateien im Verzeichnis " + htmlDb.getRootPath());
                }
            }
        }
        
        // Prüfen, ob wöchentliche Speicherung erforderlich ist
        historyService.checkAndPerformWeeklySave();
    }
    
    private void reloadData(String newPath) {
        try {
            // Zeige LoadingPanel während des Neuladen
            loadingPanel.setMessage("Lade Daten neu...");
            loadingPanel.setVisible(true);
            loadingPanel.start();
            
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    dataManager.loadData(newPath);
                    return null;
                }
                
                @Override
                protected void done() {
                    mainTable.refreshTableData();
                    updateStatusBar();
                    loadingPanel.stop();
                    loadingPanel.setVisible(false);
                }
            };
            
            worker.execute();
        } catch (Exception e) {
            LOGGER.severe("Error reloading data: " + e.getMessage());
            JOptionPane.showMessageDialog(this, 
                "Error loading data from new path", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            
            loadingPanel.stop();
            loadingPanel.setVisible(false);
        }
    }
    
    public void display() {
        SwingUtilities.invokeLater(() -> {
            setVisible(true);
            updateStatusBar();
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