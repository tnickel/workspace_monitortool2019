package components;

import java.awt.FlowLayout;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class SearchPanel extends JPanel {
    private final JTextField searchField;
    private final JButton searchButton;
    private final MainTable mainTable;
    private final int[] currentSearchIndex = {-1};
    
    public SearchPanel(MainTable table) {
        this.mainTable = table;
        this.searchField = new JTextField(20);
        this.searchButton = new JButton("Suchen");
        
        initializeUI();
        setupSearchFunction();
    }
    
    private void initializeUI() {
        setLayout(new FlowLayout(FlowLayout.RIGHT));
        add(new JLabel("Suche: "));
        add(searchField);
        add(searchButton);
    }
    
    private void setupSearchFunction() {
        ActionListener searchAction = e -> performSearch();
        
        searchButton.addActionListener(searchAction);
        searchField.addActionListener(searchAction);
        
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updateHighlight(); }
            public void removeUpdate(DocumentEvent e) { updateHighlight(); }
            public void insertUpdate(DocumentEvent e) { updateHighlight(); }
        });
    }
    
    /**
     * KORRIGIERTE Suchmethode - Index wird nicht mehr zurückgesetzt!
     */
    private void performSearch() {
        System.out.println("=== SEARCHPANEL performSearch AUFGERUFEN ===");
        System.out.println("SearchPanel Index Array Referenz: " + System.identityHashCode(currentSearchIndex));
        System.out.println("SearchPanel Index VORHER: " + currentSearchIndex[0]);
        
        String searchText = searchField.getText().toLowerCase().trim();
        if (searchText.isEmpty()) {
            mainTable.clearHighlight();
            currentSearchIndex[0] = -1;
            System.out.println("SearchPanel: Suchtext leer, Index auf -1 gesetzt");
            return;
        }

        mainTable.highlightSearchText(searchText);
        boolean found = mainTable.findAndSelectNext(searchText, currentSearchIndex);
        
        System.out.println("SearchPanel Index NACH findAndSelectNext: " + currentSearchIndex[0]);
        System.out.println("SearchPanel found: " + found);
        
        if (!found) {
            System.out.println("SearchPanel: Kein Treffer - Index wird NICHT zurückgesetzt");
            JOptionPane.showMessageDialog(this, 
                "Keine Treffer für: " + searchText,
                "Suchergebnis", JOptionPane.INFORMATION_MESSAGE);
        }
        
        System.out.println("SearchPanel Index AM ENDE: " + currentSearchIndex[0]);
        System.out.println("=== SEARCHPANEL performSearch ENDE ===");
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
     * Setzt die Suche zurück (z.B. bei Filteränderungen)
     */
    public void resetSearch() {
        currentSearchIndex[0] = -1;
        mainTable.clearHighlight();
    }
    
    /**
     * Gibt das Suchfeld zurück für weitere Konfiguration
     */
    public JTextField getSearchField() {
        return searchField;
    }
    
    /**
     * Gibt den aktuellen Suchtext zurück
     */
    public String getSearchText() {
        return searchField.getText().trim();
    }
}