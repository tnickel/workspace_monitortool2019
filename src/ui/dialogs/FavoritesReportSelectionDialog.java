package ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import ui.components.AppUIStyle;

/**
 * Dialog zur Auswahl der Favoriten-Kategorien für die Report-Generierung
 */
public class FavoritesReportSelectionDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    
    // Auswahloptionen
    public static final String OPTION_ALL = "All";
    public static final String OPTION_1_2 = "1-2";
    public static final String OPTION_1_3 = "1-3";
    
    // Rückgabewerte
    private String selectedOption = null;
    private boolean cancelled = false;
    
    // UI-Komponenten
    private JRadioButton allRadio;
    private JRadioButton cat12Radio;
    private JRadioButton cat13Radio;
    private JButton okButton;
    private JButton cancelButton;
    
    /**
     * Konstruktor für den Dialog
     * 
     * @param parent Das übergeordnete Fenster
     */
    public FavoritesReportSelectionDialog(Window parent) {
        super(parent, "Favoriten-Report Auswahl", ModalityType.APPLICATION_MODAL);
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        setupDialog();
    }
    
    /**
     * Initialisiert die UI-Komponenten
     */
    private void initializeComponents() {
        // Radio Buttons erstellen
        allRadio = new JRadioButton("Alle Favoriten (1-10)");
        cat12Radio = new JRadioButton("Favoriten Kategorie 1-2");
        cat13Radio = new JRadioButton("Favoriten Kategorie 1-3");
        
        // Styling für Radio Buttons
        allRadio.setFont(AppUIStyle.REGULAR_FONT);
        cat12Radio.setFont(AppUIStyle.REGULAR_FONT);
        cat13Radio.setFont(AppUIStyle.REGULAR_FONT);
        
        allRadio.setBackground(AppUIStyle.TEXT_FIELD_BG_COLOR);
        cat12Radio.setBackground(AppUIStyle.TEXT_FIELD_BG_COLOR);
        cat13Radio.setBackground(AppUIStyle.TEXT_FIELD_BG_COLOR);
        
        allRadio.setForeground(AppUIStyle.TEXT_COLOR);
        cat12Radio.setForeground(AppUIStyle.TEXT_COLOR);
        cat13Radio.setForeground(AppUIStyle.TEXT_COLOR);
        
        // Default: 1-2 auswählen
        cat12Radio.setSelected(true);
        
        // ButtonGroup für Radio Buttons
        ButtonGroup group = new ButtonGroup();
        group.add(allRadio);
        group.add(cat12Radio);
        group.add(cat13Radio);
        
        // Buttons erstellen
        okButton = AppUIStyle.createStyledButton("OK");
        cancelButton = AppUIStyle.createStyledButton("Abbrechen");
        
        // Button-Größen setzen
        Dimension buttonSize = new Dimension(100, 30);
        okButton.setPreferredSize(buttonSize);
        cancelButton.setPreferredSize(buttonSize);
    }
    
    /**
     * Richtet das Layout ein
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Haupt-Panel erstellen
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(AppUIStyle.TEXT_FIELD_BG_COLOR);
        
        // Titel-Label
        JLabel titleLabel = AppUIStyle.createStyledLabel("Wählen Sie die Favoriten-Kategorien für den Report:");
        titleLabel.setFont(AppUIStyle.BOLD_FONT);
        titleLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Abstand hinzufügen
        mainPanel.add(titleLabel);
        mainPanel.add(javax.swing.Box.createVerticalStrut(20));
        
        // Radio Button Panel
        JPanel radioPanel = new JPanel();
        radioPanel.setLayout(new BoxLayout(radioPanel, BoxLayout.Y_AXIS));
        radioPanel.setBackground(AppUIStyle.TEXT_FIELD_BG_COLOR);
        radioPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(AppUIStyle.SECONDARY_COLOR),
            "Auswahl",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            AppUIStyle.REGULAR_FONT,
            AppUIStyle.TEXT_COLOR
        ));
        
        // Radio Buttons hinzufügen mit Abstand
        radioPanel.add(allRadio);
        radioPanel.add(javax.swing.Box.createVerticalStrut(10));
        radioPanel.add(cat12Radio);
        radioPanel.add(javax.swing.Box.createVerticalStrut(10));
        radioPanel.add(cat13Radio);
        
        mainPanel.add(radioPanel);
        mainPanel.add(javax.swing.Box.createVerticalStrut(20));
        
        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setBackground(AppUIStyle.TEXT_FIELD_BG_COLOR);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        mainPanel.add(buttonPanel);
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    /**
     * Richtet die Event-Handler ein
     */
    private void setupEventHandlers() {
        // OK Button
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleOk();
            }
        });
        
        // Cancel Button
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleCancel();
            }
        });
        
        // ESC-Taste für Abbrechen
        getRootPane().registerKeyboardAction(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    handleCancel();
                }
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        
        // ENTER-Taste für OK
        getRootPane().registerKeyboardAction(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    handleOk();
                }
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        
        // Window-Closing Event
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleCancel();
            }
        });
        
        // Default Button setzen
        getRootPane().setDefaultButton(okButton);
    }
    
    /**
     * Konfiguriert die Dialog-Einstellungen
     */
    private void setupDialog() {
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        pack();
        
        // Dialog zentrieren
        setLocationRelativeTo(getParent());
        
        // Hintergrundfarbe setzen
        getContentPane().setBackground(AppUIStyle.TEXT_FIELD_BG_COLOR);
    }
    
    /**
     * Behandelt den OK-Button-Klick
     */
    private void handleOk() {
        // Bestimme die ausgewählte Option
        if (allRadio.isSelected()) {
            selectedOption = OPTION_ALL;
        } else if (cat12Radio.isSelected()) {
            selectedOption = OPTION_1_2;
        } else if (cat13Radio.isSelected()) {
            selectedOption = OPTION_1_3;
        }
        
        cancelled = false;
        dispose();
    }
    
    /**
     * Behandelt den Cancel-Button-Klick
     */
    private void handleCancel() {
        selectedOption = null;
        cancelled = true;
        dispose();
    }
    
    /**
     * Zeigt den Dialog an und gibt die ausgewählte Option zurück
     * 
     * @return Die ausgewählte Option (OPTION_ALL, OPTION_1_2, oder OPTION_1_3) oder null wenn abgebrochen
     */
    public String showDialog() {
        setVisible(true);
        return cancelled ? null : selectedOption;
    }
    
    /**
     * Gibt zurück, ob der Dialog abgebrochen wurde
     * 
     * @return true wenn abgebrochen, false wenn OK geklickt wurde
     */
    public boolean wasCancelled() {
        return cancelled;
    }
    
    /**
     * Gibt die ausgewählte Option zurück
     * 
     * @return Die ausgewählte Option oder null wenn abgebrochen
     */
    public String getSelectedOption() {
        return selectedOption;
    }
}