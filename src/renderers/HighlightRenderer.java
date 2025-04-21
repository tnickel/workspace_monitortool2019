package renderers;

import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import data.FavoritesManager;
import models.HighlightTableModel;

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
            
            if (table.getModel() instanceof HighlightTableModel) {
                HighlightTableModel model = (HighlightTableModel)table.getModel();
                String providerName = (String) table.getValueAt(row, 1);
                String columnName = table.getColumnName(column);
                
                if (providerName != null && model.getHtmlDatabase() != null) {
                    // Tooltips für verschiedene Spalten basierend auf Spaltennamen
                    switch (columnName) {
                        case "3MPDD":
                            setToolTipText(model.getHtmlDatabase().getMPDDTooltip(providerName, 3));
                            break;
                        case "6MPDD":
                            setToolTipText(model.getHtmlDatabase().getMPDDTooltip(providerName, 6));
                            break;
                        case "9MPDD":
                            setToolTipText(model.getHtmlDatabase().getMPDDTooltip(providerName, 9));
                            break;
                        case "12MPDD":
                            setToolTipText(model.getHtmlDatabase().getMPDDTooltip(providerName, 12));
                            break;
                        case "Stabilitaet":
                            String stabilityDetails = model.getHtmlDatabase().getStabilitaetswertDetails(providerName);
                            setToolTipText(stabilityDetails); // Der HTML-Tag ist bereits in getStabilitaetswertDetails enthalten
                            break;
                        case "3MProfProz":
                            List<String> profits = model.getHtmlDatabase().getLastThreeMonthsDetails(providerName);
                            if (!profits.isEmpty()) {
                                double sum = profits.stream()
                                    .mapToDouble(s -> {
                                        String valueStr = s.split(":")[1].trim()
                                                         .replace("%", "")
                                                         .replace(",", ".");
                                        return Double.parseDouble(valueStr);
                                    })
                                    .sum();
                                
                                StringBuilder tooltip = new StringBuilder("<html><b>3-Monats Profit Berechnung:</b><br><br>");
                                profits.forEach(profit -> tooltip.append(profit).append("<br>"));
                                tooltip.append("<br>Summe: ").append(String.format("%.2f%%", sum))
                                       .append("<br>Durchschnitt: ").append(String.format("%.2f%%", sum / profits.size()))
                                       .append(" (").append(profits.size()).append(" Monate)")
                                       .append("</html>");
                                
                                setToolTipText(tooltip.toString());
                            } else {
                                setToolTipText("Keine Profitdaten verfügbar");
                            }
                            break;
                        case "Steigung":
                            StringBuilder tooltipBuilder = new StringBuilder("<html><div style='width:400px;'>");
                            tooltipBuilder.append("<b>Steigungsberechnung:</b><br><br>");
                            tooltipBuilder.append("Die Steigung bewertet den Trend der letzten Monate wie folgt:<br><br>");
                            
                            tooltipBuilder.append("<b>Bei mindestens 3 Monaten:</b><br>");
                            tooltipBuilder.append("• slope1 = profit2 - profit1 (Veränderung zwischen ältestem und mittlerem Monat)<br>");
                            tooltipBuilder.append("• slope2 = profit3 - profit2 (Veränderung zwischen mittlerem und neuestem Monat)<br>");
                            tooltipBuilder.append("• Beide Steigungen positiv: steigung = (slope1 + slope2) / 2.0<br>");
                            tooltipBuilder.append("• Nur eine Steigung positiv: steigung = Math.max(slope1, slope2) / 4.0<br>");
                            tooltipBuilder.append("• Beide Steigungen negativ: steigung = (slope1 + slope2) / 2.0<br><br>");
                            
                            tooltipBuilder.append("<b>Bei nur 2 Monaten:</b><br>");
                            tooltipBuilder.append("• slope = profit2 - profit1 (Veränderung zwischen älterem und neuerem Monat)<br>");
                            tooltipBuilder.append("• steigung = slope * 0.8<br><br>");
                            
                            tooltipBuilder.append("<b>Bei nur 1 Monat:</b><br>");
                            tooltipBuilder.append("• steigung = profit * 0.2<br><br>");
                            
                            tooltipBuilder.append("<i>Ein hoher positiver Wert zeigt einen starken positiven Trend an,<br>");
                            tooltipBuilder.append("während ein negativer Wert auf einen abnehmenden Trend hinweist.</i>");
                            tooltipBuilder.append("</div></html>");
                            
                            setToolTipText(tooltipBuilder.toString());
                            break;
                        default:
                            setToolTipText(null);
                            break;
                    }
                }
            }
            
            // Farbliche Hervorhebung für MPDD-Spalten
            String columnName = table.getColumnName(column);
            if ((columnName.equals("3MPDD") || columnName.equals("6MPDD") || 
                 columnName.equals("9MPDD") || columnName.equals("12MPDD")) && !isSelected) {
                double mpddValue = (Double)value;
                if (mpddValue > 1.0) {
                    c.setBackground(new Color(200, 255, 200)); // Hellgrün für gute Werte
                } else if (mpddValue < 0.5) {
                    c.setBackground(new Color(255, 200, 200)); // Hellrot für schlechte Werte
                } else {
                    c.setBackground(new Color(255, 255, 200)); // Hellgelb für mittlere Werte
                }
            }
            
            // Farbliche Hervorhebung für Steigung
            if (columnName.equals("Steigung") && !isSelected) {
                double steigungValue = (Double)value;
                if (steigungValue > 5.0) {
                    c.setBackground(new Color(150, 255, 150)); // Kräftiges Grün für starken positiven Trend
                } else if (steigungValue > 2.0) {
                    c.setBackground(new Color(200, 255, 200)); // Hellgrün für positiven Trend
                } else if (steigungValue < -2.0) {
                    c.setBackground(new Color(255, 200, 200)); // Hellrot für negativen Trend
                } else if (steigungValue < -5.0) {
                    c.setBackground(new Color(255, 150, 150)); // Kräftiges Rot für starken negativen Trend
                } else {
                    c.setBackground(new Color(255, 255, 200)); // Hellgelb für neutralen Trend
                }
            }
        }
        
        // Prüfe, ob Provider ein Bad Provider ist
        if (table.getModel() instanceof HighlightTableModel) {
            HighlightTableModel model = (HighlightTableModel) table.getModel();
            
            // Extrahiere providerId aus dem Providernamen in Spalte 1
            String providerName = (String) table.getValueAt(row, 1);
            if (providerName != null) {
                try {
                    // Extrahiere providerId
                    String providerId = null;
                    if (providerName.contains("_")) {
                        providerId = providerName.substring(providerName.lastIndexOf("_") + 1).replace(".csv", "");
                    } else {
                        // Fallback für den Fall, dass der Name nicht dem erwarteten Format entspricht
                        StringBuilder digits = new StringBuilder();
                        for (char ch : providerName.toCharArray()) {
                            if (Character.isDigit(ch)) {
                                digits.append(ch);
                            }
                        }
                        if (digits.length() > 0) {
                            providerId = digits.toString();
                        }
                    }
                    
                    if (providerId != null) {
                        // Hole Root-Pfad aus dem Model
                        String rootPath = model.getRootPath();
                        
                        // Initialisiere FavoritesManager mit dem Pfad
                        FavoritesManager favoritesManager = new FavoritesManager(rootPath);
                        
                        // Wenn es ein Bad Provider ist, graue die Zeile aus
                        if (favoritesManager.isBadProvider(providerId) && !isSelected) {
                            // Grauer Hintergrund und heller Text für Bad Provider
                            c.setBackground(new Color(230, 230, 230)); // Hellgrau
                            c.setForeground(new Color(150, 150, 150)); // Dunkelgrau
                        }
                    }
                } catch (Exception e) {
                    // Fehlerbehandlung
                    System.err.println("Fehler bei der Prüfung auf Bad Provider: " + e.getMessage());
                }
            }
        }
        
        // Suchtext-Highlighting (nicht verändern)
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
                // Nur wenn es sich nicht um einen Bad Provider handelt (damit die Ausgrauung nicht überschrieben wird)
                if (!isSelected && c.getBackground().equals(table.getBackground())) {
                    c.setBackground(table.getBackground());
                }
            }
        }
        
        // Währungspaar-Tooltip für die Signal Provider Spalte
        if (column == 1 && value != null && table.getModel() instanceof HighlightTableModel) {
            HighlightTableModel model = (HighlightTableModel)table.getModel();
            String providerName = (String) value;
            
            try {
                // Versuche, die ProviderStats über DataManager zu bekommen
                data.ProviderStats stats = data.DataManager.getInstance().getStats().get(providerName);
                if (stats != null) {
                    // Erstelle Tooltip für Währungspaare
                    String currencyPairsTooltip = model.buildCurrencyPairsTooltip(stats);
                    setToolTipText(currencyPairsTooltip);
                }
            } catch (Exception e) {
                // Falls DataManager.getInstance() nicht funktioniert, ignorieren wir den Tooltip
                System.err.println("Konnte Währungspaar-Tooltip nicht erstellen: " + e.getMessage());
            }
        }
        
        return c;
    }
}