package ui.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import components.MainTable;
import utils.UIStyle;

/**
 * Eine UI-Komponente, die die Auswahl einer Favoriten-Kategorie ermöglicht
 */
public class FavoritesCategorySelector extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(FavoritesCategorySelector.class.getName());
    
    private final MainTable mainTable;
    private final JComboBox<String> categoryComboBox;
    
    public FavoritesCategorySelector(MainTable mainTable) {
        this.mainTable = mainTable;
        
        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        setBorder(new EmptyBorder(0, 5, 0, 5));
        setOpaque(false);
        
        // Label erstellen
        JLabel label = UIStyle.createStyledLabel("Favoriten-Kategorie:");
        add(label);
        
        // Dropdown erstellen
        categoryComboBox = new JComboBox<>(createCategoryOptions());
        categoryComboBox.setPreferredSize(new Dimension(150, 25));
        categoryComboBox.setSelectedIndex(0); // "Alle anzeigen" ist standardmäßig ausgewählt
        
        // Stil für die ComboBox
        UIStyle.applyStylesToComboBox(categoryComboBox);
        
        // Hinzufügen des ActionListener
        categoryComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = categoryComboBox.getSelectedIndex();
                LOGGER.info("Favoriten-Kategorie ausgewählt: " + selectedIndex);
                mainTable.filterByFavoriteCategory(selectedIndex);
            }
        });
        
        add(categoryComboBox);
    }
    
    /**
     * Erstellt die Optionen für die Kategorie-Auswahl
     * @return Array mit Kategorienamen
     */
    private String[] createCategoryOptions() {
        String[] options = new String[11]; // 0-10
        options[0] = "Alle anzeigen";
        for (int i = 1; i <= 10; i++) {
            options[i] = "Favoriten " + i;
        }
        return options;
    }
    
    /**
     * Aktualisiert die ausgewählte Kategorie
     * @param category Die neue Kategorie (0-10)
     */
    public void setSelectedCategory(int category) {
        if (category >= 0 && category <= 10) {
            categoryComboBox.setSelectedIndex(category);
        }
    }
    
    /**
     * Gibt die aktuell ausgewählte Kategorie zurück
     * @return Die ausgewählte Kategorie (0-10)
     */
    public int getSelectedCategory() {
        return categoryComboBox.getSelectedIndex();
    }
}