package renderers;



import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.regex.Pattern;

public class HighlightRenderer extends DefaultTableCellRenderer {
    private String searchText = "";
    private final Color highlightColor = new Color(255, 255, 0, 128);

    public void setSearchText(String text) {
        this.searchText = text.toLowerCase();
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        if (!searchText.isEmpty() && value != null) {
            String text = value.toString();
            if (text.toLowerCase().contains(searchText)) {
                if (!isSelected) {
                    c.setBackground(highlightColor);
                }
                String highlightedText = text.replaceAll("(?i)(" + Pattern.quote(searchText) + ")",
                        "<span style='background-color: #FFFF00'>$1</span>");
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
            ((JLabel)c).setText(value != null ? value.toString() : "");
        }
        return c;
    }
}