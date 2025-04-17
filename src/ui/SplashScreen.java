package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

public class SplashScreen extends JFrame {
    private JProgressBar progressBar;
    private JLabel statusLabel;
    
    public SplashScreen() {
        setUndecorated(true); // Keine Fensterrahmen
        setSize(400, 150);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        // Erstelle das Hauptpanel
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        panel.setBackground(Color.WHITE);
        
        // Titel
        JLabel titleLabel = new JLabel("MQL Analyzer wird geladen...");
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 16));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Fortschrittsbalken
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(350, 20));
        panel.add(progressBar, BorderLayout.CENTER);
        
        // Status-Text
        statusLabel = new JLabel("Initialisiere...");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(statusLabel, BorderLayout.SOUTH);
        
        setContentPane(panel);
    }
    
    public void setProgress(int value) {
        progressBar.setValue(value);
    }
    
    public void setStatus(String status) {
        statusLabel.setText(status);
    }
}