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
    private final Set<String> badProviders; // Neue Liste für Bad Provider
    private final Path favoritesFile;
    private final Path badProvidersFile; // Neue Datei für Bad Provider
    
    public FavoritesManager(String rootPath) {
        this.favorites = new HashSet<>();
        this.badProviders = new HashSet<>(); // Initialisiere Bad Provider Set
        
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
        this.badProvidersFile = Paths.get(rootPath, "config", "badproviders.txt"); // Neue Datei für Bad Provider
        
        System.out.println("Favoriten werden gespeichert in: " + favoritesFile.toAbsolutePath());
        System.out.println("Bad Provider werden gespeichert in: " + badProvidersFile.toAbsolutePath());
        
        loadFavorites();
        loadBadProviders(); // Lade Bad Provider
    }
    
    public boolean isFavorite(String providerId) {
        // Debug für jede Prüfung
        boolean result = favorites.contains(providerId);
        System.out.println("FavoritesManager prüft ID: " + providerId + " -> " + (result ? "ist Favorit" : "kein Favorit"));
        return result;
    }
    
    public boolean isBadProvider(String providerId) {
        // Ähnlich wie bei Favoriten
        boolean result = badProviders.contains(providerId);
        System.out.println("FavoritesManager prüft Bad Provider ID: " + providerId + " -> " + (result ? "ist Bad Provider" : "kein Bad Provider"));
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
    
    public void toggleBadProvider(String providerId) {
        if (badProviders.contains(providerId)) {
            badProviders.remove(providerId);
            System.out.println("Bad Provider entfernt: " + providerId);
        } else {
            badProviders.add(providerId);
            System.out.println("Bad Provider hinzugefügt: " + providerId);
        }
        saveBadProviders();
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
    
    private void loadBadProviders() {
        if (!badProvidersFile.toFile().exists()) {
            System.out.println("badproviders.txt existiert nicht: " + badProvidersFile);
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(badProvidersFile.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    badProviders.add(line);
                    System.out.println("Bad Provider geladen: " + line);
                }
            }
            System.out.println("Anzahl geladener Bad Provider: " + badProviders.size());
        } catch (IOException e) {
            LOGGER.warning("Error loading bad providers: " + e.getMessage());
            System.out.println("Fehler beim Laden der Bad Provider: " + e.getMessage());
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
    
    private void saveBadProviders() {
        try {
            // Stellen Sie sicher, dass das Verzeichnis existiert
            File parentDir = badProvidersFile.getParent().toFile();
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
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(badProvidersFile.toFile()))) {
                for (String providerId : badProviders) {
                    writer.println(providerId);
                }
                System.out.println("Bad Provider erfolgreich gespeichert. Anzahl: " + badProviders.size());
                System.out.println("Speicherort: " + badProvidersFile.toAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.warning("Error saving bad providers: " + e.getMessage());
            System.out.println("Fehler beim Speichern der Bad Provider: " + e.getMessage());
            e.printStackTrace();
        }
    }
}