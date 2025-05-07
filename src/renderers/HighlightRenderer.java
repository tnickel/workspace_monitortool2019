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
import db.HistoryDatabaseManager;
import models.HighlightTableModel;
import utils.ApplicationConstants;

public class HighlightRenderer extends DefaultTableCellRenderer {
    private String searchText = "";
    private final Color highlightColor = new Color(255, 255, 0, 128);
    private final DecimalFormat df = new DecimalFormat("#,##0.00");
    
    // Farben für Provider-Status
    private final Color BAD_PROVIDER_BG = new Color(230, 230, 230);     // Hellgrau
    private final Color BAD_PROVIDER_FG = new Color(150, 150, 150);     // Dunkelgrau
    private final Color FAVORITE_PROVIDER_BG = new Color(210, 230, 255); // Kräftigeres Blau, passend zum Design
    
    // Farben für EquityDrawdown3M% Hervorhebung
    private final Color HIGH_DRAWDOWN_BG = new Color(255, 220, 220);    // Helles Rot für hohe Drawdowns
    private final Color MEDIUM_DRAWDOWN_BG = new Color(255, 240, 200);  // Helles Gelb für mittlere Drawdowns
    
    public void setSearchText(String text) {
        this.searchText = text.toLowerCase();
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        // Standard-Hintergrund für alle Zellen zurücksetzen, wenn nicht selektiert
        if (!isSelected) {
            c.setBackground(table.getBackground());
            c.setForeground(table.getForeground());
        }
        
        // Setze den Tooltip standardmäßig auf null, um vorherige Tooltips zu löschen
        setToolTipText(null);
        
        // Spezialbehandlung für die No.-Spalte (erste Spalte, Index 0)
        if (column == 0 && table.getColumnName(column).equals("No.") && table.getModel() instanceof HighlightTableModel) {
            // Hole den Providernamen aus der zweiten Spalte (Spalte 1) der aktuellen Zeile
            String providerName = (String) table.getValueAt(row, 1);
            if (providerName != null) {
                try {
                    // Hole die Notizen aus der Datenbank
                    String notes = HistoryDatabaseManager.getInstance().getProviderNotes(providerName);
                    
                    // Nur Tooltip setzen, wenn Notizen vorhanden sind
                    if (notes != null && !notes.trim().isEmpty()) {
                        // Begrenze die Länge der Notizen für den Tooltip auf maximal 200 Zeichen
                        String displayNotes = notes;
                        if (notes.length() > 200) {
                            displayNotes = notes.substring(0, 197) + "...";
                        }
                        
                        // HTML-formatierter Tooltip mit den Notizen
                        StringBuilder tooltip = new StringBuilder("<html><div style='width:300px;'>");
                        tooltip.append("<b>Notizen zu ").append(providerName).append(":</b><br><br>");
                        
                        // Ersetze Zeilenumbrüche durch HTML-Zeilenumbrüche
                        displayNotes = displayNotes.replace("\n", "<br>");
                        
                        tooltip.append(displayNotes);
                        tooltip.append("</div></html>");
                        
                        setToolTipText(tooltip.toString());
                        
                        // Früher beenden, da wir den Tooltip bereits gesetzt haben
                        return c;
                    }
                } catch (Exception e) {
                    System.err.println("Fehler beim Laden der Notizen für den Tooltip: " + e.getMessage());
                }
            }
        }
        
        // Formatiere Dezimalzahlen
        if (value instanceof Double) {
            setText(df.format((Double)value));
            
            if (table.getModel() instanceof HighlightTableModel) {
                HighlightTableModel model = (HighlightTableModel)table.getModel();
                String providerName = (String) table.getValueAt(row, 1);
                String columnName = table.getColumnName(column);
                
                if (providerName != null && model.getHtmlDatabase() != null) {
                    // Spezielle Formatierung für EquityDrawdown3M%
                    if (columnName.equals("EquityDrawdown3M%")) {
                        double drawdownValue = (Double) value;
                        
                        // Farbliche Hervorhebung basierend auf Drawdown-Werten
                        if (!isSelected) {
                            if (drawdownValue >= 15.0) {
                                c.setBackground(HIGH_DRAWDOWN_BG);
                            } else if (drawdownValue >= 10.0) {
                                c.setBackground(MEDIUM_DRAWDOWN_BG);
                            }
                        }
                        
                        // Ausführlicher Tooltip mit Erklärung
                        String drawdownData = model.getHtmlDatabase().getDrawdownChartData(providerName);
                        if (drawdownData != null && !drawdownData.isEmpty()) {
                            StringBuilder tooltip = new StringBuilder("<html><div style='width:400px;'>");
                            tooltip.append("<b>Equity Drawdown (3 Monate):</b><br><br>");
                            tooltip.append("Maximaler Drawdown der letzten 3 Monate: ").append(df.format(drawdownValue)).append("%<br><br>");
                            
                            // Bewertung des Drawdown-Wertes
                            if (drawdownValue >= 15.0) {
                                tooltip.append("<span style='color:red;'><b>Hoher Drawdown:</b> Dieser Wert deutet auf ein erhöhtes Risiko hin.</span><br>");
                            } else if (drawdownValue >= 10.0) {
                                tooltip.append("<span style='color:#AA6600;'><b>Mittlerer Drawdown:</b> Dieser Wert sollte beobachtet werden.</span><br>");
                            } else if (drawdownValue >= 5.0) {
                                tooltip.append("<span style='color:#007700;'><b>Moderater Drawdown:</b> Im akzeptablen Bereich.</span><br>");
                            } else {
                                tooltip.append("<span style='color:green;'><b>Niedriger Drawdown:</b> Dieser Wert deutet auf ein gutes Risikomanagement hin.</span><br>");
                            }
                            
                            tooltip.append("<br>Basierend auf Drawdown-Daten aus der root.txt Datei.<br>");
                            tooltip.append("Nur Daten der letzten 3 Monate werden berücksichtigt.</div></html>");
                            setToolTipText(tooltip.toString());
                        } else {
                            setToolTipText("<html>Keine Drawdown-Daten verfügbar</html>");
                        }
                    }
                    
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
                            // Keine zusätzliche Aktion für andere Spalten
                            break;
                    }
                }
            }
        }
        
        // Prüfe, ob Provider ein Favorit oder Bad Provider ist
        if (table.getModel() instanceof HighlightTableModel) {
            HighlightTableModel model = (HighlightTableModel) table.getModel();
            
            // Hole den Providernamen aus der zweiten Spalte (Spalte 1) der aktuellen Zeile
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
                        // Hole Root-Pfad aus den Konstanten
                        String rootPath = ApplicationConstants.ROOT_PATH;
                        
                        // Initialisiere FavoritesManager mit dem Pfad
                        FavoritesManager favoritesManager = new FavoritesManager(rootPath);
                        
                        // Status prüfen und Highlighting anwenden
                        boolean isBadProvider = favoritesManager.isBadProvider(providerId);
                        boolean isFavorite = favoritesManager.isFavorite(providerId);
                        
                        // Wenn es ein Bad Provider ist und die Zelle nicht selektiert ist
                        if (isBadProvider && !isSelected) {
                            // Grauer Hintergrund und heller Text für Bad Provider
                            c.setBackground(BAD_PROVIDER_BG);
                            c.setForeground(BAD_PROVIDER_FG);
                        }
                        // Wenn es ein Favorit ist und die Zelle nicht selektiert ist und kein Bad Provider
                        else if (isFavorite && !isSelected && !isBadProvider) {
                            // Hellblauer Hintergrund für Favoriten
                            c.setBackground(FAVORITE_PROVIDER_BG);
                        }
                    }
                } catch (Exception e) {
                    // Fehlerbehandlung
                    System.err.println("Fehler bei der Prüfung auf Favorit/Bad Provider: " + e.getMessage());
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