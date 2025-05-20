package data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import utils.ApplicationConstants;

public class FavoritesManager {
    private static final Logger LOGGER = Logger.getLogger(FavoritesManager.class.getName());
    private final Map<String, Integer> favorites; // Geändert zu Map mit Provider-ID als Schlüssel und Kategorie als Wert
    private final Set<String> badProviders;
    private final Path favoritesFile;
    private final Path badProvidersFile;
    
    // Caching-Mechanismus für bessere Performance
    private static FavoritesManager instance;
    private static boolean dataLoaded = false;
    private static boolean outputDebugMessages = false;
    
    /**
     * Gibt die Singleton-Instanz des FavoritesManager zurück
     * @param rootPath Pfad zum Root-Verzeichnis
     * @return FavoritesManager-Instanz
     */
    public static synchronized FavoritesManager getInstance(String rootPath) {
        if (instance == null) {
            instance = new FavoritesManager(rootPath);
        }
        return instance;
    }
    
    public FavoritesManager(String rootPath) {
        // Validiere den Pfad und korrigiere ihn, falls nötig
        rootPath = ApplicationConstants.validateRootPath(rootPath, "FavoritesManager.constructor");
        
        this.favorites = new HashMap<>(); // Geändert zu HashMap für die Kategorien
        this.badProviders = new HashSet<>();
        
        if (outputDebugMessages) {
            System.out.println("FavoritesManager initialisiert mit rootPath: " + rootPath);
        }
        
        // Stellen Sie sicher, dass der Config-Ordner existiert
        File configDir = new File(rootPath, "config");
        if (!configDir.exists()) {
            boolean created = configDir.mkdirs();
            if (!created) {
                LOGGER.warning("Konnte Config-Verzeichnis nicht erstellen: " + configDir.getAbsolutePath());
                if (outputDebugMessages) {
                    System.out.println("Warnung: Config-Verzeichnis konnte nicht erstellt werden: " + configDir.getAbsolutePath());
                }
            } else if (outputDebugMessages) {
                System.out.println("Config-Verzeichnis erfolgreich erstellt: " + configDir.getAbsolutePath());
            }
        } else if (outputDebugMessages) {
            System.out.println("Config-Verzeichnis existiert bereits: " + configDir.getAbsolutePath());
        }
        
        this.favoritesFile = Paths.get(rootPath, "config", "favorites.txt");
        this.badProvidersFile = Paths.get(rootPath, "config", "badproviders.txt");
        
        if (outputDebugMessages) {
            System.out.println("Favoriten werden gespeichert in: " + favoritesFile.toAbsolutePath());
            System.out.println("Bad Provider werden gespeichert in: " + badProvidersFile.toAbsolutePath());
        }
        
        // Lade die Daten nur, wenn sie noch nicht geladen wurden
        if (!dataLoaded) {
            loadFavorites();
            loadBadProviders();
            dataLoaded = true;
        }
    }
    
    // NEU: Reload-Methode
    public void reloadFavorites() {
        loadFavorites();
        if (outputDebugMessages) {
            System.out.println("Favoriten wurden neu geladen.");
        }
        dataLoaded = true;
    }
    
    public boolean isFavorite(String providerId) {
        // Debug-Ausgabe reduziert für bessere Performance
        boolean result = favorites.containsKey(providerId);
        
        // Reduzierte Debug-Ausgabe
        if (outputDebugMessages) {
            System.out.println("FavoritesManager prüft ID: " + providerId + " -> " + (result ? "ist Favorit" : "kein Favorit"));
        }
        
        return result;
    }
    
    public boolean isFavoriteInCategory(String providerId, int category) {
        // Prüft, ob der Provider ein Favorit in der angegebenen Kategorie ist
        Integer providerCategory = favorites.get(providerId);
        boolean result = (providerCategory != null && providerCategory == category);
        
        // Reduzierte Debug-Ausgabe
        if (outputDebugMessages) {
            System.out.println("FavoritesManager prüft ID: " + providerId + " für Kategorie " + category + 
                          " -> " + (result ? "ist in dieser Kategorie" : "nicht in dieser Kategorie"));
        }
        
        return result;
    }
    
    public int getFavoriteCategory(String providerId) {
        // Gibt die Kategorie des Favoriten zurück oder 0, wenn nicht vorhanden
        return favorites.getOrDefault(providerId, 0);
    }
    
    public boolean isBadProvider(String providerId) {
        boolean result = badProviders.contains(providerId);
        return result;
    }
    
    public void toggleFavorite(String providerId, int category) {
        if (favorites.containsKey(providerId) && favorites.get(providerId) == category) {
            // Wenn der Provider bereits in dieser Kategorie ist, entferne ihn
            favorites.remove(providerId);
            if (outputDebugMessages) {
                System.out.println("Favorit entfernt: " + providerId);
            }
        } else {
            // Füge den Provider zur angegebenen Kategorie hinzu
            favorites.put(providerId, category);
            if (outputDebugMessages) {
                System.out.println("Favorit hinzugefügt: " + providerId + " in Kategorie " + category);
            }
        }
        saveFavorites();
    }
    
    public void setFavoriteCategory(String providerId, int category) {
        // Setzt die Kategorie für einen Provider, unabhängig davon, ob er bereits Favorit ist
        if (category < 0 || category > 10) {
            LOGGER.warning("Ungültige Kategorie: " + category + ". Muss zwischen 0 und 10 liegen.");
            return;
        }
        
        if (category == 0 && favorites.containsKey(providerId)) {
            // Kategorie 0 bedeutet "kein Favorit", daher entfernen
            favorites.remove(providerId);
            if (outputDebugMessages) {
                System.out.println("Favorit entfernt: " + providerId);
            }
        } else if (category > 0) {
            // Kategorie setzen
            favorites.put(providerId, category);
            if (outputDebugMessages) {
                System.out.println("Kategorie für Provider " + providerId + " auf " + category + " gesetzt");
            }
        }
        saveFavorites();
    }
    
    public void toggleBadProvider(String providerId) {
        if (badProviders.contains(providerId)) {
            badProviders.remove(providerId);
            if (outputDebugMessages) {
                System.out.println("Bad Provider entfernt: " + providerId);
            }
        } else {
            badProviders.add(providerId);
            if (outputDebugMessages) {
                System.out.println("Bad Provider hinzugefügt: " + providerId);
            }
        }
        saveBadProviders();
    }
    
    private void loadFavorites() {
        if (!favoritesFile.toFile().exists()) {
            if (outputDebugMessages) {
                System.out.println("favorites.txt existiert nicht: " + favoritesFile);
            }
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(favoritesFile.toFile()))) {
            favorites.clear(); // Cache leeren
            
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    // Format überprüfen: ID:Kategorie
                    if (line.contains(":")) {
                        String[] parts = line.split(":");
                        if (parts.length >= 2) {
                            String providerId = parts[0].trim();
                            try {
                                int category = Integer.parseInt(parts[1].trim());
                                if (category > 0 && category <= 10) {
                                    favorites.put(providerId, category);
                                    if (outputDebugMessages) {
                                        System.out.println("Favorit geladen: " + providerId + " mit Kategorie " + category);
                                    }
                                }
                            } catch (NumberFormatException e) {
                                LOGGER.warning("Ungültiges Kategorieformat für Provider " + parts[0] + ": " + parts[1]);
                            }
                        }
                    } else {
                        // Altes Format ohne Kategorie - setze auf Kategorie 1
                        favorites.put(line, 1);
                        if (outputDebugMessages) {
                            System.out.println("Favorit im alten Format geladen: " + line + " mit Standard-Kategorie 1");
                        }
                    }
                }
            }
            if (outputDebugMessages) {
                System.out.println("Anzahl geladener Favoriten: " + favorites.size());
            }
        } catch (IOException e) {
            LOGGER.warning("Error loading favorites: " + e.getMessage());
            if (outputDebugMessages) {
                System.out.println("Fehler beim Laden der Favoriten: " + e.getMessage());
            }
            e.printStackTrace();
        }
    }
    
    private void loadBadProviders() {
        if (!badProvidersFile.toFile().exists()) {
            if (outputDebugMessages) {
                System.out.println("badproviders.txt existiert nicht: " + badProvidersFile);
            }
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(badProvidersFile.toFile()))) {
            badProviders.clear(); // Cache leeren
            
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    badProviders.add(line);
                    if (outputDebugMessages) {
                        System.out.println("Bad Provider geladen: " + line);
                    }
                }
            }
            if (outputDebugMessages) {
                System.out.println("Anzahl geladener Bad Provider: " + badProviders.size());
            }
        } catch (IOException e) {
            LOGGER.warning("Error loading bad providers: " + e.getMessage());
            if (outputDebugMessages) {
                System.out.println("Fehler beim Laden der Bad Provider: " + e.getMessage());
            }
            e.printStackTrace();
        }
    }
    
    private void saveFavorites() {
        try {
            // Stellen Sie sicher, dass das Verzeichnis existiert
            File parentDir = favoritesFile.getParent().toFile();
            if (!parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                if (!created) {
                    LOGGER.warning("Konnte Verzeichnis nicht erstellen: " + parentDir.getAbsolutePath());
                    if (outputDebugMessages) {
                        System.out.println("Fehler: Verzeichnis konnte nicht erstellt werden: " + parentDir.getAbsolutePath());
                    }
                    return; // Nicht weitermachen, wenn das Verzeichnis nicht erstellt werden kann
                } else if (outputDebugMessages) {
                    System.out.println("Verzeichnis erfolgreich erstellt: " + parentDir.getAbsolutePath());
                }
            }
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(favoritesFile.toFile()))) {
                for (Map.Entry<String, Integer> entry : favorites.entrySet()) {
                    // Speichern im neuen Format: ID:Kategorie
                    writer.println(entry.getKey() + ":" + entry.getValue());
                }
                if (outputDebugMessages) {
                    System.out.println("Favoriten erfolgreich gespeichert. Anzahl: " + favorites.size());
                    System.out.println("Speicherort: " + favoritesFile.toAbsolutePath());
                }
            }
        } catch (IOException e) {
            LOGGER.warning("Error saving favorites: " + e.getMessage());
            if (outputDebugMessages) {
                System.out.println("Fehler beim Speichern der Favoriten: " + e.getMessage());
            }
            e.printStackTrace();
        }
    }
    
    private void saveBadProviders() {
        try {
            // Stellen Sie sicher, dass das Verzeichnis existiert
            File parentDir = badProvidersFile.getParent().toFile();
            if (!parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                if (!created) {
                    LOGGER.warning("Konnte Verzeichnis nicht erstellen: " + parentDir.getAbsolutePath());
                    if (outputDebugMessages) {
                        System.out.println("Fehler: Verzeichnis konnte nicht erstellt werden: " + parentDir.getAbsolutePath());
                    }
                    return; // Nicht weitermachen, wenn das Verzeichnis nicht erstellt werden kann
                } else if (outputDebugMessages) {
                    System.out.println("Verzeichnis erfolgreich erstellt: " + parentDir.getAbsolutePath());
                }
            }
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(badProvidersFile.toFile()))) {
                for (String providerId : badProviders) {
                    writer.println(providerId);
                }
                if (outputDebugMessages) {
                    System.out.println("Bad Provider erfolgreich gespeichert. Anzahl: " + badProviders.size());
                    System.out.println("Speicherort: " + badProvidersFile.toAbsolutePath());
                }
            }
        } catch (IOException e) {
            LOGGER.warning("Error saving bad providers: " + e.getMessage());
            if (outputDebugMessages) {
                System.out.println("Fehler beim Speichern der Bad Provider: " + e.getMessage());
            }
            e.printStackTrace();
        }
    }
    
    /**
     * Gibt alle Favoriten einer bestimmten Kategorie zurück
     * @param category Kategorie der Favoriten (1-10)
     * @return Set mit Provider-IDs in dieser Kategorie
     */
    public Set<String> getFavoritesInCategory(int category) {
        Set<String> result = new HashSet<>();
        
        if (category <= 0) {
            // Kategorie 0 bedeutet alle Provider (keine Filterung nach Favoriten)
            return result; // Leeres Set, da keine Filterung stattfindet
        }
        
        for (Map.Entry<String, Integer> entry : favorites.entrySet()) {
            if (entry.getValue() == category) {
                result.add(entry.getKey());
            }
        }
        
        return result;
    }
    
    /**
     * Gibt alle Favoriten zurück (unabhängig von der Kategorie)
     * @return Set mit allen Provider-IDs, die Favoriten sind
     */
    public Set<String> getAllFavorites() {
        return favorites.keySet();
    }
    
    /**
     * Gibt die Map mit allen Favoriten und ihren Kategorien zurück
     * @return Map mit Provider-ID als Schlüssel und Kategorie als Wert
     */
    public Map<String, Integer> getFavoriteCategories() {
        return new HashMap<>(favorites);
    }
}