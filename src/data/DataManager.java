package data;



import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;  // Diese Zeile war bisher nicht da
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataManager {
<<<<<<< HEAD
    private static final Logger LOGGER = Logger.getLogger(DataManager.class.getName());
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");
    private static final int EXPECTED_MIN_FIELDS = 11;
    
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
                    processFile(file, path);
                }
            }
        }
        LOGGER.info("Loaded " + signalProviderStats.size() + " providers");
    }

    private void processFile(File file, String basePath) {
        LOGGER.info("Starting to process file: " + file.getName());
        List<String> skippedLines = new ArrayList<>();
        
        // Extrahiere Provider ID aus dem Dateinamen
        String fileName = file.getName();
        String providerId = fileName.substring(fileName.lastIndexOf("_") + 1).replace(".csv", "");
        
        // HTML-Datei im gleichen Verzeichnis suchen
        File htmlFile = new File(basePath + File.separator + providerId + ".html");
        ProviderStats stats = new ProviderStats();
        
        // Debug: HTML-Datei-Info ausgeben
        LOGGER.info("Looking for HTML file: " + htmlFile.getAbsolutePath());
        LOGGER.info("HTML file exists: " + htmlFile.exists());
        
        if (htmlFile.exists()) {
            try {
                String htmlContent = new String(Files.readAllBytes(htmlFile.toPath()), StandardCharsets.UTF_8);
                
                // Debug: HTML-Content ausgeben
                LOGGER.info("HTML Content for " + providerId + ":");
                LOGGER.info(htmlContent);
                
                // Versuche verschiedene Pattern
                String[] patterns = {
                    "(\\d+)\\s*/\\s*(\\d+)[kK]\\s*([A-Za-z]+)",  // 11 / 41K USD
                    "(\\d+)\\s*/\\s*(\\d+)\\s*[kK]\\s*([A-Za-z]+)",  // Mit extra Whitespace
                    "(?s).*?(\\d+)\\s*/\\s*(\\d+)[kK]\\s*([A-Za-z]+).*",  // Mit beliebigem Content davor/danach
                    "(?i)(\\d+)\\s*/\\s*(\\d+)k\\s*([a-z]+)"  // Case-insensitive, vereinfacht
                };
                
                boolean found = false;
                for (String patternStr : patterns) {
                    Pattern pattern = Pattern.compile(patternStr);
                    Matcher matcher = pattern.matcher(htmlContent);
                    
                    LOGGER.info("Trying pattern: " + patternStr);
                    if (matcher.find()) {
                        int userCount = Integer.parseInt(matcher.group(1));
                        double investedCapital = Double.parseDouble(matcher.group(2)) * 1000;
                        String currency = matcher.group(3);
                        
                        LOGGER.info(String.format("Found match: users=%d, capital=%.2f, currency=%s", 
                                  userCount, investedCapital, currency));
                        
                        stats.setUserCount(userCount);
                        stats.setInvestedCapital(investedCapital);
                        stats.setCurrency(currency);
                        found = true;
                        break;
                    }
                }
                
                if (!found) {
                    LOGGER.warning("Could not find user data in HTML for provider " + providerId);
                }
                
            } catch (Exception e) {
                LOGGER.warning("Error reading HTML file for provider " + providerId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        try (BufferedReader reader = new BufferedReader(
                new FileReader(file, StandardCharsets.UTF_8), 32768)) {
                
            String line;
            boolean isHeader = true;
            int lineCount = 0;
            int tradeCounts = 0;
            double initialBalance = 0.0;
            boolean foundFirstBalance = false;

            while ((line = reader.readLine()) != null) {
                lineCount++;
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                line = line.trim();
                if (line.isEmpty()) continue;

                String[] data = line.split(";", -1);

                if (data.length < EXPECTED_MIN_FIELDS) {
                    LOGGER.warning(String.format("Line %d in %s has insufficient fields (%d < %d): %s",
                            lineCount, file.getName(), data.length, EXPECTED_MIN_FIELDS, line));
                    skippedLines.add(line);
                    continue;
                }

                if (data.length >= 2 && "Balance".equalsIgnoreCase(data[1])) {
                    if (!foundFirstBalance) {
                        try {
                            String balanceStr = data[data.length - 1].trim();
                            if (!balanceStr.isEmpty()) {
                                initialBalance = Double.parseDouble(balanceStr);
                                if (initialBalance >= 0) {
                                    stats.setInitialBalance(initialBalance);
                                    foundFirstBalance = true;
                                } else {
                                    LOGGER.warning("Invalid initial balance value (negative) in line " + lineCount + ": " + line);
                                }
                            }
                        } catch (NumberFormatException e) {
                            LOGGER.warning("Invalid balance value in line " + lineCount + ": " + line);
                            skippedLines.add(line);
                        }
                    }
                    continue;
                }

                try {
                    if (data[0].trim().isEmpty() || data[1].trim().isEmpty() || 
                        data[2].trim().isEmpty() || data[3].trim().isEmpty() || 
                        data[4].trim().isEmpty() || data[6].trim().isEmpty() || 
                        data[7].trim().isEmpty()) {
                        
                        LOGGER.warning("Missing or empty required fields in line " + lineCount + ": " + line);
                        skippedLines.add(line);
                        continue;
                    }
                    
                    LocalDateTime openTime = LocalDateTime.parse(data[0].trim(), DATE_TIME_FORMATTER);
                    String type = data[1].trim();
                    double lots = Double.parseDouble(data[2].trim());
                    String symbol = data[3].trim();
                    double openPrice = Double.parseDouble(data[4].trim());
                    LocalDateTime closeTime = LocalDateTime.parse(data[6].trim(), DATE_TIME_FORMATTER);
                    double closePrice = Double.parseDouble(data[7].trim());

                    double commission = 0.0;
                    double swap = 0.0;
                    double profit = 0.0;

                    try {
                        if (data.length > data.length - 3) {
                            commission = data[data.length - 3].trim().isEmpty() ? 0.0 : Double.parseDouble(data[data.length - 3].trim());
                        }
                        if (data.length > data.length - 2) {
                            swap = data[data.length - 2].trim().isEmpty() ? 0.0 : Double.parseDouble(data[data.length - 2].trim());
                        }
                        profit = Double.parseDouble(data[data.length - 1].trim());
                    } catch (NumberFormatException e) {
                        LOGGER.warning("Error parsing financial fields in line " + lineCount + ": " + line);
                        skippedLines.add(line);
                        continue;
                    }

                    stats.addTrade(
                            openTime, closeTime,
                            type, symbol, lots,
                            openPrice, closePrice,
                            commission, swap, profit
                    );

                    tradeCounts++;

                } catch (Exception e) {
                    LOGGER.warning(String.format("Error processing line %d in file %s: %s%nError: %s",
                            lineCount, file.getName(), line, e.getMessage()));
                    skippedLines.add(line);
                    continue;
                }
            }

            if (!stats.getProfits().isEmpty()) {
                signalProviderStats.put(fileName, stats);
                LOGGER.info(String.format("Successfully processed %s: %d trades loaded, Initial Balance: %.2f",
                        file.getName(), tradeCounts, initialBalance));
            }

        } catch (IOException e) {
            LOGGER.severe("Error reading file " + file.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }

        if (!skippedLines.isEmpty()) {
            LOGGER.info("Skipped lines in file " + file.getName() + ": " + skippedLines.size());
            for (String skippedLine : skippedLines) {
                LOGGER.fine("Skipped: " + skippedLine);
            }
        }
    }
    
    public Map<String, ProviderStats> getStats() {
        return signalProviderStats;
    }
=======
   private static final Logger LOGGER = Logger.getLogger(DataManager.class.getName());
   private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");
   private static final int EXPECTED_MIN_FIELDS = 11;
   private static final String BASE_URL = "https://www.mql5.com/en/signals/";
   
   private final Map<String, ProviderStats> signalProviderStats;
   
   public DataManager() {
       this.signalProviderStats = new HashMap<>();
   }
   
   private String extractProviderName(String fileName) {
       // Entferne die .csv Endung
       String name = fileName.toLowerCase().replace(".csv", "");
       // Hier k�nnen weitere Bereinigungen des Provider-Namens erfolgen
       return name;
   }
   
   private String constructProviderURL(String providerId) {
       return BASE_URL + providerId;
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
       List<String> skippedLines = new ArrayList<>();
       
       try (BufferedReader reader = new BufferedReader(
               new FileReader(file, StandardCharsets.UTF_8), 32768)) {
               
           String line;
           boolean isHeader = true;
           ProviderStats stats = new ProviderStats();
           
           // Setze Provider-Informationen
           String providerName = extractProviderName(file.getName());
           String providerURL = constructProviderURL(providerName);
           stats.setSignalProviderInfo(providerName, providerURL);
           
           int lineCount = 0;
           int tradeCounts = 0;
           double initialBalance = 0.0;
           boolean foundFirstBalance = false;

           while ((line = reader.readLine()) != null) {
               lineCount++;
               if (isHeader) {
                   isHeader = false;
                   continue;
               }

               line = line.trim();
               if (line.isEmpty()) continue;

               String[] data = line.split(";", -1);
               
               // Ignoriere leere Balance/Credit Zeilen
               if ((line.startsWith("Balance") || line.startsWith("Credit")) && data.length > 1 && data[1].trim().isEmpty()) {
                   continue;
               }

               // Ignoriere cancelled Trades
               if (line.toLowerCase().contains("cancelled")) {
                   continue;
               }

               if (data.length < EXPECTED_MIN_FIELDS) {
                   continue;
               }

               if (data.length >= 2 && "Balance".equalsIgnoreCase(data[1])) {
                   if (!foundFirstBalance) {
                       try {
                           String balanceStr = data[data.length - 1].trim();
                           if (!balanceStr.isEmpty()) {
                               initialBalance = Double.parseDouble(balanceStr);
                               if (initialBalance >= 0) {
                                   stats.setInitialBalance(initialBalance);
                                   foundFirstBalance = true;
                               }
                           }
                       } catch (NumberFormatException e) {
                           skippedLines.add(line);
                       }
                   }
                   continue;
               }

               try {
                   LocalDateTime openTime = LocalDateTime.parse(data[0].trim(), DATE_TIME_FORMATTER);
                   String type = data[1].trim();
                   double lots = Double.parseDouble(data[2].trim());
                   String symbol = data[3].trim();
                   double openPrice = Double.parseDouble(data[4].trim());

                   double stopLoss = 0.0;
                   double takeProfit = 0.0;
                   LocalDateTime closeTime;
                   double closePrice;

                   boolean hasStopLossTakeProfit = line.contains("S/L") || line.contains("T/P") || data.length >= 12;

                   if (hasStopLossTakeProfit) {
                       if (data.length > 5 && !data[5].trim().isEmpty()) {
                           stopLoss = Double.parseDouble(data[5].trim());
                       }
                       if (data.length > 6 && !data[6].trim().isEmpty()) {
                           takeProfit = Double.parseDouble(data[6].trim());
                       }

                       closeTime = LocalDateTime.parse(data[7].trim(), DATE_TIME_FORMATTER);
                       closePrice = Double.parseDouble(data[8].trim());
                   } else {
                       closeTime = LocalDateTime.parse(data[6].trim(), DATE_TIME_FORMATTER);
                       closePrice = Double.parseDouble(data[7].trim());
                   }

                   double commission = 0.0;
                   double swap = 0.0;
                   double profit = 0.0;

                   int commissionIndex = hasStopLossTakeProfit ? 9 : 8;
                   int swapIndex = hasStopLossTakeProfit ? 10 : 9;
                   int profitIndex = hasStopLossTakeProfit ? 11 : 10;

                   if (data.length > commissionIndex) {
                       commission = data[commissionIndex].trim().isEmpty() ? 0.0 : 
                                  Double.parseDouble(data[commissionIndex].trim());
                   }
                   if (data.length > swapIndex) {
                       swap = data[swapIndex].trim().isEmpty() ? 0.0 : 
                             Double.parseDouble(data[swapIndex].trim());
                   }
                   if (data.length > profitIndex) {
                       profit = Double.parseDouble(data[profitIndex].trim());
                   }

                   stats.addTrade(
                       openTime, closeTime,
                       type, symbol, lots,
                       openPrice, closePrice,
                       stopLoss, takeProfit,
                       commission, swap, profit
                   );

                   tradeCounts++;

               } catch (NumberFormatException e) {
                   skippedLines.add(line);
                   continue;
               } catch (Exception e) {
                   skippedLines.add(line);
                   continue;
               }
           }

           if (!stats.getProfits().isEmpty()) {
               signalProviderStats.put(file.getName(), stats);
               LOGGER.info(String.format("Successfully processed %s: %d trades loaded, Initial Balance: %.2f",
                       file.getName(), tradeCounts, initialBalance));
           }

       } catch (IOException e) {
           LOGGER.severe("Error reading file " + file.getName() + ": " + e.getMessage());
           e.printStackTrace();
       }
   }
   
   public Map<String, ProviderStats> getStats() {
       return signalProviderStats;
   }
>>>>>>> feb2025
}