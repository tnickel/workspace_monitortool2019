package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlParser {
    private static final Logger LOGGER = Logger.getLogger(HtmlParser.class.getName());
    
    private static final Pattern DRAWNDOWN_PATTERN = Pattern.compile(
    	    // Pattern für SVG text-Struktur
    	    "<text[^>]*>\\s*" +
    	    "<tspan[^>]*>Maximaler\\s*R(?:[üue])ckgang:?</tspan>\\s*" +
    	    "<tspan[^>]*>([-−]?[0-9]+[.,][0-9]+)%</tspan>\\s*" +
    	    "</text>",
    	    Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    	);

    
    private static final Pattern BALANCE_PATTERN = Pattern.compile(
        "<div class=\"s-list-info__item\">\\s*" +
        "<div class=\"s-list-info__label\">(Balance|Kontostand):\\s*</div>\\s*" +
        "<div class=\"s-list-info__value\">([\\d\\s\\.]+)\\s*[A-Z]{3}</div>\\s*" +
        "</div>"
    );

    // Pattern für die Profittabelle
    private static final Pattern PROFIT_TABLE_PATTERN = Pattern.compile(
            "<tbody><tr><td[^>]*>(\\d{4})</td>\\s*" +  // Beliebiges 4-stelliges Jahr
            "(?:<td[^>]*>([^<]*)</td>\\s*){12}" +      // 12 Monate
            "<td[^>]*>[^<]+</td></tr>\\s*" +           // Jahrestotal
            "(?:<tr><td[^>]*>(\\d{4})</td>\\s*" +      // Optionales weiteres Jahr
            "(?:<td[^>]*>([^<]*)</td>\\s*){12}" +      // 12 weitere Monate
            "<td[^>]*>[^<]+</td></tr>\\s*)*"           // Jahrestotal, beliebig oft wiederholbar
        );
    private static final Pattern ROW_PATTERN = Pattern.compile(
            "<tr><td[^>]*>(\\d{4})</td>\\s*" +         // Jahr
            "(?:<td[^>]*>([^<]*)</td>\\s*){12}" +      // 12 Monate
            "<td[^>]*>[^<]+</td></tr>"                 // Jahrestotal
        );

    private final String rootPath;
    private final Map<String, Double> equityDrawdownCache;
    private final Map<String, Double> balanceCache;
    private final Map<String, Double> averageProfitCache;

    public HtmlParser(String rootPath) {
        this.rootPath = rootPath;
        this.equityDrawdownCache = new HashMap<>();
        this.balanceCache = new HashMap<>();
        this.averageProfitCache = new HashMap<>();
    }

    public double getBalance(String csvFileName) {
        if (balanceCache.containsKey(csvFileName)) {
            return balanceCache.get(csvFileName);
        }

        String htmlFileName = csvFileName.replace(".csv", "_root.html");
        File htmlFile = new File(rootPath, htmlFileName);
        
        if (!htmlFile.exists()) {
            LOGGER.warning("HTML file not found: " + htmlFile.getAbsolutePath());
            return 0.0;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(htmlFile))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            String htmlContent = content.toString();

            Matcher matcher = BALANCE_PATTERN.matcher(htmlContent);
            if (matcher.find()) {
                String balanceStr = matcher.group(2)
                    .replaceAll("\\s+", "")  // Remove all whitespace
                    .replace(",", ".");       // Replace comma with dot
                try {
                    double balance = Double.parseDouble(balanceStr);
                    balanceCache.put(csvFileName, balance);
                    return balance;
                } catch (NumberFormatException e) {
                    LOGGER.warning("Could not parse balance number: " + balanceStr);
                }
            }

            // Debug-Informationen bei nicht gefundener Balance
            LOGGER.warning("No balance value found, searching for debug info...");
            String[] searchTerms = {
                "Balance:",
                "Kontostand:",
                "s-list-info__item",
                "s-list-info__label",
                "s-list-info__value"
            };
            for (String term : searchTerms) {
                int idx = htmlContent.indexOf(term);
                if (idx > 0) {
                    String context = htmlContent.substring(
                        Math.max(0, idx - 200),
                        Math.min(htmlContent.length(), idx + 500)
                    );
                    //LOGGER.info("Context:\n" + context);
                }
            }

        } catch (IOException e) {
            LOGGER.severe("Error reading HTML file: " + e.getMessage());
            e.printStackTrace();
        }

        return 0.0;
    }
    public double getEquityDrawdown(String csvFileName) {
        if (equityDrawdownCache.containsKey(csvFileName)) {
            return equityDrawdownCache.get(csvFileName);
        }

        String htmlFileName = csvFileName.replace(".csv", "_root.html");
        File htmlFile = new File(rootPath, htmlFileName);
        
        if (!htmlFile.exists()) {
            LOGGER.warning("HTML-Datei nicht gefunden: " + htmlFile.getAbsolutePath());
            return 0.0;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(htmlFile))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            String htmlContent = content.toString();
            Matcher matcher = DRAWNDOWN_PATTERN.matcher(htmlContent);
            
            if (matcher.find()) {
                String drawdownStr = matcher.group(1);
                drawdownStr = drawdownStr.replace(",", ".")
                                       .replace("−", "-")
                                       .trim();
                try {
                    double drawdown = Double.parseDouble(drawdownStr);
                    LOGGER.info("Equity Drawdown gefunden für " + csvFileName + ": " + drawdown);
                    
                    // Debug-Ausgabe des gefundenen Matches
                    int matchStart = matcher.start();
                    String context = htmlContent.substring(
                        Math.max(0, matchStart - 50),
                        Math.min(htmlContent.length(), matchStart + 150)
                    );
                    LOGGER.info("Gefundener Text-Match: " + context);
                    
                    equityDrawdownCache.put(csvFileName, drawdown);
                    return drawdown;
                } catch (NumberFormatException e) {
                    LOGGER.warning("Konnte Drawdown-Zahl nicht parsen: " + drawdownStr);
                }
            } else {
                LOGGER.warning("Kein Equity Drawdown in HTML gefunden für " + csvFileName);
                // Zeige den relevanten Teil des HTML-Inhalts für Debugging
                int idx = htmlContent.indexOf("Maximaler");
                if (idx > -1) {
                    String context = htmlContent.substring(
                        Math.max(0, idx - 100),
                        Math.min(htmlContent.length(), idx + 200)
                    );
                    LOGGER.info("Gefundener Kontext um 'Maximaler': " + context);
                }
            }
        } catch (IOException e) {
            LOGGER.severe("Fehler beim Lesen der HTML-Datei: " + e.getMessage());
        }

        return 0.0;
    }
    public double getAvr3MonthProfit(String csvFileName) {
        if (averageProfitCache.containsKey(csvFileName)) {
            return averageProfitCache.get(csvFileName);
        }

        List<String> lastThreeMonthsDetails = getLastThreeMonthsDetails(csvFileName);
        if (lastThreeMonthsDetails.isEmpty()) {
            LOGGER.warning("No last three months profit data found for " + csvFileName);
            return 0.0;
        }

        double sum = 0.0;
        int count = 0;

        for (String detail : lastThreeMonthsDetails) {
            try {
                String valueStr = detail.split(":")[1].trim()
                                        .replace("%", "")
                                        .replace(",", ".");
                double profit = Double.parseDouble(valueStr);
                sum += profit;
                count++;
            } catch (NumberFormatException e) {
                LOGGER.warning("Error parsing profit value from: " + detail);
            }
        }

        if (count == 0) {
            LOGGER.warning("No valid profit values found for " + csvFileName);
            return 0.0;
        }

        double average = sum / count;
        LOGGER.info("Calculated 3MProfProz for " + csvFileName + ": " + average);
        averageProfitCache.put(csvFileName, average);
        return average;
    }
    public List<String> getLastThreeMonthsDetails(String csvFileName) {
        List<String> details = new ArrayList<>();
        
        try {
            String htmlContent = readHtmlFile(csvFileName);
            if (htmlContent == null) return details;

            Pattern tablePattern = Pattern.compile(
                "<tbody>\\s*" +
                "(?:<tr>\\s*<td[^>]*>[^<]+</td>\\s*" +
                "(?:<td[^>]*>([^<]*)</td>\\s*){12}" +
                "<td[^>]*>[^<]+</td>\\s*</tr>\\s*)*" +
                "</tbody>"
            );
            
            Matcher tableMatcher = tablePattern.matcher(htmlContent);
            if (tableMatcher.find()) {
                String tableContent = tableMatcher.group(0);
                List<MonthValue> allMonths = new ArrayList<>();
                
                // Extrahiere alle Zeilen
                Pattern rowPattern = Pattern.compile(
                    "<tr>\\s*<td[^>]*>(\\d{4})</td>\\s*" +
                    "((?:<td[^>]*>([^<]*)</td>\\s*){12})"
                );
                
                String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                                     "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                
                Matcher rowMatcher = rowPattern.matcher(tableContent);
                while (rowMatcher.find()) {
                    String year = rowMatcher.group(1);
                    String monthsContent = rowMatcher.group(2);
                    
                    Pattern valuePattern = Pattern.compile("<td[^>]*>([^<]+)</td>");
                    Matcher valueMatcher = valuePattern.matcher(monthsContent);
                    int monthIndex = 0;
                    
                    while (valueMatcher.find() && monthIndex < 12) {
                        String value = valueMatcher.group(1).trim();
                        if (!value.isEmpty() && !value.equals("-")) {
                            try {
                                value = value.replace(",", ".")
                                           .replace("−", "-")
                                           .replaceAll("[^0-9.\\-]", "");
                                if (!value.isEmpty()) {
                                    double profit = Double.parseDouble(value);
                                    allMonths.add(new MonthValue(monthNames[monthIndex], year, profit));
                                }
                            } catch (NumberFormatException e) {
                                LOGGER.warning("Could not parse value: " + value);
                            }
                        }
                        monthIndex++;
                    }
                }
                
                // Sortiere die Monate in umgekehrter Reihenfolge (neueste zuerst)
                allMonths.sort((a, b) -> {
                    int yearCompare = b.year.compareTo(a.year);
                    if (yearCompare != 0) return yearCompare;
                    
                    int aIndex = Arrays.asList(monthNames).indexOf(a.month);
                    int bIndex = Arrays.asList(monthNames).indexOf(b.month);
                    return Integer.compare(bIndex, aIndex);
                });
                
                // Der aktuelle Monat ist der erste in der sortierten Liste
                if (allMonths.size() > 1) {  // Mindestens aktueller Monat + 1 weiterer Monat
                    // Nehme bis zu 3 Monate VOR dem aktuellen Monat
                    int monthsToUse = Math.min(3, allMonths.size() - 1);
                    for (int i = 1; i <= monthsToUse; i++) {
                        MonthValue mv = allMonths.get(i);
                        details.add(String.format("%s %s: %.2f%%", mv.month, mv.year, mv.value));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Error processing HTML: " + e.getMessage());
        }
        
        return details;
    }

    private static class MonthValue {
        String month;
        String year;
        double value;
        
        MonthValue(String month, String year, double value) {
            this.month = month;
            this.year = year;
            this.value = value;
        }
    }
    private String readHtmlFile(String csvFileName) {
        String htmlFileName = csvFileName.replace(".csv", "_root.html");
        File htmlFile = new File(rootPath, htmlFileName);
        
        if (!htmlFile.exists()) {
            LOGGER.warning("HTML file not found: " + htmlFile.getAbsolutePath());
            return null;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(htmlFile))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        } catch (IOException e) {
            LOGGER.severe("Error reading HTML file: " + e.getMessage());
            return null;
        }
    }
}