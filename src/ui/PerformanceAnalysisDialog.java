package ui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

import data.ProviderStats;
import ui.components.ChartsPanelFactory;
import ui.components.PerformanceStatisticsPanel;
import ui.components.ProviderNotesPanel;
import utils.HtmlDatabase;

/**
 * Hauptdialog für die detaillierte Analyse eines Signal Providers
 */
public class PerformanceAnalysisDialog extends JFrame {
    private final ProviderStats stats;
    private final String providerId;
    private final String providerName;
    private final String rootPath;
    private final HtmlDatabase htmlDatabase;
    
    // UI-Komponenten
    private PerformanceStatisticsPanel statisticsPanel;
    private ProviderNotesPanel notesPanel;
    private JPanel chartsPanel;
    
    /**
     * Konstruktor für den Performance-Analysis-Dialog
     */
    public PerformanceAnalysisDialog(String providerName, ProviderStats stats, String providerId,
            HtmlDatabase htmlDatabase, String rootPath) {
        super("Performance Analysis: " + providerName);
        this.stats = stats;
        this.providerId = providerId;
        this.providerName = providerName;
        this.htmlDatabase = htmlDatabase;
        this.rootPath = rootPath;
        
        // DEBUG: Ausgabe des Dateinamens
        System.out.println("Reading data for file: " + providerName + ".csv");
        
        // Monatliche Profite laden und in ProviderStats setzen
        stats.setMonthlyProfits(htmlDatabase.getMonthlyProfitPercentages(providerName + ".csv"));
        
        initializeUI();
        setupKeyBindings();
        
        // Fensterbreite auf 85% der Bildschirmbreite
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int) (screenSize.width * 0.85);
        int height = (int) (screenSize.height * 0.8);
        setSize(width, height);
        
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
    
    /**
     * Initialisiert die UI-Komponenten
     */
    private void initializeUI() {
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(UIConstants.BG_COLOR);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Statistik-Panel erstellen und hinzufügen
        statisticsPanel = new PerformanceStatisticsPanel(
                stats, providerName, providerId, htmlDatabase, rootPath);
        statisticsPanel.setAlignmentX(LEFT_ALIGNMENT);
        mainPanel.add(statisticsPanel);
        mainPanel.add(javax.swing.Box.createRigidArea(new Dimension(0, 15)));
        
        // Notizen-Panel erstellen und an der gelb markierten Stelle hinzufügen
        notesPanel = new ProviderNotesPanel(providerName);
        notesPanel.setAlignmentX(LEFT_ALIGNMENT);
        mainPanel.add(notesPanel);
        mainPanel.add(javax.swing.Box.createRigidArea(new Dimension(0, 15)));
        
        // Charts Panel erstellen und hinzufügen
        chartsPanel = ChartsPanelFactory.createChartPanels(stats, providerName, htmlDatabase);
        mainPanel.add(chartsPanel);
        
        // Scrollpane für die gesamte Ansicht
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);
        scrollPane.setBackground(UIConstants.BG_COLOR);
        add(scrollPane);
    }
    
    /**
     * Richtet Tastenkombinationen ein (z.B. ESC zum Schließen)
     */
    private void setupKeyBindings() {
        KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        Action escapeAction = new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispose();
            }
        };
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "ESCAPE");
        getRootPane().getActionMap().put("ESCAPE", escapeAction);
    }
}