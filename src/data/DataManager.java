package data;



import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DataManager {
    private final Map<String, ProviderStats> signalProviderStats;
    
    public DataManager() {
        this.signalProviderStats = new HashMap<>();
    }
    
    public void loadData(String path) {
        System.out.println("Loading data from: " + path);
        File downloadDirectory = new File(path);
        if (downloadDirectory.exists() && downloadDirectory.isDirectory()) {
            File[] files = downloadDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
            if (files != null) {
                System.out.println("Found " + files.length + " CSV files");
                for (File file : files) {
                    processFile(file);
                }
            }
        }
        System.out.println("Loaded " + signalProviderStats.size() + " providers");
    }

    private void processFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isHeader = true;
            ProviderStats stats = new ProviderStats();
            
            while ((line = reader.readLine()) != null) {
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
                    LocalDate tradeDate = LocalDate.parse(data[0].substring(0, 10), 
                        DateTimeFormatter.ofPattern("yyyy.MM.dd"));
                    double profit = Double.parseDouble(data[data.length - 1]);
                    stats.addTrade(profit, tradeDate);
                } catch (Exception e) {
                    continue;
                }
            }
            
            if (!stats.getProfits().isEmpty()) {
                signalProviderStats.put(file.getName(), stats);
                //System.out.println("\nAnalysis for " + file.getName() + ":\n" + stats.getAnalysisString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public Map<String, ProviderStats> getStats() {
        return signalProviderStats;
    }
}