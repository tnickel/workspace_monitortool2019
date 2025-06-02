package ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import utils.SignalProviderFileReader;
import utils.UIStyle;

/**
 * Dialog zur Anzeige der drei wichtigen Signal Provider Dateien:
 * - conversionLog.txt
 * - mql4download.txt  
 * - mql5download.txt
 * 
 * Mit integrierter Suchfunktion, Navigation zwischen Treffern und
 * farblicher Hervorhebung der verschiedenen Dateisektionen.
 */
public class CheckSignalProviderDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(CheckSignalProviderDialog.class.getName());
    
    private final String downloadPath;
    private final SignalProviderFileReader fileReader;
    
    // UI Komponenten
    private JTextPane contentPane;
    private JTextField searchField;
    private JButton searchButton;
    private JLabel statusLabel;
    private JScrollPane scrollPane;
    
    // Navigation Buttons
    private JButton conversionLogButton;
    private JButton mql4DownloadButton;
    private JButton mql5DownloadButton;
    private JButton topButton;
    
    // Suchfunktionalität
    private String currentSearchText = "";
    private List<Integer> searchPositions = new ArrayList<>();
    private int currentSearchIndex = -1;
    private Highlighter.HighlightPainter highlightPainter;
    
    // Dateisektionen für Navigation
    private Map<String, Integer> fileSectionPositions = new HashMap<>();
    
    // Farben für verschiedene Dateisektionen
    private static final Color CONVERSION_LOG_COLOR = new Color(230, 245, 255); // Hellblau
    private static final Color MQL4_DOWNLOAD_COLOR = new Color(255, 245, 230);  // Hellorange
    private static final Color MQL5_DOWNLOAD_COLOR = new Color(245, 255, 230);  // Hellgrün
    private static final Color HEADER_COLOR = new Color(220, 220, 220);         // Hellgrau für Header
    
    public CheckSignalProviderDialog(JFrame parent, String downloadPath) {
        super(parent, "Check Signalprovider", true);
        this.downloadPath = downloadPath;
        this.fileReader = new SignalProviderFileReader(downloadPath);
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        loadFileContent();
        
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1000, 750);
        setLocationRelativeTo(parent);
    }
    
    private void initializeComponents() {
        // Content Pane (JTextPane für Styling)
        contentPane = new JTextPane();
        contentPane.setEditable(false);
        contentPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        contentPane.setBackground(Color.WHITE);
        contentPane.setForeground(Color.BLACK);
        
        // Scroll Pane
        scrollPane = new JScrollPane(contentPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Dateiinhalt"));
        
        // Such-Komponenten
        searchField = new JTextField(20);
        searchField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        
        searchButton = new JButton("Suchen");
        searchButton.setBackground(UIStyle.PRIMARY_COLOR);
        searchButton.setForeground(Color.WHITE);
        searchButton.setFocusPainted(false);
        
        // Navigation Buttons
        conversionLogButton = createNavigationButton("ConversionLog", CONVERSION_LOG_COLOR);
        mql4DownloadButton = createNavigationButton("MQL4 Download", MQL4_DOWNLOAD_COLOR);
        mql5DownloadButton = createNavigationButton("MQL5 Download", MQL5_DOWNLOAD_COLOR);
        topButton = createNavigationButton("↑ Anfang", Color.LIGHT_GRAY);
        
        // Status Label
        statusLabel = new JLabel("Bereit für Suche");
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        statusLabel.setForeground(Color.GRAY);
        
        // Highlight Painter für Suchergebnisse
        highlightPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);
    }
    
    private JButton createNavigationButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        button.setBackground(bgColor);
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setPreferredSize(new Dimension(110, 25));
        return button;
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Suchpanel (oben)
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Suche"));
        searchPanel.add(new JLabel("Suchtext:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        
        // Navigation Panel
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        navPanel.setBorder(BorderFactory.createTitledBorder("Navigation"));
        navPanel.add(topButton);
        navPanel.add(conversionLogButton);
        navPanel.add(mql4DownloadButton);
        navPanel.add(mql5DownloadButton);
        
        // Kombiniertes Top Panel
        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        topPanel.add(searchPanel);
        topPanel.add(navPanel);
        
        // Status Panel (unten)
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(statusLabel);
        
        // Layout zusammenfügen
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
    }
    
    private void setupEventHandlers() {
        // Search Button Action
        searchButton.addActionListener(e -> performSearch());
        
        // Enter-Taste im Suchfeld
        searchField.addActionListener(e -> performSearch());
        
        // Navigation Button Actions
        topButton.addActionListener(e -> jumpToTop());
        conversionLogButton.addActionListener(e -> jumpToSection("conversionLog.txt"));
        mql4DownloadButton.addActionListener(e -> jumpToSection("mql4download.txt"));
        mql5DownloadButton.addActionListener(e -> jumpToSection("mql5download.txt"));
        
        // Tastatur-Shortcuts
        setupKeyboardShortcuts();
    }
    
    private void setupKeyboardShortcuts() {
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    clearSearch();
                } else if (e.getKeyCode() == KeyEvent.VK_F3) {
                    performSearch();
                }
            }
        });
        
        contentPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F3) {
                    performSearch();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    clearSearch();
                } else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_F) {
                    searchField.requestFocus();
                    searchField.selectAll();
                } else if (e.getKeyCode() == KeyEvent.VK_HOME && e.isControlDown()) {
                    jumpToTop();
                }
            }
        });
    }
    
    private void loadFileContent() {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Lade Dateien...");
            
            // Prüfe erst ob Dateien existieren
            List<String> missingFiles = fileReader.getMissingFiles();
            if (!missingFiles.isEmpty()) {
                String message = "Folgende Dateien wurden nicht gefunden:\n" + 
                               String.join("\n", missingFiles) + 
                               "\n\nPfad: " + downloadPath;
                JOptionPane.showMessageDialog(this, message, "Dateien nicht gefunden", JOptionPane.WARNING_MESSAGE);
            }
            
            try {
                loadAndFormatContent();
                setupNavigationPositions();
                
                // Springe zum Anfang
                jumpToTop();
                
                // Aktualisiere Status
                statusLabel.setText("Dateien geladen - Navigation verfügbar");
                
            } catch (Exception e) {
                String errorMsg = "Fehler beim Laden der Dateien: " + e.getMessage();
                contentPane.setText(errorMsg);
                statusLabel.setText("Fehler beim Laden");
                logger.warning(errorMsg);
            }
        });
    }
    
    private void loadAndFormatContent() {
        StyledDocument doc = contentPane.getStyledDocument();
        
        try {
            // Style Definitionen
            SimpleAttributeSet headerStyle = new SimpleAttributeSet();
            StyleConstants.setBackground(headerStyle, HEADER_COLOR);
            StyleConstants.setBold(headerStyle, true);
            StyleConstants.setFontFamily(headerStyle, Font.MONOSPACED);
            StyleConstants.setFontSize(headerStyle, 12);
            
            SimpleAttributeSet conversionStyle = new SimpleAttributeSet();
            StyleConstants.setBackground(conversionStyle, CONVERSION_LOG_COLOR);
            StyleConstants.setFontFamily(conversionStyle, Font.MONOSPACED);
            StyleConstants.setFontSize(conversionStyle, 12);
            
            SimpleAttributeSet mql4Style = new SimpleAttributeSet();
            StyleConstants.setBackground(mql4Style, MQL4_DOWNLOAD_COLOR);
            StyleConstants.setFontFamily(mql4Style, Font.MONOSPACED);
            StyleConstants.setFontSize(mql4Style, 12);
            
            SimpleAttributeSet mql5Style = new SimpleAttributeSet();
            StyleConstants.setBackground(mql5Style, MQL5_DOWNLOAD_COLOR);
            StyleConstants.setFontFamily(mql5Style, Font.MONOSPACED);
            StyleConstants.setFontSize(mql5Style, 12);
            
            // Lade und formatiere jede Datei einzeln
            String[] fileNames = {"conversionLog.txt", "mql4download.txt", "mql5download.txt"};
            SimpleAttributeSet[] styles = {conversionStyle, mql4Style, mql5Style};
            
            for (int i = 0; i < fileNames.length; i++) {
                String fileName = fileNames[i];
                
                // Merke Position für Navigation
                fileSectionPositions.put(fileName, doc.getLength());
                
                // Header einfügen
                String header = "=".repeat(80) + "\n" +
                               "DATEI: " + fileName.toUpperCase() + "\n" +
                               "=".repeat(80) + "\n";
                doc.insertString(doc.getLength(), header, headerStyle);
                
                // Dateiinhalt laden und einfügen
                String content = loadSingleFileContent(fileName);
                doc.insertString(doc.getLength(), content, styles[i]);
                
                // Trennlinie zwischen Dateien
                if (i < fileNames.length - 1) {
                    doc.insertString(doc.getLength(), "\n\n", null);
                }
            }
            
        } catch (BadLocationException e) {
            logger.warning("Fehler beim Formatieren des Contents: " + e.getMessage());
        }
    }
    
    private String loadSingleFileContent(String fileName) {
        return fileReader.loadSingleFileRaw(fileName);
    }
    
    private void setupNavigationPositions() {
        // Navigation Buttons aktivieren/deaktivieren basierend auf verfügbaren Dateien
        conversionLogButton.setEnabled(fileSectionPositions.containsKey("conversionLog.txt"));
        mql4DownloadButton.setEnabled(fileSectionPositions.containsKey("mql4download.txt"));
        mql5DownloadButton.setEnabled(fileSectionPositions.containsKey("mql5download.txt"));
    }
    
    private void jumpToTop() {
        contentPane.setCaretPosition(0);
        contentPane.requestFocus();
        statusLabel.setText("Sprung zum Anfang");
    }
    
    private void jumpToSection(String fileName) {
        Integer position = fileSectionPositions.get(fileName);
        if (position != null) {
            contentPane.setCaretPosition(position);
            contentPane.requestFocus();
            statusLabel.setText("Sprung zu: " + fileName);
        } else {
            statusLabel.setText("Sektion nicht gefunden: " + fileName);
        }
    }
    
    private void performSearch() {
        String searchText = searchField.getText().trim();
        
        if (searchText.isEmpty()) {
            clearSearch();
            return;
        }
        
        // Neue Suche oder nächster Treffer?
        boolean isNewSearch = !searchText.equals(currentSearchText);
        
        if (isNewSearch) {
            startNewSearch(searchText);
        } else {
            findNextOccurrence();
        }
    }
    
    private void startNewSearch(String searchText) {
        currentSearchText = searchText;
        currentSearchIndex = -1;
        searchPositions.clear();
        
        // Entferne alte Highlights
        contentPane.getHighlighter().removeAllHighlights();
        
        // Arbeite direkt mit dem StyledDocument
        StyledDocument doc = contentPane.getStyledDocument();
        String content;
        
        try {
            // Hole den kompletten Text aus dem Document
            content = doc.getText(0, doc.getLength()).toLowerCase();
        } catch (BadLocationException e) {
            logger.warning("Fehler beim Abrufen des Document-Texts: " + e.getMessage());
            return;
        }
        
        String lowerSearchText = searchText.toLowerCase();
        int index = 0;
        
        while ((index = content.indexOf(lowerSearchText, index)) != -1) {
            searchPositions.add(index);
            
            // Highlighte das Vorkommen
            try {
                contentPane.getHighlighter().addHighlight(index, index + searchText.length(), highlightPainter);
            } catch (BadLocationException e) {
                logger.warning("Fehler beim Highlighten an Position " + index + ": " + e.getMessage());
            }
            
            index += searchText.length();
        }
        
        if (searchPositions.isEmpty()) {
            statusLabel.setText("Keine Treffer für: \"" + searchText + "\"");
            JOptionPane.showMessageDialog(this, 
                "Keine Treffer für: \"" + searchText + "\"", 
                "Suchergebnis", 
                JOptionPane.INFORMATION_MESSAGE);
        } else {
            // Springe zum ersten Treffer
            currentSearchIndex = 0;
            jumpToCurrentMatch();
        }
    }
    
    private void findNextOccurrence() {
        if (searchPositions.isEmpty()) {
            statusLabel.setText("Keine Treffer vorhanden");
            return;
        }
        
        // Zum nächsten Treffer springen
        currentSearchIndex++;
        if (currentSearchIndex >= searchPositions.size()) {
            currentSearchIndex = 0; // Wieder von vorne beginnen
        }
        
        jumpToCurrentMatch();
    }
    
    private void jumpToCurrentMatch() {
        if (searchPositions.isEmpty() || currentSearchIndex < 0 || currentSearchIndex >= searchPositions.size()) {
            return;
        }
        
        int position = searchPositions.get(currentSearchIndex);
        
        try {
            // Setze Cursor und scrolle zur Position
            contentPane.setCaretPosition(position);
            
            // Zusätzlich: Stelle sicher dass die Position sichtbar ist
            contentPane.getCaret().setVisible(true);
            contentPane.requestFocus();
            
            // Scroll zur Position
            SwingUtilities.invokeLater(() -> {
                try {
                    contentPane.scrollRectToVisible(contentPane.modelToView(position));
                } catch (BadLocationException e) {
                    logger.warning("Fehler beim Scrollen zur Position: " + e.getMessage());
                }
            });
            
            // Aktualisiere Status
            statusLabel.setText(String.format("Treffer %d von %d für: \"%s\"", 
                currentSearchIndex + 1, 
                searchPositions.size(), 
                currentSearchText));
            
        } catch (Exception e) {
            logger.warning("Fehler beim Springen zur Position: " + e.getMessage());
            statusLabel.setText("Fehler beim Navigieren");
        }
    }
    
    private void clearSearch() {
        searchField.setText("");
        contentPane.getHighlighter().removeAllHighlights();
        searchPositions.clear();
        currentSearchIndex = -1;
        currentSearchText = "";
        statusLabel.setText("Suche gelöscht");
    }
    
    /**
     * Setzt den Focus auf das Suchfeld (für externe Aufrufe)
     */
    public void focusSearchField() {
        searchField.requestFocus();
        searchField.selectAll();
    }
}