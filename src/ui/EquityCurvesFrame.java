package ui;



import data.ProviderStats;
import utils.ChartFactory;
import org.jfree.chart.ChartPanel;
import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

public class EquityCurvesFrame extends JFrame {
    private final Map<String, ProviderStats> signalProviderStats;
    private final ChartFactory chartFactory;
    
    public EquityCurvesFrame(Map<String, ProviderStats> stats) {
        super("Equity Curves Comparison");
        this.signalProviderStats = stats;
        this.chartFactory = new ChartFactory();
        initializeUI();
    }
    
    private void initializeUI() {
        setSize(1200, 800);
        
        // Sort providers by total profit
        List<Map.Entry<String, ProviderStats>> sortedProviders = signalProviderStats.entrySet()
            .stream()
            .sorted((e1, e2) -> Double.compare(e2.getValue().getTotalProfit(), 
                                             e1.getValue().getTotalProfit()))
            .collect(Collectors.toList());

        // Create panel for all curves
        JPanel curvesPanel = new JPanel();
        curvesPanel.setLayout(new BoxLayout(curvesPanel, BoxLayout.Y_AXIS));

        // Create equity curve for each provider
        for (Map.Entry<String, ProviderStats> entry : sortedProviders) {
            String providerName = entry.getKey();
            ProviderStats stats = entry.getValue();
            
            JPanel curvePanel = new JPanel(new BorderLayout());
            curvePanel.setBorder(BorderFactory.createTitledBorder(
                String.format("%s (Profit: %.2f)", providerName, stats.getTotalProfit())
            ));
            
            ChartPanel chartPanel = chartFactory.createEquityCurveChart(stats);
            chartPanel.setPreferredSize(new Dimension(1100, 300));
            curvePanel.add(chartPanel, BorderLayout.CENTER);
            
            curvesPanel.add(curvePanel);
        }

        // Add scrolling
        JScrollPane scrollPane = new JScrollPane(curvesPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane);

        setLocationRelativeTo(null);
    }
}