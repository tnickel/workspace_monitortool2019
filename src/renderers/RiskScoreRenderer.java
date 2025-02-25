package renderers;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class RiskScoreRenderer extends DefaultTableCellRenderer {
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        if (value instanceof Integer || value instanceof Double) {
            double riskScore = value instanceof Integer ? (Integer)value : (Double)value;
            
            // Setze Farbe basierend auf Risiko-Score
            if (!isSelected) {
                if (riskScore >= 8) {
                    c.setBackground(new Color(255, 200, 200)); // Hellrot für hohes Risiko
                } else if (riskScore <= 3) {
                    c.setBackground(new Color(200, 255, 200)); // Hellgrün für niedriges Risiko
                } else {
                    c.setBackground(new Color(255, 255, 200)); // Hellgelb für mittleres Risiko
                }
            }
            
            // Tooltip mit Risiko-Erklärung
            StringBuilder tooltip = new StringBuilder("<html>");
            tooltip.append("<b>Risiko-Score Erklärung:</b><br><br>");
            tooltip.append("Score: ").append(String.format("%.1f", riskScore)).append("<br><br>");
            
            if (riskScore >= 8) {
                tooltip.append("<b>Hohes Risiko:</b><br>");
                tooltip.append("• Hohe Drawdowns<br>");
                tooltip.append("• Große Positionsgrößen<br>");
                tooltip.append("• Wenig konsistente Performance<br>");
                tooltip.append("• Erhöhtes Verlustrisiko");
            } else if (riskScore <= 3) {
                tooltip.append("<b>Niedriges Risiko:</b><br>");
                tooltip.append("• Moderate Drawdowns<br>");
                tooltip.append("• Angemessene Positionsgrößen<br>");
                tooltip.append("• Konsistente Performance<br>");
                tooltip.append("• Gutes Risikomanagement");
            } else {
                tooltip.append("<b>Mittleres Risiko:</b><br>");
                tooltip.append("• Durchschnittliche Drawdowns<br>");
                tooltip.append("• Normale Positionsgrößen<br>");
                tooltip.append("• Teilweise schwankende Performance<br>");
                tooltip.append("• Ausgewogenes Risiko/Rendite-Verhältnis");
            }
            tooltip.append("</html>");
            
            setToolTipText(tooltip.toString());
        }
        
        return c;
    }
}