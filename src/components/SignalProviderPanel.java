package components;



import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import org.jfree.chart.ChartPanel;
import data.ProviderStats;
import java.text.DecimalFormat;

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
        JPanel statsPanel = new JPanel(new GridLayout(2, 4, 10, 5));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        // Zeile 1
        addStatField(statsPanel, "Total Trades:", String.format("%d", stats.getTradeCount()));
        addStatField(statsPanel, "Win Rate:", pf.format(stats.getWinRate()));
        addStatField(statsPanel, "Avg Profit:", df.format(stats.getAverageProfit()));
        addStatField(statsPanel, "Max Drawdown:", pf.format(stats.getMaxDrawdown()));
        
        // Zeile 2
        addStatField(statsPanel, "Profit Factor:", df.format(stats.getProfitFactor()));
        addStatField(statsPanel, "Total Profit:", df.format(stats.getTotalProfit()));
        addStatField(statsPanel, "Max Profit:", df.format(stats.getMaxProfit()));
        addStatField(statsPanel, "Max Loss:", df.format(stats.getMaxLoss()));
        
        return statsPanel;
    }
    
    private void addStatField(JPanel panel, String label, String value) {
        JPanel fieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        fieldPanel.add(new JLabel(label));
        JLabel valueLabel = new JLabel(value);
        valueLabel.setForeground(new Color(0, 100, 0));  // Dunkelgrün
        fieldPanel.add(valueLabel);
        panel.add(fieldPanel);
    }
}