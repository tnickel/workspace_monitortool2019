package utils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * Zentrale Klasse für einheitliches Styling aller UI-Komponenten.
 * Diese Klasse vereint die Funktionalitäten von UIStyleManager, AppUIStyle, 
 * UIComponentFactory und UIConstants
 */
public class UIStyle {
    // Farben
    public static final Color PRIMARY_COLOR = new Color(26, 45, 90);    // #1A2D5A - Dunkelblau
    public static final Color SECONDARY_COLOR = new Color(62, 125, 204); // #3E7DCC - Helleres Blau
    public static final Color ACCENT_COLOR = new Color(255, 209, 102);  // #FFD166 - Gold/Gelb
    public static final Color BG_COLOR = new Color(245, 247, 250);      // #F5F7FA - Sehr helles Grau
    public static final Color TEXT_COLOR = new Color(51, 51, 51);       // #333333 - Dunkelgrau
    public static final Color TEXT_SECONDARY_COLOR = new Color(85, 85, 85); // #555555 - Helleres Grau
    public static final Color POSITIVE_COLOR = new Color(46, 139, 87);  // #2E8B57 - Grün
    public static final Color NEGATIVE_COLOR = new Color(204, 59, 59);  // #CC3B3B - Rot
    
    // Schriftarten
    public static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 18);
    public static final Font SUBTITLE_FONT = new Font("SansSerif", Font.BOLD, 14);
    public static final Font REGULAR_FONT = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font BOLD_FONT = new Font("SansSerif", Font.BOLD, 12);
    public static final Font BOLD_LARGE_FONT = new Font("SansSerif", Font.BOLD, 13);
    public static final Font ITALIC_FONT = new Font("SansSerif", Font.ITALIC, 14);
    public static final Font SMALL_FONT = new Font("SansSerif", Font.PLAIN, 10);
    
    // Standardgrößen
    public static final Dimension DEFAULT_CHART_SIZE = new Dimension(665, 240);
    public static final Dimension DURATION_CHART_SIZE = new Dimension(665, 500);
    public static final Dimension CURRENCY_PAIR_CHART_SIZE = new Dimension(665, 920);
    public static final Dimension DB_DIALOG_SIZE = new Dimension(500, 300);
    
    // Abstände
    public static final int PANEL_SPACING = 15;
    public static final int CHART_PADDING = 8;
    
    // URLs
    public static final String SIGNAL_PROVIDER_URL_FORMAT = 
            "https://www.mql5.com/de/signals/%s?source=Site+Signals+Subscriptions#!tab=account";
    
    // Rahmen
    public static Border createPrimaryBorder() {
        return new LineBorder(PRIMARY_COLOR, 1);
    }
    
    public static Border createSecondaryBorder() {
        return new LineBorder(SECONDARY_COLOR, 1);
    }
    
    public static Border createButtonBorder() {
        return new CompoundBorder(
            new LineBorder(new Color(50, 90, 150), 1),
            new EmptyBorder(5, 10, 5, 10)
        );
    }
    
    // UI-Komponenten
    public static JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(SECONDARY_COLOR);
        button.setForeground(Color.WHITE);
        button.setFont(BOLD_FONT);
        button.setFocusPainted(false);
        button.setBorder(new CompoundBorder(
            new LineBorder(new Color(50, 90, 150), 1),
            new EmptyBorder(5, 10, 5, 10)
        ));
        return button;
    }
    
    public static JTextField createStyledTextField(int columns) {
        JTextField textField = new JTextField(columns);
        textField.setBackground(Color.WHITE);
        textField.setForeground(TEXT_COLOR);
        textField.setBorder(new CompoundBorder(
            new LineBorder(SECONDARY_COLOR, 1),
            new EmptyBorder(4, 6, 4, 6)
        ));
        return textField;
    }
    
    public static JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_COLOR);
        label.setFont(REGULAR_FONT);
        return label;
    }
    
    public static JLabel createTitleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(TITLE_FONT);
        label.setForeground(PRIMARY_COLOR);
        return label;
    }
    
    public static JLabel createSubtitleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(SUBTITLE_FONT);
        label.setForeground(SECONDARY_COLOR);
        return label;
    }
    
    public static JLabel createRegularLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(REGULAR_FONT);
        label.setForeground(TEXT_COLOR);
        return label;
    }
    
    /**
     * Initialisiert globale UI-Einstellungen für die gesamte Anwendung
     */
    public static void setUpUIDefaults() {
        // Globale UI-Einstellungen
        UIManager.put("Panel.background", BG_COLOR);
        UIManager.put("OptionPane.background", BG_COLOR);
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("TextField.foreground", TEXT_COLOR);
        UIManager.put("TextField.caretForeground", PRIMARY_COLOR);
        UIManager.put("Button.background", SECONDARY_COLOR);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.font", BOLD_FONT);
        UIManager.put("Label.foreground", TEXT_COLOR);
        UIManager.put("MenuBar.background", PRIMARY_COLOR);
        UIManager.put("MenuBar.foreground", Color.WHITE);
        UIManager.put("Menu.background", PRIMARY_COLOR);
        UIManager.put("Menu.foreground", Color.WHITE);
        UIManager.put("Menu.selectionBackground", SECONDARY_COLOR);
        UIManager.put("Menu.selectionForeground", Color.WHITE);
        UIManager.put("MenuItem.background", BG_COLOR);
        UIManager.put("MenuItem.foreground", TEXT_COLOR);
        UIManager.put("MenuItem.selectionBackground", SECONDARY_COLOR);
        UIManager.put("MenuItem.selectionForeground", Color.WHITE);
        UIManager.put("Table.background", Color.WHITE);
        UIManager.put("Table.foreground", TEXT_COLOR);
        UIManager.put("Table.selectionBackground", SECONDARY_COLOR);
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("Table.gridColor", new Color(230, 230, 230));
        UIManager.put("ScrollPane.background", BG_COLOR);
        UIManager.put("ToolBar.background", PRIMARY_COLOR);
        UIManager.put("ToolBar.foreground", Color.WHITE);
    }
}