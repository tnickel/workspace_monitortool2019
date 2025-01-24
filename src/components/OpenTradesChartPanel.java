package components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import data.Trade;

public class OpenTradesChartPanel extends JPanel {
    private List<Trade> trades;
    private LocalDateTime startTime;
    private final int PADDING = 20;
    private final int ROW_HEIGHT = 30;
    private final Color BUY_COLOR = new Color(0, 150, 0);
    private final Color SELL_COLOR = new Color(200, 0, 0);
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private JScrollPane scrollPane;
    private ChartPanel chartPanel;
    private JFrame detailFrame;

    public OpenTradesChartPanel() {
        setLayout(new BorderLayout());
        
        // Chart Panel erstellen
        chartPanel = new ChartPanel();
        
        // Scroll Panel erstellen
        scrollPane = new JScrollPane(chartPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        // Button für Detailansicht
        JButton detailButton = new JButton("Show Detailed View");
        detailButton.addActionListener(e -> showDetailedView());
        
        // Layout zusammenbauen
        add(scrollPane, BorderLayout.CENTER);
        add(detailButton, BorderLayout.SOUTH);
    }

    private void showDetailedView() {
        if (detailFrame == null || !detailFrame.isVisible()) {
            detailFrame = new JFrame("Detailed Trade Timeline");
            detailFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            
            // Neues ChartPanel für die Detailansicht
            ChartPanel detailChartPanel = new ChartPanel();
            detailChartPanel.trades = this.trades;
            detailChartPanel.startTime = this.startTime;
            
            JScrollPane detailScrollPane = new JScrollPane(detailChartPanel);
            detailScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            detailScrollPane.getVerticalScrollBar().setUnitIncrement(16);
            
            detailFrame.add(detailScrollPane);
            
            // Bildschirmgröße ermitteln und Fenster anpassen
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int width = (int)(screenSize.width * 0.8);
            int height = (int)(screenSize.height * 0.8);
            detailFrame.setSize(width, height);
            
            detailFrame.setLocationRelativeTo(this);
            detailFrame.setVisible(true);
        } else {
            detailFrame.toFront();
        }
    }

    public void updateTrades(List<Trade> trades, LocalDateTime startTime) {
        this.trades = trades;
        this.startTime = startTime;
        chartPanel.trades = trades;
        chartPanel.startTime = startTime;
        
        // Höhe des ChartPanels an Anzahl der Trades anpassen
        int preferredHeight = Math.max(300, trades.size() * ROW_HEIGHT + 2 * PADDING);
        chartPanel.setPreferredSize(new Dimension(0, preferredHeight));
        
        revalidate();
        repaint();
    }

    private class ChartPanel extends JPanel {
        private List<Trade> trades;
        private LocalDateTime startTime;
        private Trade hoveredTrade;
        private Point mousePosition;

        public ChartPanel() {
            setBackground(Color.WHITE);
            
            // Mouse Listener für Tooltip
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    mousePosition = e.getPoint();
                    hoveredTrade = findTradeAtPosition(e.getPoint());
                    repaint();
                }
            });
            
            // Custom Tooltip
            ToolTipManager.sharedInstance().registerComponent(this);
        }

        private Trade findTradeAtPosition(Point p) {
            if (trades == null || trades.isEmpty()) return null;

            int y = PADDING;
            for (Trade trade : trades) {
                if (p.y >= y && p.y < y + ROW_HEIGHT) {
                    return trade;
                }
                y += ROW_HEIGHT;
            }
            return null;
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            Trade trade = findTradeAtPosition(e.getPoint());
            if (trade != null) {
                return String.format("<html>Symbol: %s<br>Type: %s<br>Lots: %.2f<br>Open: %s<br>Close: %s</html>",
                    trade.getSymbol(),
                    trade.getType(),
                    trade.getLots(),
                    trade.getOpenTime().format(timeFormatter),
                    trade.getCloseTime().format(timeFormatter));
            }
            return null;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (trades == null || trades.isEmpty()) return;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth() - 2 * PADDING;
            int height = getHeight() - 2 * PADDING;

            // Gitternetzlinien zeichnen
            drawGrid(g2, width, height);

            // Trades zeichnen
            int y = PADDING;
            for (Trade trade : trades) {
                drawTrade(g2, trade, width, y);
                y += ROW_HEIGHT;
            }

            // Zeitachse zeichnen
            drawTimeAxis(g2, width, height);
        }

        private void drawGrid(Graphics2D g2, int width, int height) {
            g2.setColor(new Color(240, 240, 240));
            // Vertikale Linien
            LocalDateTime earliest = startTime;
            LocalDateTime latest = trades.stream()
                .map(Trade::getCloseTime)
                .max(LocalDateTime::compareTo)
                .orElse(startTime.plusDays(1));

            int timeRange = (int) java.time.Duration.between(earliest, latest).toHours();
            int markInterval = Math.max(1, timeRange / 10);

            for (int i = 0; i <= timeRange; i += markInterval) {
                int x = PADDING + (int)(i * width / timeRange);
                g2.drawLine(x, PADDING, x, height + PADDING);
            }

            // Horizontale Linien
            for (int i = 0; i <= trades.size(); i++) {
                int y = PADDING + i * ROW_HEIGHT;
                g2.drawLine(PADDING, y, width + PADDING, y);
            }
        }

        private void drawTimeAxis(Graphics2D g2, int width, int height) {
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            
            // Zeitmarken zeichnen
            LocalDateTime earliest = startTime;
            LocalDateTime latest = trades.stream()
                .map(Trade::getCloseTime)
                .max(LocalDateTime::compareTo)
                .orElse(startTime.plusDays(1));

            int timeRange = (int) java.time.Duration.between(earliest, latest).toHours();
            int markInterval = Math.max(1, timeRange / 10);

            for (int i = 0; i <= timeRange; i += markInterval) {
                LocalDateTime markTime = earliest.plusHours(i);
                int x = PADDING + (int)(i * width / timeRange);
                g2.drawString(markTime.format(timeFormatter), x - 25, height + PADDING + 15);
            }
        }

        private void drawTrade(Graphics2D g2, Trade trade, int width, int y) {
            long startDiff = java.time.Duration.between(startTime, trade.getOpenTime()).toMinutes();
            long duration = java.time.Duration.between(trade.getOpenTime(), trade.getCloseTime()).toMinutes();
            
            int totalMinutes = (int) java.time.Duration.between(
                startTime, 
                trades.stream()
                    .map(Trade::getCloseTime)
                    .max(LocalDateTime::compareTo)
                    .orElse(startTime.plusDays(1))
            ).toMinutes();

            int x1 = PADDING + (int)(startDiff * width / totalMinutes);
            int x2 = PADDING + (int)((startDiff + duration) * width / totalMinutes);
            
            // Mindesthöhe für die Balken festlegen
            int barHeight = (int)(ROW_HEIGHT * 0.6);
            // Zusätzliche Höhe basierend auf Lots (max. doppelte Höhe)
            barHeight *= (1 + Math.min(1.0, trade.getLots()));
            
            int yCenter = y + ROW_HEIGHT / 2;

            // Trade-Typ-abhängige Farbe
            g2.setColor(trade.getType().equalsIgnoreCase("buy") ? BUY_COLOR : SELL_COLOR);
            
            // Balken zeichnen
            g2.fillRect(x1, yCenter - barHeight/2, Math.max(x2 - x1, 2), barHeight);
            
            // Symbol und Details
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.PLAIN, 11));
            g2.drawString(trade.getSymbol(), 5, yCenter + 5);
        }

        @Override
        public Dimension getPreferredSize() {
            if (trades == null || trades.isEmpty()) {
                return new Dimension(800, 300);
            }
            return new Dimension(800, trades.size() * ROW_HEIGHT + 2 * PADDING);
        }
    }
}