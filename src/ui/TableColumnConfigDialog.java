package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import components.MainTable;

/**
 * Dialog zum Konfigurieren der sichtbaren Spalten in der Haupttabelle
 */
public class TableColumnConfigDialog extends JDialog {
    private static final String CONFIG_FILENAME = "column_config.properties";
    private static final String PREF_PREFIX = "column_visible_";
    private final MainTable mainTable;
    private final List<JCheckBox> columnCheckboxes = new ArrayList<>();
    private final Properties properties = new Properties();
    private final File configFile;
    
    public TableColumnConfigDialog(JFrame parent, MainTable mainTable) {
        super(parent, "Tabellenspalten-Konfiguration", true);
        this.mainTable = mainTable;
        
        // Konfigurationsdatei im Verzeichnis der Anwendung
        configFile = new File(CONFIG_FILENAME);
        
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
            checkbox.setSelected(mainTable.isColumnVisible(i)); // Aktuellen Zustand der Spalte verwenden
            
            // Spalten "No." und "Signal Provider" sollten immer sichtbar sein
            if (i <= 1) {
                checkbox.setEnabled(false);
                checkbox.setSelected(true);
                checkbox.setToolTipText("Diese Spalte kann nicht ausgeblendet werden.");
            }
            
            // MaxDrawdown-Spalte soll immer deaktiviert und unsichtbar sein
            if (columnName.equals("Max Drawdown %")) {
                checkbox.setEnabled(false);
                checkbox.setSelected(false);
                checkbox.setToolTipText("Diese Spalte enthält fehlerhafte Berechnungen und kann nicht angezeigt werden.");
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
            for (int i = 0; i < columnCheckboxes.size(); i++) { // Erste 2 Spalten immer sichtbar
                JCheckBox cb = columnCheckboxes.get(i);
                // MaxDrawdown-Spalte bleibt immer ausgeblendet
                String colName = getColumnNames()[i];
                if (cb.isEnabled() && !colName.equals("Max Drawdown %")) {
                    cb.setSelected(true);
                }
            }
        });
        
        JButton selectNoneButton = new JButton("Alle abwählen");
        selectNoneButton.addActionListener(e -> {
            for (int i = 0; i < columnCheckboxes.size(); i++) { // Erste 2 Spalten immer sichtbar
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
            
            // MaxDrawdown-Spalte immer ausblenden, unabhängig von der Checkbox
            String columnName = getColumnNames()[i];
            if (columnName.equals("Max Drawdown %")) {
                visible = false;
            }
            
            mainTable.setColumnVisible(i, visible);
        }
        
        // Tabelle nach Änderungen aktualisieren
        mainTable.updateUI();
    }
    
    private void savePreferences() {
        // Speichere Sichtbarkeit für jede Spalte in den Properties
        for (int i = 0; i < columnCheckboxes.size(); i++) {
            JCheckBox checkbox = columnCheckboxes.get(i);
            boolean visible = checkbox.isSelected();
            
            // MaxDrawdown-Spalte immer als unsichtbar speichern
            String columnName = getColumnNames()[i];
            if (columnName.equals("Max Drawdown %")) {
                visible = false;
            }
            
            properties.setProperty(PREF_PREFIX + i, String.valueOf(visible));
        }
        
        // Speichere Properties in der Datei
        try (FileOutputStream out = new FileOutputStream(configFile)) {
            properties.store(out, "Tabellen-Spalten Konfiguration");
            System.out.println("Spalteneinstellungen wurden gespeichert in: " + configFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Fehler beim Speichern der Spalteneinstellungen: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadPreferences() {
        // Lade Properties aus der Datei, falls vorhanden
        if (configFile.exists()) {
            try (FileInputStream in = new FileInputStream(configFile)) {
                properties.load(in);
                
                // Aktualisiere Checkboxen basierend auf geladenen Properties
                for (int i = 0; i < columnCheckboxes.size(); i++) {
                    JCheckBox checkbox = columnCheckboxes.get(i);
                    String columnName = getColumnNames()[i];
                    
                    if (i <= 1) {
                        // Erste 2 Spalten immer sichtbar
                        checkbox.setSelected(true);
                    } else if (columnName.equals("Max Drawdown %")) {
                        // MaxDrawdown-Spalte immer ausblenden
                        checkbox.setSelected(false);
                    } else {
                        String key = PREF_PREFIX + i;
                        boolean visible = Boolean.parseBoolean(properties.getProperty(key, "true"));
                        checkbox.setSelected(visible);
                    }
                }
                
                System.out.println("Spalteneinstellungen wurden geladen aus: " + configFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Fehler beim Laden der Spalteneinstellungen: " + e.getMessage());
                e.printStackTrace();
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