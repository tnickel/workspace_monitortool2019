package ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableColumn;

import components.MainTable;

/**
 * Dialog zum Konfigurieren der sichtbaren Spalten in der Haupttabelle
 */
public class TableColumnConfigDialog extends JDialog {
    private static final String PREF_PREFIX = "column_visible_";
    private final MainTable mainTable;
    private final List<JCheckBox> columnCheckboxes = new ArrayList<>();
    private final Preferences prefs = Preferences.userNodeForPackage(TableColumnConfigDialog.class);
    public static final String PREF_IDENTIFIER = "TableColumnVisibility";
    
    public TableColumnConfigDialog(JFrame parent, MainTable mainTable) {
        super(parent, "Tabellenspalten-Konfiguration", true);
        this.mainTable = mainTable;
        
        initializeUI();
        loadPreferences();
        
        setSize(new Dimension(400, 500));
        setLocationRelativeTo(parent);
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        
        // Panel für Checkboxen
        JPanel checkboxPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        checkboxPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Checkboxen für alle Spalten erstellen
        String[] columnNames = getColumnNames();
        for (int i = 0; i < columnNames.length; i++) {
            final int columnIndex = i;
            String columnName = columnNames[i];
            
            JCheckBox checkbox = new JCheckBox(columnName);
            checkbox.setSelected(true); // Standardmäßig alle ausgewählt
            
            // Spalten "No." und "Signal Provider" sollten immer sichtbar sein
            if (i <= 1) {
                checkbox.setEnabled(false);
                checkbox.setSelected(true);
                checkbox.setToolTipText("Diese Spalte kann nicht ausgeblendet werden.");
            }
            
            columnCheckboxes.add(checkbox);
            checkboxPanel.add(checkbox);
        }
        
        JScrollPane scrollPane = new JScrollPane(checkboxPanel);
        add(scrollPane, BorderLayout.CENTER);
        
        // Button-Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton selectAllButton = new JButton("Alle auswählen");
        selectAllButton.addActionListener(e -> {
            for (int i = 2; i < columnCheckboxes.size(); i++) { // Erste 2 Spalten immer sichtbar
                JCheckBox cb = columnCheckboxes.get(i);
                if (cb.isEnabled()) {
                    cb.setSelected(true);
                }
            }
        });
        
        JButton selectNoneButton = new JButton("Alle abwählen");
        selectNoneButton.addActionListener(e -> {
            for (int i = 2; i < columnCheckboxes.size(); i++) { // Erste 2 Spalten immer sichtbar
                JCheckBox cb = columnCheckboxes.get(i);
                if (cb.isEnabled()) {
                    cb.setSelected(false);
                }
            }
        });
        
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            applyChanges();
            savePreferences();
            dispose();
        });
        
        JButton cancelButton = new JButton("Abbrechen");
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(selectAllButton);
        buttonPanel.add(selectNoneButton);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Hinweis oben
        JLabel noteLabel = new JLabel("<html>Wählen Sie die Spalten, die angezeigt werden sollen.<br>"
                + "Die Spalten \"No.\" und \"Signal Provider\" können nicht ausgeblendet werden.</html>");
        noteLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(noteLabel, BorderLayout.NORTH);
    }
    
    private String[] getColumnNames() {
        int columnCount = mainTable.getColumnCount();
        String[] names = new String[columnCount];
        
        for (int i = 0; i < columnCount; i++) {
            names[i] = mainTable.getColumnName(i);
        }
        
        return names;
    }
    
    private void applyChanges() {
        // Spaltenbreiten basierend auf Checkbox-Status anpassen
        for (int i = 0; i < columnCheckboxes.size(); i++) {
            JCheckBox checkbox = columnCheckboxes.get(i);
            boolean visible = checkbox.isSelected();
            
            mainTable.setColumnVisible(i, visible);
        }
        
        // Tabelle nach Änderungen aktualisieren
        mainTable.updateUI();
    }
    
    private void savePreferences() {
        // Speichere Sichtbarkeit für jede Spalte
        for (int i = 0; i < columnCheckboxes.size(); i++) {
            JCheckBox checkbox = columnCheckboxes.get(i);
            boolean visible = checkbox.isSelected();
            prefs.putBoolean(PREF_PREFIX + i, visible);
        }
    }
    
    private void loadPreferences() {
        // Lade gespeicherte Sichtbarkeitseinstellungen
        for (int i = 0; i < columnCheckboxes.size(); i++) {
            JCheckBox checkbox = columnCheckboxes.get(i);
            
            // Wenn ein Wert gespeichert wurde, lade ihn
            if (prefs.get(PREF_PREFIX + i, null) != null) {
                boolean visible = prefs.getBoolean(PREF_PREFIX + i, true);
                checkbox.setSelected(visible);
            }
        }
    }
    
    /**
     * Öffnet den Dialog und wendet Änderungen an
     */
    public void showDialog() {
        setVisible(true);
    }
}