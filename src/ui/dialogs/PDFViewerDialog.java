package ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;
import java.awt.FlowLayout;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import utils.UIStyle;

/**
 * Dialog zur Anzeige von PDF-Dokumenten mit Apache PDFBox
 */
public class PDFViewerDialog extends JFrame {
    private final File pdfFile;
    private PDDocument document;
    private PDFRenderer pdfRenderer;
    private int currentPage = 0;
    private int totalPages = 0;
    
    // UI-Komponenten
    private JLabel imageLabel;
    private JButton prevButton;
    private JButton nextButton;
    private JLabel pageLabel;
    private JScrollPane scrollPane;
    
    private static final float ZOOM_FACTOR = 1.5f; // 150% Zoom für bessere Lesbarkeit
    
    /**
     * Konstruktor für den PDF-Viewer-Dialog
     * @param pdfFile Die anzuzeigende PDF-Datei
     * @param title Der Titel des Fensters
     */
    public PDFViewerDialog(File pdfFile, String title) {
        super("PDF Viewer: " + title);
        this.pdfFile = pdfFile;
        
        initializeComponents();
        setupKeyBindings();
        loadPDFDocument();
        
        // Fenstergröße auf 70% der Bildschirmgröße setzen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int) (screenSize.width * 0.7);
        int height = (int) (screenSize.height * 0.8);
        setSize(width, height);
        
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
    
    /**
     * Initialisiert die UI-Komponenten
     */
    private void initializeComponents() {
        setLayout(new BorderLayout());
        
        // Haupt-Panel für PDF-Anzeige
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(UIStyle.BG_COLOR);
        
        // Image-Label für PDF-Seiten
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        
        // Scroll-Pane für große PDFs
        scrollPane = new JScrollPane(imageLabel);
        scrollPane.setBackground(UIStyle.BG_COLOR);
        scrollPane.getViewport().setBackground(UIStyle.BG_COLOR);
        
        // Navigations-Panel
        JPanel navigationPanel = createNavigationPanel();
        
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(navigationPanel, BorderLayout.SOUTH);
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    /**
     * Erstellt das Navigations-Panel mit Seitensteuerung
     */
    private JPanel createNavigationPanel() {
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        navPanel.setBackground(UIStyle.BG_COLOR);
        navPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        // Vorherige Seite Button
        prevButton = UIStyle.createStyledButton("◀ Vorherige");
        prevButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentPage > 0) {
                    currentPage--;
                    displayPage(currentPage);
                    updateNavigationButtons();
                }
            }
        });
        
        // Seiten-Info Label
        pageLabel = new JLabel("Seite 0 von 0");
        pageLabel.setFont(UIStyle.REGULAR_FONT);
        pageLabel.setForeground(UIStyle.TEXT_COLOR);
        pageLabel.setBorder(new EmptyBorder(0, 15, 0, 15));
        
        // Nächste Seite Button
        nextButton = UIStyle.createStyledButton("Nächste ▶");
        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentPage < totalPages - 1) {
                    currentPage++;
                    displayPage(currentPage);
                    updateNavigationButtons();
                }
            }
        });
        
        navPanel.add(prevButton);
        navPanel.add(pageLabel);
        navPanel.add(nextButton);
        
        return navPanel;
    }
    
    /**
     * Lädt das PDF-Dokument
     */
    private void loadPDFDocument() {
        try {
            document = PDDocument.load(pdfFile);
            pdfRenderer = new PDFRenderer(document);
            totalPages = document.getNumberOfPages();
            
            if (totalPages > 0) {
                displayPage(0);
                updateNavigationButtons();
            } else {
                showError("Das PDF-Dokument enthält keine Seiten.");
            }
            
        } catch (IOException e) {
            showError("Fehler beim Laden des PDF-Dokuments: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Zeigt eine bestimmte PDF-Seite an
     * @param pageIndex Der Index der anzuzeigenden Seite (0-basiert)
     */
    private void displayPage(int pageIndex) {
        try {
            if (pdfRenderer != null && pageIndex >= 0 && pageIndex < totalPages) {
                // PDF-Seite als Bild rendern
                BufferedImage image = pdfRenderer.renderImageWithDPI(pageIndex, 96 * ZOOM_FACTOR);
                
                // Bild im Label anzeigen
                imageLabel.setIcon(new ImageIcon(image));
                
                // Seiten-Info aktualisieren
                pageLabel.setText(String.format("Seite %d von %d", pageIndex + 1, totalPages));
                
                // Scroll-Position zurücksetzen
                scrollPane.getViewport().setViewPosition(new java.awt.Point(0, 0));
            }
        } catch (IOException e) {
            showError("Fehler beim Anzeigen der Seite: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Aktualisiert den Status der Navigations-Buttons
     */
    private void updateNavigationButtons() {
        prevButton.setEnabled(currentPage > 0);
        nextButton.setEnabled(currentPage < totalPages - 1);
    }
    
    /**
     * Zeigt eine Fehlermeldung an
     * @param message Die Fehlermeldung
     */
    private void showError(String message) {
        imageLabel.setText("<html><center><font color='red'>" + message + "</font></center></html>");
        imageLabel.setIcon(null);
        pageLabel.setText("Fehler");
        prevButton.setEnabled(false);
        nextButton.setEnabled(false);
    }
    
    /**
     * Richtet Tastenkombinationen ein
     */
    private void setupKeyBindings() {
        // ESC zum Schließen
        KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        Action escapeAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "ESCAPE");
        getRootPane().getActionMap().put("ESCAPE", escapeAction);
        
        // Pfeiltasten für Navigation
        KeyStroke leftKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false);
        Action leftAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (prevButton.isEnabled()) {
                    prevButton.doClick();
                }
            }
        };
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(leftKeyStroke, "LEFT");
        getRootPane().getActionMap().put("LEFT", leftAction);
        
        KeyStroke rightKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false);
        Action rightAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (nextButton.isEnabled()) {
                    nextButton.doClick();
                }
            }
        };
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(rightKeyStroke, "RIGHT");
        getRootPane().getActionMap().put("RIGHT", rightAction);
    }
    
    /**
     * Schließt das PDF-Dokument und gibt Ressourcen frei
     */
    @Override
    public void dispose() {
        try {
            if (document != null) {
                document.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.dispose();
    }
}