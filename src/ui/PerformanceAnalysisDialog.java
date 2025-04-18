package ui;

import java.text.DecimalFormat;

import javax.swing.JFrame;

import components.LoadingPanel;
import data.ProviderStats;
import utils.HtmlDatabase;

/**
 * Detailansicht für einen Signal Provider mit modernem Kachel-Layout
 */
public class PerformanceAnalysisDialog extends JFrame {
    private final ProviderStats stats;
    private final String providerId;
    private final String providerName;
    private final DecimalFormat df = new DecimalFormat("#,##0.00");
    private final DecimalFormat pf = new DecimalFormat("#,##0.00'%'");
    private final HtmlDatabase htmlDatabase;
    private final String rootPath;
    private final LoadingPanel loadingPanel;
    
    public PerformanceAnalysisDialog(String providerName, ProviderStats stats, String providerId,
            HtmlDatabase htmlDatabase, String rootPath) {
        super("Performance Analysis: " + providerName);
        this.stats = stats;
        this.providerId = providerId;
        this.providerName = providerName;
        this.htmlDatabase = htmlDatabase;
        this.rootPath = rootPath;
        this.loadingPanel = new LoadingPanel("Lade Daten...");
        
        // DEBUG: Ausgabe des Dateinamens
        System.out.println("Reading data for file: " + providerName + ".csv");
        
        // DEBUG: Ausgabe der gelesenen Daten
        Map<String, Double> monthlyProfits = htmlDatabase.getMonthlyProfitPercentages(providerName + ".csv");
        System.out.println("Monthly profits read: " + monthlyProfits);
        
        stats.setMonthlyProfits(monthlyProfits);
        
        initializeModernUI();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        // Fensterbreite auf 85% der Bildschirmbreite
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int) (screenSize.width * 0.85);
        int height = (int) (screenSize.height * 0.8);
        setSize(width, height);
        
        setLocationRelativeTo