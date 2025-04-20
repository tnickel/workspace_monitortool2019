package ui.components;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import ui.UIConstants;

/**
 * Factory-Klasse für einheitliche UI-Komponenten
 */
public class UIComponentFactory {
    
    /**
     * Erstellt einen Button mit einheitlichem Styling
     * 
     * @param text Der Text für den Button
     * @return Ein gestylter JButton
     */
    public static JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(UIConstants.SECONDARY_COLOR);
        button.setForeground(Color.WHITE);
        button.setFont(UIConstants.BOLD_FONT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 90, 150), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
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
        textField.setBackground(Color.WHITE);
        textField.setForeground(UIConstants.TEXT_COLOR);
        textField.setBorder(new CompoundBorder(
            new LineBorder(UIConstants.SECONDARY_COLOR, 1),
            new EmptyBorder(4, 6, 4, 6)
        ));
        return textField;
    }
    
    /**
     * Erstellt ein Label mit Titel-Styling
     * 
     * @param text Der Text für das Label
     * @return Ein gestyltes JLabel mit Titel-Formatierung
     */
    public static JLabel createTitleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UIConstants.TITLE_FONT);
        label.setForeground(UIConstants.PRIMARY_COLOR);
        return label;
    }
    
    /**
     * Erstellt ein Label mit Untertitel-Styling
     * 
     * @param text Der Text für das Label
     * @return Ein gestyltes JLabel mit Untertitel-Formatierung
     */
    public static JLabel createSubtitleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UIConstants.SUBTITLE_FONT);
        label.setForeground(UIConstants.SECONDARY_COLOR);
        return label;
    }
    
    /**
     * Erstellt ein normales Label mit Standardformatierung
     * 
     * @param text Der Text für das Label
     * @return Ein gestyltes JLabel mit Standardformatierung
     */
    public static JLabel createRegularLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UIConstants.REGULAR_FONT);
        label.setForeground(UIConstants.TEXT_COLOR);
        return label;
    }
}