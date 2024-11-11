package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import javax.swing.*;

import components.MainTable;
import components.SearchPanel;
import data.DataManager;
import utils.LoggerUtil;

public class MainFrame extends JFrame {
    private final DataManager dataManager;
    private final MainTable mainTable;
    private final SearchPanel searchPanel;
    private final JButton compareButton;
    private final JButton filterButton;
    private final JMenuBar menuBar;
    private JLabel statusLabel;
    
    public MainFrame(DataManager dataManager) {
        super("Signal Providers Performance Analysis");
        this.dataManager = dataManager;
        this.mainTable = new MainTable(dataManager);
        this.searchPanel = new SearchPanel(mainTable);
        this.compareButton = createCompareButton();
        this.filterButton = createFilterButton();
        this.menuBar = createMenuBar();
        
        initializeUI();
        LoggerUtil.info("MainFrame initialized");
    }
    
    private JButton createCompareButton() {
        JButton button = new JButton("Compare Equity Curves");
        button.setMnemonic(KeyEvent.VK_C);
        button.setToolTipText("Compare equity curves of all providers");
        return button;
    }
    
    private JButton createFilterButton() {
        JButton button = new JButton("Filter");
        button.setMnemonic(KeyEvent.VK_F);
        button.setToolTipText("Filter strategies by criteria");
        button.addActionListener(e -> showFilterDialog());
        return button;
    }
    
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // File Menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        
        JMenuItem exportItem = new JMenuItem("Export Data...", KeyEvent.VK_E);
        exportItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, 
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        exportItem.addActionListener(e -> exportData());
        
        JMenuItem exitItem = new JMenuItem("Exit", KeyEvent.VK_X);
        exitItem.addActionListener(e -> dispose());
        
        fileMenu.add(exportItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        // View Menu
        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);
        
        JMenuItem compareItem = new JMenuItem("Compare Equity Curves", KeyEvent.VK_C);
        compareItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, 
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        compareItem.addActionListener(e -> showEquityCurvesComparison());
        
        viewMenu.add(compareItem);
        
        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        
        return menuBar;
    }
    
    private void initializeUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 600);
        setJMenuBar(menuBar);
        
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Top panel for search and buttons
        JPanel topPanel = new JPanel(new BorderLayout(5, 0));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        filterButton.addActionListener(e -> showFilterDialog());
        buttonPanel.add(filterButton);
        
        compareButton.addActionListener(e -> showEquityCurvesComparison());
        buttonPanel.add(compareButton);
        
        // Right panel with search and buttons
        JPanel rightPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        rightPanel.add(searchPanel);
        rightPanel.add(buttonPanel);
        
        topPanel.add(rightPanel, BorderLayout.EAST);
        
        // Table with scrollpane
        JScrollPane scrollPane = new JScrollPane(mainTable);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        // Add components to main panel
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Status bar
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        statusLabel = new JLabel();
        statusBar.add(statusLabel);
        mainPanel.add(statusBar, BorderLayout.SOUTH);
        
        // Setze den Callback in der MainTable
        mainTable.setStatusUpdateCallback(this::updateStatus);
        
        add(mainPanel);
        setLocationRelativeTo(null);
    }
    
    private void updateStatus(String status) {
        statusLabel.setText(status);
    }
    
    private void showFilterDialog() {
        FilterDialog dialog = new FilterDialog(this, mainTable);
        dialog.setVisible(true);
    }
    
    private void showEquityCurvesComparison() {
        try {
            // Hier die gefilterten Daten übergeben
            EquityCurvesFrame comparisonFrame = new EquityCurvesFrame(mainTable.getCurrentProviderStats());
            comparisonFrame.setVisible(true);
        } catch (Exception e) {
            LoggerUtil.error("Error showing equity curves", e);
            JOptionPane.showMessageDialog(this,
                "Error showing equity curves: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void exportData() {
        // TODO: Implement export functionality
        JOptionPane.showMessageDialog(this,
            "Export functionality coming soon!",
            "Not Implemented",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    public void display() {
        setVisible(true);
    }
}