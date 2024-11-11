package ui;



import data.ProviderStats;
import components.StatsPanel;
import components.ChartsPanel;
import javax.swing.*;
import java.awt.*;

public class DetailFrame extends JFrame {
    private final ProviderStats stats;
    private final StatsPanel statsPanel;
    private final ChartsPanel chartsPanel;
    private final JButton showTradesButton;
    
    public DetailFrame(String providerName, ProviderStats stats) {
        super("Detailed Performance Analysis: " + providerName);
        this.stats = stats;
        this.statsPanel = new StatsPanel(stats);
        this.chartsPanel = new ChartsPanel(stats);
        this.showTradesButton = new JButton("Show Trade List");
        
        initializeUI();
    }
    
    private void initializeUI() {
        setSize(1000, 800);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Stats Panel with button
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        showTradesButton.addActionListener(e -> new TradeListFrame(getTitle(), stats).setVisible(true));
        buttonPanel.add(showTradesButton);
        
        topPanel.add(statsPanel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.EAST);
        
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(chartsPanel, BorderLayout.CENTER);
        
        add(mainPanel);
        setLocationRelativeTo(null);
    }
}