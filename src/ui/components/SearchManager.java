package ui.components;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JOptionPane;
import javax.swing.JTextField;

import components.MainTable;
import ui.MainFrame;

public class SearchManager {
    private final MainFrame parentFrame;
    private final MainTable mainTable;
    private final JTextField searchField;
    private final int[] currentSearchIndex;
    
    public SearchManager(MainFrame parentFrame, MainTable mainTable, 
                       JTextField searchField, int[] searchIndex) {
        this.parentFrame = parentFrame;
        this.mainTable = mainTable;
        this.searchField = searchField;
        this.currentSearchIndex = searchIndex;
        
        setupSearchField();
    }
    
    private void setupSearchField() {
        searchField.addActionListener(e -> performSearch());
        
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    clearSearch();
                }
                // F3 für "Find Next"
                else if (e.getKeyCode() == KeyEvent.VK_F3) {
                    performSearch();
                }
            }
        });
        
        // Dokument-Listener für Live-Highlighting
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateHighlight(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateHighlight(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateHighlight(); }
        });
    }
    
    private void updateHighlight() {
        String searchText = searchField.getText().trim();
        if (searchText.isEmpty()) {
            mainTable.clearHighlight();
            currentSearchIndex[0] = -1;
        } else {
            mainTable.highlightSearchText(searchText.toLowerCase());
        }
    }
    
    /**
     * KORRIGIERTE Suchmethode - Index wird nicht mehr zurückgesetzt!
     */
    public void performSearch() {
        System.out.println("=== SEARCHMANAGER performSearch AUFGERUFEN ===");
        System.out.println("SearchManager Index Array Referenz: " + System.identityHashCode(currentSearchIndex));
        System.out.println("SearchManager Index VORHER: " + currentSearchIndex[0]);
        
        String searchText = searchField.getText().toLowerCase().trim();
        if (searchText.isEmpty()) {
            mainTable.clearHighlight();
            currentSearchIndex[0] = -1;
            System.out.println("SearchManager: Suchtext leer, Index auf -1 gesetzt");
            return;
        }
        
        mainTable.highlightSearchText(searchText);
        
        boolean found = mainTable.findAndSelectNext(searchText, currentSearchIndex);
        
        System.out.println("SearchManager Index NACH findAndSelectNext: " + currentSearchIndex[0]);
        System.out.println("SearchManager found: " + found);
        
        if (!found) {
            System.out.println("SearchManager: Kein Treffer - Index wird NICHT zurückgesetzt");
            JOptionPane.showMessageDialog(parentFrame,
                "Keine Treffer für: \"" + searchField.getText().trim() + "\"",
                "Suchergebnis",
                JOptionPane.INFORMATION_MESSAGE);
        }
        
        System.out.println("SearchManager Index AM ENDE: " + currentSearchIndex[0]);
        System.out.println("=== SEARCHMANAGER performSearch ENDE ===");
    }
    
    public void clearSearch() {
        searchField.setText("");
        mainTable.clearHighlight();
        currentSearchIndex[0] = -1;
    }
    
    /**
     * Setzt die Suche zurück (z.B. bei Filteränderungen)
     */
    public void resetSearch() {
        currentSearchIndex[0] = -1;
        // Highlight bleibt bestehen, nur der Index wird zurückgesetzt
    }
    
    /**
     * Gibt den aktuellen Suchtext zurück
     */
    public String getCurrentSearchText() {
        return searchField.getText().trim();
    }
    
    /**
     * Gibt zurück, ob gerade eine Suche aktiv ist
     */
    public boolean isSearchActive() {
        return !searchField.getText().trim().isEmpty();
    }
}