package components;



import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionListener;

public class SearchPanel extends JPanel {
    private final JTextField searchField;
    private final JButton searchButton;
    private final MainTable mainTable;
    private final int[] currentSearchIndex = {-1};
    
    public SearchPanel(MainTable table) {
        this.mainTable = table;
        this.searchField = new JTextField(20);
        this.searchButton = new JButton("Search");
        
        initializeUI();
        setupSearchFunction();
    }
    
    private void initializeUI() {
        setLayout(new FlowLayout(FlowLayout.RIGHT));
        add(new JLabel("Search: "));
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
    
    private void performSearch() {
        String searchText = searchField.getText().toLowerCase();
        if (searchText.isEmpty()) {
            mainTable.clearHighlight();
            return;
        }

        mainTable.highlightSearchText(searchText);
        boolean found = mainTable.findAndSelectNext(searchText, currentSearchIndex);
        
        if (!found) {
            currentSearchIndex[0] = -1;
            JOptionPane.showMessageDialog(this, 
                "Search reached end. Starting from beginning on next search.",
                "Search", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void updateHighlight() {
        if (searchField.getText().isEmpty()) {
            mainTable.clearHighlight();
        }
    }
}