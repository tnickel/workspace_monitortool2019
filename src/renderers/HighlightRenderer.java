package renderers;

import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import data.FavoritesManager;
import utils.ApplicationConstants;
import utils.UIStyle;

/**
 * Renderer für die Zellen in der Tabelle, der Favoriten und Bad Provider farblich hervorhebt
 */
public class HighlightRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1L;
    
    private static final Color FAVORITE_COLOR = new Color(230, 255, 230); // Hellgrün
    private static final Color BAD_PROVIDER_COLOR = new Color(255, 230, 230); // Hellrot
    private static final Color NEUTRAL_COLOR = Color.WHITE;
    
    private String searchText = "";
    private final FavoritesManager favoritesManager;
    
    // Cache für Provider-Status und IDs, um wiederholte Abfragen zu vermeiden
    private final Map<String, String> providerIdCache = new HashMap<>();
    private final Map<String, Boolean> favoriteCache = new HashMap<>();
    private final Map<String, Boolean> badProviderCache = new HashMap<>();
    
    public HighlightRenderer() {
        // Verwende die Singleton-Instanz vom FavoritesManager
        this.favoritesManager = FavoritesManager.getInstance(ApplicationConstants.ROOT_PATH);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, 
                                                  boolean isSelected, boolean hasFocus, 
                                                  int row, int column) {
        
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        if (c instanceof JLabel) {
            JLabel label = (JLabel) c;
            
            // Hintergründe nur setzen, wenn die Zelle nicht ausgewählt ist
            if (!isSelected) {
                // Prüfe, ob wir in der Signal Provider-Spalte sind (normalerweise Spalte 1)
                if (column == 1 && value != null) {
                    String providerName = value.toString();
                    setBackgroundBasedOnProviderStatus(label, providerName);
                } else {
                    // Für andere Spalten den Provider-Namen aus der Signal Provider-Spalte holen
                    int modelRow = table.convertRowIndexToModel(row);
                    Object providerObj = table.getModel().getValueAt(modelRow, 1);
                    if (providerObj != null) {
                        String providerName = providerObj.toString();
                        setBackgroundBasedOnProviderStatus(label, providerName);
                    } else {
                        label.setBackground(NEUTRAL_COLOR);
                    }
                }
            }
            
            // Text-Hervorhebung für die Suche
            if (value != null && !searchText.isEmpty()) {
                String text = value.toString().toLowerCase();
                if (text.contains(searchText.toLowerCase())) {
                    // Fett schreiben und Text-Farbe ändern
                    label.setFont(UIStyle.BOLD_FONT);
                    label.setForeground(UIStyle.SECONDARY_COLOR);
                } else {
                    // Standard-Schriftart und -Farbe
                    label.setFont(UIStyle.NORMAL_FONT);
                    label.setForeground(UIStyle.TEXT_COLOR);
                }
            } else {
                // Standard-Schriftart und -Farbe, wenn keine Suche
                label.setFont(UIStyle.NORMAL_FONT);
                label.setForeground(UIStyle.TEXT_COLOR);
            }
        }
        
        return c;
    }
    
    /**
     * Setzt den Hintergrund basierend auf dem Provider-Status (Favorit, Bad Provider, neutral)
     */
    private void setBackgroundBasedOnProviderStatus(JLabel label, String providerName) {
        // Provider-ID aus Providernamen extrahieren oder aus dem Cache holen
        String providerId = getProviderId(providerName);
        
        if (isProviderBad(providerId)) {
            // Bad Provider hat Vorrang vor Favorit
            label.setBackground(BAD_PROVIDER_COLOR);
        } else if (isProviderFavorite(providerId)) {
            // Favorit
            label.setBackground(FAVORITE_COLOR);
        } else {
            // Neutrale Farbe (weder Favorit noch Bad Provider)
            label.setBackground(NEUTRAL_COLOR);
        }
    }
    
    /**
     * Holt die Provider-ID aus dem Cache oder extrahiert sie bei Bedarf
     * @param providerName Name des Providers
     * @return Die Provider-ID
     */
    private String getProviderId(String providerName) {
        // Prüfen ob ID bereits im Cache ist
        if (providerIdCache.containsKey(providerName)) {
            return providerIdCache.get(providerName);
        }
        
        // ID extrahieren und im Cache speichern
        String providerId = extractProviderId(providerName);
        providerIdCache.put(providerName, providerId);
        return providerId;
    }
    
    /**
     * Prüft, ob ein Provider ein Favorit ist (mit Caching)
     */
    private boolean isProviderFavorite(String providerId) {
        if (favoriteCache.containsKey(providerId)) {
            return favoriteCache.get(providerId);
        }
        
        boolean isFavorite = favoritesManager.isFavorite(providerId);
        favoriteCache.put(providerId, isFavorite);
        return isFavorite;
    }
    
    /**
     * Prüft, ob ein Provider ein Bad Provider ist (mit Caching)
     */
    private boolean isProviderBad(String providerId) {
        if (badProviderCache.containsKey(providerId)) {
            return badProviderCache.get(providerId);
        }
        
        boolean isBad = favoritesManager.isBadProvider(providerId);
        badProviderCache.put(providerId, isBad);
        return isBad;
    }
    
    /**
     * Extrahiert die Provider-ID aus dem Providernamen
     */
    private String extractProviderId(String providerName) {
        if (providerName.contains("_")) {
            return providerName.substring(providerName.lastIndexOf("_") + 1).replace(".csv", "");
        } else {
            // Fallback für unerwartetes Format
            StringBuilder digits = new StringBuilder();
            for (char ch : providerName.toCharArray()) {
                if (Character.isDigit(ch)) {
                    digits.append(ch);
                }
            }
            if (digits.length() > 0) {
                return digits.toString();
            }
        }
        return "";
    }
    
    /**
     * Leert alle Caches für Provider-Status und IDs
     */
    public void clearCache() {
        providerIdCache.clear();
        favoriteCache.clear();
        badProviderCache.clear();
    }
    
    /**
     * Setzt den Suchtext für die Hervorhebung
     */
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }
}