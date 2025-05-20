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
import javax.swing.SwingWorker;


public class FavoritesFilterManager {
    private static final Logger LOGGER = Logger.getLogger(FavoritesFilterManager.class.getName());
    
    private final MainTable mainTable;
    private final HighlightTableModel tableModel;
    private final Map<String, ProviderStats> allStats;
    private final FavoritesManager favoritesManager;
    private int currentCategory = 0; // 0 bedeutet "Alle anzeigen"
    
    // Caching für Kategorie-Filter-Ergebnisse
    private final Map<Integer, Map<String, ProviderStats>> categoryCache = new HashMap<>();
    private boolean initialLoadDone = false;
    
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
     * Filtert die Tabelle nach einer bestimmten Favoriten-Kategorie mit optimiertem Caching
     * @param category Die Kategorie (0-10), wobei 0 bedeutet, keine Filterung anwenden
     */
    public void filterByCategory(int category) {
        // Aktuelle Kategorie speichern
        this.currentCategory = category;
        
        // Prüfen, ob wir die Ergebnisse bereits im Cache haben
        if (categoryCache.containsKey(category)) {
            LOGGER.info("Verwende gecachte Daten für Kategorie " + category);
            
            SwingUtilities.invokeLater(() -> {
                tableModel.populateData(categoryCache.get(category));
                mainTable.updateStatus();
                mainTable.repaint();
            });
            return;
        }
        
        // Wenn wir alle anzeigen wollen und wir uns nicht in der Initialisierung befinden,
        // zeigen wir schnell die vollständige Liste an und setzen initialLoadDone auf true
        if (category == 0 && !initialLoadDone) {
            // Bei der ersten Ladung verwenden wir einen optimierten Ansatz
            showAllWithSmoothLoading();
            return;
        }
        
        // Wenn es sich nicht um "Alle anzeigen" handelt, müssen wir die Favoriten neu laden
        if (category != 0) {
            favoritesManager.reloadFavorites();
        }
        
        // SwingWorker verwenden, um die Filterung im Hintergrund durchzuführen
        SwingWorker<Map<String, ProviderStats>, Void> worker = new SwingWorker<Map<String, ProviderStats>, Void>() {
            @Override
            protected Map<String, ProviderStats> doInBackground() throws Exception {
                if (category == 0) {
                    // Zeige alle Provider an (keine Favoriten-Filterung)
                    LOGGER.info("Berechne und cache alle Provider");
                    return allStats;
                } else {
                    // Hole alle Favoriten in der angegebenen Kategorie
                    Set<String> categoryFavorites = favoritesManager.getFavoritesInCategory(category);
                    
                    if (categoryFavorites.isEmpty()) {
                        LOGGER.info("Keine Favoriten in Kategorie " + category + " gefunden");
                        return new HashMap<>();
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
                        return filteredStats;
                    }
                }
            }
            
            @Override
            protected void done() {
                try {
                    // Ergebnis des Hintergrund-Threads erhalten und in der UI anzeigen
                    Map<String, ProviderStats> result = get();
                    
                    // Ergebnis cachen
                    categoryCache.put(category, result);
                    
                    // UI aktualisieren
                    tableModel.populateData(result);
                    mainTable.updateStatus();
                    mainTable.repaint();
                } catch (Exception e) {
                    LOGGER.severe("Fehler beim Filtern nach Kategorie: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        
        worker.execute();
    }
    
    /**
     * Optimierte Methode zum ersten Laden aller Provider mit Batch-Processing
     */
    private void showAllWithSmoothLoading() {
        LOGGER.info("Optimiertes Laden aller Provider...");
        
        // Worker für das schrittweise Laden
        SwingWorker<Void, Map<String, ProviderStats>> worker = new SwingWorker<Void, Map<String, ProviderStats>>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Die vollständige Liste für die spätere Verwendung cachen
                categoryCache.put(0, allStats);
                
                // Wir erstellen einen kleinen ersten Batch für eine schnelle Anzeige
                int totalSize = allStats.size();
                int firstBatchSize = Math.min(100, totalSize); // Zeige zuerst 100 Einträge oder weniger
                
                // Ersten Batch erstellen
                Map<String, ProviderStats> firstBatch = new HashMap<>();
                int count = 0;
                for (Map.Entry<String, ProviderStats> entry : allStats.entrySet()) {
                    firstBatch.put(entry.getKey(), entry.getValue());
                    count++;
                    if (count >= firstBatchSize) break;
                }
                
                // Ersten Batch veröffentlichen
                publish(firstBatch);
                
                // Warten, damit die UI Zeit hat, das erste Batch zu verarbeiten
                Thread.sleep(100);
                
                // Vollständige Liste veröffentlichen
                publish(allStats);
                
                return null;
            }
            
            @Override
            protected void process(java.util.List<Map<String, ProviderStats>> chunks) {
                // Letzte Chunk verwenden
                Map<String, ProviderStats> latestData = chunks.get(chunks.size() - 1);
                
                tableModel.populateData(latestData);
                mainTable.updateStatus();
                mainTable.repaint();
            }
            
            @Override
            protected void done() {
                initialLoadDone = true;
                
                // Abschließende UI-Aktualisierung
                mainTable.updateStatus();
                mainTable.repaint();
            }
        };
        
        worker.execute();
    }
    
    /**
     * Cache leeren, um Daten neu zu laden (z.B. nach Änderungen)
     */
    public void clearCache() {
        LOGGER.info("Cache wird geleert");
        categoryCache.clear();
        initialLoadDone = false;
    }
    
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