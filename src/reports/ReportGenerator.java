package reports;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
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

/**
 * Klasse zum Erstellen eines HTML-Reports für favorisierte Signal Provider
 */
public class ReportGenerator {
    private static final Logger LOGGER = Logger.getLogger(ReportGenerator.class.getName());
    private final String rootPath;
    private final HtmlDatabase htmlDatabase;
    private final HistoryDatabaseManager historyDbManager;
    private final FavoritesManager favoritesManager;
    
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
        
        try (FileWriter writer = new FileWriter(reportPath)) {
            // HTML-Header schreiben
            writer.write(generateHtmlHeader(reportTitle, timestamp));
            
            // Inhaltsverzeichnis erstellen
            writer.write("<div class=\"toc\">\n");
            writer.write("<h2>Inhaltsverzeichnis</h2>\n");
            writer.write("<ul>\n");
            for (String providerName : filteredProviders.keySet()) {
                String providerId = extractProviderId(providerName);
                // Kategorie anzeigen, wenn alle Kategorien angezeigt werden
                String categoryInfo = "";
                if (category == 0) {
                    int providerCategory = favoritesManager.getFavoriteCategory(providerId);
                    if (providerCategory > 0) {
                        categoryInfo = " (Kategorie " + providerCategory + ")";
                    }
                }
                writer.write("<li><a href=\"#" + providerId + "\">" + providerName + categoryInfo + "</a></li>\n");
            }
            writer.write("</ul>\n");
            writer.write("</div>\n");
            
            // Hauptinhalt mit Provider-Informationen
            writer.write("<div class=\"main-content\">\n");
            
            // Für jeden Provider im Report
            for (Map.Entry<String, ProviderStats> entry : filteredProviders.entrySet()) {
                String providerName = entry.getKey();
                ProviderStats stats = entry.getValue();
                String providerId = extractProviderId(providerName);
                
                writer.write("<div class=\"provider-section\" id=\"" + providerId + "\">\n");
                
                // Kategorie-Info im Titel anzeigen
                int providerCategory = favoritesManager.getFavoriteCategory(providerId);
                String categoryInfo = providerCategory > 0 ? " (Kategorie " + providerCategory + ")" : "";
                writer.write("<h2>" + providerName + categoryInfo + "</h2>\n");
                
                // Lade Notizen aus der Datenbank
                String notes = historyDbManager.getProviderNotes(providerName);
                
                // Statistische Informationen
                writer.write("<div class=\"stats-info\">\n");
                writer.write("<h3>Handelsinformationen</h3>\n");
                writer.write("<table class=\"stats-table\">\n");
                writer.write("<tr><th>Kennzahl</th><th>Wert</th></tr>\n");
                writer.write("<tr><td>Total Trades</td><td>" + stats.getTrades().size() + "</td></tr>\n");
                writer.write("<tr><td>Win Rate</td><td>" + String.format("%.2f%%", stats.getWinRate() * 100) + "</td></tr>\n");
                writer.write("<tr><td>Profit</td><td>" + String.format("%.2f", stats.getTotalProfit()) + "</td></tr>\n");
                //writer.write("<tr><td>Avg Profit/Trade</td><td>" + String.format("%.2f", stats.getAvgProfitPerTrade()) + "</td></tr>\n");
                
                // MPDD-Werte
                double threeMonthProfit = htmlDatabase.getAverageMonthlyProfit(providerName, 3);
                double equityDrawdown = htmlDatabase.getEquityDrawdown(providerName);
                double maxDrawdownGraphic = htmlDatabase.getEquityDrawdownGraphic(providerName);
                
                writer.write("<tr><td>3MPDD</td><td>" + String.format("%.2f", threeMonthProfit / Math.max(0.01, equityDrawdown)) + "</td></tr>\n");
                writer.write("<tr><td>Equity Drawdown</td><td>" + String.format("%.2f%%", equityDrawdown) + "</td></tr>\n");
                writer.write("<tr><td>Max Drawdown</td><td>" + String.format("%.2f%%", maxDrawdownGraphic) + "</td></tr>\n");
                writer.write("</table>\n");
                
                // Notizen anzeigen, falls vorhanden
                if (notes != null && !notes.trim().isEmpty()) {
                    writer.write("<div class=\"notes-section\">\n");
                    writer.write("<h3>Notizen</h3>\n");
                    writer.write("<div class=\"notes-content\">\n");
                    writer.write("<p>" + notes.replace("\n", "<br>") + "</p>\n");
                    writer.write("</div>\n");
                    writer.write("</div>\n");
                }
                
                writer.write("</div>\n"); // Ende stats-info
                
                // Charts erstellen und hinzufügen
                writer.write("<div class=\"charts-section\">\n");
                writer.write("<h3>Charts</h3>\n");
                
                // Equity Drawdown Chart
                EquityDrawdownChart equityDrawdownChart = new EquityDrawdownChart(stats, maxDrawdownGraphic, htmlDatabase);
                String equityDrawdownChartPath = saveChartAsImage(equityDrawdownChart.getChart(), 
                        reportImagesDir.getPath(), providerId + "_equity_drawdown");
                if (equityDrawdownChartPath != null) {
                    writer.write("<div class=\"chart-container\">\n");
                    writer.write("<h4>Equity Drawdown</h4>\n");
                    writer.write("<img src=\"" + imagesDir + "/" + new File(equityDrawdownChartPath).getName() + 
                            "\" alt=\"Equity Drawdown Chart\" class=\"chart-image\">\n");
                    writer.write("</div>\n");
                }
                
                // Weitere Charts hinzufügen, falls genügend Trades vorhanden sind
                if (stats.getTrades().size() > 5) {
                    // Symbol Distribution Chart
                    SymbolDistributionChart symbolChart = new SymbolDistributionChart(stats.getTrades());
                    String symbolChartPath = saveChartAsImage(symbolChart.getChart(), 
                            reportImagesDir.getPath(), providerId + "_symbol_distribution");
                    if (symbolChartPath != null) {
                        writer.write("<div class=\"chart-container\">\n");
                        writer.write("<h4>Symbol Verteilung</h4>\n");
                        writer.write("<img src=\"" + imagesDir + "/" + new File(symbolChartPath).getName() + 
                                "\" alt=\"Symbol Distribution Chart\" class=\"chart-image\">\n");
                        writer.write("</div>\n");
                    }
                    
                    // Duration Profit Chart (hier nur als Komponente, daher explizites Rendering)
                    DurationProfitChart durationChart = new DurationProfitChart(stats.getTrades());
                    // Die Größe für das Rendering anpassen
                    durationChart.setSize(new Dimension(950, 400));
                    BufferedImage durationImage = new BufferedImage(950, 400, BufferedImage.TYPE_INT_ARGB);
                    durationChart.paint(durationImage.getGraphics());
                    // Bild speichern
                    String durationChartPath = reportImagesDir.getPath() + File.separator + 
                            providerId + "_duration_profit.png";
                    ImageIO.write(durationImage, "png", new File(durationChartPath));
                    writer.write("<div class=\"chart-container\">\n");
                    writer.write("<h4>Dauer/Profit Verhältnis</h4>\n");
                    writer.write("<img src=\"" + imagesDir + "/" + providerId + "_duration_profit.png" + 
                            "\" alt=\"Duration Profit Chart\" class=\"chart-image\">\n");
                    writer.write("</div>\n");
                    
                    // Weitere Charts hinzufügen, wenn mehr als 20 Trades vorhanden sind
                    if (stats.getTrades().size() > 20) {
                        // Efficiency Chart
                        EfficiencyChart efficiencyChart = new EfficiencyChart(stats.getTrades());
                        efficiencyChart.setSize(new Dimension(950, 300));
                        BufferedImage efficiencyImage = new BufferedImage(950, 300, BufferedImage.TYPE_INT_ARGB);
                        efficiencyChart.paint(efficiencyImage.getGraphics());
                        String efficiencyChartPath = reportImagesDir.getPath() + File.separator + 
                                providerId + "_efficiency.png";
                        ImageIO.write(efficiencyImage, "png", new File(efficiencyChartPath));
                        writer.write("<div class=\"chart-container\">\n");
                        writer.write("<h4>Effizienzwert</h4>\n");
                        writer.write("<img src=\"" + imagesDir + "/" + providerId + "_efficiency.png" + 
                                "\" alt=\"Efficiency Chart\" class=\"chart-image\">\n");
                        writer.write("</div>\n");
                        
                        // Weekly Lotsize Chart
                        WeeklyLotsizeChart lotsizeChart = new WeeklyLotsizeChart(stats.getTrades());
                        lotsizeChart.setSize(new Dimension(950, 300));
                        BufferedImage lotsizeImage = new BufferedImage(950, 300, BufferedImage.TYPE_INT_ARGB);
                        lotsizeChart.paint(lotsizeImage.getGraphics());
                        String lotsizeChartPath = reportImagesDir.getPath() + File.separator + 
                                providerId + "_weekly_lotsize.png";
                        ImageIO.write(lotsizeImage, "png", new File(lotsizeChartPath));
                        writer.write("<div class=\"chart-container\">\n");
                        writer.write("<h4>Wöchentliche Lot-Größe</h4>\n");
                        writer.write("<img src=\"" + imagesDir + "/" + providerId + "_weekly_lotsize.png" + 
                                "\" alt=\"Weekly Lotsize Chart\" class=\"chart-image\">\n");
                        writer.write("</div>\n");
                    }
                }
                
                writer.write("</div>\n"); // Ende charts-section
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
        
        try {
            // Report-Verzeichnis für Bilder
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imagesDir = "images_" + timestamp;
            File reportImagesDir = new File(new File(outputPath).getParentFile(), imagesDir);
            if (!reportImagesDir.exists()) {
                reportImagesDir.mkdirs();
            }
            
            try (FileWriter writer = new FileWriter(outputPath)) {
                // HTML-Header schreiben
                writer.write(generateHtmlHeader(reportTitle, timestamp));
                
                // Inhaltsverzeichnis erstellen
                writer.write("<div class=\"toc\">\n");
                writer.write("<h2>Inhaltsverzeichnis</h2>\n");
                writer.write("<ul>\n");
                for (String providerName : providerStats.keySet()) {
                    String providerId = extractProviderId(providerName);
                    
                    // Kategorie anzeigen, wenn es ein Favorit ist
                    String categoryInfo = "";
                    int providerCategory = favoritesManager.getFavoriteCategory(providerId);
                    if (providerCategory > 0) {
                        categoryInfo = " (Favorit, Kategorie " + providerCategory + ")";
                    }
                    
                    writer.write("<li><a href=\"#" + providerId + "\">" + providerName + categoryInfo + "</a></li>\n");
                }
                writer.write("</ul>\n");
                writer.write("</div>\n");
                
                // Hauptinhalt mit Provider-Informationen
                writer.write("<div class=\"main-content\">\n");
                
                // Für jeden Provider im Report
                for (Map.Entry<String, ProviderStats> entry : providerStats.entrySet()) {
                    String providerName = entry.getKey();
                    ProviderStats stats = entry.getValue();
                    String providerId = extractProviderId(providerName);
                    
                    writer.write("<div class=\"provider-section\" id=\"" + providerId + "\">\n");
                    
                    // Kategorie-Info im Titel anzeigen
                    int providerCategory = favoritesManager.getFavoriteCategory(providerId);
                    String categoryInfo = "";
                    if (providerCategory > 0) {
                        categoryInfo = " (Favorit, Kategorie " + providerCategory + ")";
                    }
                    writer.write("<h2>" + providerName + categoryInfo + "</h2>\n");
                    
                    // Lade Notizen aus der Datenbank
                    String notes = historyDbManager.getProviderNotes(providerName);
                    
                    // Statistische Informationen
                    writer.write("<div class=\"stats-info\">\n");
                    writer.write("<h3>Handelsinformationen</h3>\n");
                    writer.write("<table class=\"stats-table\">\n");
                    writer.write("<tr><th>Kennzahl</th><th>Wert</th></tr>\n");
                    writer.write("<tr><td>Total Trades</td><td>" + stats.getTrades().size() + "</td></tr>\n");
                    writer.write("<tr><td>Win Rate</td><td>" + String.format("%.2f%%", stats.getWinRate() * 100) + "</td></tr>\n");
                    writer.write("<tr><td>Profit</td><td>" + String.format("%.2f", stats.getTotalProfit()) + "</td></tr>\n");
                    
                    // MPDD-Werte
                    double threeMonthProfit = htmlDatabase.getAverageMonthlyProfit(providerName, 3);
                    double equityDrawdown = htmlDatabase.getEquityDrawdown(providerName);
                    double maxDrawdownGraphic = htmlDatabase.getEquityDrawdownGraphic(providerName);
                    
                    writer.write("<tr><td>3MPDD</td><td>" + String.format("%.2f", threeMonthProfit / Math.max(0.01, equityDrawdown)) + "</td></tr>\n");
                    writer.write("<tr><td>Equity Drawdown</td><td>" + String.format("%.2f%%", equityDrawdown) + "</td></tr>\n");
                    writer.write("<tr><td>Max Drawdown</td><td>" + String.format("%.2f%%", maxDrawdownGraphic) + "</td></tr>\n");
                    writer.write("</table>\n");
                    
                    // Notizen anzeigen, falls vorhanden
                    if (notes != null && !notes.trim().isEmpty()) {
                        writer.write("<div class=\"notes-section\">\n");
                        writer.write("<h3>Notizen</h3>\n");
                        writer.write("<div class=\"notes-content\">\n");
                        writer.write("<p>" + notes.replace("\n", "<br>") + "</p>\n");
                        writer.write("</div>\n");
                        writer.write("</div>\n");
                    }
                    
                    writer.write("</div>\n"); // Ende stats-info
                    
                    // Charts erstellen und hinzufügen - hier nur die wichtigsten Charts
                    writer.write("<div class=\"charts-section\">\n");
                    writer.write("<h3>Charts</h3>\n");
                    
                    // Equity Drawdown Chart
                    EquityDrawdownChart equityDrawdownChart = new EquityDrawdownChart(stats, maxDrawdownGraphic, htmlDatabase);
                    String equityDrawdownChartPath = saveChartAsImage(equityDrawdownChart.getChart(), 
                            reportImagesDir.getPath(), providerId + "_equity_drawdown");
                    if (equityDrawdownChartPath != null) {
                        writer.write("<div class=\"chart-container\">\n");
                        writer.write("<h4>Equity Drawdown</h4>\n");
                        writer.write("<img src=\"" + imagesDir + "/" + new File(equityDrawdownChartPath).getName() + 
                                "\" alt=\"Equity Drawdown Chart\" class=\"chart-image\">\n");
                        writer.write("</div>\n");
                    }
                    
                    writer.write("</div>\n"); // Ende charts-section
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
     * Generiert den HTML-Header für den Report
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