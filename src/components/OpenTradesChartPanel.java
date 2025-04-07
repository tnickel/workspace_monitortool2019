package components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.LocalDateTime;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import data.Trade;

public class OpenTradesChartPanel extends JPanel {
    private final TradeChartPanel chartPanel;
    private final WebViewPanel webViewPanel;
    private JSplitPane splitPane;

    public OpenTradesChartPanel() {
        setLayout(new BorderLayout());
        
        // Chart Panel erstellen
        chartPanel = new TradeChartPanel();
        
        // Web View Panel erstellen
        webViewPanel = new WebViewPanel();
        
        // Split Pane erstellen
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(chartPanel);
        splitPane.setRightComponent(webViewPanel);
        splitPane.setResizeWeight(0.7);
        splitPane.setDividerLocation(0.7);
        
        // Layout zusammenbauen
        add(splitPane, BorderLayout.CENTER);
        
        // Initiale Größe setzen
        setPreferredSize(new Dimension(1200, 600));
        
        // Trade Selection Listener
        chartPanel.setTradeSelectionListener(trade -> {
            if (trade != null && trade.getSignalProviderURL() != null) {
                webViewPanel.loadURL(trade.getSignalProviderURL());
            }
        });
    }

    public void updateTrades(List<Trade> trades, LocalDateTime startTime) {
        chartPanel.updateTrades(trades, startTime);
    }
}