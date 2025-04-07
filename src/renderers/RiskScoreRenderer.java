package renderers;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

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
                    c.setBackground(new Color(255, 200, 200)); // Hellrot f�r hohes Risiko
                } else if (riskScore <= 3) {
                    c.setBackground(new Color(200, 255, 200)); // Hellgr�n f�r niedriges Risiko
                } else {
                    c.setBackground(new Color(255, 255, 200)); // Hellgelb f�r mittleres Risiko
                }
            }
            
            // Tooltip mit Risiko-Erkl�rung
            StringBuilder tooltip = new StringBuilder("<html>");
            tooltip.append("<b>Risiko-Score Erkl�rung:</b><br><br>");
            tooltip.append("Score: ").append(String.format("%.1f", riskScore)).append("<br><br>");
            
            if (riskScore >= 8) {
                tooltip.append("<b>Hohes Risiko:</b><br>");
                tooltip.append("� Hohe Drawdowns<br>");
                tooltip.append("� Gro�e Positionsgr��en<br>");
                tooltip.append("� Wenig konsistente Performance<br>");
                tooltip.append("� Erh�htes Verlustrisiko");
            } else if (riskScore <= 3) {
                tooltip.append("<b>Niedriges Risiko:</b><br>");
                tooltip.append("� Moderate Drawdowns<br>");
                tooltip.append("� Angemessene Positionsgr��en<br>");
                tooltip.append("� Konsistente Performance<br>");
                tooltip.append("� Gutes Risikomanagement");
            } else {
                tooltip.append("<b>Mittleres Risiko:</b><br>");
                tooltip.append("� Durchschnittliche Drawdowns<br>");
                tooltip.append("� Normale Positionsgr��en<br>");
                tooltip.append("� Teilweise schwankende Performance<br>");
                tooltip.append("� Ausgewogenes Risiko/Rendite-Verh�ltnis");
            }
            tooltip.append("</html>");
            
            setToolTipText(tooltip.toString());
        }
        
        return c;
    }
}