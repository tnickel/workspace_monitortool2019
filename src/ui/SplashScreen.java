package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

public class SplashScreen extends JFrame {
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JLabel titleLabel;
    
    // Definierte Farben
    private static final Color PRIMARY_COLOR = new Color(26, 45, 90); // #1A2D5A - Dunkelblau
    private static final Color SECONDARY_COLOR = new Color(62, 125, 204); // #3E7DCC - Helleres Blau
    private static final Color ACCENT_COLOR = new Color(255, 209, 102); // #FFD166 - Gold/Gelb
    
    public SplashScreen() {
        setUndecorated(true); // Keine Fensterrahmen
        setSize(500, 220);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        // Erstelle das Hauptpanel mit Farbverlauf
        GradientPanel panel = new GradientPanel();
        panel.setLayout(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SECONDARY_COLOR, 2),
                BorderFactory.createEmptyBorder(25, 25, 25, 25)));
        
        // Titel mit besserem Styling
        titleLabel = new JLabel("MQL Analyzer wird geladen...");
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Verbesserter Fortschrittsbalken
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(450, 25));
        progressBar.setBackground(new Color(230, 230, 230));
        progressBar.setForeground(ACCENT_COLOR);
        progressBar.setBorder(BorderFactory.createEmptyBorder());
        progressBar.setFont(new Font("Dialog", Font.BOLD, 12));
        panel.add(progressBar, BorderLayout.CENTER);
        
        // Status-Text mit besserem Styling
        statusLabel = new JLabel("Initialisiere...");
        statusLabel.setFont(new Font("Dialog", Font.PLAIN, 14));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(statusLabel, BorderLayout.SOUTH);
        
        setContentPane(panel);
    }
    
    // Panel mit Farbverlauf für einen schönen Hintergrund
    private class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            
            GradientPaint gradient = new GradientPaint(
                0, 0, PRIMARY_COLOR, 
                0, getHeight(), SECONDARY_COLOR
            );
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }
    
    public void setProgress(int value) {
        progressBar.setValue(value);
    }
    
    public void setStatus(String status) {
        statusLabel.setText(status);
    }
}