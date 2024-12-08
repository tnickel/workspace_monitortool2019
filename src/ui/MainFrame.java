package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import data.DataManager;
import models.FilterCriteria;
import components.MainTable;
import ui.FilterDialog;        // Dieser Import fehlte
import ui.CompareDialog;
import ui.OpenTradesDialog;

public class MainFrame extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(MainFrame.class.getName());
    private final MainTable mainTable;
    private final JLabel statusLabel;
    private final JTextField searchField;
    private final DataManager dataManager;
    private int[] currentSearchIndex = new int[]{-1};

    public MainFrame(DataManager dataManager) {
        super("Signal Providers Performance Analysis");
        this.dataManager = dataManager;
        
        // Create components
        mainTable = new MainTable(dataManager);
        statusLabel = new JLabel(" ");
        searchField = new JTextField(20);
        
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
        
        // Status bar
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.add(statusLabel);
        contentPane.add(statusBar, BorderLayout.SOUTH);
        
        // Set table's status update callback
        mainTable.setStatusUpdateCallback(status -> statusLabel.setText(status));
        
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
        toolBar.add(compareOpenTradesButton);
        
        add(toolBar, BorderLayout.NORTH);
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
    
    private void setupStatusBar() {
        statusLabel.setBorder(new EmptyBorder(2, 5, 2, 5));
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
            currentSearchIndex[0] = -1; // Reset search
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
            mainTable.refreshTableData();
        }
    }
    
    private void showCompareDialog() {
        CompareDialog dialog = new CompareDialog(this, mainTable.getCurrentProviderStats());
        dialog.setVisible(true);
    }
    
    public void display() {
        SwingUtilities.invokeLater(() -> {
            setVisible(true);
            SwingUtilities.invokeLater(() -> {
                mainTable.updateStatus();
            });
        });
    }
}