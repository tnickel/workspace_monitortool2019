package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class HtmlDatabase {
    private static final Logger LOGGER = Logger.getLogger(HtmlDatabase.class.getName());
    
    private final String downloadPath;
    private final Map<String, Map<String, String>> dataCache;
    
    public HtmlDatabase(String downloadpath) {
        this.downloadPath = downloadpath;
        this.dataCache = new HashMap<>();
        
        // Protokolliere den tatsächlich verwendeten Pfad
        LOGGER.info("HtmlDatabase initialisiert mit Pfad: " + downloadPath);
    }
    
    // Hilfsmethode, um einen konsistenten Dateinamen als Cache-Schlüssel zu erstellen
    private String createCacheKey(String fileName) {
        if (fileName.endsWith(".csv")) {
            return fileName; // CSV-Dateien als Schlüssel belassen
        } else if (fileName.endsWith("_root.txt")) {
            return fileName.replace("_root.txt", ".csv"); // Zu CSV-Format konvertieren
        } else {
            return fileName + ".csv"; // Suffix hinzufügen für Konsistenz
        }
    }
    
    // Hilfsmethode, um einen korrekten Pfad zur TXT-Datei zu erstellen
    private String createTxtFilePath(String fileName) {
        if (fileName.endsWith("_root.txt")) {
            return fileName; // Bereits korrekt
        } else if (fileName.endsWith(".csv")) {
            return fileName.replace(".csv", "") + "_root.txt";
        } else {
            return fileName + "_root.txt"; // Suffix hinzufügen
        }
    }
    
    private Map<String, String> getFileData(String fileName) {
        String cacheKey = createCacheKey(fileName);
        
        if (dataCache.containsKey(cacheKey)) {
            return dataCache.get(cacheKey);
        }

        String txtFileName = createTxtFilePath(fileName);
        File txtFile = new File(downloadPath, txtFileName);
        
        // Protokolliere den vollständigen Pfad zur Datei
        LOGGER.info("Versuche Textdatei zu lesen: " + txtFile.getAbsolutePath());
        
        if (!txtFile.exists()) {
            LOGGER.warning("Text file not found: " + txtFile.getAbsolutePath());
            System.err.println("WARNUNG: Die Datei " + txtFile.getAbsolutePath() + " wurde nicht gefunden!");
            
            // Prüfe, ob das Verzeichnis überhaupt existiert
            File dir = new File(downloadPath);
            if (!dir.exists() || !dir.isDirectory()) {
                LOGGER.severe("Das Verzeichnis existiert nicht: " + downloadPath);
                System.err.println("FEHLER: Das Verzeichnis " + downloadPath + " existiert nicht!");
            } else {
                // Liste die Dateien im Verzeichnis auf, um zu debuggen
                File[] files = dir.listFiles((d, name) -> name.endsWith("_root.txt"));
                if (files != null && files.length > 0) {
                    System.out.println("Gefundene _root.txt Dateien im Verzeichnis (" + files.length + "):");
                    for (int i = 0; i < Math.min(files.length, 5); i++) {
                        System.out.println(" - " + files[i].getName());
                    }
                    if (files.length > 5) {
                        System.out.println(" ... und " + (files.length - 5) + " weitere");
                    }
                } else {
                    System.err.println("Keine _root.txt Dateien im Verzeichnis gefunden: " + downloadPath);
                }
            }
            
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
            
            dataCache.put(cacheKey, data);
            
            // Log die gelesenen Schlüssel
            LOGGER.info("Gelesen aus " + txtFileName + ", gefundene Schlüssel: " + data.keySet());
            
            return data;
        } catch (IOException e) {
            LOGGER.severe("Error reading text file: " + e.getMessage());
            System.err.println("FEHLER beim Lesen der Datei " + txtFile.getAbsolutePath() + ": " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * Liest die Drawdown-Chart-Daten direkt aus der .txt-Datei
     * 
     * @param fileName Name der Datei (z.B. provider_123456.csv)
     * @return String mit Drawdown-Chart-Daten oder null wenn nicht gefunden
     */
    public String readDrawdownChartDataFromFile(String fileName) {
        String txtFileName = createTxtFilePath(fileName);
        File txtFile = new File(downloadPath, txtFileName);
        
        if (!txtFile.exists()) {
            LOGGER.warning("Textdatei nicht gefunden: " + txtFile.getAbsolutePath());
            return null;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(txtFile))) {
            StringBuilder drawdownData = new StringBuilder();
            boolean inDrawdownSection = false;
            String line;
            
            while ((line = reader.readLine()) != null) {
                // Prüfe, ob die Drawdown-Sektion beginnt
                if (line.startsWith("Drawdown Chart Data=")) {
                    inDrawdownSection = true;
                    
                    // Wenn die Zeile bereits Daten enthält, füge sie hinzu
                    if (line.contains(":")) {
                        String dataLine = line.substring(line.indexOf("=") + 1).trim();
                        if (!dataLine.isEmpty()) {
                            drawdownData.append(dataLine).append("\n");
                        }
                    }
                    continue;
                }
                
                // Wenn wir in der Drawdown-Sektion sind und auf eine leere Zeile oder neue Sektion stoßen, beende die Sektion
                if (inDrawdownSection && (line.trim().isEmpty() || line.contains("="))) {
                    inDrawdownSection = false;
                }
                
                // Füge Zeilen innerhalb der Drawdown-Sektion hinzu
                if (inDrawdownSection && line.contains(":")) {
                    drawdownData.append(line).append("\n");
                }
            }
            
            String result = drawdownData.toString().trim();
            return result.isEmpty() ? null : result;
            
        } catch (IOException e) {
            LOGGER.warning("Fehler beim Lesen der Drawdown-Daten aus der Datei: " + e.getMessage());
            return null;
        }
    }

    /**
     * Ermittelt den maximalen Drawdown der letzten 3 Monate für einen Provider
     * 
     * @param fileName Dateiname des Providers
     * @return Maximaler Drawdown in Prozent oder 0.0 wenn keine Daten gefunden wurden
     */
    public double getMaxDrawdown3M(String fileName) {
        String drawdownData = readDrawdownChartDataFromFile(fileName);
        if (drawdownData == null || drawdownData.isEmpty()) {
            LOGGER.warning("Keine Drawdown-Daten für " + fileName + " gefunden");
            return 0.0;
        }
        
        // Aktuelles Datum für die 3-Monats-Berechnung
        LocalDate currentDate = LocalDate.now();
        LocalDate threeMonthsAgo = currentDate.minusMonths(3);
        
        double maxDrawdown = 0.0;
        LocalDate maxDrawdownDate = null;
        String[] lines = drawdownData.split("\n");
        
        if (lines.length <= 1) {
            LOGGER.warning("Nicht genügend Drawdown-Daten für " + fileName);
            return 0.0;
        }
        
        // Debug-Ausgabe
        System.out.println("===== Analysiere Drawdown-Daten für " + fileName + " =====");
        System.out.println("Zeitraum: von " + threeMonthsAgo + " bis " + currentDate);
        System.out.println("Gefundene Zeilen: " + lines.length);
        
        // Für Debugging: Alle Drawdown-Werte ausgeben, die größer als 10% sind
        System.out.println("Signifikante Drawdown-Werte (>10%):");
        
        int relevantDataCount = 0;
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            String[] parts = line.split(":");
            if (parts.length != 2) {
                continue;
            }
            
            try {
                // Parsen des Datums aus dem Format YYYY-MM-DD
                String dateStr = parts[0].trim();
                LocalDate lineDate = LocalDate.parse(dateStr);
                
                // Nur Daten der letzten 3 Monate berücksichtigen
                if (lineDate.isAfter(threeMonthsAgo) || lineDate.isEqual(threeMonthsAgo)) {
                    relevantDataCount++;
                    
                    // Drawdown-Wert parsen (Prozentangabe mit Komma)
                    String valueStr = parts[1].trim().replace("%", "").replace(",", ".");
                    double drawdown = Double.parseDouble(valueStr);
                    
                    // Für Debug: Signifikante Werte ausgeben
                    if (drawdown > 10.0) {
                        System.out.println(dateStr + ": " + drawdown + "%");
                    }
                    
                    // Maximum aktualisieren
                    if (drawdown > maxDrawdown) {
                        maxDrawdown = drawdown;
                        maxDrawdownDate = lineDate;
                    }
                }
            } catch (Exception e) {
                System.err.println("Fehler beim Parsen der Zeile: " + line + " - " + e.getMessage());
            }
        }
        
        System.out.println("Relevante Datenpunkte in den letzten 3 Monaten: " + relevantDataCount);
        System.out.println("Maximaler 3-Monats-Drawdown: " + maxDrawdown + "%" + 
                          (maxDrawdownDate != null ? " (am " + maxDrawdownDate + ")" : ""));
        System.out.println("============================================");
        
        // Speichere den maximalen 3-Monats-Drawdown in der Datenbank
        Map<String, String> data = getFileData(fileName);
        if (!data.isEmpty()) {
            data.put("MaxDrawdown3M", String.format("%.2f", maxDrawdown).replace(',', '.'));
            dataCache.put(createCacheKey(fileName), data);
        }
        
        return maxDrawdown;
    }
    public double getBalance(String fileName) {
        Map<String, String> data = getFileData(fileName);
        if (data.isEmpty()) {
            LOGGER.warning("Keine Daten für " + fileName + " gefunden");
            return 0.0;
        }
        
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
    
    public double getEquityDrawdown(String fileName) {
        Map<String, String> data = getFileData(fileName);
        if (data.isEmpty()) {
            LOGGER.warning("Keine Daten für " + fileName + " gefunden");
            // Standardwert zurückgeben, um Division durch Null zu vermeiden
            return 1.0;
        }
        
        String drawdownStr = data.getOrDefault("EquityDrawdown", "0,00")
                                .replace(",", ".")
                                .replace(" ", "");
        try {
            double value = Double.parseDouble(drawdownStr);
            
            // Stelle sicher, dass der Wert positiv ist (wir erwarten einen positiven Prozentsatz)
            if (value <= 0.0) {
                LOGGER.warning("EquityDrawdown ist 0 oder negativ: " + value + " für " + fileName);
                return 1.0; // Standardwert, um Division durch Null zu vermeiden
            }
            
            return value;
        } catch (NumberFormatException e) {
            LOGGER.warning("Could not parse equity drawdown: " + drawdownStr);
            return 1.0; // Standardwert
        }
    }
    
    public double getEquityDrawdownGraphic(String fileName) {
        Map<String, String> data = getFileData(fileName);
        if (data.isEmpty()) {
            LOGGER.warning("Keine Daten für " + fileName + " gefunden");
            return 0.0;
        }
        
        String drawdownGraphicStr = data.getOrDefault("MaxDDGraphic", "0")
                                   .replace(",", ".")
                                   .replace(" ", "");
        try {
            return Double.parseDouble(drawdownGraphicStr);
        } catch (NumberFormatException e) {
            LOGGER.warning("Could not parse max drawdown graphic: " + drawdownGraphicStr);
            return 0.0;
        }
    }
    
    public Map<String, Double> getMonthlyProfitPercentages(String fileName) {
        Map<String, String> data = getFileData(fileName);
        Map<String, Double> monthlyProfits = new HashMap<>();
        
        if (data.isEmpty()) {
            LOGGER.warning("Keine Daten für " + fileName + " gefunden");
            return monthlyProfits;
        }
        
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
    
    public double getAvr3MonthProfit(String fileName) {
        Map<String, String> data = getFileData(fileName);
        if (data.isEmpty()) {
            LOGGER.warning("Keine Daten für " + fileName + " gefunden");
            return 0.0;
        }
        
        String profitStr = data.getOrDefault("Average3MonthProfit", "0,00")
                              .replace(",", ".")
                              .replace(" ", "");
                              
        // Berechne die Details für den Tooltip
        updateAvr3MonthProfitCalculation(fileName);
                              
        try {
            return Double.parseDouble(profitStr);
        } catch (NumberFormatException e) {
            LOGGER.warning("Could not parse average profit: " + profitStr);
            return 0.0;
        }
    }
    
    private void updateAvr3MonthProfitCalculation(String fileName) {
        Map<String, Double> monthlyProfits = getMonthlyProfitPercentages(fileName);
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
        
        Map<String, String> data = getFileData(fileName);
        data.put("3MonthProfitCalculation", details.toString());
        dataCache.put(createCacheKey(fileName), data);
    }
    
    public String get3MonthProfitTooltip(String fileName) {
        Map<String, String> data = getFileData(fileName);
        if (data.isEmpty()) {
            return "Keine Berechnungsdetails verfügbar (Daten nicht gefunden)";
        }
        
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
    
    public double getMPDD(String fileName, int months) {
        updateMPDDCalculation(fileName, months);
        Map<String, String> data = getFileData(fileName);
        if (data.isEmpty()) {
            LOGGER.warning("Keine Daten für " + fileName + " gefunden");
            return 0.0;
        }
        
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
    
    public String getMPDDTooltip(String fileName, int months) {
        Map<String, String> data = getFileData(fileName);
        if (data.isEmpty()) {
            return String.format("Keine Berechnungsdetails für %d-Monats-Drawdown verfügbar (Daten nicht gefunden)", months);
        }
        
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
    
    private void updateMPDDCalculation(String fileName, int months) {
        Map<String, Double> monthlyProfits = getMonthlyProfitPercentages(fileName);
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
        
        Map<String, String> data = getFileData(fileName);
        data.put(months + "MPDDCalculation", details.toString());
        dataCache.put(createCacheKey(fileName), data);
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
    
    public double getStabilitaetswert(String fileName) {
        Map<String, String> data = getFileData(fileName);
        if (data.isEmpty()) {
            LOGGER.warning("Keine Daten für " + fileName + " gefunden");
            return 1.0;
        }
        
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
    
    public String getStabilitaetswertDetails(String fileName) {
        Map<String, String> data = getFileData(fileName);
        if (data.isEmpty()) {
            return "Keine Stabilitätsdetails verfügbar (Daten nicht gefunden)";
        }
        
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
    
    public double getAverageMonthlyProfit(String fileName, int n) {
        Map<String, Double> monthlyProfits = getMonthlyProfitPercentages(fileName);
        if (monthlyProfits.isEmpty() || n <= 0) {
            LOGGER.warning("Keine monatlichen Profite für " + fileName + " gefunden oder n <= 0");
            return 0.0;
        }
        
        List<String> sortedMonths = new ArrayList<>(monthlyProfits.keySet());
        if (sortedMonths.isEmpty()) {
            LOGGER.warning("Keine sortierten Monate für " + fileName + " verfügbar");
            return 0.0;
        }
        
        sortedMonths.sort((a, b) -> b.compareTo(a)); // Absteigend sortieren
        
        // Debug-Ausgabe
        LOGGER.info("Berechne " + n + "-Monats-Profit für " + fileName);
        LOGGER.info("Sortierte Monate: " + sortedMonths);
        
        // Überprüfe, ob überhaupt genug Monate vorhanden sind
        if (sortedMonths.size() < 2) { // Mindestens aktuellen Monat + 1 weiteren benötigen wir
            LOGGER.warning("Zu wenige Monate für " + fileName + ": " + sortedMonths.size());
            return 0.0;
        }
        
        // Nach Abzug des aktuellen Monats verfügbare Monate
        int availableMonths = sortedMonths.size() - 1; // -1 für aktuellen Monat
        
        // Wenn nicht genügend Monate für die angeforderte Berechnung verfügbar sind
        if (n == 3) {
            // Für 3MPDD bisheriges Verhalten beibehalten
            int monthsToUse = Math.min(n, availableMonths);
            if (monthsToUse <= 0) {
                LOGGER.warning("Keine Monate für 3MPDD nutzbar");
                return 0.0;
            }
            
            double sum = 0.0;
            for (int i = 0; i < monthsToUse; i++) {
                String month = sortedMonths.get(i + 1); // +1 um aktuellen Monat zu überspringen
                double profit = monthlyProfits.get(month);
                sum += profit;
                LOGGER.info("Monat " + month + ": " + profit);
            }
            
            double average = sum / monthsToUse;
            LOGGER.info("Durchschnitt über " + monthsToUse + " Monate: " + average);
            return average;
        } else {
            // Für 6, 9 und 12 MPDD: Nur berechnen wenn genügend Monate verfügbar
            int requiredMonths = n + 1; // +1 weil aktueller Monat nicht berücksichtigt wird
            if (sortedMonths.size() < requiredMonths) {
                LOGGER.warning("Nicht genug Monate für " + n + "-MPDD: " + sortedMonths.size() + "/" + requiredMonths);
                return 0.0;
            }
            
            double sum = 0.0;
            for (int i = 0; i < n; i++) {
                String month = sortedMonths.get(i + 1); // +1 um aktuellen Monat zu überspringen
                double profit = monthlyProfits.get(month);
                sum += profit;
                LOGGER.info("Monat " + month + ": " + profit);
            }
            
            double average = sum / n;
            LOGGER.info("Durchschnitt über " + n + " Monate: " + average);
            return average;
        }
    }
    
    public List<String> getLastThreeMonthsDetails(String fileName) {
        Map<String, String> data = getFileData(fileName);
        List<String> details = new ArrayList<>();
        
        if (data.isEmpty()) {
            LOGGER.warning("Keine Daten für " + fileName + " gefunden");
            return details;
        }
        
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
    
    public void saveSteigungswert(String fileName, double steigung) {
        Map<String, String> data = getFileData(fileName);
        if (!data.isEmpty()) {
            data.put("Steigungswert", String.format("%.2f", steigung).replace(',', '.'));
            dataCache.put(createCacheKey(fileName), data);
        }
    }

    public double getSteigungswert(String fileName) {
        Map<String, String> data = getFileData(fileName);
        if (data.isEmpty()) {
            LOGGER.warning("Keine Daten für " + fileName + " gefunden");
            return 0.0;
        }
        
        String steigungStr = data.getOrDefault("Steigungswert", "0.00")
                                 .replace(",", ".")
                                 .replace(" ", "");
        try {
            return Double.parseDouble(steigungStr);
        } catch (NumberFormatException e) {
            LOGGER.warning("Could not parse steigung value: " + steigungStr);
            return 0.0;
        }
    }
    
    // Neue Hilfsmethode zum Überprüfen des Zugriffs auf das Dateisystem
    public boolean checkFileAccess() {
        File dir = new File(downloadPath);
        if (!dir.exists()) {
            LOGGER.severe("Das Verzeichnis existiert nicht: " + downloadPath);
            return false;
        }
        
        if (!dir.isDirectory()) {
            LOGGER.severe("Der Pfad ist kein Verzeichnis: " + downloadPath);
            return false;
        }
        
        if (!dir.canRead()) {
            LOGGER.severe("Keine Leseberechtigung für: " + downloadPath);
            return false;
        }
        
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            LOGGER.warning("Verzeichnis leer oder Zugriff nicht möglich: " + downloadPath);
            return false;
        }
        
        return true;
    }
    
    public String getDrawdownChartData(String fileName) {
        return readDrawdownChartDataFromFile(fileName);
    }
    
    public String getRootPath() {
        return downloadPath;
    }
}