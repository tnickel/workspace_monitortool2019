package utils;



import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class HtmlDatabase {
    private static final Logger LOGGER = Logger.getLogger(HtmlDatabase.class.getName());
    
    private final String rootPath;
    private final Map<String, Map<String, String>> dataCache;
    
    public HtmlDatabase(String rootPath) {
        this.rootPath = rootPath;
        this.dataCache = new HashMap<>();
    }
    
    private Map<String, String> getFileData(String csvFileName) {
        if (dataCache.containsKey(csvFileName)) {
            return dataCache.get(csvFileName);
        }

        String txtFileName = csvFileName.replace(".csv", "_root.txt");
        File txtFile = new File(rootPath, txtFileName);
        
        if (!txtFile.exists()) {
            LOGGER.warning("Text file not found: " + txtFile.getAbsolutePath());
            return new HashMap<>();
        }
        
        Map<String, String> data = new HashMap<>();
        StringBuilder currentSection = new StringBuilder();
        String currentKey = null;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(txtFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Ignoriere Asterisk-Linien
                if (line.trim().matches("\\*+")) {
                    if (currentKey != null && currentSection.length() > 0) {
                        data.put(currentKey, currentSection.toString().trim());
                        currentSection = new StringBuilder();
                    }
                    continue;
                }
                
                // Prüfe ob es eine neue Sektion ist
                if (line.contains("=")) {
                    // Speichere die vorherige Sektion, falls vorhanden
                    if (currentKey != null && currentSection.length() > 0) {
                        data.put(currentKey, currentSection.toString().trim());
                        currentSection = new StringBuilder();
                    }
                    
                    String[] parts = line.split("=", 2);
                    currentKey = parts[0].trim();
                    if (parts.length > 1) {
                        currentSection.append(parts[1].trim());
                    }
                } else if (currentKey != null && !line.trim().isEmpty()) {
                    // Füge die Zeile zur aktuellen Sektion hinzu
                    if (currentSection.length() > 0) {
                        currentSection.append("\n");
                    }
                    currentSection.append(line.trim());
                }
            }
            
            // Speichere die letzte Sektion
            if (currentKey != null && currentSection.length() > 0) {
                data.put(currentKey, currentSection.toString().trim());
            }
            
            dataCache.put(csvFileName, data);
            return data;
        } catch (IOException e) {
            LOGGER.severe("Error reading text file: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    public double getBalance(String csvFileName) {
        Map<String, String> data = getFileData(csvFileName);
        String balanceStr = data.getOrDefault("Balance", "0,00")
                               .replace(",", ".")
                               .replace(" ", "");
        try {
            return Double.parseDouble(balanceStr);
        } catch (NumberFormatException e) {
            LOGGER.warning("Could not parse balance: " + balanceStr);
            return 0.0;
        }
    }
    
    public double getEquityDrawdown(String csvFileName) {
        Map<String, String> data = getFileData(csvFileName);
        String drawdownStr = data.getOrDefault("EquityDrawdown", "0,00")
                                .replace(",", ".")
                                .replace(" ", "");
        try {
            return Double.parseDouble(drawdownStr);
        } catch (NumberFormatException e) {
            LOGGER.warning("Could not parse equity drawdown: " + drawdownStr);
            return 0.0;
        }
    }
    
    public double getAvr3MonthProfit(String csvFileName) {
        Map<String, String> data = getFileData(csvFileName);
        String profitStr = data.getOrDefault("Average3MonthProfit", "0,00")
                              .replace(",", ".")
                              .replace(" ", "");
        try {
            return Double.parseDouble(profitStr);
        } catch (NumberFormatException e) {
            LOGGER.warning("Could not parse average profit: " + profitStr);
            return 0.0;
        }
    }
    
    public List<String> getLastThreeMonthsDetails(String csvFileName) {
        Map<String, String> data = getFileData(csvFileName);
        List<String> details = new ArrayList<>();
        
        // Hole den gesamten Text zwischen "Last 3 Months Details=" und den nächsten Asterisk-Block
        String monthsSection = data.get("Last 3 Months Details");
        if (monthsSection != null) {
            // Split den Text in Zeilen und verarbeite jede Zeile
            String[] lines = monthsSection.split("\n");
            for (String line : lines) {
                line = line.trim();
                // Prüfe ob die Zeile ein gültiges Monatsformat hat (YYYY/MM: value)
                if (line.matches("\\d{4}/\\d{1,2}:\\s*[0-9.]+")) {
                    details.add(line);
                }
            }
        }
        
        return details;
    }
    
    public double getStabilitaetswert(String csvFileName) {
        Map<String, String> data = getFileData(csvFileName);
        String stabilityStr = data.getOrDefault("StabilityValue", "1,00")
                                 .replace(",", ".")
                                 .replace(" ", "");
        try {
            return Double.parseDouble(stabilityStr);
        } catch (NumberFormatException e) {
            LOGGER.warning("Could not parse stability value: " + stabilityStr);
            return 1.0;
        }
    }
    
    public String getStabilitaetswertDetails(String csvFileName) {
        Map<String, String> data = getFileData(csvFileName);
        String details = data.get("Stability Details");
        
        if (details == null || details.trim().isEmpty()) {
            return "Keine Stabilitätsdetails verfügbar";
        }

        StringBuilder formattedDetails = new StringBuilder();
        formattedDetails.append("<html><div style='padding: 5px; white-space: nowrap;'>");
        formattedDetails.append("<b>Stabilitätsanalyse:</b><br>");
        formattedDetails.append("<br>");

        // Verarbeite die Details zeilenweise
        String[] lines = details.split("\n");
        boolean inMonthsList = false;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("Verwendete Monatswerte")) {
                // Setzt "Verwendete Monatswerte:" in eine eigene Zeile
                formattedDetails.append("<b>Verwendete Monatswerte:</b>");
                formattedDetails.append("<br>");
                inMonthsList = true;
                continue;
            } 
            
            if (line.startsWith("-") && inMonthsList) {
                // Einrückung für Monatswerte und erzwungener Zeilenumbruch
                formattedDetails.append("&nbsp;&nbsp;")
                              .append(line)
                              .append("<br>");
                continue;
            }
            
            if (line.contains(":") && !line.startsWith("-")) {
                inMonthsList = false;
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    formattedDetails.append("<b>")
                                  .append(parts[0])
                                  .append(":</b> ")
                                  .append(parts[1].trim())
                                  .append("<br>");
                }
            }
        }

        formattedDetails.append("</div></html>");
        return formattedDetails.toString();
    }
}