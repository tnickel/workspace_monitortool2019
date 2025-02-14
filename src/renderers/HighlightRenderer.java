package renderers;

import models.HighlightTableModel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.regex.Pattern;

public class HighlightRenderer extends DefaultTableCellRenderer {
    private String searchText = "";
    private final Color highlightColor = new Color(255, 255, 0, 128);
    private final DecimalFormat df = new DecimalFormat("#,##0.00");
    
    public void setSearchText(String text) {
        this.searchText = text.toLowerCase();
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        // Formatiere Dezimalzahlen
        if (value instanceof Double) {
            setText(df.format((Double)value));
            
            // Wenn es die Stabilitätsspalte ist (Index 20)
            if (column == 20 && table.getModel() instanceof HighlightTableModel) {
                HighlightTableModel model = (HighlightTableModel)table.getModel();
                String providerName = (String) table.getValueAt(row, 1);
                if (providerName != null && model.getHtmlDatabase() != null) {
                    String stabilityDetails = model.getHtmlDatabase().getStabilitaetswertDetails(providerName);
                    setToolTipText("<html>" + stabilityDetails + "</html>");
                }
            }
        }
        
        // Suchtext-Highlighting
        if (!searchText.isEmpty() && value != null) {
            String text = value.toString().toLowerCase();
            if (text.contains(searchText)) {
                if (!isSelected) {
                    c.setBackground(highlightColor);
                }
                String highlightedText = value.toString().replaceAll(
                    "(?i)(" + Pattern.quote(searchText) + ")",
                    "<span style='background-color: #FFFF00'>$1</span>"
                );
                ((JLabel)c).setText("<html>" + highlightedText + "</html>");
            } else {
                if (!isSelected) {
                    c.setBackground(table.getBackground());
                }
            }
        } else {
            if (!isSelected) {
                c.setBackground(table.getBackground());
            }
        }
        
        return c;
    }
}