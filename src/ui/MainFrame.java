package ui;



import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import components.MainTable;
import components.SearchPanel;
import data.DataManager;

public class MainFrame extends JFrame {
    private final DataManager dataManager;
    private final MainTable mainTable;
    private final SearchPanel searchPanel;
    private final JButton compareButton;
    
    public MainFrame(DataManager dataManager) {
        super("Signal Providers Performance Analysis");
        this.dataManager = dataManager;
        this.mainTable = new MainTable(dataManager);
        this.searchPanel = new SearchPanel(mainTable);
        this.compareButton = new JButton("Compare Equity Curves");
        
        initializeUI();
    }
    
    private void initializeUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 600);
        
        // Main panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Top panel for search and compare button
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        compareButton.addActionListener(e -> showEquityCurvesComparison());
        buttonPanel.add(compareButton);

        // Combine search and button panels
        JPanel rightPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        rightPanel.add(searchPanel);
        rightPanel.add(buttonPanel);
        
        topPanel.add(rightPanel, BorderLayout.EAST);
        
        // Add components to main panel
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(mainTable), BorderLayout.CENTER);
        
        // Add main panel to frame
        add(mainPanel);
        setLocationRelativeTo(null);
    }
    
    private void showEquityCurvesComparison() {
        EquityCurvesFrame comparisonFrame = new EquityCurvesFrame(dataManager.getStats());
        comparisonFrame.setVisible(true);
    }
    
    public void display() {
        setVisible(true);
    }
}