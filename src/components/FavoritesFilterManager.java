package components;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import data.FavoritesManager;
import data.ProviderStats;
import models.HighlightTableModel;

public class FavoritesFilterManager {
    private static final Logger LOGGER = Logger.getLogger(FavoritesFilterManager.class.getName());
    private final MainTable mainTable;
    private final HighlightTableModel tableModel;
    private final FavoritesManager favoritesManager;
    private final Map<String, ProviderStats> allStats;

    public FavoritesFilterManager(MainTable mainTable, HighlightTableModel tableModel, 
                                 Map<String, ProviderStats> allStats, String rootPath) {
        this.mainTable = mainTable;
        this.tableModel = tableModel;
        this.allStats = allStats;
        this.favoritesManager = new FavoritesManager(rootPath);
    }

    public void filterByFavorites() {
        LOGGER.info("Filterung nach Favoriten");
        
        Map<String, ProviderStats> filteredStats = new HashMap<>();
        
        for (Map.Entry<String, ProviderStats> entry : allStats.entrySet()) {
            String providerName = entry.getKey();
            String providerId = extractProviderId(providerName);
            
            LOGGER.fine("Prüfe Provider: " + providerName + " mit ID: " + providerId);
            
            if (favoritesManager.isFavorite(providerId)) {
                LOGGER.fine("  -> Ist ein Favorit!");
                filteredStats.put(providerName, entry.getValue());
            }
        }
        
        tableModel.populateData(filteredStats);
        mainTable.updateStatus();
    }
    
    // Neue Methode, um nur die Bad Provider zu filtern
    public void filterByBadProviders() {
        LOGGER.info("Filterung nach Bad Providern");
        
        Map<String, ProviderStats> filteredStats = new HashMap<>();
        
        for (Map.Entry<String, ProviderStats> entry : allStats.entrySet()) {
            String providerName = entry.getKey();
            String providerId = extractProviderId(providerName);
            
            LOGGER.fine("Prüfe Provider: " + providerName + " mit ID: " + providerId);
            
            if (favoritesManager.isBadProvider(providerId)) {
                LOGGER.fine("  -> Ist ein Bad Provider!");
                filteredStats.put(providerName, entry.getValue());
            }
        }
        
        tableModel.populateData(filteredStats);
        mainTable.updateStatus();
    }
    
    private String extractProviderId(String providerName) {
        // Variante 1: Name_123456.csv -> 123456
        int underscoreIndex = providerName.lastIndexOf("_");
        int dotIndex = providerName.lastIndexOf(".");
        
        if (underscoreIndex > 0 && dotIndex > underscoreIndex) {
            return providerName.substring(underscoreIndex + 1, dotIndex);
        }
        
        // Variante 2: Falls das Format anders ist, versuche Zahlen zu extrahieren
        StringBuilder digits = new StringBuilder();
        for (char c : providerName.toCharArray()) {
            if (Character.isDigit(c)) {
                digits.append(c);
            }
        }
        
        if (digits.length() > 0) {
            return digits.toString();
        }
        
        // Fallback
        return providerName;
    }
    
    public FavoritesManager getFavoritesManager() {
        return favoritesManager;
    }
}