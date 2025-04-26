package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import charts.EquityDrawdownChart;
import data.ProviderStats;
import data.Trade;
import utils.HtmlDatabase;

public class EquityDrawdownDialog extends JDialog {
    private static final Logger LOGGER = Logger.getLogger(EquityDrawdownDialog.class.getName());
    private final Map<String, ProviderStats> providers;
    private final HtmlDatabase htmlDatabase;
    private final String rootPath;
    private final JTable providerTable;
    private final JLabel statusLabel;

    /**
     * Konstruktor für den EquityDrawdownDialog
     */
    public EquityDrawdownDialog(Window owner, Map<String, ProviderStats> providers, 
            HtmlDatabase htmlDatabase, String rootPath) {
        super(owner, "Equity Drawdown Comparison", Dialog.ModalityType.MODELESS);
        
        LOGGER.info("EquityDrawdownDialog wird erstellt");
        
        // Defensive Kopie der Map erstellen, um null zu vermeiden
        if (providers == null) {
            LOGGER.severe("Providers Map ist null, erstelle leere Map");
            this.providers = new HashMap<>();
        } else {
            this.providers = new HashMap<>(providers);
        }
        
        this.htmlDatabase = htmlDatabase;
        this.rootPath = rootPath;

        // Debug-Informationen loggen
        LOGGER.info("Anzahl der Provider: " + this.providers.size());
        if (this.providers.size() > 0) {
            int count = 0;
            for (String key : this.providers.keySet()) {
                LOGGER.info("Provider " + count + ": " + key);
                count++;
                if (count >= 5) {
                    LOGGER.info("... und " + (this.providers.size() - 5) + " weitere Provider");
                    break;
                }
            }
        }
        LOGGER.info("htmlDatabase ist " + (htmlDatabase != null ? "vorhanden" : "null"));
        LOGGER.info("rootPath: " + rootPath);

        // Status Label erstellen
        statusLabel = new JLabel(" ");
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        statusPanel.setBackground(new Color(238, 238, 238));

        // Tabelle erstellen - Differenzierung nach Provider-Verfügbarkeit
        if (this.providers.isEmpty()) {
            LOGGER.warning("Keine Provider-Daten zum Anzeigen verfügbar!");
            providerTable = createEmptyTable();
            statusLabel.setText("Keine Provider gefunden. Bitte laden Sie Provider oder überprüfen Sie Ihre Filter.");
        } else {
            providerTable = createProviderTable();
            statusLabel.setText(this.providers.size() + " Provider gefunden.");
        }
        
        // Layout zusammenbauen
        JScrollPane scrollPane = new JScrollPane(providerTable);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        setSize(1560, 800);
        setLocationRelativeTo(owner);

        // ESC-Taste zum Schließen des Dialogs
        javax.swing.KeyStroke escapeKeyStroke = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0, false);
        javax.swing.Action escapeAction = new javax.swing.AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispose();
            }
        };
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "ESCAPE");
        getRootPane().getActionMap().put("ESCAPE", escapeAction);
        
        // DEBUG: Noch einmal Größe und Sichtbarkeit überprüfen
        LOGGER.info("Dialog wurde erstellt mit Größe " + getSize());
        LOGGER.info("Dialog wird jetzt angezeigt");
    }
    
    /**
     * Erstellt ein komplett neues Drawdown-Chart mit passender Skalierung
     * 
     * @param stats Die ProviderStats für den Provider
     * @param providerName Der Name des Providers
     * @return Ein ChartPanel mit korrekt skaliertem Chart
     */
    private ChartPanel createCustomDrawdownChart(ProviderStats stats, String providerName) {
        LOGGER.info("Erstelle komplett neues Chart für " + providerName);
        
        try {
            // Erstelle eine TimeSeries für den Drawdown
            TimeSeries drawdownSeries = new TimeSeries("Drawdown");
            
            // Maximalen Drawdown aus den Stats abrufen
            double maxDrawdownFromStats = stats.getMaxDrawdown();
            LOGGER.info("Max Drawdown aus Stats für " + providerName + ": " + maxDrawdownFromStats + "%");
            
            // Versuche Daten aus der HTML-Datenbank zu laden
            boolean dataFound = false;
            
            if (htmlDatabase != null) {
                // Beide Varianten der Dateipfade versuchen
                String txtFileName = providerName + "_root.txt";
                String csvFileName = providerName + ".csv";
                
                // Versuche Drawdown-Daten zu laden
                String drawdownData = htmlDatabase.getDrawdownChartData(txtFileName);
                if (drawdownData == null || drawdownData.isEmpty()) {
                    drawdownData = htmlDatabase.getDrawdownChartData(csvFileName);
                }
                
                if (drawdownData != null && !drawdownData.isEmpty()) {
                    LOGGER.info("Drawdown-Daten für " + providerName + " gefunden");
                    dataFound = processDrawdownData(drawdownData, drawdownSeries);
                }
            }
            
            // Wenn keine Daten in der DB gefunden wurden, berechne selbst
            if (!dataFound || drawdownSeries.isEmpty()) {
                LOGGER.info("Berechne Drawdown aus den Trades für " + providerName);
                calculateDrawdownFromTrades(stats, drawdownSeries);
            }
            
            // Stelle sicher, dass die Serie nicht leer ist
            if (drawdownSeries.isEmpty()) {
                LOGGER.warning("Keine Drawdown-Daten für " + providerName + " gefunden");
                // Füge Dummy-Daten hinzu, basierend auf maxDrawdownFromStats
                LocalDate now = LocalDate.now();
                drawdownSeries.addOrUpdate(new Day(Date.from(now.minusDays(60).atStartOfDay(ZoneId.systemDefault()).toInstant())), 0.0);
                drawdownSeries.addOrUpdate(new Day(Date.from(now.minusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant())), Math.min(maxDrawdownFromStats, 10.0)); // Begrenze auf maximal 10% für Dummy-Daten
                drawdownSeries.addOrUpdate(new Day(Date.from(now.atStartOfDay(ZoneId.systemDefault()).toInstant())), 0.0);
            }
            
            // Debug-Ausgabe der ersten paar Datenpunkte
            LOGGER.info("Erste 5 Datenpunkte für " + providerName + ":");
            int count = 0;
            for (int i = 0; i < Math.min(5, drawdownSeries.getItemCount()); i++) {
                LOGGER.info("  Punkt " + i + ": Zeit=" + drawdownSeries.getTimePeriod(i) + ", Wert=" + drawdownSeries.getValue(i));
                count++;
            }
            if (drawdownSeries.getItemCount() > 5) {
                LOGGER.info("  ... und " + (drawdownSeries.getItemCount() - 5) + " weitere Punkte");
            }

            // Finde den maximalen Drawdown in den tatsächlichen Daten
            double maxDrawdownInData = 0.0;
            int maxIndex = -1;
            for (int i = 0; i < drawdownSeries.getItemCount(); i++) {
                if (drawdownSeries.getValue(i) != null) {
                    double value = drawdownSeries.getValue(i).doubleValue();
                    if (value > maxDrawdownInData) {
                        maxDrawdownInData = value;
                        maxIndex = i;
                    }
                }
            }

            if (maxIndex >= 0) {
                LOGGER.info("Max Drawdown in Daten für " + providerName + ": " + maxDrawdownInData + 
                          "% (Punkt " + maxIndex + " von " + drawdownSeries.getItemCount() + ")");
            } else {
                LOGGER.warning("Keine gültigen Datenpunkte gefunden für " + providerName);
                maxDrawdownInData = 5.0; // Fallback-Wert
            }
            
            // Prüfung der Werte und Korrektur
            LOGGER.info("DIREKTE WERTE FÜR " + providerName + ":");
            LOGGER.info("  maxDrawdownFromStats = " + maxDrawdownFromStats);
            LOGGER.info("  maxDrawdownInData = " + maxDrawdownInData);

            // Bestimme den tatsächlichen max Drawdown
            double actualMaxDrawdown;
            
            // Prüfe auf unplausible Werte im stats-Objekt
            if (maxDrawdownFromStats > 100.0 || maxDrawdownFromStats < 0.0) {
                LOGGER.warning("  Unplausibler maxDrawdownFromStats-Wert: " + maxDrawdownFromStats + 
                              "% - verwende nur den Wert aus den Daten");
                // Ignoriere den Wert aus stats
                actualMaxDrawdown = maxDrawdownInData;
            } else {
                // Beide Werte berücksichtigen
                actualMaxDrawdown = Math.max(maxDrawdownFromStats, maxDrawdownInData);
            }

            LOGGER.info("  Berechneter actualMaxDrawdown = " + actualMaxDrawdown + "%");
            
            // Dataset erstellen
            TimeSeriesCollection dataset = new TimeSeriesCollection(drawdownSeries);
            
            // Chart erstellen
            JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Drawdown - " + providerName,    // Titel
                "Zeit",                          // X-Achsenbeschriftung
                "Drawdown (%)",                  // Y-Achsenbeschriftung
                dataset,                         // Datensatz
                true,                            // Legende anzeigen
                true,                            // Tooltips anzeigen
                false                            // URLs nicht anzeigen
            );
            
            // Chart anpassen
            chart.setBackgroundPaint(Color.WHITE);
            
            XYPlot plot = (XYPlot) chart.getPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
            plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
            
            // X-Achse (Zeit) anpassen
            DateAxis dateAxis = (DateAxis) plot.getDomainAxis();
            dateAxis.setDateFormatOverride(new SimpleDateFormat("yyyy-MM-dd"));
            
            // Y-Achse (Drawdown) anpassen mit korrekter Skalierung
            NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
            yAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());
            yAxis.setInverted(true); // Y-Achse umdrehen, damit Drawdown nach unten geht
            yAxis.setLabelPaint(new Color(180, 0, 0)); // Rote Beschriftung
            yAxis.setTickLabelPaint(new Color(180, 0, 0)); // Rote Tick-Beschriftungen
            
            // VERBESSERTE SKALIERUNGSBERECHNUNG
            // Obere Grenze basierend auf dem maximalen Drawdown festlegen
            double upperBound;
            
            if (actualMaxDrawdown < 0.1) { // Fast kein Drawdown
                upperBound = 5.0; // Mindestens 5% zeigen
            } else if (actualMaxDrawdown <= 5.0) {
                // Kleiner Drawdown bis 5% - zeige bis 5%
                upperBound = 5.0;
            } else if (actualMaxDrawdown <= 10.0) {
                // Mittlerer Drawdown zwischen 5% und 10% - zeige bis 10%
                upperBound = 10.0;
            } else {
                // Größerer Drawdown - runde auf die nächste 10er-Einheit auf
                upperBound = Math.ceil(actualMaxDrawdown / 10.0) * 10.0;
                
                // Nicht über 100% hinausgehen
                upperBound = Math.min(upperBound, 100.0);
            }
            
            // Setze die Grenzen der Y-Achse
            yAxis.setLowerBound(0.0);
            yAxis.setUpperBound(upperBound);
            
            // Auto-Range deaktivieren, damit unsere Grenzen nicht überschrieben werden
            yAxis.setAutoRange(false);
            
            LOGGER.info("Y-Achse für " + providerName + " auf [0, " + upperBound + "%] festgelegt");
            
            // Linienrenderer anpassen
            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
            renderer.setSeriesPaint(0, new Color(180, 0, 0)); // Kräftiges Rot für die Linie
            renderer.setSeriesStroke(0, new java.awt.BasicStroke(2.0f)); // Linienstärke
            renderer.setSeriesShapesVisible(0, false); // Keine Datenpunktmarker anzeigen
            plot.setRenderer(renderer);
            
            // Füge horizontale Linien für die Prozentwerte hinzu
            addHorizontalLines(plot, upperBound);
            
            // ChartPanel erstellen und konfigurieren
            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new Dimension(950, 300));
            chartPanel.setMinimumDrawWidth(10);
            chartPanel.setMaximumDrawWidth(2000);
            chartPanel.setMinimumDrawHeight(10);
            chartPanel.setMaximumDrawHeight(2000);
            
            return chartPanel;
        } catch (Exception e) {
            LOGGER.severe("Fehler beim Erstellen des Charts für " + providerName + ": " + e.getMessage());
            e.printStackTrace();
            
            // Im Fehlerfall Platzhalter zurückgeben
            JLabel errorLabel = new JLabel("Fehler beim Laden des Charts: " + e.getMessage());
            errorLabel.setForeground(Color.RED);
            
            JPanel errorPanel = new JPanel(new BorderLayout());
            errorPanel.add(errorLabel, BorderLayout.CENTER);
            errorPanel.setPreferredSize(new Dimension(950, 300));
            
            // Erstelle ein leeres Chart
            JFreeChart dummyChart = ChartFactory.createTimeSeriesChart(
                "Fehler", "Zeit", "Drawdown (%)", new TimeSeriesCollection(), true, true, false);
            return new ChartPanel(dummyChart);
        }
    }
    
    /**
     * Fügt horizontale Linien und farbliche Hintergrundmarkierungen zum Plot hinzu
     */
    private void addHorizontalLines(XYPlot plot, double maxValue) {
        // Farben für verschiedene Drawdown-Bereiche
        Color[] colors = {
            new Color(220, 255, 220), // 0-10% - Sehr hell grün
            new Color(200, 255, 200), // 10-20% - Hell grün
            new Color(255, 255, 200), // 20-30% - Hell gelb
            new Color(255, 220, 180), // 30-40% - Hell orange
            new Color(255, 200, 180)  // 40-50% - Hell rot
        };
        
        // Hintergrundmarkierungen für die Farbzonen
        for (int i = 0; i < colors.length && i * 10.0 < maxValue; i++) {
            double lowerBound = i * 10.0;
            double upperBound = (i + 1) * 10.0;
            
            // Nur Zonen bis zum Maximum anzeigen
            IntervalMarker marker = new IntervalMarker(lowerBound, Math.min(upperBound, maxValue));
            marker.setPaint(colors[i]);
            marker.setAlpha(0.3f); // Transparenz
            plot.addRangeMarker(marker, Layer.BACKGROUND);
        }
        
        // Horizontale Linien für jede 10%-Marke
        for (double i = 0; i <= maxValue; i += 10.0) {
            ValueMarker marker = new ValueMarker(i);
            marker.setPaint(Color.GRAY);
            marker.setStroke(new java.awt.BasicStroke(1.0f, 
                                                    java.awt.BasicStroke.CAP_BUTT, 
                                                    java.awt.BasicStroke.JOIN_MITER, 
                                                    1.0f, 
                                                    new float[] {3.0f, 3.0f}, 
                                                    0.0f)); // Gestrichelte Linie
            
            // Beschriftung für die Linie
            marker.setLabel(String.format("%.0f%%", i));
            marker.setLabelAnchor(RectangleAnchor.LEFT);
            marker.setLabelTextAnchor(TextAnchor.CENTER_LEFT);
            marker.setLabelFont(new Font("SansSerif", Font.PLAIN, 9));
            
            plot.addRangeMarker(marker);
        }
    }
    
    /**
     * Verarbeitet Drawdown-Daten aus der Datenbank
     */
    private boolean processDrawdownData(String drawdownData, TimeSeries drawdownSeries) {
        String[] lines = drawdownData.split("\n");
        
        LOGGER.info("Verarbeite " + lines.length + " Zeilen mit Drawdown-Daten");
        
        int processedLines = 0;
        String lastDate = null;
        int timeOffset = 0;
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            String[] parts = line.split(":");
            if (parts.length != 2) {
                continue;
            }
            
            String dateStr = parts[0].trim();
            String valueStr = parts[1].trim().replace("%", "").replace(",", ".");
            
            try {
                // Prüfen ob das Datum gleich dem vorherigen ist
                if (dateStr.equals(lastDate)) {
                    timeOffset += 1; // Inkrementiere für doppelte Daten
                } else {
                    timeOffset = 0; // Zurücksetzen für neues Datum
                    lastDate = dateStr;
                }
                
                // Parse Datum und Wert
                LocalDate date = LocalDate.parse(dateStr);
                LocalDateTime dateTime = date.atStartOfDay().plusSeconds(timeOffset);
                double value = Double.parseDouble(valueStr);
                
                // Zum Dataset hinzufügen
                Date javaDate = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
                drawdownSeries.addOrUpdate(new Day(javaDate), value);
                
                processedLines++;
            } catch (Exception e) {
                // Fehler beim Parsen ignorieren
            }
        }
        
        LOGGER.info("Drawdown-Daten verarbeitet: " + processedLines + " gültige Einträge");
        return processedLines > 0;
    }
    
    /**
     * Berechnet Drawdown-Daten aus den Trades
     */
    private void calculateDrawdownFromTrades(ProviderStats stats, TimeSeries drawdownSeries) {
        List<Trade> trades = stats.getTrades();
        if (trades == null || trades.isEmpty()) {
            LOGGER.warning("Keine Trades zum Berechnen des Drawdowns vorhanden");
            return;
        }
        
        // Trades nach Schließzeit sortieren
        List<Trade> sortedTrades = new ArrayList<>(trades);
        sortedTrades.sort((t1, t2) -> t1.getCloseTime().compareTo(t2.getCloseTime()));
        
        double currentBalance = stats.getInitialBalance();
        double peak = currentBalance;
        
        for (Trade trade : sortedTrades) {
            currentBalance += trade.getTotalProfit();
            
            if (currentBalance > peak) {
                peak = currentBalance;
            }
            
            if (peak > 0) {
                double drawdownPercent = ((peak - currentBalance) / peak) * 100.0;
                
                Date date = Date.from(trade.getCloseTime()
                    .atZone(ZoneId.systemDefault())
                    .toInstant());
                
                // Verwende Day statt Millisecond für bessere Performance
                drawdownSeries.addOrUpdate(new Day(date), drawdownPercent);
            }
        }
    }
    
    /**
     * Erstellt die Tabelle mit Provider-Daten und Drawdown-Charts
     */
    private JTable createProviderTable() {
        LOGGER.info("Erstelle Provider-Tabelle mit " + providers.size() + " Providern");
        
        // Erstelle ein Model mit zwei Spalten
        DefaultTableModel model = new DefaultTableModel(
            new String[]{"Signal Provider", "Drawdown Chart"}, 0);
        
        // Arrays für Objekte statt direktem Hinzufügen zum Model
        Object[][] data = new Object[providers.size()][2];
        int rowIndex = 0;
        
        // Füge für jeden Provider eine Zeile hinzu
        for (Map.Entry<String, ProviderStats> entry : providers.entrySet()) {
            String providerName = entry.getKey();
            
            // NULL-Check für den Namen
            if (providerName == null) {
                LOGGER.warning("Provider-Name ist null, wird übersprungen");
                continue;
            }
            
            ProviderStats stats = entry.getValue();
            
            // Überspringe Provider mit null-Stats
            if (stats == null) {
                LOGGER.warning("Provider " + providerName + " hat null ProviderStats, wird übersprungen");
                continue;
            }
            
            LOGGER.info("Füge Provider hinzu: " + providerName);
            
            // Provider-Name in erste Spalte
            data[rowIndex][0] = providerName;
            
            // Optimiertes Drawdown-Chart in zweite Spalte
            ChartPanel chartPanel = createCustomDrawdownChart(stats, providerName);
            data[rowIndex][1] = chartPanel;
            
            rowIndex++;
        }
        
        // Nur die tatsächlich gefüllten Zeilen zum Model hinzufügen
        for (int i = 0; i < rowIndex; i++) {
            model.addRow(new Object[]{data[i][0], data[i][1]});
        }
        
        // Prüfen ob nach dem Filtern noch Provider übrig sind
        if (model.getRowCount() == 0) {
            LOGGER.warning("Nach dem Hinzufügen sind keine Provider in der Tabelle");
            model.addRow(new Object[]{"KEINE PROVIDER GEFUNDEN", "KEINE CHART-DATEN VERFÜGBAR"});
        } else {
            LOGGER.info("Tabelle enthält " + model.getRowCount() + " Zeilen");
        }
        
        // Tabelle erstellen
        JTable table = new JTable(model) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Keine Zellen editierbar
            }
            
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 1) return Component.class; // Zweite Spalte enthält Komponenten (Charts)
                return Object.class;
            }
        };
        
        // Custom Renderer für die Chart-Spalte
        table.getColumnModel().getColumn(1).setCellRenderer(new ComponentRenderer());
        
        // Spaltenbreiten anpassen
        table.getColumnModel().getColumn(0).setPreferredWidth(300);
        table.getColumnModel().getColumn(1).setPreferredWidth(1200);
        
        // Zeilenhöhe anpassen
        table.setRowHeight(300);
        
        return table;
    }
    
    /**
     * Erstellt eine leere Tabelle mit Hinweismeldung
     */
    private JTable createEmptyTable() {
        LOGGER.info("Erstelle leere Tabelle mit Hinweismeldung");
        DefaultTableModel model = new DefaultTableModel(
            new String[]{"Signal Provider", "Drawdown Chart"}, 0);
        model.addRow(new Object[]{"KEINE PROVIDER GEFUNDEN", "KEINE CHART-DATEN VERFÜGBAR"});
        
        JTable table = new JTable(model);
        table.setRowHeight(50);
        return table;
    }
    
    /**
     * Renderer für Komponenten in der Tabelle (für die Chart-Spalte)
     */
    private static class ComponentRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                      boolean isSelected, boolean hasFocus, int row, int column) {
            return (Component) value;
        }
    }
}