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
     * Sucht und selektiert das nächste Element mit dem Suchtext
     * 
     * @param searchText Der zu suchende Text
     * @param currentIndex Array mit dem aktuellen Index [0] = row
     * @return true wenn gefunden, false sonst
     */
    public boolean findAndSelectNext(String searchText, int[] currentIndex) {
        int startRow = currentIndex[0] + 1;
        
        for (int row = startRow; row < mainTable.getRowCount(); row++) {
            for (int col = 0; col < mainTable.getColumnCount(); col++) {
                Object value = mainTable.getValueAt(row, col);
                if (value != null && value.toString().toLowerCase().contains(searchText)) {
                    currentIndex[0] = row;
                    mainTable.scrollRectToVisible(mainTable.getCellRect(row, 0, true));
                    mainTable.setRowSelectionInterval(row, row);
                    return true;
                }
            }
        }
        return false;
    }
}