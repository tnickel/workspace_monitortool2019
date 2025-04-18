package components;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Ein Panel mit Fade-In-Animation für visuelle Übergänge
 */
public class AnimatedPanel extends JPanel {
    private float alpha = 0.0f;
    private Timer fadeInTimer;
    
    public AnimatedPanel() {
        setOpaque(false);
        
        fadeInTimer = new Timer(20, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                alpha += 0.05f;
                if (alpha >= 1.0f) {
                    alpha = 1.0f;
                    fadeInTimer.stop();
                }
                repaint();
            }
        });
    }
    
    /**
     * Startet die Fade-In-Animation
     */
    public void startAnimation() {
        alpha = 0.0f;
        fadeInTimer.start();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
        super.paintComponent(g2);
        g2.dispose();
    }
}