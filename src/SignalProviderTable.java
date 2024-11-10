import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.stream.Stream;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

public class SignalProviderTable {
    public static void main(String[] args) {
        String rootPath = "c:\\tmp\\mql5";
        String downloadPath = rootPath + "\\download";

        Map<String, ProviderStats> signalProviderStats = new HashMap<>();
        File downloadDirectory = new File(downloadPath);

        if (downloadDirectory.exists() && downloadDirectory.isDirectory()) {
            File[] files = downloadDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        try (Stream<String> lines = Files.lines(Path.of(file.getPath()))) {
                            Iterator<String> iterator = lines.iterator();
                            if (iterator.hasNext()) iterator.next(); // Skip the header line
                            iterator.forEachRemaining(line -> {
                                if (line.trim().isEmpty() || line.startsWith("Time")) return; // Skip header line or empty line
                                String[] data = line.split(";", -1); // Adjust delimiter if necessary
                                String signalProviderName = file.getName(); // Use filename as the signal provider name
                                LocalDate tradeDate = null;
                                double profit = 0.0;
                                if (data.length > 0 && !data[0].isEmpty() && !line.contains("Balance")) {
                                    try {
                                        tradeDate = LocalDate.parse(data[0].substring(0, 10), DateTimeFormatter.ofPattern("yyyy.MM.dd")); // Assuming trade date is in the first column
                                    } catch (Exception e) {
                                        System.err.println("Invalid date format: " + data[0]);
                                    }
                                }
                                if (data.length > 8 && !data[8].isEmpty()) {
                                    try {
                                        profit = Double.parseDouble(data[8]);
                                    } catch (NumberFormatException e) {
                                        System.err.println("Invalid profit format: " + data[8]);
                                    }
                                }

                                signalProviderStats.putIfAbsent(signalProviderName, new ProviderStats());
                                ProviderStats stats = signalProviderStats.get(signalProviderName);
                                stats.incrementTradeCount();
                                if (tradeDate != null) {
                                    stats.updateDates(tradeDate);
                                }
                                stats.addProfit(profit);
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        SwingUtilities.invokeLater(() -> createAndShowGUI(signalProviderStats));
    }

    private static void createAndShowGUI(Map<String, ProviderStats> signalProviderStats) {
        JFrame frame = new JFrame("Signal Providers Trade List");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 400);

        String[] columnNames = {"Nr.", "Signal Provider Name", "Anzahl der Trades", "Startdatum", "Enddatum", "Tage zwischen Start und Ende", "Gesamtgewinn"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 0:
                    case 2:
                    case 5:
                        return Integer.class;
                    case 3:
                    case 4:
                        return LocalDate.class;
                    case 6:
                        return Double.class;
                    default:
                        return String.class;
                }
            }
        };

        int index = 1;
        for (Map.Entry<String, ProviderStats> entry : signalProviderStats.entrySet()) {
            ProviderStats stats = entry.getValue();
            long daysBetween = stats.getDaysBetween();
            double totalProfit = stats.getTotalProfit();
            model.addRow(new Object[]{index++, entry.getKey(), stats.getTradeCount(), stats.getStartDate(), stats.getEndDate(), (int) daysBetween, totalProfit});
        }

        JTable table = new JTable(model);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        JScrollPane scrollPane = new JScrollPane(table);
        frame.add(scrollPane);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row != -1) {
                        String signalProviderName = (String) table.getValueAt(row, 1);
                        ProviderStats stats = signalProviderStats.get(signalProviderName);
                        showProfitCurve(signalProviderName, stats);
                    }
                }
            }
        });

        frame.setVisible(true);
    }

    private static void showProfitCurve(String signalProviderName, ProviderStats stats) {
        JFrame chartFrame = new JFrame("Profit Curve for " + signalProviderName);
        chartFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        chartFrame.setSize(800, 600);

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        double cumulativeProfit = 0.0;
        for (int i = 0; i < stats.getProfits().size(); i++) {
            cumulativeProfit += stats.getProfits().get(i);
            dataset.addValue(cumulativeProfit, "Profit", "Trade " + (i + 1));
        }

        JFreeChart chart = ChartFactory.createLineChart(
                "Profit Curve",
                "Trade Number",
                "Cumulative Profit",
                dataset
        );

        ChartPanel chartPanel = new ChartPanel(chart);
        chartFrame.add(chartPanel);
        chartFrame.setVisible(true);
    }
}

class ProviderStats {
    private int tradeCount;
    private LocalDate startDate;
    private LocalDate endDate;
    private java.util.List<Double> profits = new java.util.ArrayList<>();

    public void incrementTradeCount() {
        tradeCount++;
    }

    public int getTradeCount() {
        return tradeCount;
    }

    public void updateDates(LocalDate tradeDate) {
        if (startDate == null || tradeDate.isBefore(startDate)) {
            startDate = tradeDate;
        }
        if (endDate == null || tradeDate.isAfter(endDate)) {
            endDate = tradeDate;
        }
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public long getDaysBetween() {
        if (startDate != null && endDate != null) {
            return Duration.between(startDate.atStartOfDay(), endDate.atStartOfDay()).toDays();
        }
        return 0;
    }

    public void addProfit(double profit) {
        profits.add(profit);
    }

    public java.util.List<Double> getProfits() {
        return profits;
    }

    public double getTotalProfit() {
        return profits.stream().mapToDouble(Double::doubleValue).sum();
    }
}
