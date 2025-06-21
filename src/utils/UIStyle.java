package utils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicComboBoxUI;

public class UIStyle {
    // Farben für die Anwendung
    public static final Color PRIMARY_COLOR = new Color(51, 102, 204);
    public static final Color SECONDARY_COLOR = new Color(41, 82, 164);
    public static final Color ACCENT_COLOR = new Color(255, 220, 0);
    public static final Color BACKGROUND_COLOR = new Color(240, 245, 250);
    public static final Color BG_COLOR = BACKGROUND_COLOR; // Alias für Kompatibilität
    public static final Color TEXT_COLOR = new Color(50, 50, 50);
    public static final Color TEXT_SECONDARY_COLOR = new Color(100, 100, 100);
    public static final Color POSITIVE_COLOR = new Color(0, 150, 50);
    public static final Color NEGATIVE_COLOR = new Color(220, 50, 50);
    
    // Schriftarten
    public static final Font HEADER_FONT = new Font("SansSerif", Font.BOLD, 14);
    public static final Font NORMAL_FONT = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font BOLD_FONT = new Font("SansSerif", Font.BOLD, 12);
    public static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 16);
    public static final Font REGULAR_FONT = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font BOLD_LARGE_FONT = new Font("SansSerif", Font.BOLD, 14);
    
    // URL Format
    public static final String SIGNAL_PROVIDER_URL_FORMAT = "https://www.mql5.com/de/signals/%s?source=Site+Signals+Subscriptions#!tab=account";
    
    // Chart-Größen und Abstand
    public static final Dimension DEFAULT_CHART_SIZE = new Dimension(800, 400);
    public static final Dimension DURATION_CHART_SIZE = new Dimension(800, 500);
    public static final Dimension CURRENCY_PAIR_CHART_SIZE = new Dimension(800, 600);
    public static final int CHART_PADDING = 10;
    public static final int PANEL_SPACING = 20;
    
    /**
     * Setzt die UIManager-Einstellungen für ein einheitliches Look-and-Feel
     */
    public static void setUpUIDefaults() {
        try {
            // Look-and-Feel auf System-Standard setzen
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            // Standard-Farben und Schriftarten setzen
            UIManager.put("Panel.background", BACKGROUND_COLOR);
            UIManager.put("OptionPane.background", BACKGROUND_COLOR);
            UIManager.put("Button.background", PRIMARY_COLOR);
            UIManager.put("Button.foreground", Color.WHITE);
            UIManager.put("Button.font", BOLD_FONT);
            UIManager.put("Label.font", NORMAL_FONT);
            UIManager.put("TextField.font", NORMAL_FONT);
            UIManager.put("ComboBox.font", NORMAL_FONT);
            
        } catch (Exception e) {
            System.err.println("Fehler beim Setzen des Look-and-Feel: " + e);
        }
    }
    
    /**
     * Erstellt ein gestyltes JLabel
     * @param text Der Text des Labels
     * @return Das gestylte JLabel
     */
    public static JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(NORMAL_FONT);
        label.setForeground(TEXT_COLOR);
        return label;
    }
    
    /**
     * Erstellt ein gestyltes JTextField
     * @param columns Anzahl der Spalten
     * @return Das gestylte JTextField
     */
    public static JTextField createStyledTextField(int columns) {
        JTextField textField = new JTextField(columns);
        textField.setFont(NORMAL_FONT);
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SECONDARY_COLOR),
            BorderFactory.createEmptyBorder(2, 5, 2, 5)
        ));
        return textField;
    }
    
    /**
     * Erstellt einen gestylten JButton
     * @param text Der Text des Buttons
     * @return Der gestylte JButton
     */
    public static JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(SECONDARY_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setBorder(createButtonBorder());
        button.setFont(BOLD_FONT);
        return button;
    }
    
    /**
     * Erstellt einen Standard-Rahmen für Buttons
     * @return Der Button-Rahmen
     */
    public static Border createButtonBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SECONDARY_COLOR),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        );
    }
    
    /**
     * Wendet Stile auf eine JComboBox an
     * @param comboBox Die zu stylende ComboBox
     */
    public static void applyStylesToComboBox(JComboBox<?> comboBox) {
        comboBox.setFont(NORMAL_FONT);
        comboBox.setBackground(Color.WHITE);
        comboBox.setForeground(TEXT_COLOR);
        comboBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SECONDARY_COLOR),
            BorderFactory.createEmptyBorder(2, 5, 2, 5)
        ));
        
        // Eigene UI für die ComboBox
        comboBox.setUI(new BasicComboBoxUI() {
            @Override
            public void paintCurrentValueBackground(Graphics g, java.awt.Rectangle bounds, boolean hasFocus) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (comboBox.isEnabled()) {
                    GradientPaint gp = new GradientPaint(
                        bounds.x, bounds.y, Color.WHITE,
                        bounds.x, bounds.y + bounds.height, new Color(240, 240, 240)
                    );
                    g2d.setPaint(gp);
                } else {
                    g2d.setColor(UIManager.getColor("ComboBox.background"));
                }
                
                g2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        });
    }
}