package renderers;

import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import utils.UIStyleManager;

/**
 * Ein verbesserter Table Cell Renderer mit einheitlichem Styling und Hervorhebungen
 */
public class StyledTableCellRenderer extends DefaultTableCellRenderer {
    private String searchText = "";
    private final DecimalFormat df = new DecimalFormat("#,##0.00");
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        // Grundformatierung
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(240, 240, 240)),
            BorderFactory.createEmptyBorder(3, 5, 3, 5)
        ));
        
        // Formatiere Dezimalzahlen
        if (value instanceof Double) {
            setText(df.format((Double)value));
            setHorizontalAlignment(SwingConstants.RIGHT);
            
            double numValue = (Double)value;
            if (!isSelected) {
                if (numValue > 0) {
                    c.setForeground(UIStyleManager.POSITIVE_COLOR);
                } else if (numValue < 0) {
                    c.setForeground(UIStyleManager.NEGATIVE_COLOR);
                } else {
                    c.setForeground(UIStyleManager.TEXT_COLOR);
                }
            }
        } else {
            setHorizontalAlignment(SwingConstants.LEFT);
        }
        
        // Zeilen-Styling
        if (isSelected) {
            c.setBackground(UIStyleManager.SECONDARY_COLOR);
            c.setForeground(Color.WHITE);
        } else {
            // Zeilen abwechselnd einfärben
            if (row % 2 == 0) {
                c.setBackground(Color.WHITE);
            } else {
                c.setBackground(new Color(245, 247, 250));
            }
            
            // Nur Textfarbe setzen, wenn es nicht bereits für Zahlen gesetzt wurde
            if (!(value instanceof Double)) {
                c.setForeground(UIStyleManager.TEXT_COLOR);
            }
        }
        
        // Suchtext-Highlighting
        if (!searchText.isEmpty() && value != null) {
            String text = value.toString().toLowerCase();
            if (text.contains(searchText.toLowerCase())) {
                if (!isSelected) {
                    c.setBackground(new Color(255, 255, 0, 80));
                }
                
                // Hervorhebung des gefundenen Texts mit HTML
                String htmlText = value.toString().replaceAll(
                    "(?i)(" + java.util.regex.Pattern.quote(searchText) + ")",
                    "<span style='background-color: #FFFF00'>$1</span>"
                );
                ((JLabel)c).setText("<html>" + htmlText + "</html>");
            }
        }
        
        return c;
    }
    
    /**
     * Setzt den Suchtext für die Hervorhebung
     * 
     * @param text Der zu suchende Text
     */
    public void setSearchText(String text) {
        this.searchText = text;
    }
}