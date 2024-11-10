import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.stream.Stream;

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
                                LocalDateTime tradeDateTime = null;
                                if (data.length > 0 && !data[0].isEmpty()) {
                                    try {
                                        tradeDateTime = LocalDateTime.parse(data[0], DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss")); // Assuming trade date is in the first column
                                    } catch (Exception e) {
                                        System.err.println("Invalid date format: " + data[0]);
                                    }
                                }

                                signalProviderStats.putIfAbsent(signalProviderName, new ProviderStats());
                                ProviderStats stats = signalProviderStats.get(signalProviderName);
                                stats.incrementTradeCount();
                                if (tradeDateTime != null) {
                                    stats.updateDates(tradeDateTime);
                                }
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

        String[] columnNames = {"Nr.", "Signal Provider Name", "Anzahl der Trades", "Startdatum", "Enddatum", "Tage zwischen Start und Ende"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        int index = 1;
        for (Map.Entry<String, ProviderStats> entry : signalProviderStats.entrySet()) {
            ProviderStats stats = entry.getValue();
            long daysBetween = stats.getDaysBetween();
            model.addRow(new Object[]{index++, entry.getKey(), stats.getTradeCount(), stats.getStartDate(), stats.getEndDate(), daysBetween});
        }

        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        frame.add(scrollPane);

        frame.setVisible(true);
    }
}

class ProviderStats {
    private int tradeCount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    public void incrementTradeCount() {
        tradeCount++;
    }

    public int getTradeCount() {
        return tradeCount;
    }

    public void updateDates(LocalDateTime tradeDateTime) {
        if (startDate == null || tradeDateTime.isBefore(startDate)) {
            startDate = tradeDateTime;
        }
        if (endDate == null || tradeDateTime.isAfter(endDate)) {
            endDate = tradeDateTime;
        }
    }

    public String getStartDate() {
        return startDate != null ? startDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss")) : "";
    }

    public String getEndDate() {
        return endDate != null ? endDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss")) : "";
    }

    public long getDaysBetween() {
        if (startDate != null && endDate != null) {
            return Duration.between(startDate, endDate).toDays();
        }
        return 0;
    }
}
