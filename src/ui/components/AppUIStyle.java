package ui.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;

/**
 * Zentrale Klasse für UI-Styling der Anwendung
 */
public class AppUIStyle {
    // Farben
    public static final Color PRIMARY_COLOR = new Color(30, 70, 130); // Dunkelblau
    public static final Color SECONDARY_COLOR = new Color(60, 100, 170); // Mittelblau
    public static final Color ACCENT_COLOR = new Color(0, 150, 220); // Akzentfarbe für Hervorhebungen
    
    // Hellere Farbe für die Filter-Buttons
    public static final Color BUTTON_COLOR = new Color(90, 130, 210); // Helleres Blau für die Buttons
    
    // Status-Farben (NEU)
    public static final Color SUCCESS_COLOR = new Color(0, 150, 0); // Grün für Erfolg
    public static final Color ERROR_COLOR = new Color(180, 0, 0); // Rot für Fehler
    public static final Color WARNING_COLOR = new Color(255, 140, 0); // Orange für Warnungen
    
    // Weitere Farben
    public static final Color TEXT_COLOR = new Color(50, 50, 50);
    public static final Color TEXT_FIELD_BG_COLOR = new Color(250, 250, 252);
    public static final Color HIGHLIGHT_COLOR = new Color(255, 240, 200);
    
    // Schriftarten
    public static final Font REGULAR_FONT = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font BOLD_FONT = new Font("SansSerif", Font.BOLD, 12);
    public static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 16);
    public static final Font SUBTITLE_FONT = new Font("SansSerif", Font.BOLD, 14);
    
    /**
     * Setzt die UI-Defaults für die Anwendung
     */
    public static void setUpUIDefaults() {
        try {
            // UI-Defaults setzen
            javax.swing.UIManager.put("Button.background", BUTTON_COLOR);
            javax.swing.UIManager.put("Button.foreground", Color.WHITE);
            javax.swing.UIManager.put("Panel.background", new Color(240, 245, 250));
            javax.swing.UIManager.put("Label.foreground", TEXT_COLOR);
            javax.swing.UIManager.put("TextField.background", TEXT_FIELD_BG_COLOR);
            javax.swing.UIManager.put("TextField.foreground", TEXT_COLOR);
            javax.swing.UIManager.put("TextField.caretForeground", TEXT_COLOR);
            javax.swing.UIManager.put("ComboBox.background", TEXT_FIELD_BG_COLOR);
            javax.swing.UIManager.put("ComboBox.foreground", TEXT_COLOR);
            
            // Weitere UI-Einstellungen nach Bedarf
        } catch (Exception e) {
            System.err.println("Fehler beim Setzen der UI-Defaults: " + e.getMessage());
        }
    }
    
    /**
     * Erstellt einen Button mit einheitlichem Styling
     * Button-Farbe ist jetzt heller für bessere Sichtbarkeit
     * 
     * @param text Der Text für den Button
     * @return Ein gestylter JButton
     */
    public static JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(BUTTON_COLOR); // Hellere Farbe für bessere Sichtbarkeit
        button.setForeground(Color.WHITE);
        button.setFont(BOLD_FONT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 100, 180), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        // Sicherstellen, dass die Hintergrundfarbe angezeigt wird
        button.setOpaque(true);
        
        return button;
    }
    
    /**
     * Erstellt ein Textfeld mit einheitlichem Styling
     * 
     * @param columns Die Anzahl der Spalten
     * @return Ein gestyltes JTextField
     */
    public static JTextField createStyledTextField(int columns) {
        JTextField textField = new JTextField(columns);
        textField.setBackground(TEXT_FIELD_BG_COLOR);
        textField.setForeground(TEXT_COLOR);
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SECONDARY_COLOR, 1),
            BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));
        return textField;
    }
    
    /**
     * Erstellt ein Label mit einheitlichem Styling
     * 
     * @param text Der Text für das Label
     * @return Ein gestyltes JLabel
     */
    public static JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(REGULAR_FONT);
        label.setForeground(TEXT_COLOR);
        return label;
    }
    
    /**
     * Erstellt ein Status-Label mit Farbe für Erfolg (NEU)
     * 
     * @param text Der Text für das Label
     * @return Ein gestyltes JLabel mit Erfolgsfarbe
     */
    public static JLabel createSuccessLabel(String text) {
        JLabel label = createStyledLabel(text);
        label.setForeground(SUCCESS_COLOR);
        return label;
    }
    
    /**
     * Erstellt ein Status-Label mit Farbe für Fehler (NEU)
     * 
     * @param text Der Text für das Label
     * @return Ein gestyltes JLabel mit Fehlerfarbe
     */
    public static JLabel createErrorLabel(String text) {
        JLabel label = createStyledLabel(text);
        label.setForeground(ERROR_COLOR);
        return label;
    }
    
    /**
     * Erstellt ein Status-Label mit Farbe für Warnungen (NEU)
     * 
     * @param text Der Text für das Label
     * @return Ein gestyltes JLabel mit Warnfarbe
     */
    public static JLabel createWarningLabel(String text) {
        JLabel label = createStyledLabel(text);
        label.setForeground(WARNING_COLOR);
        return label;
    }
    
    /**
     * Erstellt ein Panel für Reports mit blauem Hintergrund
     * 
     * @return Ein gestyltes JPanel
     */
    public static JPanel createReportPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                
                int w = getWidth();
                int h = getHeight();
                
                // Hellerer Farbverlauf für das Report-Panel
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(80, 120, 200), 
                    0, h, new Color(110, 150, 220)
                );
                
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, w, h);
            }
        };
        
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SECONDARY_COLOR, 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        return panel;
    }
    
    /**
     * Wendet Styling auf eine ComboBox an (FEHLENDE METHODE)
     * 
     * @param comboBox Die zu stylende JComboBox
     */
    public static void applyStylesToComboBox(javax.swing.JComboBox<?> comboBox) {
        comboBox.setBackground(TEXT_FIELD_BG_COLOR);
        comboBox.setForeground(TEXT_COLOR);
        comboBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SECONDARY_COLOR, 1),
            BorderFactory.createEmptyBorder(2, 4, 2, 4)
        ));
        comboBox.setFont(REGULAR_FONT);
    }
    
    /**
     * Gibt ein Border für Panels zurück
     * 
     * @return Ein Border für Panels
     */
    public static Border getPanelBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SECONDARY_COLOR, 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        );
    }
}