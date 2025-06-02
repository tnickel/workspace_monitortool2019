package renderers;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;

/**
 * Einfacher Renderer für die Risiko-Spalte - nutzt HighlightRenderer für Farben
 */
public class RisikoRenderer extends HighlightRenderer {
    private static final long serialVersionUID = 1L;
    
    /**
     * Konstruktor
     */
    public RisikoRenderer(HighlightRenderer baseRenderer) {
        // Kopiere die wichtigen Eigenschaften vom baseRenderer
        super();
        setHorizontalAlignment(SwingConstants.CENTER);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                  boolean isSelected, boolean hasFocus,
                                                  int row, int column) {
        
        // Rufe den HighlightRenderer auf - der macht die Farben richtig
        Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        // Nur Tooltip hinzufügen, wenn es kein "-" ist
        if (comp instanceof JLabel && value != null && !"-".equals(value.toString())) {
            try {
                int riskCategory = Integer.parseInt(value.toString());
                ((JLabel) comp).setToolTipText(getRiskTooltip(riskCategory));
            } catch (NumberFormatException e) {
                // Ignorieren, falls es keine Zahl ist
            }
        } else if (comp instanceof JLabel && "-".equals(value)) {
            ((JLabel) comp).setToolTipText(getRiskTooltip(0));
        }
        
        return comp;
    }
    
    /**
     * Erstellt einen Tooltip-Text für die Risiko-Kategorie
     * @param riskCategory Die Risiko-Kategorie (0-10)
     * @return Tooltip-Text
     */
    private String getRiskTooltip(int riskCategory) {
        switch (riskCategory) {
            case 0:
                return "<html><b>Kein Risiko gesetzt</b><br>Noch keine Risikobewertung vorgenommen</html>";
            case 1:
            case 2:
                return "<html><b>Sehr niedrig (Risiko " + riskCategory + ")</b><br>Sehr sicherer Signal Provider</html>";
            case 3:
            case 4:
                return "<html><b>Niedrig (Risiko " + riskCategory + ")</b><br>Geringes Risiko, stabile Performance</html>";
            case 5:
            case 6:
                return "<html><b>Mittel (Risiko " + riskCategory + ")</b><br>Moderates Risiko bei guter Performance</html>";
            case 7:
            case 8:
                return "<html><b>Hoch (Risiko " + riskCategory + ")</b><br>Erhöhtes Risiko, höhere Volatilität</html>";
            case 9:
                return "<html><b>Sehr hoch (Risiko " + riskCategory + ")</b><br>Hohes Risiko, starke Schwankungen</html>";
            case 10:
                return "<html><b>Extrem hoch (Risiko " + riskCategory + ")</b><br>Maximales Risiko, sehr volatil</html>";
            default:
                return "<html><b>Unbekanntes Risiko</b><br>Ungültige Risikokategorie</html>";
        }
    }
}