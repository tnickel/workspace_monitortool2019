package components;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import data.DataManager;
import data.ProviderStats;
import models.HighlightTableModel;
import ui.PerformanceAnalysisDialog;
import utils.HtmlDatabase;

/**
 * Klasse für das Event-Handling der MainTable.
 * Behandelt Mouse-Events, Keyboard-Events und deren Aktionen.
 */
public class TableEventHandler {
    private static final Logger LOGGER = Logger.getLogger(TableEventHandler.class.getName());
    
    private final MainTable mainTable;
    private final HighlightTableModel model;
    private final DataManager dataManager;
    private final HtmlDatabase htmlDatabase;
    private final String rootPath;
    private final TableProviderManager providerManager;
    
    // EIGENE Such-State-Verwaltung
    private int currentSearchIndex = -1;
    private String lastSearchText = "";
    
    public TableEventHandler(MainTable mainTable, HighlightTableModel model, DataManager dataManager, 
                           HtmlDatabase htmlDatabase, String rootPath, TableProviderManager providerManager) {
        this.mainTable = mainTable;
        this.model = model;
        this.dataManager = dataManager;
        this.htmlDatabase = htmlDatabase;
        this.rootPath = rootPath;
        this.providerManager = providerManager;
    }
    
    /**
     * Richtet Mouse-Listener für die Tabelle ein
     */
    public void setupMouseListener() {
        mainTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    handleDoubleClick();
                }
            }
        });
    }
    
    /**
     * Behandelt Doppelklick-Events auf der Tabelle
     */
    private void handleDoubleClick() {
        int row = mainTable.getSelectedRow();
        if (row != -1) {
            row = mainTable.convertRowIndexToModel(row);
            String providerName = (String) model.getValueAt(row, 1);
            ProviderStats stats = dataManager.getStats().get(providerName);
            String providerId = providerName.substring(providerName.lastIndexOf("_") + 1).replace(".csv", "");
            
            if (stats != null) {
                PerformanceAnalysisDialog detailFrame = new PerformanceAnalysisDialog(
                    providerName, stats, providerId, htmlDatabase, rootPath);
                detailFrame.setVisible(true);
            }
        }
    }
    
    /**
     * Setzt Key-Bindings für Tastaturaktionen auf der Tabelle
     */
    public void setupKeyBindings() {
        // Key-Binding für die Delete-Taste
        mainTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
            "deleteSelectedProviders"
        );
        
        mainTable.getActionMap().put("deleteSelectedProviders", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleDeleteAction();
            }
        });
    }
    
    /**
     * Behandelt die Delete-Aktion
     */
    private void handleDeleteAction() {
        providerManager.deleteSelectedProviders();
    }
    
    /**
     * NEUE Suchlogik mit eigenem Index-Management
     * Diese Methode behält für Kompatibilität die alte Signatur, ignoriert aber das Array
     * 
     * @param searchText Der zu suchende Text
     * @param externalIndex Wird ignoriert - wir verwenden unseren eigenen Index
     * @return true wenn gefunden, false wenn kein Treffer in der ganzen Tabelle existiert
     */
    public boolean findAndSelectNext(String searchText, int[] externalIndex) {
        // Prüfe ob sich der Suchtext geändert hat - dann Reset
        if (!searchText.equals(lastSearchText)) {
            currentSearchIndex = -1;
            lastSearchText = searchText;
            LOGGER.info("Neuer Suchtext erkannt, Index zurückgesetzt: '" + searchText + "'");
        }
        
        return findAndSelectNextInternal(searchText);
    }
    
    /**
     * Interne Suchlogik mit eigenem Index-Management
     */
    private boolean findAndSelectNextInternal(String searchText) {
        int totalRows = mainTable.getRowCount();
        
        LOGGER.info("=== SUCHE mit eigenem Index ===");
        LOGGER.info("Eigener Index VORHER: " + currentSearchIndex);
        LOGGER.info("Suchtext: '" + searchText + "'");
        LOGGER.info("Gesamte Zeilen: " + totalRows);
        
        if (totalRows == 0) {
            return false;
        }
        
        int startRow = currentSearchIndex + 1;
        
        // Erste Suche: Von der aktuellen Position bis zum Ende der Tabelle
        LOGGER.info("Phase 1: Suche von Zeile " + startRow + " bis " + (totalRows-1));
        for (int row = startRow; row < totalRows; row++) {
            if (searchInRow(row, searchText)) {
                currentSearchIndex = row;
                selectAndScrollToRow(row);
                LOGGER.info("*** TREFFER GEFUNDEN in Phase 1, Zeile: " + row + " ***");
                LOGGER.info("Eigener Index NACHHER: " + currentSearchIndex);
                return true;
            }
        }
        
        // Zweite Suche (Wraparound): Vom Anfang der Tabelle bis zur ursprünglichen Position
        int endRow = Math.min(startRow, totalRows);
        LOGGER.info("Phase 2: Wraparound-Suche von Zeile 0 bis " + (endRow-1));
        for (int row = 0; row < endRow; row++) {
            if (searchInRow(row, searchText)) {
                currentSearchIndex = row;
                selectAndScrollToRow(row);
                LOGGER.info("*** TREFFER GEFUNDEN in Phase 2 (Wraparound), Zeile: " + row + " ***");
                LOGGER.info("Eigener Index NACHHER: " + currentSearchIndex);
                return true;
            }
        }
        
        LOGGER.info("*** KEIN TREFFER GEFUNDEN ***");
        return false;
    }
    
    /**
     * Hilfsmethode: Sucht in einer bestimmten Zeile nach dem Suchtext
     * 
     * @param row Die zu durchsuchende Zeile
     * @param searchText Der zu suchende Text
     * @return true wenn gefunden, false sonst
     */
    private boolean searchInRow(int row, String searchText) {
        for (int col = 0; col < mainTable.getColumnCount(); col++) {
            Object value = mainTable.getValueAt(row, col);
            if (value != null && value.toString().toLowerCase().contains(searchText)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Hilfsmethode: Selektiert eine Zeile und scrollt zu ihr
     * 
     * @param row Die zu selektierende Zeile
     */
    private void selectAndScrollToRow(int row) {
        mainTable.scrollRectToVisible(mainTable.getCellRect(row, 0, true));
        mainTable.setRowSelectionInterval(row, row);
        LOGGER.info("Suchergebnis gefunden und selektiert in Zeile: " + row);
    }
    
    /**
     * Setzt den Such-State zurück (z.B. bei Tabellen-Updates)
     */
    public void resetSearchState() {
        currentSearchIndex = -1;
        lastSearchText = "";
        LOGGER.info("Such-State zurückgesetzt");
    }
    
    /**
     * Gibt den aktuellen Suchindex zurück (für Debugging)
     */
    public int getCurrentSearchIndex() {
        return currentSearchIndex;
    }
}