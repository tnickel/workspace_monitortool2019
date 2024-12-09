package data;

import java.io.*;
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
        this.favoritesFile = Paths.get(rootPath, "conv", "favorites.txt");
        loadFavorites();
    }
    
    public boolean isFavorite(String providerId) {
        return favorites.contains(providerId);
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
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(favoritesFile.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    favorites.add(line.trim());
                }
            }
        } catch (IOException e) {
            LOGGER.warning("Error loading favorites: " + e.getMessage());
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
        }
    }
}