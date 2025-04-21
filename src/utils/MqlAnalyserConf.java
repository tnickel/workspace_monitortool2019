package utils;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Logger;

public class MqlAnalyserConf {
    private static final Logger LOGGER = Logger.getLogger(MqlAnalyserConf.class.getName());
    private final Properties properties;
    private final Path configPath;
    
    public MqlAnalyserConf(String rootPath) {
        // Validiere den Pfad und korrigiere ihn, falls nötig
        rootPath = ApplicationConstants.validateRootPath(rootPath, "MqlAnalyserConf.constructor");
        
        this.configPath = Paths.get(rootPath, "config", "MqlAnalyzerConfig.txt");
        this.properties = new Properties();
        loadConfig();
    }
    
    private void loadConfig() {
        try {
            // Stelle sicher, dass der Ordner existiert
            Path configFolder = configPath.getParent();
            if (!Files.exists(configFolder)) {
                Files.createDirectories(configFolder);
            }
            
            // Prüfe, ob die Config-Datei existiert
            if (Files.exists(configPath)) {
                try (Reader reader = Files.newBufferedReader(configPath)) {
                    properties.load(reader);
                }
            } else {
                // Wenn die Datei nicht existiert, setze Standardwerte
                setDefaultValues();
            }
        } catch (IOException e) {
            LOGGER.warning("Could not load config file: " + e.getMessage());
            setDefaultValues();
        }
    }
    
    private void setDefaultValues() {
        properties.setProperty("downloadPath", ApplicationConstants.ROOT_PATH + "\\download");
        properties.setProperty("baseUrl", "https://www.mql5.com/en/signals");
        saveConfig();
    }
    
    public void saveConfig() {
        try {
            // Stelle sicher, dass der Ordner existiert
            Path configFolder = configPath.getParent();
            if (!Files.exists(configFolder)) {
                Files.createDirectories(configFolder);
            }
            
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                properties.store(writer, "MQL5 Analyser Configuration");
            }
        } catch (IOException e) {
            LOGGER.severe("Could not save config file: " + e.getMessage());
        }
    }
    
    public String getDownloadPath() {
        return properties.getProperty("downloadPath", ApplicationConstants.ROOT_PATH + "\\download");
    }
    
    public void setDownloadPath(String path) {
        properties.setProperty("downloadPath", path);
        saveConfig();
    }
    
    public String getUsername() {
        return properties.getProperty("username", "");
    }
    
    public void setUsername(String username) {
        properties.setProperty("username", username);
        saveConfig();
    }
    
    public String getPassword() {
        return properties.getProperty("password", "");
    }
    
    public void setPassword(String password) {
        properties.setProperty("password", password);
        saveConfig();
    }
    
    public String getBaseUrl() {
        return properties.getProperty("baseUrl", "https://www.mql5.com/en/signals");
    }
    
    public void setBaseUrl(String url) {
        properties.setProperty("baseUrl", url);
        saveConfig();
    }
    
    public String getSignalId() {
        return properties.getProperty("SignalId", "");
    }
    
    public void setSignalId(String id) {
        properties.setProperty("SignalId", id);
        saveConfig();
    }
}