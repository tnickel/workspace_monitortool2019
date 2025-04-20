package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

import utils.UIStyle;

/**
 * Dialog mit Fortschrittsanzeige für langwierige Operationen
 */
public class LoadingDialog extends JDialog {
    private JProgressBar progressBar;
    private JLabel statusLabel;
    
    /**
     * Erstellt einen neuen Loading-Dialog
     * 
     * @param parent Das übergeordnete Fenster
     * @param title Der Titel des Dialogs
     * @param message Die initial anzuzeigende Nachricht
     */
    public LoadingDialog(Frame parent, String title, String message) {
        super(parent, title, true);
        
        setUndecorated(true); // Keine Fensterrahmen
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIStyle.SECONDARY_COLOR, 2),
            BorderFactory.createEmptyBorder(20, 25, 20, 25)
        ));
        mainPanel.setBackground(Color.WHITE);
        
        // Status-Label
        statusLabel = new JLabel(message, SwingConstants.CENTER);
        statusLabel.setFont(new Font("Dialog", Font.BOLD, 14));
        statusLabel.setForeground(UIStyle.PRIMARY_COLOR);
        mainPanel.add(statusLabel, BorderLayout.NORTH);
        
        // Fortschrittsbalken
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(350, 25));
        progressBar.setBackground(new Color(230, 230, 230));
        progressBar.setForeground(UIStyle.SECONDARY_COLOR);
        progressBar.setBorder(BorderFactory.createEmptyBorder());
        progressBar.setFont(new Font("Dialog", Font.BOLD, 12));
        
        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.setBackground(Color.WHITE);
        progressPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));
        progressPanel.add(progressBar, BorderLayout.CENTER);
        mainPanel.add(progressPanel, BorderLayout.CENTER);
        
        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(parent);
    }
    
    /**
     * Setzt den Fortschritt in Prozent (0-100)
     */
    public void setProgress(int progress) {
        progressBar.setValue(progress);
    }
    
    /**
     * Aktualisiert die Statusnachricht
     */
    public void setStatus(String status) {
        statusLabel.setText(status);
    }
    
    /**
     * Markiert den Vorgang als abgeschlossen und schließt den Dialog
     */
    public void complete() {
        progressBar.setValue(100);
        dispose();
    }
}