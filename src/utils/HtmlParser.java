package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  private static final Pattern PROFIT_TABLE_PATTERN = Pattern.compile(
          "<tbody><tr><td[^>]*>(\\d{4})</td>\\s*" +  // Beliebiges 4-stelliges Jahr
          "(?:<td[^>]*>([^<]*)</td>\\s*){12}" +      // 12 Monate
          "<td[^>]*>[^<]+</td></tr>\\s*" +           // Jahrestotal
          "(?:<tr><td[^>]*>(\\d{4})</td>\\s*" +      // Optionales weiteres Jahr
          "(?:<td[^>]*>([^<]*)</td>\\s*){12}" +      // 12 weitere Monate
          "<td[^>]*>[^<]+</td></tr>\\s*)*"           // Jahrestotal, beliebig oft wiederholbar
      );

  private final String rootPath;
  private final Map<String, String> htmlContentCache;
  private final Map<String, Double> equityDrawdownCache;
  private final Map<String, Double> balanceCache;
  private final Map<String, Double> averageProfitCache;

  public HtmlParser(String rootPath) {
      this.rootPath = rootPath;
      this.htmlContentCache = new HashMap<>();
      this.equityDrawdownCache = new HashMap<>();
      this.balanceCache = new HashMap<>();
      this.averageProfitCache = new HashMap<>();
  }

  private String getHtmlContent(String csvFileName) {
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
          htmlContentCache.put(csvFileName, htmlContent);
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

      LOGGER.warning("No balance value found for " + csvFileName);
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
              LOGGER.warning("Could not parse drawdown number: " + drawdownStr);
          }
      }

      return 0.0;
  }

  public double getAvr3MonthProfit(String csvFileName) {
      if (averageProfitCache.containsKey(csvFileName)) {
          return averageProfitCache.get(csvFileName);
      }

      List<String> lastMonthsDetails = getLastThreeMonthsDetails(csvFileName);
      if (lastMonthsDetails.isEmpty()) {
          return 0.0;
      }

      double sum = 0.0;
      int count = 0;

      for (String detail : lastMonthsDetails) {
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

      if (count == 0) return 0.0;

      double average = sum / count;
      averageProfitCache.put(csvFileName, average);
      return average;
  }

  public List<String> getLastThreeMonthsDetails(String csvFileName) {
      List<String> details = new ArrayList<>();
      List<String> months = new ArrayList<>();
      Set<String> seenDates = new HashSet<>();
      
      try {
          String htmlContent = getHtmlContent(csvFileName);
          if (htmlContent == null) return details;

          Pattern yearRowPattern = Pattern.compile(
              "<tr>\\s*<td[^>]*>(\\d{4})</td>\\s*" +
              "((?:<td[^>]*>([^<]*)</td>\\s*){12})"
          );
          
          Matcher rowMatcher = yearRowPattern.matcher(htmlContent);
          boolean foundDuplicate = false;
          
          while (rowMatcher.find() && !foundDuplicate) {
              String year = rowMatcher.group(1);
              String monthsContent = rowMatcher.group(2);
              
              Pattern valuePattern = Pattern.compile("<td[^>]*>([^<]*)</td>");
              Matcher valueMatcher = valuePattern.matcher(monthsContent);
              
              int monthIndex = 0;
              while (valueMatcher.find() && monthIndex < 12) {
                  String value = valueMatcher.group(1).trim();
                  if (!value.isEmpty()) {
                      try {
                          value = value.replace(",", ".")
                                     .replace("−", "-")
                                     .replaceAll("[^0-9.\\-]", "");
                          if (!value.isEmpty()) {
                              String date = year + "/" + (monthIndex + 1);
                              
                              if (seenDates.contains(date)) {
                                  foundDuplicate = true;
                                  break;
                              }
                              
                              seenDates.add(date);
                              months.add(date + ": " + value);
                          }
                      } catch (NumberFormatException e) {
                          LOGGER.warning("Could not parse value for " + year + "/" + (monthIndex + 1));
                      }
                  }
                  monthIndex++;
              }
          }

          if (months.size() >= 2) {
              int startIndex = months.size() - 2;
              int monthsToUse = Math.min(3, startIndex + 1);

              for (int i = startIndex; i > startIndex - monthsToUse; i--) {
                  details.add(months.get(i));
              }
          }
          
      } catch (Exception e) {
          LOGGER.severe("Error processing HTML for " + csvFileName + ": " + e.getMessage());
      }
      
      return details;
  }
}