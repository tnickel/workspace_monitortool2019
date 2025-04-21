package ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.net.URI;
import java.text.DecimalFormat;
import java.time.temporal.ChronoUnit;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import data.FavoritesManager;
import data.ProviderStats;
import ui.TradeListFrame;
import ui.dialogs.DatabaseInfoDialog;
import utils.HtmlDatabase;
import utils.UIStyle;

/**
 * Panel zur Anzeige der wichtigsten Statistiken eines Signal Providers
 */
public class PerformanceStatisticsPanel extends JPanel {
    // Formatierungsklassen
    private final DecimalFormat df = new DecimalFormat("#,##0.00");
    private final DecimalFormat pf = new DecimalFormat("#,##0.00'%'");
    
    // Daten
    private final ProviderStats stats;
    private final String providerId;
    private final String providerName;
    private final String rootPath;
    private final HtmlDatabase htmlDatabase;
    
    // UI-Komponenten
    private JButton favoriteButton;
    private JButton badProviderButton; // Neuer Bad Provider Button
    
    /**
     * Konstruktor für das StatisticsPanel
     */
    public PerformanceStatisticsPanel(ProviderStats stats, String providerName, 
                                      String providerId, HtmlDatabase htmlDatabase, String rootPath) {
        this.stats = stats;
        this.providerId = providerId;
        this.providerName = providerName;
        this.htmlDatabase = htmlDatabase;
        this.rootPath = rootPath;
        
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new CompoundBorder(
            new LineBorder(UIStyle.SECONDARY_COLOR, 1),
            new EmptyBorder(10, 10, 10, 10)
        ));
        
        initializeUI();
    }
    
    /**
     * Initialisiert alle UI-Komponenten
     */
    private void initializeUI() {
        // Header Panel mit Provider-Name und URL
        JPanel headerPanel = createHeaderPanel();
        
        // Statistik-Panel mit den wichtigsten Kennzahlen
        JPanel statsGrid = createStatsGrid();
        
        // Button-Panel für Aktionen
        JPanel buttonPanel = createButtonPanel();
        
        // Alles zusammenfügen
        JPanel contentPanel = new JPanel(new BorderLayout(0, 10));
        contentPanel.setOpaque(false);
        contentPanel.add(headerPanel, BorderLayout.NORTH);
        contentPanel.add(statsGrid, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(contentPanel, BorderLayout.CENTER);
    }
    
    /**
     * Erstellt das Header-Panel mit Titel und URL
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(5, 0));
        headerPanel.setOpaque(false);
        
        // Provider-Name als Titel
        JLabel titleLabel = new JLabel(providerName);
        titleLabel.setFont(UIStyle.TITLE_FONT);
        titleLabel.setForeground(UIStyle.PRIMARY_COLOR);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        // URL als klickbarer Link
        String urlText = String.format(
                "<html><u>" + UIStyle.SIGNAL_PROVIDER_URL_FORMAT + "</u></html>",
                providerId);
        JLabel urlLabel = new JLabel(urlText);
        urlLabel.setFont(UIStyle.REGULAR_FONT);
        urlLabel.setForeground(UIStyle.SECONDARY_COLOR);
        urlLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        urlLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                try {
                    Desktop.getDesktop().browse(new URI(String.format(
                            UIStyle.SIGNAL_PROVIDER_URL_FORMAT,
                            providerId)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        
        JPanel urlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        urlPanel.setOpaque(false);
        urlPanel.add(urlLabel);
        headerPanel.add(urlPanel, BorderLayout.EAST);
        
        return headerPanel;
    }
    
    /**
     * Erstellt das Raster mit den Statistik-Werten
     */
    private JPanel createStatsGrid() {
        JPanel statsGrid = new JPanel(new GridLayout(2, 7, 10, 5));
        statsGrid.setOpaque(false);
        
        String csvFileName = providerName + ".csv";
        double equityDrawdown = htmlDatabase.getEquityDrawdown(csvFileName);
        double threeMonthProfit = htmlDatabase.getAverageMonthlyProfit(csvFileName, 3);
        double sixMonthProfit = htmlDatabase.getAverageMonthlyProfit(csvFileName, 6);
        double nineMonthProfit = htmlDatabase.getAverageMonthlyProfit(csvFileName, 9);
        
        double mpdd3 = calculateMPDD(threeMonthProfit, equityDrawdown);
        double mpdd6 = calculateMPDD(sixMonthProfit, equityDrawdown);
        double mpdd9 = calculateMPDD(nineMonthProfit, equityDrawdown);
        
        // Erste Zeile mit Statistiken
        addStatField(statsGrid, "Total Trades: ", String.format("%d", stats.getTrades().size()));
        addStatField(statsGrid, "Win Rate: ", pf.format(stats.getWinRate()));
        addStatField(statsGrid, "Total Profit: ", df.format(stats.getTotalProfit()));
        addStatField(statsGrid, "Profit Factor: ", df.format(stats.getProfitFactor()));
        addStatField(statsGrid, "Max Concurrent Lots: ", df.format(stats.getMaxConcurrentLots()));
        addStatField(statsGrid, "Stability: ", df.format(htmlDatabase.getStabilitaetswert(csvFileName)));
        addStatField(statsGrid, "Days: ", String.format("%d", calculateDaysBetween(stats)));
        
        // Zweite Zeile mit Statistiken
        addStatField(statsGrid, "Avg Profit/Trade: ", df.format(stats.getAverageProfit()));
        addStatField(statsGrid, "Equity Drawdown: ", pf.format(equityDrawdown));
        addStatField(statsGrid, "3MPDD: ", df.format(mpdd3));
        addStatField(statsGrid, "6MPDD: ", df.format(mpdd6));
        addStatField(statsGrid, "9MPDD: ", df.format(mpdd9));
        
        return statsGrid;
    }
    
    /**
     * Erstellt ein Panel mit Aktions-Buttons
     */
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setOpaque(false);
        
        // Den FavoritesManager initialisieren
        FavoritesManager favoritesManager = new FavoritesManager(rootPath);
        boolean isFavorite = favoritesManager.isFavorite(providerId);
        boolean isBadProvider = favoritesManager.isBadProvider(providerId); // Prüfe, ob Bad Provider
        
        favoriteButton = UIStyle.createStyledButton(
                isFavorite ? "Remove Favorite" : "Set Favorite");
        if (isFavorite) {
            favoriteButton.setBackground(UIStyle.ACCENT_COLOR);
            favoriteButton.setForeground(UIStyle.TEXT_COLOR);
        }
        
        // Bad Provider Button erstellen
        badProviderButton = UIStyle.createStyledButton(
                isBadProvider ? "Remove from Bad List" : "Set as Bad Provider");
        if (isBadProvider) {
            badProviderButton.setBackground(UIStyle.NEGATIVE_COLOR);
            badProviderButton.setForeground(Color.WHITE);
        } else {
            // Roter Rand für den Button, wenn er noch nicht aktiv ist
            badProviderButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyle.NEGATIVE_COLOR, 2),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)
            ));
        }
        
        JButton showTradesButton = UIStyle.createStyledButton("Show Trade List");
        JButton showDbInfoButton = UIStyle.createStyledButton("Show DB Info");
        
        favoriteButton.addActionListener(e -> {
            favoritesManager.toggleFavorite(providerId);
            boolean isNowFavorite = favoritesManager.isFavorite(providerId);
            favoriteButton.setText(isNowFavorite ? "Remove Favorite" : "Set Favorite");
            
            if (isNowFavorite) {
                favoriteButton.setBackground(UIStyle.ACCENT_COLOR);
                favoriteButton.setForeground(UIStyle.TEXT_COLOR);
            } else {
                favoriteButton.setBackground(UIStyle.SECONDARY_COLOR);
                favoriteButton.setForeground(Color.WHITE);
            }
            
            System.out.println("Favorit-Status für Provider " + providerId + " geändert: " + isNowFavorite);
        });
        
        // Action Listener für Bad Provider Button
        badProviderButton.addActionListener(e -> {
            favoritesManager.toggleBadProvider(providerId);
            boolean isNowBadProvider = favoritesManager.isBadProvider(providerId);
            badProviderButton.setText(isNowBadProvider ? "Remove from Bad List" : "Set as Bad Provider");
            
            if (isNowBadProvider) {
                badProviderButton.setBackground(UIStyle.NEGATIVE_COLOR);
                badProviderButton.setForeground(Color.WHITE);
                badProviderButton.setBorder(UIStyle.createButtonBorder());
            } else {
                badProviderButton.setBackground(UIStyle.SECONDARY_COLOR);
                badProviderButton.setForeground(Color.WHITE);
                badProviderButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIStyle.NEGATIVE_COLOR, 2),
                    BorderFactory.createEmptyBorder(3, 8, 3, 8)
                ));
            }
            
            System.out.println("Bad Provider-Status für Provider " + providerId + " geändert: " + isNowBadProvider);
        });
        
        showTradesButton.addActionListener(e -> {
            TradeListFrame tradeListFrame = new TradeListFrame(providerName, stats);
            tradeListFrame.setVisible(true);
        });
        
        showDbInfoButton.addActionListener(e -> {
            DatabaseInfoDialog dialog = new DatabaseInfoDialog(
                    javax.swing.SwingUtilities.getWindowAncestor(this), 
                    providerName);
            dialog.setVisible(true);
        });
        
        buttonPanel.add(favoriteButton);
        buttonPanel.add(badProviderButton); // Füge Bad Provider Button hinzu
        buttonPanel.add(showTradesButton);
        buttonPanel.add(showDbInfoButton);
        
        return buttonPanel;
    }
    
    /**
     * Fügt ein Statistik-Feld zum Panel hinzu
     */
    private void addStatField(JPanel panel, String label, String value) {
        JPanel fieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        fieldPanel.setOpaque(false);
        
        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(UIStyle.BOLD_FONT);
        labelComponent.setForeground(UIStyle.TEXT_COLOR);
        fieldPanel.add(labelComponent);
        
        JLabel valueComponent = new JLabel(value);
        valueComponent.setFont(UIStyle.BOLD_LARGE_FONT);
        
        // Farbliche Formatierung je nach Wert
        if (label.contains("Profit") || label.contains("Win Rate") || 
            label.contains("MPDD") || label.contains("Stability")) {
            try {
                double numValue = Double.parseDouble(value.replace("%", "").replace(",", "."));
                if (numValue > 0) {
                    valueComponent.setForeground(UIStyle.POSITIVE_COLOR);
                } else if (numValue < 0) {
                    valueComponent.setForeground(UIStyle.NEGATIVE_COLOR);
                } else {
                    valueComponent.setForeground(UIStyle.TEXT_COLOR);
                }
            } catch (NumberFormatException e) {
                valueComponent.setForeground(UIStyle.TEXT_COLOR);
            }
        } else {
            valueComponent.setForeground(UIStyle.TEXT_SECONDARY_COLOR);
        }
        
        fieldPanel.add(valueComponent);
        panel.add(fieldPanel);
    }
    
    /**
     * Berechnet die Anzahl der Tage zwischen Start- und Enddatum
     */
    private long calculateDaysBetween(ProviderStats stats) {
        return Math.abs(ChronoUnit.DAYS.between(stats.getStartDate(), stats.getEndDate())) + 1;
    }
    
    /**
     * Berechnet den MPDD-Wert aus monatlichem Profit und Drawdown
     */
    private double calculateMPDD(double monthlyProfitPercent, double maxEquityDrawdown) {
        if (maxEquityDrawdown == 0.0) {
            return 0.0; // Verhindert Division durch Null
        }
        return monthlyProfitPercent / maxEquityDrawdown;
    }
}