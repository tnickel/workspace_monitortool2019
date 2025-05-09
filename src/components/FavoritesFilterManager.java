package components;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import data.FavoritesManager;
import data.ProviderStats;
import models.HighlightTableModel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class FavoritesFilterManager {
    private static final Logger LOGGER = Logger.getLogger(FavoritesFilterManager.class.getName());
    
    private final MainTable mainTable;
    private final HighlightTableModel tableModel;
    private final Map<String, ProviderStats> allStats;
    private final FavoritesManager favoritesManager;
    private int currentCategory = 0; // 0 bedeutet "Alle anzeigen"
    
    public FavoritesFilterManager(MainTable mainTable, HighlightTableModel tableModel, 
                                 Map<String, ProviderStats> allStats, String rootPath) {
        this.mainTable = mainTable;
        this.tableModel = tableModel;
        this.allStats = allStats;
        this.favoritesManager = new FavoritesManager(rootPath);
    }
    
    /**
     * Filtert die Tabelle, um nur Favoriten anzuzeigen
     * (ohne eine spezifische Kategorie - alle Favoriten)
     */
    public void filterByFavorites() {
        filterByCategory(1); // Standardkategorie 1
    }
    
    /**
     * Filtert die Tabelle nach einer bestimmten Favoriten-Kategorie
     * @param category Die Kategorie (0-10), wobei 0 bedeutet, keine Filterung anwenden
     */
    public void filterByCategory(int category) {
        this.currentCategory = category;
        
        if (category == 0) {
            // Zeige alle Provider an (keine Favoriten-Filterung)
            tableModel.populateData(allStats);
            LOGGER.info("Zeige alle Provider an (keine Favoriten-Filterung)");
        } else {
            // Hole alle Favoriten in der angegebenen Kategorie
            Set<String> categoryFavorites = favoritesManager.getFavoritesInCategory(category);
            
            if (categoryFavorites.isEmpty()) {
                LOGGER.info("Keine Favoriten in Kategorie " + category + " gefunden");
                // Leere die Tabelle, wenn keine Favoriten gefunden wurden - mit einer leeren Map
                tableModel.populateData(new HashMap<>());
            } else {
                // Filtere die Stats nach den Favoriten
                Map<String, ProviderStats> filteredStats = new HashMap<>();
                
                for (Map.Entry<String, ProviderStats> entry : allStats.entrySet()) {
                    String providerName = entry.getKey();
                    ProviderStats stats = entry.getValue();
                    
                    // Extrahiere die Provider-ID aus dem Namen
                    String providerId = extractProviderId(providerName);
                    
                    if (categoryFavorites.contains(providerId)) {
                        filteredStats.put(providerName, stats);
                    }
                }
                
                LOGGER.info("Zeige " + filteredStats.size() + " Favoriten in Kategorie " + category);
                tableModel.populateData(filteredStats);
            }
        }
        
        mainTable.updateStatus();
        mainTable.repaint();
    }
    
    /**
     * Extrahiert die Provider-ID aus dem Providernamen
     * @param providerName Name des Providers (normalerweise ein Dateipfad)
     * @return Die extrahierte Provider-ID
     */
    private String extractProviderId(String providerName) {
        // Provider-ID aus dem Namen extrahieren
        String providerId = "";
        if (providerName.contains("_")) {
            providerId = providerName.substring(providerName.lastIndexOf("_") + 1).replace(".csv", "");
        } else {
            // Fallback für unerwartetes Format
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
        return providerId;
    }
    
    /**
     * Gibt die aktuelle Kategorie zurück
     * @return Die aktuell ausgewählte Kategorie (0-10)
     */
    public int getCurrentCategory() {
        return currentCategory;
    }
    
    /**
     * Gibt den FavoritesManager zurück
     * @return Der FavoritesManager
     */
    public FavoritesManager getFavoritesManager() {
        return favoritesManager;
    }
}