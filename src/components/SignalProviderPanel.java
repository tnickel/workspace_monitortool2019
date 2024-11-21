package components;

import java.awt.Desktop;
import java.net.URI;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import org.jfree.chart.ChartPanel;

public class SignalProviderPanel extends JPanel {
    private final String providerId;
    
    public SignalProviderPanel(ChartPanel chartPanel, String fileName) {
        setLayout(new BorderLayout());
        add(chartPanel, BorderLayout.CENTER);
        
        // Extract ID from filename (assuming format ProviderName_ID.csv)
        providerId = fileName.substring(fileName.lastIndexOf("_") + 1).replace(".csv", "");
        
        // Create URL panel
        JPanel urlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel urlLabel = new JLabel("<html><u>https://www.mql5.com/de/signals/" + 
            providerId + "?source=Site+Signals+Subscriptions#!tab=account</u></html>");
        urlLabel.setForeground(Color.BLUE);
        urlLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        urlLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://www.mql5.com/de/signals/" + 
                        providerId + "?source=Site+Signals+Subscriptions#!tab=account"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        
        urlPanel.add(urlLabel);
        add(urlPanel, BorderLayout.SOUTH);
    }
}