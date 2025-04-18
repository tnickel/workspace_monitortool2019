package components;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

import utils.UIStyleManager;

/**
 * Ein Panel mit Farbverlauf-Hintergrund
 */
public class GradientPanel extends JPanel {
    private Color startColor;
    private Color endColor;
    private boolean isVertical;
    
    /**
     * Erstellt ein neues Panel mit Standard-Farbverlauf (vertikal)
     */
    public GradientPanel() {
        this(UIStyleManager.BG_COLOR, new Color(230, 238, 245), true);
    }
    
    /**
     * Erstellt ein neues Panel mit angegebenem Farbverlauf
     * 
     * @param startColor Startfarbe des Verlaufs
     * @param endColor Endfarbe des Verlaufs
     * @param isVertical true für vertikalen, false für horizontalen Verlauf
     */
    public GradientPanel(Color startColor, Color endColor, boolean isVertical) {
        this.startColor = startColor;
        this.endColor = endColor;
        this.isVertical = isVertical;
        setOpaque(false);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        GradientPaint gradient;
        if (isVertical) {
            gradient = new GradientPaint(
                0, 0, startColor, 
                0, getHeight(), endColor
            );
        } else {
            gradient = new GradientPaint(
                0, 0, startColor, 
                getWidth(), 0, endColor
            );
        }
        
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());
    }
}