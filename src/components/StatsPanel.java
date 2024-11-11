package components;



import data.ProviderStats;
import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;

public class StatsPanel extends JPanel {
    private final ProviderStats stats;
    private final DecimalFormat df = new DecimalFormat("#,##0.00");
    private final DecimalFormat pf = new DecimalFormat("#,##0.00'%'");
    
    public StatsPanel(ProviderStats stats) {
        this.stats = stats;
        createStatsDisplay();
    }
    
    private void createStatsDisplay() {
        setLayout(new GridLayout(0, 4, 10, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        addStatRow("Total Trades:", String.valueOf(stats.getTradeCount()));
        addStatRow("Win Rate:", pf.format(stats.getWinRate()));
        addStatRow("Total Profit:", df.format(stats.getTotalProfit()));
        addStatRow("Profit Factor:", df.format(stats.getProfitFactor()));
        addStatRow("Avg Profit/Trade:", df.format(stats.getAverageProfit()));
        addStatRow("Max Drawdown:", pf.format(stats.getMaxDrawdown()));
        addStatRow("Largest Win:", df.format(stats.getMaxProfit()));
        addStatRow("Largest Loss:", df.format(stats.getMaxLoss()));
    }
    
    private void addStatRow(String label, String value) {
        add(new JLabel(label, SwingConstants.RIGHT));
        add(new JLabel(value, SwingConstants.LEFT));
    }
}