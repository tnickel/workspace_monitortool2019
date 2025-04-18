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
        this.configPath = Paths.get(rootPath, "config", "MqlAnalyzerConfig.txt");
        this.properties = new Properties();
        loadConfig();
    }
    
    private void loadConfig() {
        try (Reader reader = Files.newBufferedReader(configPath)) {
            properties.load(reader);
        } catch (IOException e) {
            LOGGER.warning("Could not load config file: " + e.getMessage());
            setDefaultValues();
        }
    }
    
    private void setDefaultValues() {
        properties.setProperty("downloadPath", "c:\\Forex\\MqlAnalyzer\\download");
        properties.setProperty("baseUrl", "https://www.mql5.com/en/signals");
        saveConfig();
    }
    
    public void saveConfig() {
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            properties.store(writer, "MQL5 Analyser Configuration");
        } catch (IOException e) {
            LOGGER.severe("Could not save config file: " + e.getMessage());
        }
    }
    
    public String getDownloadPath() {
        return properties.getProperty("downloadPath", "c:\\Forex\\MqlAnalyzer\\download");
    }
    
    public void setDownloadPath(String path) {
        properties.setProperty("downloadPath", path);
        saveConfig();
    }
    
    public String getUsername() {
        return properties.getProperty("username", "");
    }
    
    public String getPassword() {
        return properties.getProperty("password", "");
    }
    
    public String getBaseUrl() {
        return properties.getProperty("baseUrl", "https://www.mql5.com/en/signals");
    }
    
    public String getSignalId() {
        return properties.getProperty("SignalId", "");
    }
}