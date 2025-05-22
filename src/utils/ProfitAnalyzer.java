package utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import utils.HtmlDatabase;

/**
 * Klasse für Profit-Berechnungen und zugehörige Tooltips.
 * Enthält Methoden für durchschnittliche Profite und deren Visualisierung.
 */
public class ProfitAnalyzer {
    private static final Logger LOGGER = Logger.getLogger(ProfitAnalyzer.class.getName());
    
    private final FileDataReader fileDataReader;
    private final BasicDataProvider basicDataProvider;
    
    public ProfitAnalyzer(FileDataReader fileDataReader, BasicDataProvider basicDataProvider) {
        this.fileDataReader = fileDataReader;
        this.basicDataProvider = basicDataProvider;
    }
    
    /**
     * Holt den durchschnittlichen 3-Monats-Profit
     * 
     * @param fileName Name der Provider-Datei
     * @return Durchschnittlicher 3-Monats-Profit
     */
    public double getAvr3MonthProfit(String fileName) {
        Map<String, String> data = fileDataReader.getFileData(fileName);
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
    
    /**
     * Aktualisiert die Berechnungsdetails für den 3-Monats-Profit
     * 
     * @param fileName Name der Provider-Datei
     */
    private void updateAvr3MonthProfitCalculation(String fileName) {
        Map<String, Double> monthlyProfits = basicDataProvider.getMonthlyProfitPercentages(fileName);
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
        
        fileDataReader.updateCacheData(fileName, "3MonthProfitCalculation", details.toString());
    }
    
    /**
     * Erstellt einen HTML-Tooltip für die 3-Monats-Profit-Berechnung
     * 
     * @param fileName Name der Provider-Datei
     * @return HTML-formatierter Tooltip
     */
    public String get3MonthProfitTooltip(String fileName) {
        Map<String, String> data = fileDataReader.getFileData(fileName);
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
    
    /**
     * Berechnet durchschnittlichen monatlichen Profit über n Monate
     * Verwendet jetzt die Fallback-Implementierung direkt
     * 
     * @param fileName Name der Provider-Datei
     * @param n Anzahl der Monate
     * @return Durchschnittlicher monatlicher Profit
     */
    public double getAverageMonthlyProfit(String fileName, int n) {
        return calculateAverageMonthlyProfitFallback(fileName, n);
    }
    
    /**
     * Berechnung für durchschnittlichen monatlichen Profit
     * 
     * @param fileName Name der Provider-Datei
     * @param n Anzahl der Monate
     * @return Durchschnittlicher monatlicher Profit
     */
    private double calculateAverageMonthlyProfitFallback(String fileName, int n) {
        Map<String, Double> monthlyProfits = basicDataProvider.getMonthlyProfitPercentages(fileName);
        if (monthlyProfits.isEmpty()) {
            return 0.0;
        }
        
        List<String> sortedMonths = new ArrayList<>(monthlyProfits.keySet());
        sortedMonths.sort((a, b) -> b.compareTo(a)); // Absteigend sortieren
        
        if (sortedMonths.size() < 2) { // Mindestens aktueller + 1 weiterer Monat
            return 0.0;
        }
        
        double sum = 0.0;
        int monthsToUse = Math.min(n, sortedMonths.size() - 1);
        
        for (int i = 1; i <= monthsToUse; i++) { // +1 um aktuellen Monat zu überspringen
            String month = sortedMonths.get(i);
            double profit = monthlyProfits.get(month);
            sum += profit;
        }
        
        return monthsToUse > 0 ? sum / monthsToUse : 0.0;
    }
    
    /**
     * Holt die Details der letzten drei Monate
     * 
     * @param fileName Name der Provider-Datei
     * @return Liste mit Details der letzten drei Monate
     */
    public List<String> getLastThreeMonthsDetails(String fileName) {
        Map<String, String> data = fileDataReader.getFileData(fileName);
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
}