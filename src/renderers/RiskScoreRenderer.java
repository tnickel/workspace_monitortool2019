package renderers;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Renderer für die Risiko-Score-Spalte mit farblichen Abstufungen
 */
public class RiskScoreRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1L;
    
    private static final Color LOW_RISK_COLOR = new Color(150, 255, 150); // Hellgrün
    private static final Color MEDIUM_RISK_COLOR = new Color(255, 255, 150); // Hellgelb
    private static final Color HIGH_RISK_COLOR = new Color(255, 200, 150); // Hellorange
    private static final Color VERY_HIGH_RISK_COLOR = new Color(255, 150, 150); // Hellrot
    
    private final HighlightRenderer baseRenderer;
    
    /**
     * Konstruktor ohne Parameter
     */
    public RiskScoreRenderer() {
        this.baseRenderer = null;
        setHorizontalAlignment(SwingConstants.CENTER);
    }
    
    /**
     * Konstruktor mit HighlightRenderer
     * @param baseRenderer Der HighlightRenderer für Hintergrundfarben
     */
    public RiskScoreRenderer(HighlightRenderer baseRenderer) {
        this.baseRenderer = baseRenderer;
        setHorizontalAlignment(SwingConstants.CENTER);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                  boolean isSelected, boolean hasFocus,
                                                  int row, int column) {
        // Wenn ein Basis-Renderer vorhanden ist, erst diesen für Hintergrundfarben aufrufen
        Component comp;
        if (baseRenderer != null) {
            comp = baseRenderer.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
        } else {
            comp = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
        }
        
        // Dann unsere eigene Formatierung anwenden
        if (comp instanceof JLabel && value instanceof Number && !isSelected) {
            JLabel label = (JLabel) comp;
            int riskScore = ((Number) value).intValue();
            
            // Risiko-Farbe basierend auf dem Score setzen
            if (riskScore <= 5) {
                label.setBackground(LOW_RISK_COLOR);
            } else if (riskScore <= 10) {
                label.setBackground(MEDIUM_RISK_COLOR);
            } else if (riskScore <= 15) {
                label.setBackground(HIGH_RISK_COLOR);
            } else {
                label.setBackground(VERY_HIGH_RISK_COLOR);
            }
        }
        
        return comp;
    }
    
    /**
     * Gibt den zugrundeliegenden HighlightRenderer zurück
     * @return Der HighlightRenderer oder null, wenn keiner verwendet wird
     */
    public HighlightRenderer getBaseRenderer() {
        return this.baseRenderer;
    }
}