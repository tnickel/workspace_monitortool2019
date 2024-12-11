package renderers;



import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.JTable;
import java.awt.Component;
import java.awt.Color;
import services.RiskAnalysisServ;

public class RiskScoreRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value,
            boolean isSelected, boolean hasFocus,
            int row, int column) {
        
        Component c = super.getTableCellRendererComponent(
            table, value, isSelected, hasFocus, row, column);
            
        if (value instanceof Number) {
            int score = ((Number) value).intValue();
            String category = RiskAnalysisServ.getRiskCategory(score);
            
            setText(score + " (" + category + ")");
            
            if (!isSelected) {
                if (score <= 20) {
                    setForeground(new Color(0, 100, 0));  // Dunkelgrün
                } else if (score <= 40) {
                    setForeground(new Color(0, 150, 0));  // Grün
                } else if (score <= 60) {
                    setForeground(new Color(180, 180, 0)); // Gelb
                } else if (score <= 80) {
                    setForeground(new Color(200, 100, 0)); // Orange
                } else {
                    setForeground(new Color(200, 0, 0));   // Rot
                }
            }
        }
        
        return c;
    }
}