package ui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import ui.UIConstants;

/**
 * Dekorationsklasse für Chart-Komponenten, die einen einheitlichen Rahmen und Titel hinzufügt
 */
public class ChartPanel extends JPanel {
    
    /**
     * Konstruktor für einen Chart mit Standardrahmen und Titel
     * 
     * @param chartComponent Die eigentliche Chart-Komponente
     * @param title Der Titel für das Chart-Panel
     * @param size Die Größe für das Chart-Panel
     */
    public ChartPanel(Component chartComponent, String title, Dimension size) {
        setLayout(new BorderLayout());
        setOpaque(false);
        
        // Rahmen mit Titel erstellen
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIConstants.SECONDARY_COLOR, 1),
                title,
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 12),
                UIConstants.SECONDARY_COLOR
            ),
            BorderFactory.createEmptyBorder(
                UIConstants.CHART_PADDING, 
                UIConstants.CHART_PADDING, 
                UIConstants.CHART_PADDING, 
                UIConstants.CHART_PADDING
            )
        ));
        
        // Chart-Komponente hinzufügen
        add(chartComponent, BorderLayout.CENTER);
        
        // Größe festlegen
        chartComponent.setPreferredSize(size);
        chartComponent.setMaximumSize(size);
        chartComponent.setMinimumSize(size);
        
        // Ausrichtung für BoxLayout setzen
        setAlignmentX(LEFT_ALIGNMENT);
    }
}