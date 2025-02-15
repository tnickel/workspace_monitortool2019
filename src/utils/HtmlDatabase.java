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
    
    public Map<String, Double> getMonthlyProfitPercentages(String csvFileName) {
        Map<String, String> data = getFileData(csvFileName);
        Map<String, Double> monthlyProfits = new HashMap<>();
        
        String profitData = data.get("MonthProfitProz");
        if (profitData == null || profitData.trim().isEmpty()) {
            return monthlyProfits;
        }
        
        String[] monthEntries = profitData.split(",");
        for (String entry : monthEntries) {
            String[] parts = entry.split("=");
            if (parts.length == 2) {
                try {
                    String yearMonth = parts[0].trim();
                    double profitPercentage = Double.parseDouble(parts[1].trim());
                    monthlyProfits.put(yearMonth, profitPercentage);
                } catch (NumberFormatException e) {
                    LOGGER.warning("Could not parse profit percentage for entry: " + entry);
                }
            }
        }
        
        return monthlyProfits;
    }
    
    public double getAvr3MonthProfit(String csvFileName) {
        Map<String, String> data = getFileData(csvFileName);
        String profitStr = data.getOrDefault("Average3MonthProfit", "0,00")
                              .replace(",", ".")
                              .replace(" ", "");
                              
        // Berechne die Details für den Tooltip
        updateAvr3MonthProfitCalculation(csvFileName);
                              
        try {
            return Double.parseDouble(profitStr);
        } catch (NumberFormatException e) {
            LOGGER.warning("Could not parse average profit: " + profitStr);
            return 0.0;
        }
    }
    
    private void updateAvr3MonthProfitCalculation(String csvFileName) {
        Map<String, Double> monthlyProfits = getMonthlyProfitPercentages(csvFileName);
        if (monthlyProfits.isEmpty()) {
            return;
        }
        
        List<String> sortedMonths = new ArrayList<>(monthlyProfits.keySet());
        sortedMonths.sort((a, b) -> b.compareTo(a)); // Absteigend sortieren
        
        StringBuilder details = new StringBuilder();
        details.append("Verwendete Monate für die Berechnung:\n");
        
        double sum = 0.0;
        int count = 0;
        int maxMonths = Math.min(3, sortedMonths.size() - 1); // -1 für aktuellen Monat
        
        for (int i = 1; i <= maxMonths; i++) {
            String month = sortedMonths.get(i);
            double profit = monthlyProfits.get(month);
            sum += profit;
            count++;
            details.append(String.format("- %s: %.2f%%\n", month, profit));
        }
        
        double average = count > 0 ? sum / count : 0.0;
        details.append(String.format("\nDurchschnitt über %d Monate: %.2f%%", count, average));
        
        Map<String, String> data = getFileData(csvFileName);
        data.put("3MonthProfitCalculation", details.toString());
        dataCache.put(csvFileName, data);
    }
    
    public String get3MonthProfitTooltip(String csvFileName) {
        Map<String, String> data = getFileData(csvFileName);
        String details = data.get("3MonthProfitCalculation");
        
        if (details == null || details.trim().isEmpty()) {
            return "Keine Berechnungsdetails verfügbar";
        }

        StringBuilder tooltip = new StringBuilder();
        tooltip.append("<html><div style='padding: 5px; white-space: nowrap;'>");
        tooltip.append("<b>3-Monats-Profit Berechnung:</b><br>");
        tooltip.append("<br>");

        String[] lines = details.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("-")) {
                tooltip.append("&nbsp;&nbsp;")
                      .append(line)
                      .append("<br>");
            } else {
                tooltip.append(line).append("<br>");
            }
        }

        tooltip.append("</div></html>");
        return tooltip.toString();
    }
    
    public double getMPDD(String csvFileName, int months) {
        updateMPDDCalculation(csvFileName, months);
        Map<String, String> data = getFileData(csvFileName);
        String mpddStr = data.getOrDefault(months + "MPDD", "0,00")
                            .replace(",", ".")
                            .replace(" ", "");
        try {
            return Double.parseDouble(mpddStr);
        } catch (NumberFormatException e) {
            LOGGER.warning("Could not parse " + months + "MPDD: " + mpddStr);
            return 0.0;
        }
    }
    
    public String getMPDDTooltip(String csvFileName, int months) {
        Map<String, String> data = getFileData(csvFileName);
        String details = data.get(months + "MPDDCalculation");
        
        if (details == null || details.trim().isEmpty()) {
            return String.format("Keine Berechnungsdetails für %d-Monats-Drawdown verfügbar", months);
        }

        StringBuilder tooltip = new StringBuilder();
        tooltip.append("<html><div style='padding: 5px; white-space: nowrap;'>");
        tooltip.append(String.format("<b>%d-Monats-Drawdown Berechnung:</b><br>", months));
        tooltip.append("<br>");

        String[] lines = details.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("-")) {
                tooltip.append("&nbsp;&nbsp;")
                      .append(line)
                      .append("<br>");
            } else if (line.contains(":") && !line.startsWith("-")) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    tooltip.append("<b>")
                          .append(parts[0])
                          .append(":</b> ")
                          .append(parts[1].trim())
                          .append("<br>");
                }
            } else {
                tooltip.append(line).append("<br>");
            }
        }

        tooltip.append("</div></html>");
        return tooltip.toString();
    }
    
    private void updateMPDDCalculation(String csvFileName, int months) {
        Map<String, Double> monthlyProfits = getMonthlyProfitPercentages(csvFileName);
        if (monthlyProfits.isEmpty()) {
            return;
        }
        
        List<String> sortedMonths = new ArrayList<>(monthlyProfits.keySet());
        sortedMonths.sort((a, b) -> b.compareTo(a));
        
        StringBuilder details = new StringBuilder();
        details.append(String.format("Verwendete Monate für %d-Monats-Drawdown Berechnung:\n", months));
        
        double maxDrawdown = 0.0;
        int maxStart = 0;
        int maxEnd = 0;
        
        int availableMonths = Math.min(months, sortedMonths.size() - 1);
        
        if (availableMonths > 0) {
            for (int i = 1; i <= availableMonths - 1; i++) {
                for (int j = i + 1; j <= availableMonths; j++) {
                    double drawdown = calculateDrawdown(monthlyProfits, sortedMonths.subList(i, j + 1));
                    if (drawdown > maxDrawdown) {
                        maxDrawdown = drawdown;
                        maxStart = i;
                        maxEnd = j;
                    }
                }
            }
            
            details.append("\nBetrachteter Zeitraum:\n");
            for (int i = 1; i <= availableMonths; i++) {
                String month = sortedMonths.get(i);
                double profit = monthlyProfits.get(month);
                details.append(String.format("- %s: %.2f%%\n", month, profit));
            }
            
            if (maxDrawdown > 0) {
                details.append(String.format("\nMaximaler Drawdown: %.2f%%\n", maxDrawdown));
                details.append("Zeitraum des max. Drawdowns:\n");
                for (int i = maxStart; i <= maxEnd; i++) {
                    String month = sortedMonths.get(i);
                    double profit = monthlyProfits.get(month);
                    details.append(String.format("- %s: %.2f%%\n", month, profit));
                }
            }
        } else {
            details.append("\nNicht genügend Monate für die Berechnung verfügbar.");
        }
        
        Map<String, String> data = getFileData(csvFileName);
        data.put(months + "MPDDCalculation", details.toString());
        dataCache.put(csvFileName, data);
    }
    
    private double calculateDrawdown(Map<String, Double> profits, List<String> months) {
        double cumulativeProfit = 100.0;
        double minEquity = cumulativeProfit;
        
        for (String month : months) {
            double monthProfit = profits.get(month);
            cumulativeProfit *= (1 + monthProfit / 100.0);
            minEquity = Math.min(minEquity, cumulativeProfit);
        }
        
        return 100.0 * (1 - minEquity / 100.0);
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

        String[] lines = details.split("\n");
        boolean inMonthsList = false;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("Verwendete Monatswerte")) {
                formattedDetails.append("<b>Verwendete Monatswerte:</b>");
                formattedDetails.append("<br>");
                inMonthsList = true;
                continue;
            } 
            
            if (line.startsWith("-") && inMonthsList) {
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
    
    public double getAverageMonthlyProfit(String csvFileName, int n) {
        Map<String, Double> monthlyProfits = getMonthlyProfitPercentages(csvFileName);
        if (monthlyProfits.isEmpty() || n <= 0) {
            return 0.0;
        }
        
        List<String> sortedMonths = new ArrayList<>(monthlyProfits.keySet());
        sortedMonths.sort((a, b) -> b.compareTo(a)); // Absteigend sortieren
        
        // Nach Abzug des aktuellen Monats verfügbare Monate
        int availableMonths = sortedMonths.size() - 1; // -1 für aktuellen Monat
        
        // Wenn nicht genügend Monate für die angeforderte Berechnung verfügbar sind
        if (n == 3) {
            // Für 3MPDD bisheriges Verhalten beibehalten
            int monthsToUse = Math.min(n, availableMonths);
            if (monthsToUse <= 0) {
                return 0.0;
            }
            
            double sum = 0.0;
            for (int i = 0; i < monthsToUse; i++) {
                sum += monthlyProfits.get(sortedMonths.get(i + 1)); // +1 um aktuellen Monat zu überspringen
            }
            return sum / monthsToUse;
        } else {
            // Für 6, 9 und 12 MPDD: Nur berechnen wenn genügend Monate verfügbar
            int requiredMonths = n + 1; // +1 weil aktueller Monat nicht berücksichtigt wird
            if (sortedMonths.size() < requiredMonths) {
                return 0.0;
            }
            
            double sum = 0.0;
            for (int i = 0; i < n; i++) {
                sum += monthlyProfits.get(sortedMonths.get(i + 1)); // +1 um aktuellen Monat zu überspringen
            }
            return sum / n;
        }
    }
    
    public List<String> getLastThreeMonthsDetails(String csvFileName) {
        Map<String, String> data = getFileData(csvFileName);
        List<String> details = new ArrayList<>();
        
        String monthsSection = data.get("Last 3 Months Details");
        if (monthsSection != null) {
            String[] lines = monthsSection.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.matches("\\d{4}/\\d{1,2}:\\s*[0-9.]+")) {
                    details.add(line);
                }
            }
        }
        
        return details;
    }
}