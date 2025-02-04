package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;

public class HtmlParser {
    private static final Logger LOGGER = Logger.getLogger(HtmlParser.class.getName());
    
    private static final Pattern SVG_TEXT_PATTERN = Pattern.compile(
        "<text[^>]*>\\s*<tspan[^>]*>Maximaler R.ckgang:</tspan>\\s*<tspan[^>]*>([0-9]+[.,][0-9]+)%</tspan></text>"
    );
    
    private static final Pattern BALANCE_PATTERN = Pattern.compile(
    	    "<div class=\"s-list-info__item\">\\s*" +
    	    "<div class=\"s-list-info__label\">(Balance|Kontostand):\\s*</div>\\s*" +
    	    "<div class=\"s-list-info__value\">([\\d\\s\\.]+)\\s*[A-Z]{3}</div>\\s*" +
    	    "</div>"
    	);

    private final String rootPath;
    private final Map<String, Double> equityDrawdownCache;
    private final Map<String, Double> balanceCache;

    public HtmlParser(String rootPath) {
        this.rootPath = rootPath;
        this.equityDrawdownCache = new HashMap<>();
        this.balanceCache = new HashMap<>();
        LOGGER.info("HtmlParser initialized with root path: " + rootPath);
    }

    public double getBalance(String csvFileName) {
        if (balanceCache.containsKey(csvFileName)) {
            return balanceCache.get(csvFileName);
        }

        String htmlFileName = csvFileName.replace(".csv", "_root.html");
        File htmlFile = new File(rootPath, htmlFileName);

        LOGGER.info("Trying to read HTML file for balance: " + htmlFile.getAbsolutePath());
        
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
            LOGGER.info("HTML content length: " + htmlContent.length());

            Matcher matcher = BALANCE_PATTERN.matcher(htmlContent);
            if (matcher.find()) {
                String balanceStr = matcher.group(2)
                    .replaceAll("\\s+", "")  // Remove all whitespace
                    .replace(",", ".");       // Replace comma with dot
                LOGGER.info("Found balance value: " + balanceStr);
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
                    LOGGER.info("Found '" + term + "' at index " + idx);
                    String context = htmlContent.substring(
                        Math.max(0, idx - 200),
                        Math.min(htmlContent.length(), idx + 500)
                    );
                    LOGGER.info("Context:\n" + context);
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
            
            Matcher matcher = SVG_TEXT_PATTERN.matcher(htmlContent);
            List<Double> foundValues = new ArrayList<>();
            
            while (matcher.find()) {
                String drawdownStr = matcher.group(1).replace(",", ".");
                try {
                    double drawdown = Double.parseDouble(drawdownStr);
                    foundValues.add(drawdown);
                } catch (NumberFormatException e) {
                    LOGGER.warning("Could not parse drawdown number: " + drawdownStr);
                }
            }

            if (!foundValues.isEmpty()) {
                double maxDrawdown = foundValues.stream()
                    .mapToDouble(Double::doubleValue)
                    .max()
                    .getAsDouble();
                equityDrawdownCache.put(csvFileName, maxDrawdown);
                return maxDrawdown;
            }
        } catch (IOException e) {
            LOGGER.severe("Error reading HTML file: " + e.getMessage());
            e.printStackTrace();
        }

        return 0.0;
    }
}