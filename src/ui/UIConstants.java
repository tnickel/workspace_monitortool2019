package ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Zentrale Definition für UI-Konstanten, die in der gesamten Anwendung verwendet werden
 */
public class UIConstants {
    // Farben
    public static final Color PRIMARY_COLOR = new Color(26, 45, 90);      // #1A2D5A - Dunkelblau
    public static final Color SECONDARY_COLOR = new Color(62, 125, 204);  // #3E7DCC - Helleres Blau
    public static final Color ACCENT_COLOR = new Color(255, 209, 102);    // #FFD166 - Gold/Gelb
    public static final Color BG_COLOR = new Color(245, 247, 250);        // #F5F7FA - Sehr helles Grau
    public static final Color TEXT_COLOR = new Color(51, 51, 51);         // #333333 - Dunkelgrau
    public static final Color TEXT_SECONDARY_COLOR = new Color(85, 85, 85);// #555555 - Helleres Grau
    public static final Color POSITIVE_COLOR = new Color(46, 139, 87);    // #2E8B57 - Grün
    public static final Color NEGATIVE_COLOR = new Color(204, 59, 59);    // #CC3B3B - Rot
    
    // Schriftarten
    public static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 18);
    public static final Font SUBTITLE_FONT = new Font("SansSerif", Font.BOLD, 14);
    public static final Font REGULAR_FONT = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font BOLD_FONT = new Font("SansSerif", Font.BOLD, 12);
    public static final Font BOLD_LARGE_FONT = new Font("SansSerif", Font.BOLD, 13);
    public static final Font ITALIC_FONT = new Font("SansSerif", Font.ITALIC, 14);
    
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
}