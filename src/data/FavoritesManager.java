package data;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class FavoritesManager {
    private static final Logger LOGGER = Logger.getLogger(FavoritesManager.class.getName());
    private final Set<String> favorites;
    private final Path favoritesFile;
    
  
        
        public FavoritesManager(String rootPath) {
            this.favorites = new HashSet<>();
            this.favoritesFile = Paths.get(rootPath, "config", "favorites.txt");
            loadFavorites();
        }
    
    public boolean isFavorite(String providerId) {
        // Debug für jede Prüfung
        boolean result = favorites.contains(providerId);
        System.out.println("FavoritesManager prüft ID: " + providerId + " -> " + (result ? "ist Favorit" : "kein Favorit"));
        return result;
    }
    
    public void toggleFavorite(String providerId) {
        if (favorites.contains(providerId)) {
            favorites.remove(providerId);
        } else {
            favorites.add(providerId);
        }
        saveFavorites();
    }
    
    private void loadFavorites() {
        if (!favoritesFile.toFile().exists()) {
            System.out.println("favorites.txt existiert nicht: " + favoritesFile);
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(favoritesFile.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    favorites.add(line);
                    System.out.println("Favorit geladen: " + line);
                }
            }
        } catch (IOException e) {
            LOGGER.warning("Error loading favorites: " + e.getMessage());
            System.out.println("Fehler beim Laden der Favoriten: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void saveFavorites() {
        try {
            // Stelle sicher, dass das Verzeichnis existiert
            favoritesFile.getParent().toFile().mkdirs();
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(favoritesFile.toFile()))) {
                for (String providerId : favorites) {
                    writer.println(providerId);
                }
            }
        } catch (IOException e) {
            LOGGER.warning("Error saving favorites: " + e.getMessage());
            System.out.println("Fehler beim Speichern der Favoriten: " + e.getMessage());
            e.printStackTrace();
        }
    }
}