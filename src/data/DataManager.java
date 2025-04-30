package data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class DataManager {
   private static final Logger LOGGER = Logger.getLogger(DataManager.class.getName());
   private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");
   private static final int EXPECTED_MIN_FIELDS = 11;
   private static final String BASE_URL = "https://www.mql5.com/en/signals/";
   
   private final Map<String, ProviderStats> signalProviderStats;
   
   // Fortschritts-Callback
   private Consumer<Integer> progressCallback;
   private Consumer<String> statusCallback;
   
   // Singleton-Instanz
   private static DataManager instance;
   
   // Debug-Einstellungen
   private boolean showDebugDialog = true;
   
   public DataManager() {
       this.signalProviderStats = new HashMap<>();
   }
   
   public void setProgressCallback(Consumer<Integer> progressCallback) {
       this.progressCallback = progressCallback;
   }
   
   public void setStatusCallback(Consumer<String> statusCallback) {
       this.statusCallback = statusCallback;
   }
   
   public static synchronized DataManager getInstance() {
       if (instance == null) {
           instance = new DataManager();
       }
       return instance;
   }
   
   public static void setInstance(DataManager manager) {
       instance = manager;
   }
   
   private String extractProviderName(String fileName) {
       // Entferne die .csv Endung
       String name = fileName.toLowerCase().replace(".csv", "");
       // Hier können weitere Bereinigungen des Provider-Namens erfolgen
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
               
               // Status melden
               if (statusCallback != null) {
                   statusCallback.accept("Lade Dateien: 0/" + files.length);
               }
               
               // Fortschritt für alle Dateien
               for (int i = 0; i < files.length; i++) {
                   File file = files[i];
                   // Status melden
                   if (statusCallback != null) {
                       statusCallback.accept("Lade Datei: " + file.getName() + " (" + (i+1) + "/" + files.length + ")");
                   }
                   
                   processFile(file);
                   
                   // Fortschritt aktualisieren
                   if (progressCallback != null) {
                       int progress = (int) ((i + 1) / (double) files.length * 100);
                       progressCallback.accept(progress);
                   }
               }
           }
       }
       LOGGER.info("Loaded " + signalProviderStats.size() + " providers");
       
       // Status melden
       if (statusCallback != null) {
           statusCallback.accept("Daten geladen: " + signalProviderStats.size() + " Provider");
       }
       
       // Setze instance beim Laden, falls noch nicht gesetzt
       if (instance == null) {
           instance = this;
       }
       
     
   }

   private void processFile(File file) {
       LOGGER.info("Starting to process file: " + file.getName());
       List<String> allLines = new ArrayList<>();
       List<String> skippedLines = new ArrayList<>();
       
       try (BufferedReader reader = new BufferedReader(
               new FileReader(file, StandardCharsets.UTF_8), 32768)) {
           String line;
           while ((line = reader.readLine()) != null) {
               allLines.add(line);
           }
       } catch (IOException e) {
           LOGGER.severe("Error reading file " + file.getName() + ": " + e.getMessage());
           return;
       }
       
       if (allLines.isEmpty()) {
           LOGGER.warning("File " + file.getName() + " is empty");
           return;
       }
       
       // Provider-Informationen
       String providerName = extractProviderName(file.getName());
       String providerURL = constructProviderURL(providerName);
       
       // Debug-Variablen für Datumsbereich
       LocalDateTime earliestDate = null;
       LocalDateTime latestDate = null;
       
       // Provider-Stats erstellen
       ProviderStats stats = new ProviderStats();
       stats.setSignalProviderInfo(providerName, providerURL);
       
       // Header-Zeile überspringen
       boolean isHeader = true;
       double initialBalance = 0.0;
       boolean foundFirstBalance = false;
       int tradeCounts = 0;
       
       for (String line : allLines) {
           if (isHeader) {
               isHeader = false;
               continue;
           }
           
           line = line.trim();
           if (line.isEmpty()) continue;
           
           String[] data = line.split(";", -1);
           
           // Logge die ersten paar Zeilen zur Überprüfung
           if (tradeCounts < 3 || allLines.size() - allLines.indexOf(line) < 3) {
               LOGGER.info("Sample line: " + line);
           }
           
           // Ignoriere leere Balance/Credit Zeilen
           if ((line.startsWith("Balance") || line.startsWith("Credit")) && data.length > 1 && data[1].trim().isEmpty()) {
               continue;
           }
           
           // Verarbeite Balance-Einträge
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
           
           // Ignoriere cancelled Trades
           if (line.toLowerCase().contains("cancelled")) {
               continue;
           }
           
           try {
               // Prüfe, ob wir genügend Felder haben
               if (data.length < 10) {
                   LOGGER.warning("Line with too few fields: " + line + " (has " + data.length + " fields)");
                   skippedLines.add(line);
                   continue;
               }
               
               // Zeit-Felder
               LocalDateTime openTime;
               LocalDateTime closeTime;
               
               try {
                   openTime = LocalDateTime.parse(data[0].trim(), DATE_TIME_FORMATTER);
               } catch (DateTimeParseException e) {
                   LOGGER.warning("Failed to parse open time: " + data[0] + " in line: " + line);
                   skippedLines.add(line);
                   continue;
               }
               
               // Aktualisiere den ersten Trade-Zeitpunkt
               if (earliestDate == null || openTime.isBefore(earliestDate)) {
                   earliestDate = openTime;
               }
               
               // Trade-Typ und Symbol
               String type = data[1].trim();
               String symbol = data[3].trim();
               
               // Volume, Open Price und Close Price
               double lots, openPrice, closePrice;
               try {
                   lots = Double.parseDouble(data[2].trim().replace(",", "."));
                   openPrice = Double.parseDouble(data[4].trim().replace(",", ".").replace(" ", ""));
               } catch (NumberFormatException e) {
                   LOGGER.warning("Failed to parse numeric value in line: " + line);
                   skippedLines.add(line);
                   continue;
               }
               
               // S/L und T/P können leer sein
               double stopLoss = 0.0;
               double takeProfit = 0.0;
               
               // Bestimme das Format: mit oder ohne S/L und T/P
               boolean hasStopLossTakeProfit = false;
               
               // Versuche zu erkennen, ob S/L oder T/P vorhanden sind
               if (data.length > 6) {
                   try {
                       // Versuche S/L als Zahl zu parsen - wenn es klappt, ist es ein S/L
                       if (!data[5].trim().isEmpty()) {
                           stopLoss = Double.parseDouble(data[5].trim().replace(",", ".").replace(" ", ""));
                           hasStopLossTakeProfit = true;
                       }
                       
                       // Versuche T/P als Zahl zu parsen - wenn es klappt, ist es ein T/P
                       if (!data[6].trim().isEmpty()) {
                           takeProfit = Double.parseDouble(data[6].trim().replace(",", ".").replace(" ", ""));
                           hasStopLossTakeProfit = true;
                       }
                   } catch (NumberFormatException e) {
                       // Wenn das Parsen fehlschlägt, ist es wahrscheinlich kein S/L oder T/P
                       hasStopLossTakeProfit = false;
                   }
                   
                   // Wenn Feld 7 ein Datum ist, haben wir definitiv S/L und T/P
                   if (data.length > 7) {
                       try {
                           LocalDateTime.parse(data[7].trim(), DATE_TIME_FORMATTER);
                           hasStopLossTakeProfit = true;
                       } catch (DateTimeParseException e) {
                           // Wenn das nicht klappt, ist es kein Datum
                       }
                   }
               }
               
               // Bestimme die Indizes für Close Time und Close Price
               int closeTimeIndex = hasStopLossTakeProfit ? 7 : 5;
               int closePriceIndex = hasStopLossTakeProfit ? 8 : 6;
               
               // Prüfe, ob genügend Felder vorhanden sind
               if (data.length <= closeTimeIndex) {
                   LOGGER.warning("Not enough fields for close time at index " + closeTimeIndex + " in line: " + line);
                   skippedLines.add(line);
                   continue;
               }
               
               // Parse Close Time
               try {
                   closeTime = LocalDateTime.parse(data[closeTimeIndex].trim(), DATE_TIME_FORMATTER);
               } catch (DateTimeParseException e) {
                   LOGGER.warning("Failed to parse close time: " + data[closeTimeIndex] + " in line: " + line);
                   skippedLines.add(line);
                   continue;
               }
               
               // Aktualisiere den letzten Trade-Zeitpunkt
               if (latestDate == null || closeTime.isAfter(latestDate)) {
                   latestDate = closeTime;
               }
               
               // Parse Close Price
               try {
                   if (data.length <= closePriceIndex) {
                       LOGGER.warning("Not enough fields for close price at index " + closePriceIndex + " in line: " + line);
                       skippedLines.add(line);
                       continue;
                   }
                   closePrice = Double.parseDouble(data[closePriceIndex].trim().replace(",", ".").replace(" ", ""));
               } catch (NumberFormatException e) {
                   LOGGER.warning("Failed to parse close price: " + data[closePriceIndex] + " in line: " + line);
                   skippedLines.add(line);
                   continue;
               }
               
               // Bestimme die Indizes für Commission, Swap und Profit
               int commissionIndex = hasStopLossTakeProfit ? 9 : 7;
               int swapIndex = hasStopLossTakeProfit ? 10 : 8;
               int profitIndex = hasStopLossTakeProfit ? 11 : 9;
               
               // Parse Commission, Swap und Profit
               double commission = 0.0;
               double swap = 0.0;
               double profit = 0.0;
               
               try {
                   if (data.length > commissionIndex && !data[commissionIndex].trim().isEmpty()) {
                       commission = Double.parseDouble(data[commissionIndex].trim().replace(",", "."));
                   }
                   
                   if (data.length > swapIndex && !data[swapIndex].trim().isEmpty()) {
                       swap = Double.parseDouble(data[swapIndex].trim().replace(",", "."));
                   }
                   
                   if (data.length > profitIndex) {
                       String profitStr = data[profitIndex].trim();
                       
                       // Bereinige den Profit von Kommentaren wie [sl]
                       if (profitStr.contains("[")) {
                           profitStr = profitStr.split("\\[")[0].trim();
                       }
                       
                       profit = Double.parseDouble(profitStr.replace(",", "."));
                   } else {
                       LOGGER.warning("Not enough fields for profit at index " + profitIndex + " in line: " + line);
                       skippedLines.add(line);
                       continue;
                   }
               } catch (NumberFormatException e) {
                   LOGGER.warning("Failed to parse commission/swap/profit in line: " + line);
                   skippedLines.add(line);
                   continue;
               }
               
               // Erstelle den Trade und füge ihn zu den Stats hinzu
               stats.addTrade(
                   openTime, closeTime,
                   type, symbol, lots,
                   openPrice, closePrice,
                   stopLoss, takeProfit,
                   commission, swap, profit
               );
               
               tradeCounts++;
               
           } catch (Exception e) {
               LOGGER.log(Level.WARNING, "General error parsing line: " + line, e);
               skippedLines.add(line);
           }
       }
       
       // Nur hinzufügen, wenn Trades vorhanden sind
       if (!stats.getProfits().isEmpty()) {
           signalProviderStats.put(file.getName(), stats);
           LOGGER.info(String.format("Successfully processed %s: %d trades loaded, Initial Balance: %.2f",
                   file.getName(), tradeCounts, initialBalance));
           
           // Log der Zeit-Spanne
           if (earliestDate != null && latestDate != null) {
               LOGGER.info(String.format("Date range for %s: %s to %s",
                                        file.getName(),
                                        earliestDate.format(DATE_TIME_FORMATTER),
                                        latestDate.format(DATE_TIME_FORMATTER)));
           }
       } else {
           LOGGER.warning("No trades processed for file: " + file.getName());
       }
       
       // Log der übersprungenen Zeilen
       if (!skippedLines.isEmpty()) {
           LOGGER.info(String.format("Skipped %d lines in %s", skippedLines.size(), file.getName()));
       }
   }
   
  
   
   public Map<String, ProviderStats> getStats() {
       return signalProviderStats;
   }
}