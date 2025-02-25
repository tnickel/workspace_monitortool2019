package components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import data.Trade;

public class TradeChartPanel extends JPanel {
    private List<Trade> trades;
    private LocalDateTime startTime;
    private final int PADDING = 20;
    private final int ROW_HEIGHT = 30;
    private final Color BUY_COLOR = new Color(0, 150, 0);
    private final Color SELL_COLOR = new Color(200, 0, 0);
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    private ChartPanel chartPanel;
    private Trade selectedTrade;
    
    public interface TradeSelectionListener {
        void onTradeSelected(Trade trade);
    }
    
    private TradeSelectionListener selectionListener;

    public TradeChartPanel() {
        setLayout(new BorderLayout());
        chartPanel = new ChartPanel();
        JScrollPane scrollPane = new JScrollPane(chartPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    public void setTradeSelectionListener(TradeSelectionListener listener) {
        this.selectionListener = listener;
    }

    public void updateTrades(List<Trade> trades, LocalDateTime startTime) {
        this.trades = trades;
        this.startTime = startTime;
        chartPanel.trades = trades;
        chartPanel.startTime = startTime;
        
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
            
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    mousePosition = e.getPoint();
                    hoveredTrade = findTradeAtPosition(e.getPoint());
                    repaint();
                }
            });
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Trade clickedTrade = findTradeAtPosition(e.getPoint());
                    if (clickedTrade != null && selectionListener != null) {
                        selectedTrade = clickedTrade;
                        selectionListener.onTradeSelected(clickedTrade);
                        repaint();
                    }
                }
            });
            
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
                return String.format("<html>Symbol: %s<br>Type: %s<br>Lots: %.2f<br>Open: %s<br>Close: %s<br>Provider: %s</html>",
                    trade.getSymbol(),
                    trade.getType(),
                    trade.getLots(),
                    trade.getOpenTime().format(timeFormatter),
                    trade.getCloseTime().format(timeFormatter),
                    trade.getSignalProvider());
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

            drawGrid(g2, width, height);

            int y = PADDING;
            for (Trade trade : trades) {
                drawTrade(g2, trade, width, y);
                y += ROW_HEIGHT;
            }

            drawTimeAxis(g2, width, height);
            
            if (hoveredTrade != null) {
                int hoverY = PADDING + trades.indexOf(hoveredTrade) * ROW_HEIGHT;
                g2.setColor(new Color(240, 240, 255, 128));
                g2.fillRect(0, hoverY, getWidth(), ROW_HEIGHT);
            }
        }

        private void drawGrid(Graphics2D g2, int width, int height) {
            g2.setColor(new Color(240, 240, 240));
            
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

            for (int i = 0; i <= trades.size(); i++) {
                int y = PADDING + i * ROW_HEIGHT;
                g2.drawLine(PADDING, y, width + PADDING, y);
            }
        }

        private void drawTimeAxis(Graphics2D g2, int width, int height) {
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            
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
            
            int barHeight = (int)(ROW_HEIGHT * 0.6);
            barHeight *= (1 + Math.min(1.0, trade.getLots()));
            
            int yCenter = y + ROW_HEIGHT / 2;

            g2.setColor(trade.getType().equalsIgnoreCase("buy") ? BUY_COLOR : SELL_COLOR);
            g2.fillRect(x1, yCenter - barHeight/2, Math.max(x2 - x1, 2), barHeight);
            
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.PLAIN, 11));
            String tradeInfo = String.format("%s (%s)", trade.getSymbol(), trade.getSignalProvider());
            g2.drawString(tradeInfo, 5, yCenter + 5);
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