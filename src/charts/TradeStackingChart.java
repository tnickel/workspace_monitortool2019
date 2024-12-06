package charts;

import data.Trade;
import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TradeStackingChart extends JPanel {
   private final List<Trade> trades;
   private static final int HEIGHT = 400;
   private static final int WIDTH = 950;
   private static final int PADDING = 50;
   private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

   public TradeStackingChart(List<Trade> trades) {
       this.trades = new ArrayList<>(trades);
       this.trades.sort((t1, t2) -> t1.getOpenTime().compareTo(t2.getOpenTime()));
       setPreferredSize(new Dimension(WIDTH, HEIGHT));
   }

   @Override
   protected void paintComponent(Graphics g) {
       super.paintComponent(g);
       Graphics2D g2 = (Graphics2D) g;
       g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

       if (trades.isEmpty()) return;

       // Weiﬂer Hintergrund
       g2.setColor(Color.WHITE);
       g2.fillRect(0, 0, getWidth(), getHeight());
       g2.setColor(Color.BLACK);

       List<Trade> activeTrades = new ArrayList<>();
       int maxTrades = 0;
       double maxLots = 0;

       // Maxima berechnen
       for (Trade trade : trades) {
           activeTrades.removeIf(t -> t.getCloseTime().compareTo(trade.getOpenTime()) <= 0);
           activeTrades.add(trade);
           maxTrades = Math.max(maxTrades, activeTrades.size());
           maxLots = Math.max(maxLots, activeTrades.stream().mapToDouble(Trade::getLots).sum());
       }

       // Minimumwerte setzen
       maxTrades = Math.max(1, maxTrades); 
       maxLots = Math.max(0.1, maxLots);

       // Y-Achsen-Beschriftung
       int ySteps = maxTrades + 1;
       double lotStep = maxLots / ySteps;
       for (int i = 0; i <= ySteps; i++) {
           int y = HEIGHT - PADDING - (i * (HEIGHT - 2*PADDING) / ySteps);
           g2.setColor(Color.LIGHT_GRAY);
           g2.drawLine(PADDING, y, WIDTH-PADDING, y);
           g2.setColor(Color.BLACK);
           g2.drawString(String.format("%d", i), 5, y);                    // Trades-Skala
           g2.drawString(String.format("%.2f", i * lotStep), WIDTH-45, y); // Lots-Skala
       }

       // Achsen zeichnen
       g2.setColor(Color.BLACK);
       g2.drawLine(PADDING, HEIGHT-PADDING, WIDTH-PADDING, HEIGHT-PADDING); // X-Achse
       g2.drawLine(PADDING, PADDING, PADDING, HEIGHT-PADDING); // Y-Achse

       // Balken zeichnen
       activeTrades.clear();
       int barWidth = Math.max(5, (WIDTH - 2*PADDING) / trades.size());
       int x = PADDING;

       for (Trade trade : trades) {
           activeTrades.removeIf(t -> t.getCloseTime().compareTo(trade.getOpenTime()) <= 0);
           activeTrades.add(trade);
           
           int currentTrades = activeTrades.size();
           double currentLots = activeTrades.stream().mapToDouble(Trade::getLots).sum();
           
           int tradeHeight = (int)((HEIGHT - 2*PADDING) * (currentTrades / (double)maxTrades));
           int lotHeight = (int)((HEIGHT - 2*PADDING) * (currentLots / maxLots));
           
           // Trades-Balken (blau)
           g2.setColor(new Color(100, 100, 255, 200));
           g2.fillRect(x, HEIGHT-PADDING-tradeHeight, barWidth/2, tradeHeight);
           
           // Lots-Balken (rot)
           g2.setColor(new Color(255, 100, 100, 200));
           g2.fillRect(x + barWidth/2, HEIGHT-PADDING-lotHeight, barWidth/2, lotHeight);

           // Beschriftung bei Maxima
           if (currentTrades == maxTrades || Math.abs(currentLots - maxLots) < 0.001) {
               g2.setColor(Color.BLACK);
               String label = String.format("Trades: %d, Lots: %.2f", currentTrades, currentLots);
               g2.drawString(label, x, HEIGHT-PADDING-Math.max(tradeHeight, lotHeight)-5);
               g2.drawString(trade.getOpenTime().format(DATE_FORMATTER), x-20, HEIGHT-PADDING+15);
           }

           x += barWidth;
       }

       // Legende
       g2.setColor(new Color(100, 100, 255, 200));
       g2.fillRect(WIDTH-120, 20, 20, 20);
       g2.setColor(new Color(255, 100, 100, 200));
       g2.fillRect(WIDTH-120, 50, 20, 20);
       g2.setColor(Color.BLACK);
       g2.drawString("Trades", WIDTH-90, 35);
       g2.drawString("Lots", WIDTH-90, 65);

       // Aktuelle Werte
       String currentStats = String.format("Current - Trades: %d, Lots: %.2f", 
           activeTrades.size(), 
           activeTrades.stream().mapToDouble(Trade::getLots).sum());
       g2.drawString(currentStats, PADDING + 10, 30);
   }
}