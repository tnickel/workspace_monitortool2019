package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import components.MainTable;
import data.DataManager;
import models.FilterCriteria;
import services.ProviderHistoryService;
import ui.components.MenuManager;
import ui.components.SearchManager;
import ui.components.ToolbarManager;
import utils.ApplicationConstants;
import utils.HtmlDatabase;
import utils.MqlAnalyserConf;
import utils.UIStyle;

public class MainFrame extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(MainFrame.class.getName());
    private final MainTable mainTable;
    private final JLabel statusLabel;
    private final JTextField searchField;
    private final DataManager dataManager;
    private final MqlAnalyserConf config;
    private final ProviderHistoryService historyService;
    private final int[] currentSearchIndex = {-1};
    private String rootPath;
    
    // Manager für UI-Komponenten
    private final MenuManager menuManager;
    private final ToolbarManager toolbarManager;
    private final SearchManager searchManager;
    
    public MainFrame(DataManager dataManager, String rootPathStr, MqlAnalyserConf config) {
        super("Signal Providers Performance Analysis");
        
        // Validiere den Pfad und korrigiere ihn, falls nötig
        rootPathStr = ApplicationConstants.validateRootPath(rootPathStr, "MainFrame.constructor");
        
        this.dataManager = dataManager;
        this.config = config;
        this.rootPath = rootPathStr;
        
        // UI-Manager initialisieren
        UIStyle.setUpUIDefaults();
        
        // Status-Label und Such-Feld erstellen
        this.statusLabel = UIStyle.createStyledLabel("");
        this.searchField = UIStyle.createStyledTextField(20);
        
        // Provider History Service initialisieren
        this.historyService = ProviderHistoryService.getInstance();
        this.historyService.initialize(rootPath);
        
        // MainTable erstellen
        this.mainTable = new MainTable(dataManager, rootPath);
        this.mainTable.setStatusUpdateCallback(text -> updateStatusBar());
        
        // Manager für UI-Komponenten initialisieren
        this.menuManager = new MenuManager(this, dataManager, mainTable, config, historyService, rootPath);
        this.toolbarManager = new ToolbarManager(this, mainTable, searchField, currentSearchIndex, historyService, rootPath);
        this.searchManager = new SearchManager(this, mainTable, searchField, currentSearchIndex);
        
        // Menüleiste setzen
        setJMenuBar(menuManager.getMenuBar());
        
        setupUI();
        setupStatusBar();
        
        // Prüfe Dateisystemzugriff
        SwingUtilities.invokeLater(() -> verifyFileSystemAccess());
        
        // Hinzufügen eines WindowListeners für sauberes Herunterfahren
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                LOGGER.info("Fenster wird geschlossen, Ressourcen werden freigegeben...");
                historyService.shutdown();
            }
        });
    }
    
    private void setupUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Hintergrund für das Hauptpanel
        JPanel contentPane = new GradientPanel();
        contentPane.setLayout(new BorderLayout(5, 5));
        contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        
        // Toolbar hinzufügen
        add(toolbarManager.getToolBar(), BorderLayout.NORTH);
        
        // Mittlerer Bereich mit Tabelle und Report-Button-Panel
        JPanel centerPanel = new JPanel(new BorderLayout(0, 5));
        centerPanel.setOpaque(false);
        
        // Tabelle mit schönem Rahmen
        JScrollPane scrollPane = new JScrollPane(mainTable);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIStyle.SECONDARY_COLOR, 1),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Gelb markiertes Panel mit Report-Button unter der Tabelle
        centerPanel.add(toolbarManager.getReportPanel(), BorderLayout.SOUTH);
        
        // Mittleren Bereich zum Hauptpanel hinzufügen
        contentPane.add(centerPanel, BorderLayout.CENTER);
        
        setSize(1200, 800);
        setLocationRelativeTo(null);
    }
    
    // Neues JPanel mit Farbverlauf für den Hintergrund
    private class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            
            // Heller Farbverlauf als Hintergrund
            GradientPaint gradient = new GradientPaint(
                0, 0, new Color(245, 247, 250), 
                0, getHeight(), new Color(230, 238, 245)
            );
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }
    
    private void setupStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(UIStyle.SECONDARY_COLOR);
        statusBar.setBorder(BorderFactory.createEmptyBorder(4, 5, 4, 5));
        
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        
        statusBar.add(statusLabel, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
    }
    
    public void updateStatusBar() {
        String statusText = mainTable.getStatusText();
        statusLabel.setText(statusText);
    }
    
    public void deleteSelectedProviders() {
        List<String> selectedProviders = mainTable.getSelectedProviders();
        if (selectedProviders.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Bitte wählen Sie mindestens einen Provider aus.",
                "Keine Auswahl",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        int result = JOptionPane.showConfirmDialog(this,
            "Möchten Sie die " + selectedProviders.size() + " ausgewählten Signal Provider löschen?",
            "Provider löschen",
            JOptionPane.YES_NO_OPTION);
            
        if (result == JOptionPane.YES_OPTION) {
            DeleteProviderDialog dialog = new DeleteProviderDialog(
                this,
                rootPath,
                dataManager,
                mainTable.getSelectedProvidersMap(),
                () -> {
                    reloadData(config.getDownloadPath());
                    mainTable.refreshTableData();
                    updateStatusBar();
                }
            );
            dialog.setVisible(true);
        }
    }
    
    public void reloadData(String newPath) {
        try {
            dataManager.loadData(newPath);
            mainTable.refreshTableData();
            updateStatusBar();
        } catch (Exception e) {
            LOGGER.severe("Error reloading data: " + e.getMessage());
            JOptionPane.showMessageDialog(this, 
                "Error loading data from new path", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void resetAll() {
        searchManager.clearSearch();
        mainTable.resetFilter();
        updateStatusBar();
    }
    
    public void verifyFileSystemAccess() {
    	String downloadPath = config.getDownloadPath();
        HtmlDatabase htmlDb = mainTable.getHtmlDatabase();
        if (htmlDb != null) {
            boolean accessOk = htmlDb.checkFileAccess();
            if (!accessOk) {
                JOptionPane.showMessageDialog(
                    this,
                    "WARNUNG: Es gibt Probleme beim Zugriff auf das Datenverzeichnis:\n" +
                    		downloadPath + "\n\n" +
                    "Dies kann zu fehlenden Daten wie 3MPDD-Werten führen.\n" +
                    "Bitte überprüfen Sie den Pfad in den Einstellungen.",
                    "Dateizugriffsproblem",
                    JOptionPane.WARNING_MESSAGE
                );
            } else {
                // Prüfe auf _root.txt Dateien
                File dir = new File(downloadPath);
                File[] rootTxtFiles = dir.listFiles((d, name) -> name.endsWith("_root.txt"));
                if (rootTxtFiles == null || rootTxtFiles.length == 0) {
                    JOptionPane.showMessageDialog(
                        this,
                        "WARNUNG: Im Datenverzeichnis wurden keine _root.txt Dateien gefunden:\n" +
                        		downloadPath + "\n\n" +
                        "Dies führt dazu, dass 3MPDD-Werte und andere Statistiken nicht berechnet werden können.\n" +
                        "Bitte überprüfen Sie den Pfad in den Einstellungen.",
                        "Fehlende Datendateien",
                        JOptionPane.WARNING_MESSAGE
                    );
                } else {
                    LOGGER.info("Gefunden " + rootTxtFiles.length + " _root.txt Dateien im Verzeichnis " + downloadPath);
                }
            }
        }
    }
    
    public void display() {
        SwingUtilities.invokeLater(() -> {
            setVisible(true);
            updateStatusBar();
            
            // Prüfen, ob wöchentliche Speicherung erforderlich ist
            historyService.checkAndPerformWeeklySave();
        });
    }
}