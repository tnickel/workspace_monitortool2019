package components;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import utils.UIStyleManager;

/**
 * Ein Panel, das einen animierten Ladeindikator mit Nachricht anzeigt
 */
public class LoadingPanel extends JPanel {
    private final Timer animationTimer;
    private int angle = 0;
    private JLabel messageLabel;
    
    /**
     * Erstellt ein neues Ladepanel mit der angegebenen Nachricht
     * 
     * @param message Die anzuzeigende Nachricht
     */
    public LoadingPanel(String message) {
        setLayout(new BorderLayout());
        setBackground(new Color(255, 255, 255, 220));
        
        messageLabel = new JLabel(message, SwingConstants.CENTER);
        messageLabel.setFont(UIStyleManager.REGULAR_FONT);
        
        add(messageLabel, BorderLayout.CENTER);
        
        animationTimer = new Timer(40, e -> {
            angle = (angle + 10) % 360;
            repaint();
        });
    }
    
    /**
     * Startet die Ladeanimation
     */
    public void start() {
        animationTimer.start();
    }
    
    /**
     * Stoppt die Ladeanimation
     */
    public void stop() {
        animationTimer.stop();
    }
    
    /**
     * Ändert die angezeigte Nachricht
     * 
     * @param message Die neue Nachricht
     */
    public void setMessage(String message) {
        messageLabel.setText(message);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2 - 30;
        int radius = 20;
        
        g2.setColor(UIStyleManager.SECONDARY_COLOR);
        g2.setStroke(new BasicStroke(4));
        
        g2.drawArc(centerX - radius, centerY - radius, 2 * radius, 2 * radius, angle, 270);
    }
}