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
                    searchField.setText("");
                    mainTable.clearHighlight();
                    currentSearchIndex[0] = -1;
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
        if (searchField.getText().isEmpty()) {
            mainTable.clearHighlight();
            currentSearchIndex[0] = -1;
        } else {
            mainTable.highlightSearchText(searchField.getText().toLowerCase());
        }
    }
    
    public void performSearch() {
        String searchText = searchField.getText().toLowerCase().trim();
        if (searchText.isEmpty()) {
            mainTable.clearHighlight();
            currentSearchIndex[0] = -1;
            return;
        }
        
        mainTable.highlightSearchText(searchText);
        
        if (!mainTable.findAndSelectNext(searchText, currentSearchIndex)) {
            currentSearchIndex[0] = -1;
            if (!mainTable.findAndSelectNext(searchText, currentSearchIndex)) {
                JOptionPane.showMessageDialog(parentFrame,
                    "No matches found for: " + searchText,
                    "Search Result",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
    
    public void clearSearch() {
        searchField.setText("");
        mainTable.clearHighlight();
        currentSearchIndex[0] = -1;
    }
}