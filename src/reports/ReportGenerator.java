package reports;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.jfree.chart.JFreeChart;

import charts.CurrencyPairTradesChart;
import charts.DrawdownChart;
import charts.DurationProfitChart;
import charts.EfficiencyChart;
import charts.EquityDrawdownChart;
import charts.SymbolDistributionChart;
import charts.TradeStackingChart;
import charts.WeeklyLotsizeChart;
import data.FavoritesManager;
import data.ProviderStats;
import data.Trade;
import db.HistoryDatabaseManager;
import utils.ApplicationConstants;
import utils.HtmlDatabase;
import utils.UIStyle;

/**
 * Klasse zum Erstellen eines HTML-Reports für favorisierte Signal Provider
 * Erweitert um PDF-Integration für zusätzliche Analysen
 */
public class ReportGenerator {
    private static final Logger LOGGER = Logger.getLogger(ReportGenerator.class.getName());
    private final String rootPath;
    private final HtmlDatabase htmlDatabase;
    private final HistoryDatabaseManager historyDbManager;
    private final FavoritesManager favoritesManager;
    private final PdfManager pdfManager;
    private final HtmlPdfIntegrator pdfIntegrator;
    
    /**
     * Konstruktor für den ReportGenerator
     * 
     * @param rootPath Root-Pfad für die Anwendung
     * @param htmlDatabase Die HtmlDatabase für den Zugriff auf Daten
     */
    public ReportGenerator(String rootPath, HtmlDatabase htmlDatabase) {
        // Validiere den Pfad und korrigiere ihn, falls nötig
        this.rootPath = ApplicationConstants.validateRootPath(rootPath, "ReportGenerator.constructor");
        this.htmlDatabase = htmlDatabase;
        this.historyDbManager = HistoryDatabaseManager.getInstance(rootPath);
        this.favoritesManager = new FavoritesManager(rootPath);
        this.pdfManager = new PdfManager(rootPath);
        this.pdfIntegrator = new HtmlPdfIntegrator(pdfManager);
        
        // Stelle sicher, dass das Report-Verzeichnis existiert
        createReportDirectory();
    }
    
    /**
     * Konstruktor nur mit rootPath
     */
    public ReportGenerator(String rootPath) {
        this(rootPath, new HtmlDatabase(rootPath));
    }
    
    /**
     * Erstellt das Report-Verzeichnis, falls es nicht existiert
     */
    private void createReportDirectory() {
        File reportDir = new File(rootPath, "report");
        if (!reportDir.exists()) {
            boolean created = reportDir.mkdirs();
            if (!created) {
                LOGGER.warning("Konnte Report-Verzeichnis nicht erstellen: " + reportDir.getAbsolutePath());
            } else {
                LOGGER.info("Report-Verzeichnis erfolgreich erstellt: " + reportDir.getAbsolutePath());
            }
        }
    }
    
    /**
     * Hilfsmethode zur Sortierung der Provider nach Kategorien und dann alphabetisch
     * 
     * @param providerStats Map mit den Providern und ihren Statistiken
     * @return Sortierte Liste der Provider-Einträge
     */
    private List<Map.Entry<String, ProviderStats>> getSortedProviderList(Map<String, ProviderStats> providerStats) {
        List<Map.Entry<String, ProviderStats>> sortedList = new ArrayList<>(providerStats.entrySet());
        
        // Sortierung: Erst nach Kategorie (1, 2, 3, ...), dann alphabetisch nach Provider-Namen
        sortedList.sort(new Comparator<Map.Entry<String, ProviderStats>>() {
            @Override
            public int compare(Map.Entry<String, ProviderStats> entry1, Map.Entry<String, ProviderStats> entry2) {
                String providerName1 = entry1.getKey();
                String providerName2 = entry2.getKey();
                
                String providerId1 = extractProviderId(providerName1);
                String providerId2 = extractProviderId(providerName2);
                
                int category1 = favoritesManager.getFavoriteCategory(providerId1);
                int category2 = favoritesManager.getFavoriteCategory(providerId2);
                
                // Zuerst nach Kategorie sortieren
                int categoryComparison = Integer.compare(category1, category2);
                if (categoryComparison != 0) {
                    return categoryComparison;
                }
                
                // Wenn gleiche Kategorie, dann alphabetisch nach Provider-Namen
                return providerName1.compareTo(providerName2);
            }
        });
        
        return sortedList;
    }
    
    /**
     * Hilfsmethode, um den passenden CSS-Klassenname für eine Kategorie zu erhalten
     * 
     * @param category Die Kategorie-Nummer
     * @return Die CSS-Klasse für diese Kategorie
     */
    private String getCategoryStyleClass(int category) {
        if (category <= 0) {
            return "";
        } else if (category <= 2) {
            return "category-dark-green";
        } else if (category == 3) {
            return "category-light-green";
        } else if (category == 4) {
            return "category-orange";
        } else {
            return "category-default";
        }
    }
    
    /**
     * Hilfsmethode, um formatierte Kategorieinformation mit Farbklasse zu erstellen
     * 
     * @param category Die Kategorie-Nummer
     * @param isCustomReport Flag, ob es sich um einen benutzerdefinierten Report handelt
     * @return Formatierte HTML-Ausgabe für die Kategorie
     */
    private String formatCategoryInfo(int category, boolean isCustomReport) {
        if (category <= 0) {
            return "";
        }
        
        String categoryText = isCustomReport ? "(Favorit, Kategorie " + category + ")" : "(Kategorie " + category + ")";
        String styleClass = getCategoryStyleClass(category);
        
        if (!styleClass.isEmpty()) {
            return " <span class=\"" + styleClass + "\">" + categoryText + "</span>";
        } else {
            return " " + categoryText;
        }
    }
    
    /**
     * Sammelt alle Provider-IDs für PDF-Kopierung
     * 
     * @param providerStats Map mit den Providern
     * @return Liste der Provider-IDs
     */
    private List<String> collectProviderIds(Map<String, ProviderStats> providerStats) {
        List<String> providerIds = new ArrayList<>();
        for (String providerName : providerStats.keySet()) {
            providerIds.add(extractProviderId(providerName));
        }
        return providerIds;
    }
    
    /**
     * Generiert einen HTML-Report für die angegebenen Signal Provider
     * 
     * @param favoriteProviders Map mit den Favoriten-Providern und ihren Statistiken
     * @return Der Pfad zur generierten HTML-Datei
     */
    public String generateReport(Map<String, ProviderStats> favoriteProviders) {
        return generateReport(favoriteProviders, 0); // 0 bedeutet alle Favoriten-Kategorien
    }
    
    /**
     * Generiert einen HTML-Report für die angegebenen Signal Provider einer bestimmten Kategorie
     * 
     * @param favoriteProviders Map mit den Favoriten-Providern und ihren Statistiken
     * @param category Die Favoriten-Kategorie (1-10) oder 0 für alle Kategorien
     * @return Der Pfad zur generierten HTML-Datei
     */
    public String generateReport(Map<String, ProviderStats> favoriteProviders, int category) {
        if (favoriteProviders.isEmpty()) {
            LOGGER.warning("Keine Favoriten-Provider zum Erstellen des Reports gefunden");
            return null;
        }
        
        // Filtere nach Kategorie, falls erforderlich
        Map<String, ProviderStats> filteredProviders = favoriteProviders;
        if (category > 0) {
            filteredProviders = new HashMap<>();
            for (Map.Entry<String, ProviderStats> entry : favoriteProviders.entrySet()) {
                String providerName = entry.getKey();
                String providerId = extractProviderId(providerName);
                if (favoritesManager.isFavoriteInCategory(providerId, category)) {
                    filteredProviders.put(providerName, entry.getValue());
                }
            }
            
            if (filteredProviders.isEmpty()) {
                LOGGER.warning("Keine Provider in Kategorie " + category + " gefunden");
                return null;
            }
        }
        
        // PDFs für alle Provider kopieren
        List<String> providerIds = collectProviderIds(filteredProviders);
        String pdfCopyStatus = pdfIntegrator.copyPdfsAndGenerateStatus(providerIds);
        
        // Sortierte Provider-Liste erstellen
        List<Map.Entry<String, ProviderStats>> sortedProviders = getSortedProviderList(filteredProviders);
        
        // Dateiname und Pfad für den Report
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = dateFormat.format(new Date());
        String categoryStr = category > 0 ? "_cat" + category : "";
        String reportFileName = "favorites" + categoryStr + "_report_" + timestamp + ".html";
        String reportPath = rootPath + File.separator + "report" + File.separator + reportFileName;
        
        // Report-Verzeichnis für Bilder
        String imagesDir = "images_" + timestamp;
        File reportImagesDir = new File(rootPath + File.separator + "report" + File.separator + imagesDir);
        if (!reportImagesDir.exists()) {
            reportImagesDir.mkdirs();
        }
        
        String reportTitle = "Favoriten Signal Provider Report";
        if (category > 0) {
            reportTitle += " - Kategorie " + category;
        }
        
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(reportPath), StandardCharsets.UTF_8)) {
            // HTML-Header schreiben (jetzt mit PDF-CSS)
            writer.write(generateHtmlHeader(reportTitle, timestamp));
            
            // PDF-Kopier-Status als Kommentar hinzufügen
            writer.write(pdfCopyStatus);
            
            // Inhaltsverzeichnis erstellen
            writer.write("<div class=\"toc\">\n");
            writer.write("<h2>Inhaltsverzeichnis</h2>\n");
            writer.write("<ul>\n");
            for (Map.Entry<String, ProviderStats> entry : sortedProviders) {
                String providerName = entry.getKey();
                String providerId = extractProviderId(providerName);
                // Kategorie anzeigen, wenn alle Kategorien angezeigt werden
                String categoryInfo = "";
                if (category == 0) {
                    int providerCategory = favoritesManager.getFavoriteCategory(providerId);
                    categoryInfo = formatCategoryInfo(providerCategory, false);
                }
                // PDF-Info hinzufügen
                String pdfInfo = pdfIntegrator.generateTocPdfInfo(providerId);
                writer.write("<li><a href=\"#" + providerId + "\">" + providerName + categoryInfo + pdfInfo + "</a></li>\n");
            }
            writer.write("</ul>\n");
            writer.write("</div>\n");
            
            // Hauptinhalt mit Provider-Informationen
            writer.write("<div class=\"main-content\">\n");
            
            // Für jeden Provider im Report (jetzt sortiert)
            for (Map.Entry<String, ProviderStats> entry : sortedProviders) {
                String providerName = entry.getKey();
                ProviderStats stats = entry.getValue();
                String providerId = extractProviderId(providerName);
                
                writer.write("<div class=\"provider-section\" id=\"" + providerId + "\">\n");
                
                // Kategorie-Info im Titel anzeigen
                int providerCategory = favoritesManager.getFavoriteCategory(providerId);
                String categoryInfo = formatCategoryInfo(providerCategory, false);
                writer.write("<h2>" + providerName + categoryInfo + "</h2>\n");
                
                // MQL-Link hinzufügen
                String mqlUrl = String.format(UIStyle.SIGNAL_PROVIDER_URL_FORMAT, providerId);
                writer.write("<div class=\"mql-link\">\n");
                writer.write("<a href=\"" + mqlUrl + "\" target=\"_blank\">MQL5 Webseite des Providers</a>\n");
                writer.write("</div>\n");
                
                // PDF-Links hinzufügen (NEUE FUNKTION!)
                String pdfLinksHtml = pdfIntegrator.generatePdfLinksHtml(providerId);
                if (!pdfLinksHtml.isEmpty()) {
                    writer.write(pdfLinksHtml);
                }
                
                // Lade Notizen aus der Datenbank
                String notes = historyDbManager.getProviderNotes(providerName);
                
                // Statistische Informationen
                writer.write(generateStatsSection(stats, providerName));
                
                // Notizen anzeigen, falls vorhanden
                if (notes != null && !notes.trim().isEmpty()) {
                    writer.write("<div class=\"notes-section\">\n");
                    writer.write("<h3>Notizen</h3>\n");
                    writer.write("<div class=\"notes-content\">\n");
                    writer.write("<p>" + notes.replace("\n", "<br>") + "</p>\n");
                    writer.write("</div>\n");
                    writer.write("</div>\n");
                }
                
                // Charts erstellen und hinzufügen
                writer.write(generateChartsSection(stats, providerId, reportImagesDir.getPath(), imagesDir));
                
                writer.write("</div>\n"); // Ende provider-section
                
                // Horizontale Linie zwischen den Providern
                writer.write("<hr>\n");
            }
            
            writer.write("</div>\n"); // Ende main-content
            writer.write(generateHtmlFooter());
            
            LOGGER.info("Report erfolgreich erstellt: " + reportPath);
            return reportPath;
            
        } catch (IOException e) {
            LOGGER.severe("Fehler beim Erstellen des Reports: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Generiert die Statistik-Sektion für einen Provider
     * 
     * @param stats Die Provider-Statistiken
     * @param providerName Der Provider-Name
     * @return HTML-String für die Statistik-Sektion
     */
    private String generateStatsSection(ProviderStats stats, String providerName) {
        StringBuilder htmlBuilder = new StringBuilder();
        
        htmlBuilder.append("<div class=\"stats-info\">\n");
        htmlBuilder.append("<h3>Handelsinformationen</h3>\n");
        htmlBuilder.append("<table class=\"stats-table\">\n");
        htmlBuilder.append("<tr><th>Kennzahl</th><th>Wert</th></tr>\n");
        htmlBuilder.append("<tr><td>Total Trades</td><td>").append(stats.getTrades().size()).append("</td></tr>\n");
        
        // Win Rate berechnen
        double winRatePercent = stats.getWinRate() * 100;
        if (winRatePercent > 100) {
            winRatePercent = stats.getWinRate();
        }
        htmlBuilder.append("<tr><td>Win Rate</td><td>").append(String.format("%.2f%%", winRatePercent)).append("</td></tr>\n");
        htmlBuilder.append("<tr><td>Profit</td><td>").append(String.format("%.2f", stats.getTotalProfit())).append("</td></tr>\n");
        
        // MPDD-Werte
        double threeMonthProfit = htmlDatabase.getAverageMonthlyProfit(providerName, 3);
        double equityDrawdown = htmlDatabase.getEquityDrawdown(providerName);
        double maxDrawdownGraphic = htmlDatabase.getEquityDrawdownGraphic(providerName);
        
        htmlBuilder.append("<tr><td>3MPDD</td><td>").append(String.format("%.2f", threeMonthProfit / Math.max(0.01, equityDrawdown))).append("</td></tr>\n");
        htmlBuilder.append("<tr><td>Equity Drawdown</td><td>").append(String.format("%.2f%%", equityDrawdown)).append("</td></tr>\n");
        htmlBuilder.append("<tr><td>Max Drawdown</td><td>").append(String.format("%.2f%%", maxDrawdownGraphic)).append("</td></tr>\n");
        htmlBuilder.append("</table>\n");
        htmlBuilder.append("</div>\n");
        
        return htmlBuilder.toString();
    }
    
    /**
     * Generiert die Charts-Sektion für einen Provider
     * 
     * @param stats Die Provider-Statistiken
     * @param providerId Die Provider-ID
     * @param reportImagesPath Pfad für die Bilder
     * @param imagesDir Name des Bilder-Verzeichnisses
     * @return HTML-String für die Charts-Sektion
     */
    private String generateChartsSection(ProviderStats stats, String providerId, String reportImagesPath, String imagesDir) {
        StringBuilder htmlBuilder = new StringBuilder();
        
        htmlBuilder.append("<div class=\"charts-section\">\n");
        htmlBuilder.append("<h3>Charts</h3>\n");
        
        // MPDD-Werte für Equity Drawdown Chart
        double maxDrawdownGraphic = htmlDatabase.getEquityDrawdownGraphic(stats.getTrades().get(0).getSymbol().split("_")[0]); // Provisorisch
        
        // Equity Drawdown Chart
        EquityDrawdownChart equityDrawdownChart = new EquityDrawdownChart(stats, maxDrawdownGraphic, htmlDatabase);
        String equityDrawdownChartPath = saveChartAsImage(equityDrawdownChart.getChart(), 
                reportImagesPath, providerId + "_equity_drawdown");
        if (equityDrawdownChartPath != null) {
            htmlBuilder.append("<div class=\"chart-container\">\n");
            htmlBuilder.append("<h4>Equity Drawdown</h4>\n");
            htmlBuilder.append("<img src=\"").append(imagesDir).append("/").append(new File(equityDrawdownChartPath).getName()).append("\" alt=\"Equity Drawdown Chart\" class=\"chart-image\">\n");
            htmlBuilder.append("</div>\n");
        }
        
        // Weitere Charts hinzufügen, falls genügend Trades vorhanden sind
        if (stats.getTrades().size() > 5) {
            // Symbol Distribution Chart
            SymbolDistributionChart symbolChart = new SymbolDistributionChart(stats.getTrades());
            String symbolChartPath = saveChartAsImage(symbolChart.getChart(), 
                    reportImagesPath, providerId + "_symbol_distribution");
            if (symbolChartPath != null) {
                htmlBuilder.append("<div class=\"chart-container\">\n");
                htmlBuilder.append("<h4>Symbol Verteilung</h4>\n");
                htmlBuilder.append("<img src=\"").append(imagesDir).append("/").append(new File(symbolChartPath).getName()).append("\" alt=\"Symbol Distribution Chart\" class=\"chart-image\">\n");
                htmlBuilder.append("</div>\n");
            }
        }
        
        htmlBuilder.append("</div>\n"); // Ende charts-section
        return htmlBuilder.toString();
    }
    
    /**
     * Generiert einen benutzerdefinierten Report für ausgewählte Provider
     * 
     * @param providerStats Map mit den ausgewählten Providern
     * @param reportTitle Titel des Reports
     * @param outputPath Pfad zur Ausgabedatei
     * @return Pfad zur erzeugten HTML-Datei oder null bei Fehler
     */
    public String generateReport(Map<String, ProviderStats> providerStats, 
                                String reportTitle, 
                                String outputPath) {
        if (providerStats.isEmpty()) {
            LOGGER.warning("Keine Provider zum Erstellen des Reports gefunden");
            return null;
        }
        
        // PDFs für alle Provider kopieren
        List<String> providerIds = collectProviderIds(providerStats);
        String pdfCopyStatus = pdfIntegrator.copyPdfsAndGenerateStatus(providerIds);
        
        // Sortierte Provider-Liste erstellen
        List<Map.Entry<String, ProviderStats>> sortedProviders = getSortedProviderList(providerStats);
        
        try {
            // Report-Verzeichnis für Bilder
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imagesDir = "images_" + timestamp;
            File reportImagesDir = new File(new File(outputPath).getParentFile(), imagesDir);
            if (!reportImagesDir.exists()) {
                reportImagesDir.mkdirs();
            }
            
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputPath), StandardCharsets.UTF_8)) {
                // HTML-Header schreiben (jetzt mit PDF-CSS)
                writer.write(generateHtmlHeader(reportTitle, timestamp));
                
                // PDF-Kopier-Status als Kommentar hinzufügen
                writer.write(pdfCopyStatus);
                
                // Inhaltsverzeichnis erstellen
                writer.write("<div class=\"toc\">\n");
                writer.write("<h2>Inhaltsverzeichnis</h2>\n");
                writer.write("<ul>\n");
                for (Map.Entry<String, ProviderStats> entry : sortedProviders) {
                    String providerName = entry.getKey();
                    String providerId = extractProviderId(providerName);
                    
                    // Kategorie anzeigen, wenn es ein Favorit ist
                    int providerCategory = favoritesManager.getFavoriteCategory(providerId);
                    String categoryInfo = formatCategoryInfo(providerCategory, true);
                    
                    // PDF-Info hinzufügen
                    String pdfInfo = pdfIntegrator.generateTocPdfInfo(providerId);
                    
                    writer.write("<li><a href=\"#" + providerId + "\">" + providerName + categoryInfo + pdfInfo + "</a></li>\n");
                }
                writer.write("</ul>\n");
                writer.write("</div>\n");
                
                // Hauptinhalt mit Provider-Informationen
                writer.write("<div class=\"main-content\">\n");
                
                // Für jeden Provider im Report (jetzt sortiert)
                for (Map.Entry<String, ProviderStats> entry : sortedProviders) {
                    String providerName = entry.getKey();
                    ProviderStats stats = entry.getValue();
                    String providerId = extractProviderId(providerName);
                    
                    writer.write("<div class=\"provider-section\" id=\"" + providerId + "\">\n");
                    
                    // Kategorie-Info im Titel anzeigen
                    int providerCategory = favoritesManager.getFavoriteCategory(providerId);
                    String categoryInfo = formatCategoryInfo(providerCategory, true);
                    writer.write("<h2>" + providerName + categoryInfo + "</h2>\n");
                    
                    // MQL-Link hinzufügen
                    String mqlUrl = String.format(UIStyle.SIGNAL_PROVIDER_URL_FORMAT, providerId);
                    writer.write("<div class=\"mql-link\">\n");
                    writer.write("<a href=\"" + mqlUrl + "\" target=\"_blank\">MQL5 Webseite des Providers</a>\n");
                    writer.write("</div>\n");
                    
                    // PDF-Links hinzufügen (NEUE FUNKTION!)
                    String pdfLinksHtml = pdfIntegrator.generatePdfLinksHtml(providerId);
                    if (!pdfLinksHtml.isEmpty()) {
                        writer.write(pdfLinksHtml);
                    }
                    
                    // Lade Notizen aus der Datenbank
                    String notes = historyDbManager.getProviderNotes(providerName);
                    
                    // Statistische Informationen
                    writer.write(generateStatsSection(stats, providerName));
                    
                    // Notizen anzeigen, falls vorhanden
                    if (notes != null && !notes.trim().isEmpty()) {
                        writer.write("<div class=\"notes-section\">\n");
                        writer.write("<h3>Notizen</h3>\n");
                        writer.write("<div class=\"notes-content\">\n");
                        writer.write("<p>" + notes.replace("\n", "<br>") + "</p>\n");
                        writer.write("</div>\n");
                        writer.write("</div>\n");
                    }
                    
                    // Charts erstellen und hinzufügen - hier nur die wichtigsten Charts
                    writer.write(generateChartsSection(stats, providerId, reportImagesDir.getPath(), imagesDir));
                    
                    writer.write("</div>\n"); // Ende provider-section
                    
                    // Horizontale Linie zwischen den Providern
                    writer.write("<hr>\n");
                }
                
                writer.write("</div>\n"); // Ende main-content
                writer.write(generateHtmlFooter());
            }
            
            LOGGER.info("Report erfolgreich erstellt: " + outputPath);
            return outputPath;
            
        } catch (IOException e) {
            LOGGER.severe("Fehler beim Erstellen des Reports: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Extrahiert die Provider-ID aus dem Provider-Namen
     */
    private String extractProviderId(String providerName) {
        if (providerName.contains("_")) {
            return providerName.substring(providerName.lastIndexOf("_") + 1).replace(".csv", "");
        } else {
            // Fallback: Nur Zahlen aus dem Namen extrahieren
            StringBuilder digits = new StringBuilder();
            for (char ch : providerName.toCharArray()) {
                if (Character.isDigit(ch)) {
                    digits.append(ch);
                }
            }
            return digits.length() > 0 ? digits.toString() : "provider" + providerName.hashCode();
        }
    }
    
    /**
     * Speichert ein JFreeChart als Bild im angegebenen Verzeichnis
     * 
     * @param chart Das JFreeChart-Objekt
     * @param directory Das Zielverzeichnis
     * @param name Der Basisname für die Bilddatei
     * @return Der Pfad zur erstellten Bilddatei oder null bei Fehler
     */
    private String saveChartAsImage(JFreeChart chart, String directory, String name) {
        if (chart == null) {
            return null;
        }
        
        try {
            String imagePath = directory + File.separator + name + ".png";
            File imageFile = new File(imagePath);
            
            // Größe des Charts für den Export
            BufferedImage image = chart.createBufferedImage(950, 300);
            ImageIO.write(image, "png", imageFile);
            
            return imagePath;
        } catch (IOException e) {
            LOGGER.warning("Fehler beim Speichern des Charts als Bild: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Generiert den HTML-Header für den Report (erweitert um PDF-CSS)
     */
    private String generateHtmlHeader(String title, String timestamp) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"de\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>" + title + "</title>\n" +
                "    <style>\n" +
                "        body {\n" +
                "            font-family: Arial, sans-serif;\n" +
                "            line-height: 1.6;\n" +
                "            margin: 0;\n" +
                "            padding: 20px;\n" +
                "            color: #333;\n" +
                "            background-color: #f8f8f8;\n" +
                "        }\n" +
                "        header {\n" +
                "            background-color: #2c3e50;\n" +
                "            color: white;\n" +
                "            padding: 20px;\n" +
                "            margin-bottom: 20px;\n" +
                "            border-radius: 5px;\n" +
                "        }\n" +
                "        h1, h2, h3, h4 {\n" +
                "            color: #2c3e50;\n" +
                "        }\n" +
                "        .toc {\n" +
                "            background-color: white;\n" +
                "            padding: 20px;\n" +
                "            margin-bottom: 20px;\n" +
                "            border-radius: 5px;\n" +
                "            box-shadow: 0 2px 4px rgba(0,0,0,0.1);\n" +
                "        }\n" +
                "        .toc ul {\n" +
                "            list-style-type: none;\n" +
                "            padding-left: 20px;\n" +
                "        }\n" +
                "        .toc a {\n" +
                "            color: #3498db;\n" +
                "            text-decoration: none;\n" +
                "        }\n" +
                "        .toc a:hover {\n" +
                "            text-decoration: underline;\n" +
                "        }\n" +
                "        .provider-section {\n" +
                "            background-color: white;\n" +
                "            padding: 20px;\n" +
                "            margin-bottom: 20px;\n" +
                "            border-radius: 5px;\n" +
                "            box-shadow: 0 2px 4px rgba(0,0,0,0.1);\n" +
                "        }\n" +
                "        .stats-info {\n" +
                "            margin-bottom: 20px;\n" +
                "        }\n" +
                "        .stats-table {\n" +
                "            width: 100%;\n" +
                "            border-collapse: collapse;\n" +
                "            margin-bottom: 20px;\n" +
                "        }\n" +
                "        .stats-table th, .stats-table td {\n" +
                "            border: 1px solid #ddd;\n" +
                "            padding: 8px;\n" +
                "            text-align: left;\n" +
                "        }\n" +
                "        .stats-table th {\n" +
                "            background-color: #f2f2f2;\n" +
                "        }\n" +
                "        .stats-table tr:nth-child(even) {\n" +
                "            background-color: #f9f9f9;\n" +
                "        }\n" +
                "        .notes-section {\n" +
                "            background-color: #f9f9e7;\n" +
                "            padding: 15px;\n" +
                "            border-radius: 5px;\n" +
                "            margin-bottom: 20px;\n" +
                "            border-left: 4px solid #f0e68c;\n" +
                "        }\n" +
                "        .charts-section {\n" +
                "            margin-top: 20px;\n" +
                "        }\n" +
                "        .chart-container {\n" +
                "            margin-bottom: 30px;\n" +
                "        }\n" +
                "        .chart-image {\n" +
                "            max-width: 100%;\n" +
                "            height: auto;\n" +
                "            border: 1px solid #ddd;\n" +
                "            border-radius: 5px;\n" +
                "        }\n" +
                "        .mql-link {\n" +
                "            margin-bottom: 15px;\n" +
                "        }\n" +
                "        .mql-link a {\n" +
                "            color: #3498db;\n" +
                "            text-decoration: none;\n" +
                "            font-weight: bold;\n" +
                "        }\n" +
                "        .mql-link a:hover {\n" +
                "            text-decoration: underline;\n" +
                "        }\n" +
                "        footer {\n" +
                "            margin-top: 30px;\n" +
                "            text-align: center;\n" +
                "            font-size: 0.8em;\n" +
                "            color: #777;\n" +
                "        }\n" +
                "        hr {\n" +
                "            border: none;\n" +
                "            height: 1px;\n" +
                "            background-color: #ddd;\n" +
                "            margin: 30px 0;\n" +
                "        }\n" +
                "        /* Kategorie-Farben */\n" +
                "        .category-dark-green {\n" +
                "            color: white;\n" +
                "            background-color: #006400;\n" +
                "            padding: 2px 6px;\n" +
                "            border-radius: 4px;\n" +
                "            font-weight: bold;\n" +
                "        }\n" +
                "        .category-light-green {\n" +
                "            color: white;\n" +
                "            background-color: #32CD32;\n" +
                "            padding: 2px 6px;\n" +
                "            border-radius: 4px;\n" +
                "            font-weight: bold;\n" +
                "        }\n" +
                "        .category-orange {\n" +
                "            color: white;\n" +
                "            background-color: #FF8C00;\n" +
                "            padding: 2px 6px;\n" +
                "            border-radius: 4px;\n" +
                "            font-weight: bold;\n" +
                "        }\n" +
                "        .category-default {\n" +
                "            color: white;\n" +
                "            background-color: #777;\n" +
                "            padding: 2px 6px;\n" +
                "            border-radius: 4px;\n" +
                "            font-weight: bold;\n" +
                "        }\n" +
                // PDF-CSS hinzufügen
                pdfIntegrator.generatePdfCss() +
                pdfIntegrator.generateTocPdfCss() +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <header>\n" +
                "        <h1>" + title + "</h1>\n" +
                "        <p>Erstellt am: " + new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date()) + "</p>\n" +
                "    </header>\n";
    }
    
    /**
     * Generiert den HTML-Footer für den Report
     */
    private String generateHtmlFooter() {
        return "    <footer>\n" +
                "        <p>Dieser Report wurde automatisch generiert.</p>\n" +
                "    </footer>\n" +
                "</body>\n" +
                "</html>";
    }
}