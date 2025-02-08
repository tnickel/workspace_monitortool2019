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

    private final String rootPath;
    private final Map<String, String> htmlContentCache;  // Cache für HTML Content
    private final Map<String, Double> equityDrawdownCache;
    private final Map<String, Double> balanceCache;
    private final Map<String, Double> averageProfitCache;

    public HtmlParser(String rootPath) {
        this.rootPath = rootPath;
        this.htmlContentCache = new HashMap<>();  // Initialisiere HTML Cache
        this.equityDrawdownCache = new HashMap<>();
        this.balanceCache = new HashMap<>();
        this.averageProfitCache = new HashMap<>();
    }

    private String getHtmlContent(String csvFileName) {
        // Prüfe zuerst den Cache
        if (htmlContentCache.containsKey(csvFileName)) {
            return htmlContentCache.get(csvFileName);
        }

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
            String htmlContent = content.toString();
            htmlContentCache.put(csvFileName, htmlContent);  // Speichere im Cache
            return htmlContent;
        } catch (IOException e) {
            LOGGER.severe("Error reading HTML file: " + e.getMessage());
            return null;
        }
    }

    public double getBalance(String csvFileName) {
        if (balanceCache.containsKey(csvFileName)) {
            return balanceCache.get(csvFileName);
        }

        String htmlContent = getHtmlContent(csvFileName);
        if (htmlContent == null) return 0.0;

        Matcher matcher = BALANCE_PATTERN.matcher(htmlContent);
        if (matcher.find()) {
            String balanceStr = matcher.group(2)
                .replaceAll("\\s+", "")
                .replace(",", ".");
            try {
                double balance = Double.parseDouble(balanceStr);
                balanceCache.put(csvFileName, balance);
                return balance;
            } catch (NumberFormatException e) {
                LOGGER.warning("Could not parse balance number: " + balanceStr);
            }
        }
        return 0.0;
    }

    public double getEquityDrawdown(String csvFileName) {
        if (equityDrawdownCache.containsKey(csvFileName)) {
            return equityDrawdownCache.get(csvFileName);
        }

        String htmlContent = getHtmlContent(csvFileName);
        if (htmlContent == null) return 0.0;

        Matcher matcher = DRAWNDOWN_PATTERN.matcher(htmlContent);
        if (matcher.find()) {
            String drawdownStr = matcher.group(1)
                .replace(",", ".")
                .replace("−", "-")
                .trim();
            try {
                double drawdown = Double.parseDouble(drawdownStr);
                equityDrawdownCache.put(csvFileName, drawdown);
                return drawdown;
            } catch (NumberFormatException e) {
                LOGGER.warning("Konnte Drawdown-Zahl nicht parsen: " + drawdownStr);
            }
        }
        return 0.0;
    }

    public double getAvr3MonthProfit(String csvFileName) {
        if (averageProfitCache.containsKey(csvFileName)) {
            return averageProfitCache.get(csvFileName);
        }

        List<String> lastThreeMonthsDetails = getLastThreeMonthsDetails(csvFileName);
        if (lastThreeMonthsDetails.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;
        int count = 0;

        for (String detail : lastThreeMonthsDetails) {
            try {
                String valueStr = detail.split(":")[1].trim()
                    .replace("%", "")
                    .replace(",", ".");
                sum += Double.parseDouble(valueStr);
                count++;
            } catch (NumberFormatException e) {
                LOGGER.warning("Error parsing profit value from: " + detail);
            }
        }

        if (count == 0) return 0.0;

        double average = sum / count;
        averageProfitCache.put(csvFileName, average);
        return average;
    }

    public List<String> getLastThreeMonthsDetails(String csvFileName) {
        List<String> details = new ArrayList<>();
        String htmlContent = getHtmlContent(csvFileName);
        if (htmlContent == null) return details;

        Pattern tablePattern = Pattern.compile(
            "<table class=\"svg-chart__ui-table svg-chart__table\">.*?<tbody>(.*?)</tbody>",
            Pattern.DOTALL
        );
        
        Matcher tableMatcher = tablePattern.matcher(htmlContent);
        if (tableMatcher.find()) {
            String tableBody = tableMatcher.group(1);
            List<MonthValue> allMonths = new ArrayList<>();
            
            Pattern rowPattern = Pattern.compile(
                "<tr>\\s*" +
                "<td class=\"svg-chart__main\">(\\d{4})</td>\\s*" +  // Jahr
                "<td[^>]*>([^<]*)</td>\\s*" +    // Jan
                "<td[^>]*>([^<]*)</td>\\s*" +    // Feb
                "<td[^>]*>([^<]*)</td>\\s*" +    // Mar
                "<td[^>]*>([^<]*)</td>\\s*" +    // Apr
                "<td[^>]*>([^<]*)</td>\\s*" +    // May
                "<td[^>]*>([^<]*)</td>\\s*" +    // Jun
                "<td[^>]*>([^<]*)</td>\\s*" +    // Jul
                "<td[^>]*>([^<]*)</td>\\s*" +    // Aug
                "<td[^>]*>([^<]*)</td>\\s*" +    // Sep
                "<td[^>]*>([^<]*)</td>\\s*" +    // Oct
                "<td[^>]*>([^<]*)</td>\\s*" +    // Nov
                "<td[^>]*>([^<]*)</td>",         // Dec
                Pattern.DOTALL
            );

            String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                                 "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            
            Matcher rowMatcher = rowPattern.matcher(tableBody);
            while (rowMatcher.find()) {
                String year = rowMatcher.group(1);
                
                for (int i = 0; i < 12; i++) {
                    String value = rowMatcher.group(i + 2).trim();
                    if (!value.isEmpty() && !value.equals("-")) {
                        value = value.replace(",", ".")
                                   .replace("−", "-")
                                   .replaceAll("[^0-9.\\-]", "");
                        try {
                            double profit = Double.parseDouble(value);
                            allMonths.add(new MonthValue(monthNames[i], year, profit));
                        } catch (NumberFormatException e) {
                            LOGGER.warning("Konnte Wert nicht parsen: " + value);
                        }
                    }
                }
            }
            
            allMonths.sort((a, b) -> {
                int yearCompare = a.year.compareTo(b.year);
                if (yearCompare != 0) return yearCompare;
                return Integer.compare(
                    Arrays.asList(monthNames).indexOf(a.month),
                    Arrays.asList(monthNames).indexOf(b.month)
                );
            });
            
            int size = allMonths.size();
            if (size > 4) {
                for (int i = size - 4; i < size - 1; i++) {
                    MonthValue mv = allMonths.get(i);
                    details.add(String.format("%s %s: %.2f%%", mv.month, mv.year, mv.value));
                }
            }
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
}