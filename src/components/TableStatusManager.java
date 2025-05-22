package components;

import java.util.function.Consumer;
import java.util.logging.Logger;

import data.DataManager;
import models.HighlightTableModel;
import utils.ApplicationConstants;
import utils.MqlAnalyserConf;

/**
 * Klasse für das Status-Management der MainTable.
 * Behandelt Status-Updates, Callbacks und Status-Text-Generierung.
 */
public class TableStatusManager {
    private static final Logger LOGGER = Logger.getLogger(TableStatusManager.class.getName());
    
    private final MainTable mainTable;
    private final HighlightTableModel model;
    private final DataManager dataManager;
    private final TableFilterManager filterManager;
    private Consumer<String> statusUpdateCallback;
    
    public TableStatusManager(MainTable mainTable, HighlightTableModel model, 
                             DataManager dataManager, TableFilterManager filterManager) {
        this.mainTable = mainTable;
        this.model = model;
        this.dataManager = dataManager;
        this.filterManager = filterManager;
    }
    
    /**
     * Setzt den Callback für Status-Updates
     * 
     * @param callback Consumer der bei Status-Änderungen aufgerufen wird
     */
    public void setStatusUpdateCallback(Consumer<String> callback) {
        this.statusUpdateCallback = callback;
        LOGGER.info("Status-Update-Callback gesetzt");
    }
    
    /**
     * Erstellt den aktuellen Status-Text
     * 
     * @return Formatierter Status-String
     */
    public String getStatusText() {
        int totalProviders = dataManager.getStats().size();
        int visibleProviders = model.getRowCount();
        
        StringBuilder status = new StringBuilder()
            .append(String.format("%d/%d Signal Providers", visibleProviders, totalProviders));
            
        // Filter-Status hinzufügen
        if (filterManager.getCurrentFilter() != null) {
            status.append(" (filtered)");
        }
        
        // Download-Pfad hinzufügen
        try {
            MqlAnalyserConf config = new MqlAnalyserConf(ApplicationConstants.ROOT_PATH);
            status.append(" | Download Path: " + config.getDownloadPath());
        } catch (Exception e) {
            LOGGER.warning("Konnte Download-Pfad nicht ermitteln: " + e.getMessage());
            status.append(" | Download Path: [nicht verfügbar]");
        }

        return status.toString();
    }
    
    /**
     * Aktualisiert den Status und benachrichtigt Callbacks
     */
    public void updateStatus() {
        String statusText = getStatusText();
        
        // Callback aufrufen falls gesetzt
        if (statusUpdateCallback != null) {
            try {
                statusUpdateCallback.accept(statusText);
            } catch (Exception e) {
                LOGGER.warning("Fehler beim Ausführen des Status-Update-Callbacks: " + e.getMessage());
            }
        }
        
        // Tabelle neu zeichnen
        mainTable.repaint();
        
        LOGGER.fine("Status aktualisiert: " + statusText);
    }
    
    /**
     * Erzwingt ein Status-Update mit einem benutzerdefinierten Text
     * 
     * @param customText Benutzerdefinierter Status-Text
     */
    public void forceStatusUpdate(String customText) {
        if (statusUpdateCallback != null) {
            try {
                statusUpdateCallback.accept(customText);
            } catch (Exception e) {
                LOGGER.warning("Fehler beim Ausführen des erzwungenen Status-Updates: " + e.getMessage());
            }
        }
        
        LOGGER.info("Erzwungenes Status-Update: " + customText);
    }
    
    /**
     * Gibt detaillierte Status-Informationen zurück
     * 
     * @return Map mit detaillierten Status-Informationen
     */
    public StatusInfo getDetailedStatus() {
        int totalProviders = dataManager.getStats().size();
        int visibleProviders = model.getRowCount();
        boolean isFiltered = filterManager.getCurrentFilter() != null;
        
        String downloadPath = "[nicht verfügbar]";
        try {
            MqlAnalyserConf config = new MqlAnalyserConf(ApplicationConstants.ROOT_PATH);
            downloadPath = config.getDownloadPath();
        } catch (Exception e) {
            LOGGER.warning("Konnte Download-Pfad nicht ermitteln: " + e.getMessage());
        }
        
        return new StatusInfo(totalProviders, visibleProviders, isFiltered, downloadPath);
    }
    
    /**
     * Datenklasse für detaillierte Status-Informationen
     */
    public static class StatusInfo {
        private final int totalProviders;
        private final int visibleProviders;
        private final boolean isFiltered;
        private final String downloadPath;
        
        public StatusInfo(int totalProviders, int visibleProviders, boolean isFiltered, String downloadPath) {
            this.totalProviders = totalProviders;
            this.visibleProviders = visibleProviders;
            this.isFiltered = isFiltered;
            this.downloadPath = downloadPath;
        }
        
        public int getTotalProviders() {
            return totalProviders;
        }
        
        public int getVisibleProviders() {
            return visibleProviders;
        }
        
        public boolean isFiltered() {
            return isFiltered;
        }
        
        public String getDownloadPath() {
            return downloadPath;
        }
        
        @Override
        public String toString() {
            return String.format("StatusInfo{total=%d, visible=%d, filtered=%s, path='%s'}", 
                                totalProviders, visibleProviders, isFiltered, downloadPath);
        }
    }
    
    /**
     * Loggt den aktuellen Status für Debugging-Zwecke
     */
    public void logCurrentStatus() {
        StatusInfo status = getDetailedStatus();
        LOGGER.info("Aktueller Tabellen-Status: " + status.toString());
    }
    
    /**
     * Prüft ob ein Status-Update-Callback gesetzt ist
     * 
     * @return true wenn Callback verfügbar ist
     */
    public boolean hasStatusCallback() {
        return statusUpdateCallback != null;
    }
    
    /**
     * Entfernt den Status-Update-Callback
     */
    public void clearStatusCallback() {
        this.statusUpdateCallback = null;
        LOGGER.info("Status-Update-Callback entfernt");
    }
}