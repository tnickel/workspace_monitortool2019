package data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
        
        System.out.println("FavoritesManager initialisiert mit rootPath: " + rootPath);
        
        // Stellen Sie sicher, dass der Config-Ordner existiert
        File configDir = new File(rootPath, "config");
        if (!configDir.exists()) {
            boolean created = configDir.mkdirs();
            if (!created) {
                LOGGER.warning("Konnte Config-Verzeichnis nicht erstellen: " + configDir.getAbsolutePath());
                System.out.println("Warnung: Config-Verzeichnis konnte nicht erstellt werden: " + configDir.getAbsolutePath());
            } else {
                System.out.println("Config-Verzeichnis erfolgreich erstellt: " + configDir.getAbsolutePath());
            }
        } else {
            System.out.println("Config-Verzeichnis existiert bereits: " + configDir.getAbsolutePath());
        }
        
        this.favoritesFile = Paths.get(rootPath, "config", "favorites.txt");
        System.out.println("Favoriten werden gespeichert in: " + favoritesFile.toAbsolutePath());
        
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
            System.out.println("Favorit entfernt: " + providerId);
        } else {
            favorites.add(providerId);
            System.out.println("Favorit hinzugefügt: " + providerId);
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
            System.out.println("Anzahl geladener Favoriten: " + favorites.size());
        } catch (IOException e) {
            LOGGER.warning("Error loading favorites: " + e.getMessage());
            System.out.println("Fehler beim Laden der Favoriten: " + e.getMessage());
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
                    System.out.println("Fehler: Verzeichnis konnte nicht erstellt werden: " + parentDir.getAbsolutePath());
                    return; // Nicht weitermachen, wenn das Verzeichnis nicht erstellt werden kann
                } else {
                    System.out.println("Verzeichnis erfolgreich erstellt: " + parentDir.getAbsolutePath());
                }
            }
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(favoritesFile.toFile()))) {
                for (String providerId : favorites) {
                    writer.println(providerId);
                }
                System.out.println("Favoriten erfolgreich gespeichert. Anzahl: " + favorites.size());
                System.out.println("Speicherort: " + favoritesFile.toAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.warning("Error saving favorites: " + e.getMessage());
            System.out.println("Fehler beim Speichern der Favoriten: " + e.getMessage());
            e.printStackTrace();
        }
    }
}