package data;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

public class DataManager {
    private static final Logger LOGGER = Logger.getLogger(DataManager.class.getName());
    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");
    
    private final Map<String, ProviderStats> signalProviderStats;
    
    public DataManager() {
        this.signalProviderStats = new HashMap<>();
    }
    
    public void loadData(String path) {
        LOGGER.info("Loading data from: " + path);
        File downloadDirectory = new File(path);
        if (downloadDirectory.exists() && downloadDirectory.isDirectory()) {
            File[] files = downloadDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
            if (files != null) {
                LOGGER.info("Found " + files.length + " CSV files");
                for (File file : files) {
                    processFile(file);
                }
            }
        }
        LOGGER.info("Loaded " + signalProviderStats.size() + " providers");
    }

    private void processFile(File file) {
        LOGGER.info("Starting to process file: " + file.getName());
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isHeader = true;
            ProviderStats stats = new ProviderStats();
            int lineCount = 0;
            int tradeCounts = 0;
            
            while ((line = reader.readLine()) != null) {
                lineCount++;
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                
                if (line.trim().isEmpty()) continue;
                
                String[] data = line.split(";", -1);
                if (line.contains("Balance")) {
                    double balance = Double.parseDouble(data[data.length - 1]);
                    stats.setInitialBalance(balance);
                    continue;
                }
                
                try {
                    // Format: OpenTime;Type;Lots;Symbol;OpenPrice;Lots;CloseTime;ClosePrice;...;Profit
                    LocalDateTime openTime = LocalDateTime.parse(data[0], DATE_TIME_FORMATTER);
                    LocalDateTime closeTime = LocalDateTime.parse(data[6], DATE_TIME_FORMATTER);
                    double lots = Double.parseDouble(data[2]);
                    double profit = Double.parseDouble(data[data.length - 1]);
                    
                    stats.addTrade(openTime, closeTime, lots, profit); 
                    tradeCounts++;
                } catch (Exception e) {
                    LOGGER.warning("Error processing line " + lineCount + " in file " + file.getName() + ": " + line);
                    LOGGER.warning("Error details: " + e.getMessage());
                    continue;
                }
            }
            
            if (!stats.getProfits().isEmpty()) {
                signalProviderStats.put(file.getName(), stats);
                LOGGER.info(String.format("Successfully processed %s: %d trades loaded, Max concurrent trades: %d", 
                    file.getName(), tradeCounts, stats.getMaxConcurrentTrades()));
            }
        } catch (IOException e) {
            LOGGER.severe("Error reading file " + file.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public Map<String, ProviderStats> getStats() {
        return signalProviderStats;
    }
}