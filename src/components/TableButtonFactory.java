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
     * Generiert den Favoriten-Report mit Kategorien-Auswahl
     */
    private void generateFavoritesReport() {
        try {
            // Dialog für die Kategorien-Auswahl anzeigen (im EDT)
            final String[] selectedOption = new String[1];
            final boolean[] cancelled = new boolean[1];
            
            SwingUtilities.invokeAndWait(() -> {
                ui.dialogs.FavoritesReportSelectionDialog dialog = 
                    new ui.dialogs.FavoritesReportSelectionDialog(
                        SwingUtilities.getWindowAncestor(mainTable)
                    );
                
                selectedOption[0] = dialog.showDialog();
                cancelled[0] = dialog.wasCancelled();
            });
            
            // Prüfen, ob der Dialog abgebrochen wurde
            if (selectedOption[0] == null || cancelled[0]) {
                return; // Benutzer hat abgebrochen
            }
            
            // Entsprechende Favoriten basierend auf der Auswahl sammeln
            Map<String, ProviderStats> favorites = collectFavoritesForOption(selectedOption[0]);
            
            // Prüfen, ob Favoriten gefunden wurden
            if (favorites.isEmpty()) {
                SwingUtilities.invokeLater(() -> {
                    String message = getNoFavoritesMessage(selectedOption[0]);
                    JOptionPane.showMessageDialog(mainTable,
                            message,
                            "Keine Favoriten",
                            JOptionPane.INFORMATION_MESSAGE);
                });
                return;
            }
            
            // Fortschrittsanzeige während der Report-Generierung
            final int totalProviders = favorites.size();
            SwingUtilities.invokeLater(() -> {
                if (statusUpdateCallback != null) {
                    String optionText = getOptionDisplayText(selectedOption[0]);
                    statusUpdateCallback.accept("Generiere Report für " + totalProviders + 
                        " Favoriten (" + optionText + ")...");
                }
            });
            
            // Report-Generator initialisieren und Report erstellen
            ReportGenerator reportGenerator = new ReportGenerator(rootPath, htmlDatabase);
            String reportPath = generateReportForOption(reportGenerator, favorites, selectedOption[0]);
            
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
        } catch (Exception ex) {
            ex.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(mainTable,
                        "Unerwarteter Fehler beim Erstellen des Reports: " + ex.getMessage(),
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE);
            });
        }
    }
    
    /**
     * Sammelt Favoriten basierend auf der ausgewählten Option
     * 
     * @param selectedOption Die ausgewählte Option (All, 1-2, 1-3)
     * @return Map mit den entsprechenden Favoriten
     */
    private Map<String, ProviderStats> collectFavoritesForOption(String selectedOption) {
        FavoritesManager favManager = FavoritesManager.getInstance(rootPath);
        Map<String, ProviderStats> favorites = new HashMap<>();
        
        // Über alle verfügbaren Provider iterieren
        for (Map.Entry<String, ProviderStats> entry : dataManager.getStats().entrySet()) {
            String providerName = entry.getKey();
            ProviderStats stats = entry.getValue();
            
            // Provider-ID extrahieren
            String providerId = extractProviderId(providerName);
            
            if (providerId.isEmpty()) {
                continue; // Überspringe Provider ohne gültige ID
            }
            
            // Prüfen, ob dieser Provider in der gewählten Kategorie ist
            if (isProviderInSelectedCategory(favManager, providerId, selectedOption)) {
                favorites.put(providerName, stats);
            }
        }
        
        return favorites;
    }
    
    /**
     * Prüft, ob ein Provider in der ausgewählten Kategorie ist
     * 
     * @param favManager Der FavoritesManager
     * @param providerId Die Provider-ID
     * @param selectedOption Die ausgewählte Option
     * @return true wenn der Provider in der Kategorie ist
     */
    private boolean isProviderInSelectedCategory(FavoritesManager favManager, 
                                               String providerId, 
                                               String selectedOption) {
        if (!favManager.isFavorite(providerId)) {
            return false; // Nicht mal ein Favorit
        }
        
        int category = favManager.getFavoriteCategory(providerId);
        
        switch (selectedOption) {
            case ui.dialogs.FavoritesReportSelectionDialog.OPTION_ALL:
                return true; // Alle Favoriten (1-10)
                
            case ui.dialogs.FavoritesReportSelectionDialog.OPTION_1_2:
                return category == 1 || category == 2;
                
            case ui.dialogs.FavoritesReportSelectionDialog.OPTION_1_3:
                return category >= 1 && category <= 3;
                
            default:
                return false;
        }
    }
    
    /**
     * Generiert den Report für die ausgewählte Option
     * 
     * @param reportGenerator Der ReportGenerator
     * @param favorites Die Favoriten-Provider
     * @param selectedOption Die ausgewählte Option
     * @return Der Pfad zum generierten Report
     */
    private String generateReportForOption(ReportGenerator reportGenerator, 
                                          Map<String, ProviderStats> favorites, 
                                          String selectedOption) {
        
        // Für "All" können wir den Standard-ReportGenerator verwenden
        if (ui.dialogs.FavoritesReportSelectionDialog.OPTION_ALL.equals(selectedOption)) {
            return reportGenerator.generateReport(favorites);
        }
        
        // Für spezifische Kategorien (1-2, 1-3) erstellen wir einen Custom-Report
        return generateCustomFavoritesReport(reportGenerator, favorites, selectedOption);
    }
    
    /**
     * Generiert einen benutzerdefinierten Report für spezifische Kategorien
     * 
     * @param reportGenerator Der ReportGenerator
     * @param favorites Die Favoriten-Provider
     * @param selectedOption Die ausgewählte Option
     * @return Der Pfad zum generierten Report
     */
    private String generateCustomFavoritesReport(ReportGenerator reportGenerator,
                                                Map<String, ProviderStats> favorites,
                                                String selectedOption) {
        
        // Benutzerdefinierten Titel und Pfad erstellen
        String optionText = getOptionDisplayText(selectedOption);
        String reportTitle = "Favoriten Signal Provider Report - " + optionText;
        
        // Dateiname mit Kategorie-Information
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = dateFormat.format(new java.util.Date());
        String categoryStr = getFilenameCategory(selectedOption);
        String reportFileName = "favorites_" + categoryStr + "_report_" + timestamp + ".html";
        String reportPath = rootPath + File.separator + "report" + File.separator + reportFileName;
        
        // Custom-Report generieren
        return reportGenerator.generateReport(favorites, reportTitle, reportPath);
    }
    
    /**
     * Gibt den Anzeige-Text für eine Option zurück
     * 
     * @param selectedOption Die ausgewählte Option
     * @return Der Anzeige-Text
     */
    private String getOptionDisplayText(String selectedOption) {
        switch (selectedOption) {
            case ui.dialogs.FavoritesReportSelectionDialog.OPTION_ALL:
                return "Alle Kategorien";
            case ui.dialogs.FavoritesReportSelectionDialog.OPTION_1_2:
                return "Kategorie 1-2";
            case ui.dialogs.FavoritesReportSelectionDialog.OPTION_1_3:
                return "Kategorie 1-3";
            default:
                return selectedOption;
        }
    }
    
    /**
     * Gibt den Dateinamen-Teil für eine Option zurück
     * 
     * @param selectedOption Die ausgewählte Option
     * @return Der Dateinamen-Teil
     */
    private String getFilenameCategory(String selectedOption) {
        switch (selectedOption) {
            case ui.dialogs.FavoritesReportSelectionDialog.OPTION_ALL:
                return "all";
            case ui.dialogs.FavoritesReportSelectionDialog.OPTION_1_2:
                return "1-2";
            case ui.dialogs.FavoritesReportSelectionDialog.OPTION_1_3:
                return "1-3";
            default:
                return "unknown";
        }
    }
    
    /**
     * Gibt die entsprechende Nachricht zurück, wenn keine Favoriten gefunden wurden
     * 
     * @param selectedOption Die ausgewählte Option
     * @return Die Nachricht
     */
    private String getNoFavoritesMessage(String selectedOption) {
        switch (selectedOption) {
            case ui.dialogs.FavoritesReportSelectionDialog.OPTION_ALL:
                return "Es wurden keine Favoriten gefunden.\nBitte markieren Sie zuerst einige Signal Provider als Favoriten.";
            case ui.dialogs.FavoritesReportSelectionDialog.OPTION_1_2:
                return "Es wurden keine Favoriten in den Kategorien 1-2 gefunden.\nBitte markieren Sie zuerst einige Signal Provider als Favoriten in diesen Kategorien.";
            case ui.dialogs.FavoritesReportSelectionDialog.OPTION_1_3:
                return "Es wurden keine Favoriten in den Kategorien 1-3 gefunden.\nBitte markieren Sie zuerst einige Signal Provider als Favoriten in diesen Kategorien.";
            default:
                return "Es wurden keine Favoriten in der ausgewählten Kategorie gefunden.";
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