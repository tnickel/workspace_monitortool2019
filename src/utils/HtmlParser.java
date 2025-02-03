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
    
    // Pattern für SVG Text Element
    private static final Pattern SVG_TEXT_PATTERN = Pattern.compile(
        "<text[^>]*>\\s*<tspan[^>]*>Maximaler R.ckgang:</tspan>\\s*<tspan[^>]*>([0-9]+[.,][0-9]+)%</tspan></text>"
    );

    private final String rootPath;
    private final Map<String, Double> equityDrawdownCache;

    public HtmlParser(String rootPath) {
        this.rootPath = rootPath;
        this.equityDrawdownCache = new HashMap<>();
        LOGGER.info("HtmlParser initialized with root path: " + rootPath);
    }

    public double getEquityDrawdown(String csvFileName) {
        if (equityDrawdownCache.containsKey(csvFileName)) {
            return equityDrawdownCache.get(csvFileName);
        }

        String htmlFileName = csvFileName.replace(".csv", "_root.html");
        File htmlFile = new File(rootPath, htmlFileName);

        LOGGER.info("Trying to read HTML file: " + htmlFile.getAbsolutePath());
        
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

            // Alle SVG text Elemente mit "Maximaler Rückgang" finden
            Matcher matcher = SVG_TEXT_PATTERN.matcher(htmlContent);
            List<Double> foundValues = new ArrayList<>();
            
            while (matcher.find()) {
                String drawdownStr = matcher.group(1).replace(",", ".");
                LOGGER.info("Found potential drawdown value: " + drawdownStr);
                try {
                    double drawdown = Double.parseDouble(drawdownStr);
                    foundValues.add(drawdown);
                    // Debug: Kontext um den Fund herum ausgeben
                    int start = Math.max(0, matcher.start() - 100);
                    int end = Math.min(htmlContent.length(), matcher.end() + 100);
                    LOGGER.info("Context for " + drawdown + "%:\n" + 
                              htmlContent.substring(start, end));
                } catch (NumberFormatException e) {
                    LOGGER.warning("Could not parse number: " + drawdownStr);
                }
            }

            // Wenn mehrere Werte gefunden wurden, nehmen wir den größten
            if (!foundValues.isEmpty()) {
                double maxDrawdown = foundValues.stream()
                    .mapToDouble(Double::doubleValue)
                    .max()
                    .getAsDouble();
                LOGGER.info("Using maximum drawdown value: " + maxDrawdown);
                equityDrawdownCache.put(csvFileName, maxDrawdown);
                return maxDrawdown;
            }

            // Wenn nichts gefunden wurde, suche nach dem Text um zu debuggen
            LOGGER.warning("No drawdown value found in SVG, searching for text snippets...");
            String[] searchTerms = {
                "Maximaler Rückgang",
                "Maximaler R&#252;ckgang",
                "ckgang",
                "<text",
                "<tspan"
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
}