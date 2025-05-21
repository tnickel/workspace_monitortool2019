package components;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.JTable;

import data.FavoritesManager;
import data.ProviderStats;
import models.HighlightTableModel;

/**
 * Manager-Klasse für die Filterung der Tabelle nach Favoriten-Kategorien
 */
public class FavoritesFilterManager {
    private static final Logger LOGGER = Logger.getLogger(FavoritesFilterManager.class.getName());
    
    private final JTable table;
    private final HighlightTableModel model;
    private final Map<String, ProviderStats> allStats;
    private final String rootPath;
    private final FavoritesManager favoritesManager;
    
    // Aktuelle Kategorie für die Filterung
    private int currentCategory = 0;
    
    // Cache für die Provider-IDs, um nicht jedes Mal die ID neu zu extrahieren
    private final Map<String, String> providerIdCache = new HashMap<>();
    
    /**
     * Konstruktor
     * 
     * @param table Die JTable, die gefiltert werden soll
     * @param model Das Tabellenmodell
     * @param allStats Alle Provider-Statistiken
     * @param rootPath Der Root-Pfad
     */
    public FavoritesFilterManager(JTable table, HighlightTableModel model, Map<String, ProviderStats> allStats, String rootPath) {
        this.table = table;
        this.model = model;
        this.allStats = allStats;
        this.rootPath = rootPath;
        
        // Verwenden der Singleton-Instanz vom FavoritesManager
        this.favoritesManager = FavoritesManager.getInstance(rootPath);
        
        // Lade die Provider-ID-Cache initial
        loadProviderIdCache();
    }
    
    /**
     * Lädt den Provider-ID-Cache für alle vorhandenen Provider
     */
    private void loadProviderIdCache() {
        for (String providerName : allStats.keySet()) {
            // Provider-ID aus dem Namen extrahieren und cachen
            String providerId = extractProviderId(providerName);
            providerIdCache.put(providerName, providerId);
        }
    }
    
    /**
     * Extrahiert die Provider-ID aus dem Providernamen (Dateiname)
     * 
     * @param providerName Der Name des Providers (normalerweise ein Dateiname)
     * @return Die extrahierte Provider-ID
     */
    private String extractProviderId(String providerName) {
        // Prüfen, ob die ID bereits im Cache ist
        if (providerIdCache.containsKey(providerName)) {
            return providerIdCache.get(providerName);
        }
        
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
        
        // ID im Cache speichern für zukünftige Abfragen
        providerIdCache.put(providerName, providerId);
        
        return providerId;
    }
    
    /**
     * Filtert die Tabelle, um nur Favoriten anzuzeigen
     */
    public void filterByFavorites() {
        filterByCategory(1); // Standardkategorie 1 für Favoriten
    }
    
    /**
     * Filtert die Tabelle nach einer bestimmten Favoriten-Kategorie
     * 
     * @param category Die Kategorie (0-10), wobei 0 bedeutet: keine Filterung
     */
    public void filterByCategory(int category) {
        // Speichere die aktuelle Kategorie
        this.currentCategory = category;
        
        // Wenn Kategorie 0, zeige alle Provider (keine Filterung)
        if (category == 0) {
            model.populateData(allStats);
            return;
        }
        
        // Hole alle Provider-IDs in der ausgewählten Kategorie
        Set<String> favoritesInCategory = favoritesManager.getFavoritesInCategory(category);
        
        // Wenn keine Favoriten in dieser Kategorie, zeige leere Tabelle
        if (favoritesInCategory.isEmpty()) {
        	model.populateData(new HashMap<>()); // Leere Map übergeben statt clear() aufzurufen
            return;
        }
        
        // Filtere die Provider nach Kategorie
        Map<String, ProviderStats> filteredStats = new HashMap<>();
        
        for (Map.Entry<String, ProviderStats> entry : allStats.entrySet()) {
            String providerName = entry.getKey();
            String providerId = extractProviderId(providerName);
            
            if (favoritesInCategory.contains(providerId)) {
                filteredStats.put(providerName, entry.getValue());
            }
        }
        
        // Aktualisiere das Modell mit den gefilterten Daten
        model.populateData(filteredStats);
    }
    
    /**
     * Gibt die aktuelle Kategorie zurück
     * 
     * @return Die aktuelle Kategorie
     */
    public int getCurrentCategory() {
        return currentCategory;
    }
    
    /**
     * Gibt den FavoritesManager zurück
     * 
     * @return Der FavoritesManager
     */
    public FavoritesManager getFavoritesManager() {
        return favoritesManager;
    }
 // In der FavoritesFilterManager-Klasse, füge diese Methode hinzu oder aktualisiere sie

    /**
     * Fügt den Provider als Favorit hinzu oder entfernt ihn als Favorit, und aktualisiert die Ansicht
     * @param providerName Der Name des Providers
     * @param category Die Kategorie (1-10, oder 0 zum Entfernen)
     * @param updateRenderers True, um alle Renderer zu aktualisieren
     */
    public void toggleFavorite(String providerName, int category, boolean updateRenderers) {
        // Provider-ID extrahieren
        String providerId = extractProviderId(providerName);
        
        if (providerId != null && !providerId.isEmpty()) {
            // Favoriten-Status umschalten
            getFavoritesManager().setFavoriteCategory(providerId, category);
            
            // Wenn die aktuelle Kategorie-Anzeige betroffen ist, aktualisiere sie
            if (currentCategory == category || currentCategory == 0) {
                filterByCategory(currentCategory);
            }
            
            // Tabelle zum Neuzeichnen zwingen, falls gewünscht
            if (updateRenderers && table != null) {
                if (table instanceof MainTable) {
                    // MainTable hat spezielle Refresh-Methode
                    ((MainTable)table).refreshTableRendering();
                } else {
                    // Fallback für normale JTable
                    table.repaint();
                }
            }
        }
    }

    /**
     * Convenience-Methode zum Umschalten des Favoriten-Status mit Aktualisierung der Renderer
     * @param providerName Der Name des Providers
     * @param category Die Kategorie (1-10, oder 0 zum Entfernen)
     */
    public void toggleFavoriteWithUpdate(String providerName, int category) {
        toggleFavorite(providerName, category, true);
    }
}