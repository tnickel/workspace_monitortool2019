package ui;

import java.awt.BorderLayout;
import ui.RiskScoreExplanationDialog;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
import ui.CompareDialog;

public class MainFrame extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(MainFrame.class.getName());
    private final MainTable mainTable;
    private final JLabel statusLabel;
    private final JTextField searchField;
    private final DataManager dataManager;
    private int[] currentSearchIndex = {-1};
    private String rootPath_glob=null;

    public MainFrame(DataManager dataManager, String rootPath) {
        super("Signal Providers Performance Analysis");
        this.dataManager = dataManager;
        this.statusLabel = new JLabel();
        this.searchField = new JTextField(20);
        rootPath_glob=rootPath;
        
        // Create components
        mainTable = new MainTable(dataManager);
        mainTable.setStatusUpdateCallback(text -> statusLabel.setText(text));
        
        setupUI();
        setupSearch();
        setupStatusBar();
    }
    
    private void setupUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Main layout
        JPanel contentPane = new JPanel(new BorderLayout(5, 5));
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        
        // Toolbar
        createToolBar();
        
        // Main table
        JScrollPane scrollPane = new JScrollPane(mainTable);
        contentPane.add(scrollPane, BorderLayout.CENTER);
        
        // Set initial size
        setSize(1200, 800);
        setLocationRelativeTo(null);
    }

    private void createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        
        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search: "));
        searchPanel.add(searchField);
        
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> performSearch());
        searchPanel.add(searchButton);
        
        toolBar.add(searchPanel);
        toolBar.addSeparator();
        
        // Filter button
        JButton filterButton = new JButton("Filter");
        filterButton.addActionListener(e -> showFilterDialog());
        toolBar.add(filterButton);
        
        // Show Favorites button
        JButton showFavoritesButton = new JButton("Show Favorites");
        toolBar.add(showFavoritesButton);
        
        // Compare Equity Curves button
        JButton compareButton = new JButton("Compare Equity Curves");
        compareButton.addActionListener(e -> showCompareDialog());
        toolBar.add(compareButton);
        
        // Compare Open Trades button
        JButton compareOpenTradesButton = new JButton("Compare Open Trades");
        compareOpenTradesButton.addActionListener(e -> {
            OpenTradesDialog dialog = new OpenTradesDialog(this, mainTable.getCurrentProviderStats());
            dialog.setVisible(true);
        });
        // Risk Score Explanation Button
        JButton riskScoreButton = new JButton("Risk Score Explanation");
        riskScoreButton.addActionListener(e -> {
            RiskScoreExplanationDialog dialog = new RiskScoreExplanationDialog(this);
            dialog.setVisible(true);
        });
        toolBar.add(compareOpenTradesButton);
        toolBar.add(riskScoreButton);
        
        add(toolBar, BorderLayout.NORTH);
    }
    
    private void setupStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(new BevelBorder(BevelBorder.LOWERED));
        
        statusLabel.setBorder(new EmptyBorder(2, 5, 2, 5));
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(240, 240, 240));  // Hellgrauer Hintergrund
        
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
        FilterDialog dialog = new FilterDialog(this);
        FilterCriteria criteria = dialog.showDialog();
        if (criteria != null) {
            mainTable.applyFilter(criteria);
        }
    }
    
    private void showCompareDialog() {
        CompareDialog dialog = new CompareDialog(this, mainTable.getCurrentProviderStats(),rootPath_glob);
        dialog.setVisible(true);
    }
    
    public void display() {
        SwingUtilities.invokeLater(() -> {
            setVisible(true);
            mainTable.updateStatus(); // Direkt aufrufen, keine weitere Verzögerung nötig
            
            // Optional: Erst nach kurzer Verzögerung updaten falls nötig
            // Timer timer = new Timer(100, e -> mainTable.updateStatus());
            // timer.setRepeats(false);
            // timer.start();
        });
    }
}