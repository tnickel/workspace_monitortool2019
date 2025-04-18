package renderers;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import utils.UIStyleManager;

/**
 * Ein verbesserter Renderer für Tabellen-Kopfzeilen mit modernem Erscheinungsbild
 */
public class StyledTableHeaderRenderer extends DefaultTableCellRenderer {
    
    public StyledTableHeaderRenderer() {
        setHorizontalAlignment(JLabel.CENTER);
        setFont(UIStyleManager.SUBTITLE_FONT);
        setBackground(UIStyleManager.PRIMARY_COLOR);
        setForeground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 1, UIStyleManager.SECONDARY_COLOR),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        // Füge Tooltip hinzu, der den vollständigen Spaltennamen anzeigt
        if (value != null) {
            setToolTipText(value.toString());
        }
        
        // Text zentriert anzeigen
        setHorizontalAlignment(SwingConstants.CENTER);
        
        return c;
    }
}