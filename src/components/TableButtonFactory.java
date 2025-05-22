package components;

import java.awt.Desktop;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import data.DataManager;
import data.FavoritesManager;
import data.ProviderStats;
import reports.ReportGenerator;
import ui.ShowSignalProviderList;
import utils.HtmlDatabase;

/**
 * Factory-Klasse für die Erstellung von Buttons der MainTable.
 * Behandelt die Erstellung und Konfiguration aller Buttons.
 */
public class TableButtonFactory {
    private static final Logger LOGGER = Logger.getLogger(TableButtonFactory.class.getName());
    
    private final MainTable mainTable;
    private final DataManager dataManager;
    private final HtmlDatabase htmlDatabase;
    private final String rootPath;
    private final TableFilterManager filterManager;
    private Consumer<String> statusUpdateCallback;
    
    public TableButtonFactory(MainTable mainTable, DataManager dataManager, HtmlDatabase htmlDatabase, 
                             String rootPath, TableFilterManager filterManager) {
        this.mainTable = mainTable;
        this.dataManager = dataManager;
        this.htmlDatabase = htmlDatabase;
        this.rootPath = rootPath;
        this.filterManager = filterManager;
    }
    
    /**
     * Setzt den Status-Update-Callback
     */
    public void setStatusUpdateCallback(Consumer<String> callback) {
        this.statusUpdateCallback = callback;
    }
    
    /**
     * Erstellt den Button zum Anzeigen der Signal Provider Liste
     */
    public JButton createShowSignalProviderButton() {
        JButton showProvidersButton = new JButton("Show Signal Providers");
        showProvidersButton.addActionListener(e -> {
            Map<String, ProviderStats> currentStats = getCurrentProviderStats();
            ShowSignalProviderList dialog = new ShowSignalProviderList(
                SwingUtilities.getWindowAncestor(mainTable),
                currentStats,
                htmlDatabase,
                rootPath
            );
            dialog.setVisible(true);
        });
        return showProvidersButton;
    }
    
    /**
     * Erstellt den Button für die Report-Generierung
     */
    public JButton createReportButton() {
        JButton reportButton = new JButton("Favoriten-Report erstellen");
        reportButton.addActionListener(e -> {
            // Dialoge und Fortschrittsanzeigen hier in einem separaten Thread, um die UI nicht zu blockieren
            new Thread(() -> {
                try {
                    generateFavoritesReport();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(mainTable,
                                "Unerwarteter Fehler: " + ex.getMessage(),
                                "Fehler",
                                JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
        });
        
        return reportButton;
    }
    
    /**
     * Generiert den Favoriten-Report
     */
    private void generateFavoritesReport() {
        // Alle Favoriten ermitteln
        FavoritesManager favManager = FavoritesManager.getInstance(rootPath);
        Map<String, ProviderStats> favorites = new HashMap<>();
        
        // Über alle verfügbaren Provider iterieren
        for (Map.Entry<String, ProviderStats> entry : dataManager.getStats().entrySet()) {
            String providerName = entry.getKey();
            ProviderStats stats = entry.getValue();
            
            // Provider-ID extrahieren
            String providerId = extractProviderId(providerName);
            
            // Prüfen, ob dieser Provider ein Favorit ist
            if (!providerId.isEmpty() && favManager.isFavorite(providerId)) {
                favorites.put(providerName, stats);
            }
        }
        
        // Prüfen, ob Favoriten gefunden wurden
        if (favorites.isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(mainTable,
                        "Es wurden keine Favoriten gefunden.\nBitte markieren Sie zuerst einige Signal Provider als Favoriten.",
                        "Keine Favoriten",
                        JOptionPane.INFORMATION_MESSAGE);
            });
            return;
        }
        
        // Fortschrittsanzeige während der Report-Generierung
        final int totalProviders = favorites.size();
        SwingUtilities.invokeLater(() -> {
            if (statusUpdateCallback != null) {
                statusUpdateCallback.accept("Generiere Report für " + totalProviders + " Favoriten...");
            }
        });
        
        // Report-Generator initialisieren und Report erstellen
        ReportGenerator reportGenerator = new ReportGenerator(rootPath, htmlDatabase);
        String reportPath = reportGenerator.generateReport(favorites);
        
        if (reportPath != null) {
            handleReportSuccess(reportPath);
        } else {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(mainTable,
                        "Fehler beim Erstellen des Reports. Bitte prüfen Sie die Logs.",
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE);
            });
        }
    }
    
    /**
     * Behandelt erfolgreiche Report-Generierung
     */
    private void handleReportSuccess(String reportPath) {
        SwingUtilities.invokeLater(() -> {
            // Statusmeldung aktualisieren
            if (statusUpdateCallback != null) {
                statusUpdateCallback.accept("Report erfolgreich erstellt: " + reportPath);
            }
            
            // Erfolgsmeldung anzeigen
            int choice = JOptionPane.showConfirmDialog(mainTable,
                    "Report wurde erfolgreich erstellt in:\n" + reportPath + "\n\nMöchten Sie den Report jetzt öffnen?",
                    "Report erstellt",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE);
            
            // Bei Bedarf den Report im Standardbrowser öffnen
            if (choice == JOptionPane.YES_OPTION) {
                try {
                    File htmlFile = new File(reportPath);
                    Desktop.getDesktop().browse(htmlFile.toURI());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(mainTable,
                            "Fehler beim Öffnen des Reports: " + ex.getMessage(),
                            "Fehler",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }
    
    /**
     * Holt die aktuellen Provider-Statistiken (gefiltert oder alle)
     */
    private Map<String, ProviderStats> getCurrentProviderStats() {
        // Debug-Ausgabe hinzufügen, um das Problem zu identifizieren
        LOGGER.info("getCurrentProviderStats wird aufgerufen");
        
        // Versuche die gefilterten Daten vom FilterManager zu bekommen
        Map<String, ProviderStats> filteredStats = filterManager.getFilteredProviderStats();
        
        // Überprüfe, ob die gefilterten Daten gültig sind
        if (filteredStats == null || filteredStats.isEmpty()) {
            LOGGER.warning("Gefilterte Provider-Statistiken sind leer oder null");
            
            // Als Fallback verwenden wir alle verfügbaren Statistiken vom DataManager
            Map<String, ProviderStats> allStats = dataManager.getStats();
            
            LOGGER.info("Verwende alle verfügbaren Provider als Fallback: " + 
                       (allStats != null ? allStats.size() : "null") + " Provider gefunden");
            
            return allStats != null ? allStats : new HashMap<>();
        }
        
        LOGGER.info("Gefilterte Provider-Statistiken enthalten " + filteredStats.size() + " Provider");
        return filteredStats;
    }
    
    /**
     * Extrahiert die Provider-ID aus dem Providernamen
     */
    private String extractProviderId(String providerName) {
        // Provider-ID aus dem Namen extrahieren
        String providerId = "";
        if (providerName.contains("_")) {
            providerId = providerName.substring(providerName.lastIndexOf("_") + 1).replace(".csv", "");
        } else {
            // Fallback für unerwartetes Format
            StringBuilder digits = new StringBuilder();
            for (char ch : providerName.toCharArray()) {
                if (Character.isDigit(ch)) {
                    digits.append(ch);
                }
            }
            if (digits.length() > 0) {
                providerId = digits.toString();
            }
        }
        return providerId;
    }
}