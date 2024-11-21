package components;



import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;

import data.ProviderStats;
import ui.TradeListFrame;

public class SignalProviderPanel extends JPanel {
    private final String providerId;
    private final ProviderStats stats;
    private final DecimalFormat df = new DecimalFormat("#,##0.00");
    private final DecimalFormat pf = new DecimalFormat("#,##0.00'%'");
    
    public SignalProviderPanel(ChartPanel chartPanel, String fileName, ProviderStats stats) {
        this.stats = stats;
        setLayout(new BorderLayout());
        add(chartPanel, BorderLayout.CENTER);
        
        // Extract ID from filename
        providerId = fileName.substring(fileName.lastIndexOf("_") + 1).replace(".csv", "");
        
        // Create south panel
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
        
        // URL Panel
        JPanel urlPanel = createUrlPanel();
        southPanel.add(urlPanel);
        
        // Stats Panel
        JPanel statsPanel = createStatsPanel();
        southPanel.add(statsPanel);
        
        add(southPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createUrlPanel() {
        JPanel urlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel urlLabel = new JLabel("<html><u>https://www.mql5.com/de/signals/" + 
            providerId + "?source=Site+Signals+Subscriptions#!tab=account</u></html>");
        urlLabel.setForeground(Color.BLUE);
        urlLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        urlLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://www.mql5.com/de/signals/" + 
                        providerId + "?source=Site+Signals+Subscriptions#!tab=account"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        
        urlPanel.add(urlLabel);
        return urlPanel;
    }
    
    private JPanel createStatsPanel() {
        JPanel mainStatsPanel = new JPanel(new BorderLayout());
        
        // Statistik-Grid wie bisher
        JPanel statsGrid = new JPanel(new GridLayout(2, 4, 10, 5));
        statsGrid.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Zeile 1
        addStatField(statsGrid, "Total Trades:", String.format("%d", stats.getTradeCount()));
        addStatField(statsGrid, "Win Rate:", pf.format(stats.getWinRate()));
        addStatField(statsGrid, "Avg Profit:", df.format(stats.getAverageProfit()));
        addStatField(statsGrid, "Max Drawdown:", pf.format(stats.getMaxDrawdown()));

        // Zeile 2
        addStatField(statsGrid, "Profit Factor:", df.format(stats.getProfitFactor()));
        addStatField(statsGrid, "Total Profit:", df.format(stats.getTotalProfit()));
        addStatField(statsGrid, "Max Profit:", df.format(stats.getMaxProfit()));
        addStatField(statsGrid, "Max Loss:", df.format(stats.getMaxLoss()));

        mainStatsPanel.add(statsGrid, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton showTradesButton = new JButton("Show Trades");
        showTradesButton.addActionListener(e -> showTradeList());
        buttonPanel.add(showTradesButton);

        mainStatsPanel.add(buttonPanel, BorderLayout.EAST);

        return mainStatsPanel;
    }

    
    private void addStatField(JPanel panel, String label, String value) {
        JPanel fieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        fieldPanel.add(new JLabel(label));
        JLabel valueLabel = new JLabel(value);
        valueLabel.setForeground(new Color(0, 100, 0));  // Dunkelgrün
        fieldPanel.add(valueLabel);
        panel.add(fieldPanel);
    }
    private void showTradeList() {
        TradeListFrame tradeListFrame = new TradeListFrame(providerId, stats);
        tradeListFrame.setVisible(true);
    }
}