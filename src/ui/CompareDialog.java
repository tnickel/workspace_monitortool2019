package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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

public class CompareDialog extends JDialog {
    private final Map<String, ProviderStats> providerStats;

    public CompareDialog(JFrame parent, Map<String, ProviderStats> stats) {
        super(parent, "Compare Equity Curves", true);
        this.providerStats = stats;

        // Hauptpanel für die Charts
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Erstelle für jeden Provider einen Chart
        for (Map.Entry<String, ProviderStats> entry : providerStats.entrySet()) {
            String providerName = entry.getKey();
            ProviderStats currentStats = entry.getValue();

            // Chart erstellen
            JFreeChart chart = createChart(providerName, currentStats);
            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new Dimension(800, 300));

            // Panel für Provider mit vergrößertem Titel
            JPanel providerPanel = new JPanel(new BorderLayout());
            
            // Erstelle TitledBorder mit größerer Schrift
            Font currentFont = UIManager.getFont("TitledBorder.font");
            Font largerFont = currentFont.deriveFont(currentFont.getSize() * 2.0f);
            TitledBorder titledBorder = BorderFactory.createTitledBorder(providerName);
            titledBorder.setTitleFont(largerFont);
            
            providerPanel.setBorder(titledBorder);
            providerPanel.add(chartPanel, BorderLayout.CENTER);

            mainPanel.add(providerPanel, gbc);
            gbc.gridy++;
        }

        // Scrollpane für alle Charts
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Dialog-Einstellungen
        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        setSize(1000, 800);
        setLocationRelativeTo(parent);
    }

    private JFreeChart createChart(String providerName, ProviderStats stats) {
        TimeSeries series = new TimeSeries(providerName);
        TimeSeriesCollection dataset = new TimeSeriesCollection();

        List<Date> dates = stats.getTradeDates().stream()
            .map(date -> Date.from(date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()))
            .toList();

        List<Double> profits = stats.getProfits();
        double equity = stats.getInitialBalance();

        for (int i = 0; i < dates.size(); i++) {
            equity += profits.get(i);
            series.addOrUpdate(new Day(dates.get(i)), equity);
        }

        dataset.addSeries(series);

        // Erstelle Chart
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            "Equity Curve",
            "Time",
            "Equity",
            dataset,
            true,
            true,
            false
        );

        // Customize chart
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