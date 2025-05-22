package components;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import data.DataManager;
import data.FavoritesManager;
import data.ProviderStats;
import models.HighlightTableModel;
import renderers.HighlightRenderer;
import utils.MqlAnalyserConf;

/**
 * Klasse für Provider-spezifische Operationen der MainTable.
 * Behandelt das Löschen, Auswählen und Verwalten von Signal-Providern.
 */
public class TableProviderManager {
    private static final Logger LOGGER = Logger.getLogger(TableProviderManager.class.getName());
    
    private final MainTable mainTable;
    private final HighlightTableModel model;
    private final DataManager dataManager;
    private final String rootPath;
    private final HighlightRenderer renderer;
    private final TableFilterManager filterManager;
    private Consumer<String> statusUpdateCallback;
    
    public TableProviderManager(MainTable mainTable, HighlightTableModel model, DataManager dataManager, 
                               String rootPath, HighlightRenderer renderer, TableFilterManager filterManager) {
        this.mainTable = mainTable;
        this.model = model;
        this.dataManager = dataManager;
        this.rootPath = rootPath;
        this.renderer = renderer;
        this.filterManager = filterManager;
    }
    
    /**
     * Setzt den Status-Update-Callback
     */
    public void setStatusUpdateCallback(Consumer<String> callback) {
        this.statusUpdateCallback = callback;
    }
    
    /**
     * Löscht die ausgewählten Signal-Provider aus dem Downloadbereich
     */
    public void deleteSelectedProviders() {
        List<String> selectedProviders = getSelectedProviders();
        
        if (selectedProviders.isEmpty()) {
            return;
        }
        
        // Bestätigung vom Benutzer einholen
        int result = JOptionPane.showConfirmDialog(
            mainTable,
            "Möchten Sie die ausgewählten " + selectedProviders.size() + " Signal Provider wirklich löschen?\n" +
            "Diese Aktion kann nicht rückgängig gemacht werden.",
            "Signal Provider löschen",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        
        MqlAnalyserConf config = new MqlAnalyserConf(rootPath);
        String downloadPath = config.getDownloadPath();
        
        boolean anyDeleted = false;
        List<String> deletedProvidersList = new ArrayList<>();
        
        // Speichern der Namen der zu löschenden Provider, um sie später aus dem Model zu entfernen
        List<String> providersToRemove = new ArrayList<>();
        
        for (String providerName : selectedProviders) {
            boolean providerDeleted = deleteProviderFiles(providerName, downloadPath);
            
            if (providerDeleted) {
                deletedProvidersList.add(providerName);
                providersToRemove.add(providerName);
                anyDeleted = true;
            }
        }
        
        if (anyDeleted) {
            handleSuccessfulDeletion(providersToRemove, deletedProvidersList);
        } else {
            JOptionPane.showMessageDialog(
                mainTable,
                "Es konnten keine Provider gelöscht werden.",
                "Keine Änderungen",
                JOptionPane.INFORMATION_MESSAGE
            );
        }
    }
    
    /**
     * Löscht alle Dateien für einen Provider
     */
    private boolean deleteProviderFiles(String providerName, String downloadPath) {
        boolean providerDeleted = false;
        
        LOGGER.info("Versuche Provider zu löschen: " + providerName);
        
        // Lösche die CSV-Datei
        File csvFile = new File(downloadPath, providerName);
        if (csvFile.exists()) {
            if (csvFile.delete()) {
                providerDeleted = true;
                LOGGER.info("CSV-Datei gelöscht: " + csvFile.getAbsolutePath());
            } else {
                LOGGER.warning("Konnte CSV-Datei nicht löschen: " + csvFile.getAbsolutePath());
            }
        } else {
            LOGGER.warning("CSV-Datei existiert nicht: " + csvFile.getAbsolutePath());
        }
        
        // Lösche die TXT-Datei
        String txtFileName = providerName.replace(".csv", "") + "_root.txt";
        File txtFile = new File(downloadPath, txtFileName);
        if (txtFile.exists()) {
            if (txtFile.delete()) {
                providerDeleted = true;
                LOGGER.info("TXT-Datei gelöscht: " + txtFile.getAbsolutePath());
            } else {
                LOGGER.warning("Konnte TXT-Datei nicht löschen: " + txtFile.getAbsolutePath());
            }
        } else {
            LOGGER.warning("TXT-Datei existiert nicht: " + txtFile.getAbsolutePath());
        }
        
        // Lösche die HTML-Datei - mit dem korrekten "_root.html" Suffix
        String baseProviderName = providerName.replace(".csv", "");
        String htmlFileName = baseProviderName + "_root.html";
        File htmlFile = new File(downloadPath, htmlFileName);
        
        if (htmlFile.exists()) {
            if (htmlFile.delete()) {
                providerDeleted = true;
                LOGGER.info("HTML-Datei gelöscht: " + htmlFile.getAbsolutePath());
            } else {
                LOGGER.warning("Konnte HTML-Datei nicht löschen: " + htmlFile.getAbsolutePath());
            }
        } else {
            LOGGER.warning("HTML-Datei mit _root.html existiert nicht: " + htmlFile.getAbsolutePath());
            
            // Alternative HTML-Dateiformate versuchen
            String[] alternativeSuffixes = {".html", "_index.html", "_history.html"};
            for (String suffix : alternativeSuffixes) {
                String altHtmlFileName = baseProviderName + suffix;
                File altHtmlFile = new File(downloadPath, altHtmlFileName);
                
                if (altHtmlFile.exists()) {
                    if (altHtmlFile.delete()) {
                        providerDeleted = true;
                        LOGGER.info("Alternative HTML-Datei gelöscht: " + altHtmlFile.getAbsolutePath());
                    } else {
                        LOGGER.warning("Konnte alternative HTML-Datei nicht löschen: " + altHtmlFile.getAbsolutePath());
                    }
                }
            }
        }
        
        return providerDeleted;
    }
    
    /**
     * Behandelt erfolgreiches Löschen von Providern
     */
    private void handleSuccessfulDeletion(List<String> providersToRemove, List<String> deletedProvidersList) {
        // Entferne die Provider aus dem DataManager-Cache
        Map<String, ProviderStats> stats = dataManager.getStats();
        for (String provider : providersToRemove) {
            stats.remove(provider);
        }
        
        // Cache im Renderer leeren
        if (renderer != null) {
            renderer.clearCache();
        }
        
        // Tabelle vollständig neu laden
        SwingUtilities.invokeLater(() -> {
            // Das Model komplett neu befüllen
            model.populateData(dataManager.getStats());
            
            // Neu filtern, falls ein Filter aktiv ist
            if (filterManager.getCurrentFilter() != null) {
                filterManager.applyFilter(filterManager.getCurrentFilter());
            }
            
            // Die Tabelle aktualisieren
            mainTable.updateUI();
            mainTable.repaint();
            
            // Statusmeldung aktualisieren
            if (statusUpdateCallback != null) {
                statusUpdateCallback.accept(getStatusText());
            }
        });
        
        showDeletionSuccessMessage(deletedProvidersList);
    }
    
    /**
     * Zeigt Erfolgsmeldung nach dem Löschen
     */
    private void showDeletionSuccessMessage(List<String> deletedProvidersList) {
        // Erstelle die Nachricht mit Begrenzung auf 20 Provider
        StringBuilder message = new StringBuilder("Gelöschte Provider:\n");
        int maxProvidersToShow = Math.min(20, deletedProvidersList.size());
        
        for (int i = 0; i < maxProvidersToShow; i++) {
            message.append("- ").append(deletedProvidersList.get(i)).append("\n");
        }
        
        // Falls mehr als 20 Provider gelöscht wurden, zeige einen Hinweis
        if (deletedProvidersList.size() > 20) {
            int remaining = deletedProvidersList.size() - 20;
            message.append("\n... und ").append(remaining).append(" weitere");
        }
        
        // Info-Dialog mit gelöschten Providern anzeigen
        JOptionPane.showMessageDialog(
            mainTable,
            message.toString(),
            "Provider gelöscht",
            JOptionPane.INFORMATION_MESSAGE
        );
    }
    
    /**
     * Gibt die ausgewählten Provider-Namen zurück
     */
    public List<String> getSelectedProviders() {
        int[] selectedRows = mainTable.getSelectedRows();
        List<String> providers = new ArrayList<>();
        for (int row : selectedRows) {
            int modelRow = mainTable.convertRowIndexToModel(row);
            String provider = (String) model.getValueAt(modelRow, 1);
            providers.add(provider);
        }
        return providers;
    }

    /**
     * Gibt die ausgewählten Provider als Map zurück
     */
    public Map<String, ProviderStats> getSelectedProvidersMap() {
        List<String> selectedProviders = getSelectedProviders();
        Map<String, ProviderStats> selectedStats = new HashMap<>();
        
        Map<String, ProviderStats> allStats = dataManager.getStats();
        for (String provider : selectedProviders) {
            if (allStats.containsKey(provider)) {
                selectedStats.put(provider, allStats.get(provider));
            }
        }
        return selectedStats;
    }
    
    /**
     * Verwaltet die Favoriten-Kategorie für einen Provider
     */
    public void manageFavoriteCategory(String providerId, int currentCategory) {
        Object[] options = new Object[11];
        options[0] = "Kein Favorit";
        for (int i = 1; i <= 10; i++) {
            options[i] = "Kategorie " + i;
        }
        
        int selectedOption = JOptionPane.showOptionDialog(
            SwingUtilities.getWindowAncestor(mainTable),
            "Bitte wählen Sie die Favoriten-Kategorie für Provider " + providerId,
            "Favoriten-Kategorie wählen",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[currentCategory]
        );
        
        if (selectedOption >= 0) {
            // FavoritesManager-Singleton direkt holen und verwenden
            FavoritesManager favManager = FavoritesManager.getInstance(rootPath);
            
            // Alte Kategorie speichern für Logging
            int oldCategory = favManager.getFavoriteCategory(providerId);
            
            // Kategorie direkt setzen
            favManager.setFavoriteCategory(providerId, selectedOption);
            
            LOGGER.info("Favorit-Kategorie für " + providerId + " geändert von " + oldCategory + " auf " + selectedOption);
        }
    }

    /**
     * Extrahiert die Provider-ID aus dem Providernamen
     * @param providerName Name des Providers (normalerweise ein Dateipfad)
     * @return Die extrahierte Provider-ID
     */
    public String extractProviderId(String providerName) {
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
    
    /**
     * Hilfsmethode für Status-Text (falls benötigt)
     */
    private String getStatusText() {
        int totalProviders = dataManager.getStats().size();
        int visibleProviders = model.getRowCount();
        
        StringBuilder status = new StringBuilder()
            .append(String.format("%d/%d Signal Providers", visibleProviders, totalProviders));
            
        if (filterManager.getCurrentFilter() != null) {
            status.append(" (filtered)");
        }
        
        return status.toString();
    }
}