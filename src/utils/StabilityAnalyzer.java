package utils;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Klasse für Stabilitäts- und Steigungsanalysen.
 * Enthält Methoden zur Berechnung und Anzeige von Stabilitätswerten.
 */
public class StabilityAnalyzer {
    private static final Logger LOGGER = Logger.getLogger(StabilityAnalyzer.class.getName());
    
    private final FileDataReader fileDataReader;
    
    public StabilityAnalyzer(FileDataReader fileDataReader) {
        this.fileDataReader = fileDataReader;
    }
    
    /**
     * Holt den Stabilitätswert für einen Provider
     * 
     * @param fileName Name der Provider-Datei
     * @return Stabilitätswert als double
     */
    public double getStabilitaetswert(String fileName) {
        Map<String, String> data = fileDataReader.getFileData(fileName);
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
    
    /**
     * Erstellt detaillierte HTML-Stabilitätsdetails für Tooltips
     * 
     * @param fileName Name der Provider-Datei
     * @return HTML-formatierte Stabilitätsdetails
     */
    public String getStabilitaetswertDetails(String fileName) {
        Map<String, String> data = fileDataReader.getFileData(fileName);
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
    
    /**
     * Speichert den Steigungswert für einen Provider
     * 
     * @param fileName Name der Provider-Datei
     * @param steigung Der zu speichernde Steigungswert
     */
    public void saveSteigungswert(String fileName, double steigung) {
        fileDataReader.updateCacheData(fileName, "Steigungswert", String.format("%.2f", steigung).replace(',', '.'));
    }

    /**
     * Holt den Steigungswert für einen Provider
     * 
     * @param fileName Name der Provider-Datei
     * @return Steigungswert als double
     */
    public double getSteigungswert(String fileName) {
        Map<String, String> data = fileDataReader.getFileData(fileName);
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
}