package ui;



import data.ProviderStats;
import components.TradeListTable;
import javax.swing.*;
import java.awt.*;

public class TradeListFrame extends JFrame {
    private final TradeListTable tradeTable;
    
    public TradeListFrame(String providerName, ProviderStats stats) {
        super("Trade List: " + providerName);
        this.tradeTable = new TradeListTable(stats);
        initializeUI();
    }
    
    private void initializeUI() {
        setSize(800, 600);
        add(new JScrollPane(tradeTable));
        setLocationRelativeTo(null);
    }
}