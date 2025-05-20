package components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.jfree.chart.ChartPanel;

import data.FavoritesManager;
import data.ProviderStats;
import ui.TradeListFrame;
import utils.UIStyle;

/**
 * Panel zur Darstellung der Informationen zu einem Signal Provider,
 * inklusive Chart und Buttons für Favoriten und Bad Provider.
 */
public class SignalProviderPanel extends JPanel {
    private final DecimalFormat df = new DecimalFormat("#,##0.00");
    private final DecimalFormat pf = new DecimalFormat("#,##0.00'%'");
    private final String providerId;
    private final String providerName;
    private final String rootPath;
    private final FavoritesManager favoritesManager;
    private final ChartPanel chartPanel;
    private final ProviderStats stats;
    
    private JButton favoriteButton;
    private JButton badProviderButton;
    private JComboBox<String> categoryComboBox;
    private JLabel favoriteCategoryLabel;
    
    /**
     * Konstruktor für ein Signal Provider Panel
     * 
     * @param chartPanel Das ChartPanel, das angezeigt werden soll
     * @param providerName Der Name des Signal Providers
     * @param stats Die ProviderStats-Daten
     */
    public SignalProviderPanel(ChartPanel chartPanel, String providerName, ProviderStats stats) {
        this.chartPanel = chartPanel;
        this.providerName = providerName;
        this.stats = stats;
        
        // Extrahiere providerId aus dem Namen
        String tempId = "";
        if (providerName.contains("_")) {
            tempId = providerName.substring(providerName.lastIndexOf("_") + 1);
            tempId = tempId.replace(".csv", "");
        } else {
            // Fallback: Zahlen aus dem Namen extrahieren
            StringBuilder sb = new StringBuilder();
            for (char c : providerName.toCharArray()) {
                if (Character.isDigit(c)) {
                    sb.append(c);
                }
            }
            tempId = sb.toString();
        }
        this.providerId = tempId;
        
        // Root-Pfad aus ApplicationConstants holen
        this.rootPath = utils.ApplicationConstants.ROOT_PATH;
        this.favoritesManager = new FavoritesManager(rootPath);
        
        initializeUI();
    }
    
    /**
     * Initialisiert die UI-Komponenten
     */
    private void initializeUI() {
        setLayout(new BorderLayout(10, 5));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIStyle.SECONDARY_COLOR, 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        setBackground(Color.WHITE);
        
        // Oberes Panel mit Titel, Link und Buttons
        JPanel headerPanel = createHeaderPanel();
        
        // Statistik-Panel mit den wichtigsten Kennzahlen
        JPanel statsPanel = createStatsPanel();
        
        // Chart in der Mitte
        chartPanel.setBorder(createStandardBorder());
        
        // Layout zusammenbauen
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(headerPanel, BorderLayout.NORTH);
        topPanel.add(statsPanel, BorderLayout.SOUTH);
        
        add(topPanel, BorderLayout.NORTH);
        add(chartPanel, BorderLayout.CENTER);
    }
    
    /**
     * Erstellt das Header-Panel mit Titel, Link und Buttons
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);
        
        // Titel-Panel mit Link
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        
        // Provider-Name als Titel
        JLabel titleLabel = new JLabel(providerName);
        titleLabel.setFont(UIStyle.TITLE_FONT);
        titleLabel.setForeground(UIStyle.PRIMARY_COLOR);
        
        // URL als klickbarer Link
        String urlText = String.format(
                "<html><u>https://www.mql5.com/de/signals/%s?source=Site+Signals+Subscriptions#!tab=account</u></html>",
                providerId);
        JLabel urlLabel = createLinkLabel(urlText);
        
        titlePanel.add(titleLabel, BorderLayout.WEST);
        titlePanel.add(urlLabel, BorderLayout.EAST);
        
        // Button-Panel
        JPanel buttonPanel = createButtonPanel();
        
        // Alles zum Header-Panel hinzufügen
        headerPanel.add(titlePanel);
        headerPanel.add(Box.createVerticalStrut(10));
        headerPanel.add(buttonPanel);
        
        return headerPanel;
    }
    
    /**
     * Erstellt ein klickbares Link-Label
     */
    private JLabel createLinkLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UIStyle.REGULAR_FONT);
        label.setForeground(UIStyle.SECONDARY_COLOR);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                try {
                    Desktop.getDesktop().browse(new URI(String.format(
                            "https://www.mql5.com/de/signals/%s?source=Site+Signals+Subscriptions#!tab=account",
                            providerId)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        
        return label;
    }
    
    /**
     * Erstellt das Button-Panel mit Favoriten, Bad Provider und Show Trade List Buttons
     */
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        
        // Aktuelle Favoriten-Kategorie abrufen
        int favoriteCategory = favoritesManager.getFavoriteCategory(providerId);
        boolean isFavorite = favoriteCategory > 0;
        boolean isBadProvider = favoritesManager.isBadProvider(providerId);
        
        // Panel für Favoriten-Bereich erstellen
        JPanel favoritePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        favoritePanel.setOpaque(false);
        
        // Favoriten-Button
        favoriteButton = UIStyle.createStyledButton(
                isFavorite ? "Remove Favorite" : "Set Favorite");
        if (isFavorite) {
            favoriteButton.setBackground(UIStyle.ACCENT_COLOR);
            favoriteButton.setForeground(UIStyle.TEXT_COLOR);
        }
        
        // Kategorie-Auswahl (nur anzeigen, wenn es ein Favorit ist)
        if (isFavorite) {
            favoriteCategoryLabel = new JLabel("Kategorie: ");
            favoritePanel.add(favoriteCategoryLabel);
            
            categoryComboBox = new JComboBox<>(createCategoryOptions());
            categoryComboBox.setSelectedIndex(favoriteCategory);
            UIStyle.applyStylesToComboBox(categoryComboBox);
            
            categoryComboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int selectedCategory = categoryComboBox.getSelectedIndex();
                    if (selectedCategory == 0) {
                        // Wenn "Kein Favorit" ausgewählt wurde, den Favoriten entfernen
                        favoritesManager.setFavoriteCategory(providerId, 0);
                        favoritesManager.reloadFavorites(); // << Diese Zeile ist neu!
                        updateFavoriteUI(false);
                    } else {
                        // Sonst die Kategorie setzen
                        favoritesManager.setFavoriteCategory(providerId, selectedCategory);
                    }
                }
            });
            
            favoritePanel.add(categoryComboBox);
        }
        
        // Favoriten-Button Action
        favoriteButton.addActionListener(e -> {
            if (isFavorite) {
                // Wenn es ein Favorit ist, entfernen
                favoritesManager.setFavoriteCategory(providerId, 0);
                updateFavoriteUI(false);
            } else {
                // Wenn es kein Favorit ist, Kategorie auswählen
                selectFavoriteCategory();
            }
        }
    		  		);
       
        
        
        // Bad Provider Button
        badProviderButton = UIStyle.createStyledButton(
                isBadProvider ? "Remove from Bad List" : "Set as Bad Provider");
        if (isBadProvider) {
            badProviderButton.setBackground(UIStyle.NEGATIVE_COLOR);
            badProviderButton.setForeground(Color.WHITE);
        } else {
            badProviderButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyle.NEGATIVE_COLOR, 2),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)
            ));
        }
        
        // Bad Provider Button Action
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
        });
        
        // Show Trades Button
        JButton showTradesButton = UIStyle.createStyledButton("Show Trade List");
        showTradesButton.addActionListener(e -> {
            TradeListFrame tradeListFrame = new TradeListFrame(providerName, stats);
            tradeListFrame.setVisible(true);
        });
        
        // Panel für weitere Buttons
        JPanel actionButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionButtonsPanel.setOpaque(false);
        
        favoritePanel.add(favoriteButton);
        actionButtonsPanel.add(badProviderButton);
        actionButtonsPanel.add(showTradesButton);
        
        // Gesamtes Button-Panel
        JPanel fullButtonPanel = new JPanel(new BorderLayout());
        fullButtonPanel.setOpaque(false);
        fullButtonPanel.add(favoritePanel, BorderLayout.WEST);
        fullButtonPanel.add(actionButtonsPanel, BorderLayout.EAST);
        
        return fullButtonPanel;
    }
    
    /**
     * Zeigt einen Dialog zur Auswahl der Favoriten-Kategorie
     */
    private void selectFavoriteCategory() {
        Object[] options = createCategoryOptions();
        
        int selectedOption = JOptionPane.showOptionDialog(
            this,
            "Bitte wählen Sie die Favoriten-Kategorie für Provider " + providerId,
            "Favoriten-Kategorie wählen",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[1] // Standardmäßig Kategorie 1 auswählen
        );
        
        if (selectedOption > 0) { // Nur wenn eine gültige Kategorie ausgewählt wurde
            favoritesManager.setFavoriteCategory(providerId, selectedOption);
            updateFavoriteUI(true);
        }
    }
    
    /**
     * Aktualisiert die UI-Elemente für den Favoriten-Status
     * @param isFavorite true, wenn der Provider ein Favorit ist, sonst false
     */
    private void updateFavoriteUI(boolean isFavorite) {
        favoriteButton.setText(isFavorite ? "Remove Favorite" : "Set Favorite");
        
        if (isFavorite) {
            favoriteButton.setBackground(UIStyle.ACCENT_COLOR);
            favoriteButton.setForeground(UIStyle.TEXT_COLOR);
            
            // Kategorie-UI neu erstellen
            JPanel parentPanel = (JPanel) favoriteButton.getParent();
            
            // Existierende Kategorie-Komponenten entfernen, falls vorhanden
            if (favoriteCategoryLabel != null) {
                parentPanel.remove(favoriteCategoryLabel);
            }
            if (categoryComboBox != null) {
                parentPanel.remove(categoryComboBox);
            }
            
            // Neue Kategorie-Komponenten erstellen
            favoriteCategoryLabel = new JLabel("Kategorie: ");
            categoryComboBox = new JComboBox<>(createCategoryOptions());
            int currentCategory = favoritesManager.getFavoriteCategory(providerId);
            categoryComboBox.setSelectedIndex(currentCategory);
            UIStyle.applyStylesToComboBox(categoryComboBox);
            
            categoryComboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int selectedCategory = categoryComboBox.getSelectedIndex();
                    if (selectedCategory == 0) {
                        // Wenn "Kein Favorit" ausgewählt wurde, den Favoriten entfernen
                        favoritesManager.setFavoriteCategory(providerId, 0);
                        updateFavoriteUI(false);
                    } else {
                        // Sonst die Kategorie setzen
                        favoritesManager.setFavoriteCategory(providerId, selectedCategory);
                    }
                }
            });
            
            // Komponenten hinzufügen
            parentPanel.add(favoriteCategoryLabel, 0);
            parentPanel.add(categoryComboBox, 1);
            
        } else {
            favoriteButton.setBackground(UIStyle.SECONDARY_COLOR);
            favoriteButton.setForeground(Color.WHITE);
            
            // Kategorie-UI entfernen
            JPanel parentPanel = (JPanel) favoriteButton.getParent();
            
            if (favoriteCategoryLabel != null) {
                parentPanel.remove(favoriteCategoryLabel);
                favoriteCategoryLabel = null;
            }
            if (categoryComboBox != null) {
                parentPanel.remove(categoryComboBox);
                categoryComboBox = null;
            }
        }
        
        // Panel neu zeichnen
        revalidate();
        repaint();
    }
    
    /**
     * Erstellt die Optionen für die Kategorie-Auswahl
     * @return Array mit Kategorienamen
     */
    private String[] createCategoryOptions() {
        String[] options = new String[11]; // 0-10
        options[0] = "Kein Favorit";
        for (int i = 1; i <= 10; i++) {
            options[i] = "Kategorie " + i;
        }
        return options;
    }
    
    /**
     * Erstellt das Statistik-Panel mit den wichtigsten Kennzahlen
     */
    private JPanel createStatsPanel() {
        JPanel statsPanel = new JPanel(new GridLayout(2, 3, 10, 5));
        statsPanel.setBorder(createStandardBorder());
        statsPanel.setOpaque(false);
        
        addStatField(statsPanel, "Total Trades: ", String.format("%d", stats.getTradeCount()));
        addStatField(statsPanel, "Win Rate: ", pf.format(stats.getWinRate()));
        addStatField(statsPanel, "Total Profit: ", df.format(stats.getTotalProfit()));
        addStatField(statsPanel, "Profit Factor: ", df.format(stats.getProfitFactor()));
        addStatField(statsPanel, "Max Drawdown: ", pf.format(stats.getMaxDrawdown()));
        addStatField(statsPanel, "Avg Profit/Trade: ", df.format(stats.getAverageProfit()));
        
        return statsPanel;
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
        if (label.contains("Profit") || label.contains("Win Rate")) {
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
        } else if (label.contains("Drawdown")) {
            try {
                double numValue = Double.parseDouble(value.replace("%", "").replace(",", "."));
                valueComponent.setForeground(UIStyle.NEGATIVE_COLOR);
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
     * Standard-Rahmen für Panel-Komponenten
     */
    private Border createStandardBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIStyle.SECONDARY_COLOR, 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        );
    }
}