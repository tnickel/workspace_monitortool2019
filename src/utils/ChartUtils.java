package utils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.XYPlot;

/**
 * Hilfsfunktionen für einheitliches Styling von JFreeChart-Diagrammen
 */
public class ChartUtils {
    
    /**
     * Erstellt ein einheitlich gestyltes ChartPanel für ein JFreeChart
     * 
     * @param chart Das zu verwendende JFreeChart
     * @param title Optionaler Titel für das Panel (null für keinen Titel)
     * @param size Gewünschte Größe des Panels
     * @return Ein gestyltes ChartPanel
     */
    public static ChartPanel createStyledChartPanel(JFreeChart chart, String title, Dimension size) {
        // Chart-Styling
        chart.setBackgroundPaint(Color.WHITE);
        
        if (chart.getTitle() != null) {
            chart.getTitle().setFont(UIStyleManager.SUBTITLE_FONT);
            chart.getTitle().setPaint(UIStyleManager.PRIMARY_COLOR);
        }
        
        if (chart.getLegend() != null) {
            chart.getLegend().setBackgroundPaint(new Color(245, 245, 245));
            chart.getLegend().setItemFont(UIStyleManager.REGULAR_FONT);
        }
        
        // Plot-spezifisches Styling
        if (chart.getPlot() instanceof XYPlot) {
            XYPlot plot = (XYPlot) chart.getPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setDomainGridlinePaint(new Color(240, 240, 240));
            plot.setRangeGridlinePaint(new Color(240, 240, 240));
            
            // Achsen formatieren
            if (plot.getDomainAxis() != null) {
                plot.getDomainAxis().setLabelFont(UIStyleManager.REGULAR_FONT);
                plot.getDomainAxis().setTickLabelFont(UIStyleManager.SMALL_FONT);
                
                // Spezielle Formatierung für Datumsachsen
                if (plot.getDomainAxis() instanceof DateAxis) {
                    DateAxis dateAxis = (DateAxis) plot.getDomainAxis();
                    dateAxis.setDateFormatOverride(new java.text.SimpleDateFormat("yyyy-MM-dd"));
                }
            }
            
            if (plot.getRangeAxis() != null) {
                plot.getRangeAxis().setLabelFont(UIStyleManager.REGULAR_FONT);
                plot.getRangeAxis().setTickLabelFont(UIStyleManager.SMALL_FONT);
            }
        } else if (chart.getPlot() instanceof CategoryPlot) {
            CategoryPlot plot = (CategoryPlot) chart.getPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setDomainGridlinePaint(new Color(240, 240, 240));
            plot.setRangeGridlinePaint(new Color(240, 240, 240));
            
            // Achsen formatieren
            if (plot.getDomainAxis() != null) {
                plot.getDomainAxis().setLabelFont(UIStyleManager.REGULAR_FONT);
                plot.getDomainAxis().setTickLabelFont(UIStyleManager.SMALL_FONT);
            }
            
            if (plot.getRangeAxis() != null) {
                plot.getRangeAxis().setLabelFont(UIStyleManager.REGULAR_FONT);
                plot.getRangeAxis().setTickLabelFont(UIStyleManager.SMALL_FONT);
            }
        }
        
        // ChartPanel erstellen und konfigurieren
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(size);
        
        // Rahmen hinzufügen
        if (title != null && !title.isEmpty()) {
            chartPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIStyleManager.SECONDARY_COLOR, 1),
                title,
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                UIStyleManager.SUBTITLE_FONT,
                UIStyleManager.SECONDARY_COLOR
            ));
        } else {
            chartPanel.setBorder(BorderFactory.createLineBorder(UIStyleManager.SECONDARY_COLOR, 1));
        }
        
        return chartPanel;
    }
    
    /**
     * Erstellt eine Statistik-Kachel mit Titel und Wert
     * 
     * @param title Beschriftung der Kachel
     * @param value Anzuzeigender Wert
     * @param valueColor Farbe des Werts
     * @return Ein gestyltes JPanel
     */
    public static JPanel createStatTile(String title, String value, Color valueColor) {
        JPanel panel = new JPanel(new java.awt.BorderLayout(5, 5));
        panel.setBackground(new Color(255, 255, 255, 240));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(valueColor, 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        javax.swing.JLabel titleLabel = new javax.swing.JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        titleLabel.setForeground(UIStyleManager.TEXT_SECONDARY_COLOR);
        
        javax.swing.JLabel valueLabel = new javax.swing.JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        valueLabel.setForeground(valueColor);
        valueLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        
        panel.add(titleLabel, java.awt.BorderLayout.NORTH);
        panel.add(valueLabel, java.awt.BorderLayout.CENTER);
        
        return panel;
    }
}