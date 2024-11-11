package components;



import data.ProviderStats;
import utils.ChartFactory;
import javax.swing.*;
import java.awt.*;

public class ChartsPanel extends JPanel {
    private final ChartFactory chartFactory;
    private final ProviderStats stats;
    
    public ChartsPanel(ProviderStats stats) {
        this.stats = stats;
        this.chartFactory = new ChartFactory();
        createCharts();
    }
    
    private void createCharts() {
        setLayout(new GridLayout(2, 1));
        add(chartFactory.createEquityCurveChart(stats));
        add(chartFactory.createMonthlyProfitChart(stats));
    }
}