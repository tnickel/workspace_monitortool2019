
package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import data.ProviderStats;
import data.Trade;

public class CompareDialog extends JDialog {
    private final Map<String, ProviderStats> providerStats;

    public CompareDialog(JFrame parent, Map<String, ProviderStats> stats) {
        super(parent, "Compare Equity Curves", true);
        this.providerStats = stats;

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        for (Map.Entry<String, ProviderStats> entry : providerStats.entrySet()) {
            String providerName = entry.getKey();
            ProviderStats currentStats = entry.getValue();

            JFreeChart chart = createChart(providerName, currentStats);
            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new Dimension(800, 300));

            JPanel providerPanel = new JPanel(new BorderLayout());
            
            Font currentFont = UIManager.getFont("TitledBorder.font");
            Font largerFont = currentFont.deriveFont(currentFont.getSize() * 2.0f);
            TitledBorder titledBorder = BorderFactory.createTitledBorder(providerName);
            titledBorder.setTitleFont(largerFont);
            
            providerPanel.setBorder(titledBorder);
            providerPanel.add(chartPanel, BorderLayout.CENTER);

            mainPanel.add(providerPanel, gbc);
            gbc.gridy++;
        }

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        setSize(1000, 800);
        setLocationRelativeTo(parent);
    }

    private JFreeChart createChart(String providerName, ProviderStats stats) {
    	   TimeSeries series = new TimeSeries(providerName);
    	   TimeSeriesCollection dataset = new TimeSeriesCollection();

    	   List<Trade> trades = stats.getTrades();
    	   trades.sort((t1, t2) -> t1.getCloseTime().compareTo(t2.getCloseTime()));
    	   
    	   double equity = stats.getInitialBalance();
    	   
    	   // Für jeden Trade Equity berechnen
    	   for (Trade trade : trades) {
    	       if (trade.getCloseTime() != null && !trade.getCloseTime().isAfter(LocalDateTime.now())) {
    	           equity += trade.getTotalProfit();
    	           Date closeDate = Date.from(trade.getCloseTime().atZone(ZoneId.systemDefault()).toInstant());
    	           series.addOrUpdate(new Day(closeDate), equity);
    	       }
    	   }

    	   dataset.addSeries(series);

    	   JFreeChart chart = ChartFactory.createTimeSeriesChart(
    	       "Equity Curve",
    	       "Time",
    	       "Equity",
    	       dataset,
    	       true,
    	       true,
    	       false
    	   );

    	   XYPlot plot = (XYPlot) chart.getPlot();
    	   plot.setBackgroundPaint(Color.WHITE);
    	   plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
    	   plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

    	   XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
    	   renderer.setDefaultShapesVisible(false);
    	   plot.setRenderer(renderer);

    	   return chart;
    	}
}
