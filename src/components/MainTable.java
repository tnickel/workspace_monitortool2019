package components;



import models.HighlightTableModel;
import renderers.HighlightRenderer;
import data.DataManager;
import data.ProviderStats;
import ui.DetailFrame;
import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MainTable extends JTable {
    private final HighlightTableModel model;
    private final HighlightRenderer renderer;
    private final DataManager dataManager;
    
    public MainTable(DataManager dataManager) {
        this.dataManager = dataManager;
        this.model = new HighlightTableModel();
        this.renderer = new HighlightRenderer();
        initializeTable();
        setupMouseListener();
    }
    
    private void initializeTable() {
        setModel(model);
        setRowSorter(new TableRowSorter<>(model));
        
        // Set renderer for all columns
        for (int i = 0; i < getColumnCount(); i++) {
            getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
        
        // Debug output
        System.out.println("Loading data into table...");
        System.out.println("Number of providers: " + dataManager.getStats().size());
        
        // Populate data
        model.populateData(dataManager.getStats());
        
        // More debug
        System.out.println("Table rows after population: " + model.getRowCount());
    }
    
    private void setupMouseListener() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = getSelectedRow();
                    if (row != -1) {
                        row = convertRowIndexToModel(row);
                        String providerName = (String) model.getValueAt(row, 1);
                        new DetailFrame(providerName, 
                            dataManager.getStats().get(providerName)).setVisible(true);
                    }
                }
            }
        });
    }
    
    public void highlightSearchText(String text) {
        renderer.setSearchText(text);
        repaint();
    }
    
    public void clearHighlight() {
        renderer.setSearchText("");
        repaint();
    }
    
    public boolean findAndSelectNext(String searchText, int[] currentIndex) {
        int startRow = currentIndex[0] + 1;
        
        for (int row = startRow; row < getRowCount(); row++) {
            for (int col = 0; col < getColumnCount(); col++) {
                Object value = getValueAt(row, col);
                if (value != null && value.toString().toLowerCase().contains(searchText)) {
                    currentIndex[0] = row;
                    scrollRectToVisible(getCellRect(row, 0, true));
                    setRowSelectionInterval(row, row);
                    return true;
                }
            }
        }
        return false;
    }
}