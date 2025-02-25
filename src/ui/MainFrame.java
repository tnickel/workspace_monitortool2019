package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;

import components.MainTable;
import data.DataManager;
import models.FilterCriteria;
import utils.MqlAnalyserConf;

public class MainFrame extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(MainFrame.class.getName());
    private final MainTable mainTable;
    private final JLabel statusLabel;
    private final JTextField searchField;
    private final DataManager dataManager;
    private final MqlAnalyserConf config;
    private int[] currentSearchIndex = {-1};
    private String rootPath_glob = null;

    public MainFrame(DataManager dataManager, String rootPath, MqlAnalyserConf config) {
        super("Signal Providers Performance Analysis");
        this.dataManager = dataManager;
        this.config = config;
        this.statusLabel = new JLabel();
        this.searchField = new JTextField(20);
        rootPath_glob = rootPath;
        
        mainTable = new MainTable(dataManager, config.getDownloadPath());
        mainTable.setStatusUpdateCallback(text -> updateStatusBar());
        
        setupUI();
        setupSearch();
        setupStatusBar();
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
        JMenu configMenu = new JMenu("Config");
        
        JMenuItem pathMenuItem = new JMenuItem("Set Download Path");
        pathMenuItem.addActionListener(e -> setDownloadPath());
        
        configMenu.add(pathMenuItem);
        menuBar.add(configMenu);
        setJMenuBar(menuBar);
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

        JButton riskScoreButton = new JButton("Risk Score Explanation");
        riskScoreButton.addActionListener(e -> {
            RiskScoreExplanationDialog dialog = new RiskScoreExplanationDialog(this);
            dialog.setVisible(true);
        });
        toolBar.add(compareOpenTradesButton);
        toolBar.add(riskScoreButton);
        
        add(toolBar, BorderLayout.NORTH);
    }
    
    private void deleteSelectedProviders() {
        List<String> selectedProviders = mainTable.getSelectedProviders();
        if (selectedProviders.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Bitte w�hlen Sie mindestens einen Provider aus.",
                "Keine Auswahl",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        int result = JOptionPane.showConfirmDialog(this,
            "M�chten Sie die " + selectedProviders.size() + " ausgew�hlten Signal Provider l�schen?",
            "Provider l�schen",
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
        FilterDialog dialog = new FilterDialog(this, mainTable.getCurrentFilter()); // Filterkriterien �bergeben
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
        });
    }
}