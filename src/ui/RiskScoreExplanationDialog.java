package ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class RiskScoreExplanationDialog extends JDialog {
    
    public RiskScoreExplanationDialog(JFrame parent) {
        super(parent, "Risk Score Explanation", true);
        
        // Create main panel with vertical BoxLayout
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Add title
        JLabel titleLabel = new JLabel("Risk Score Calculation");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        // Add explanation text
        String explanation = "<html><body style='width: 400px; padding: 5px'>" +
            "<p>The Risk Score is a comprehensive metric that evaluates the overall risk level " +
            "of a trading strategy. It combines multiple risk factors into a single score " +
            "ranging from 0 (lowest risk) to 100 (highest risk).</p>" +
            
            "<h2>Components of the Risk Score:</h2>" +
            
            "<h3>1. Drawdown Risk (40% weight)</h3>" +
            "<p>- Maximum drawdown percentage<br>" +
            "- Duration of drawdowns<br>" +
            "- Frequency of significant drawdowns</p>" +
            
            "<h3>2. Position Risk (30% weight)</h3>" +
            "<p>- Maximum number of concurrent trades<br>" +
            "- Maximum concurrent lots<br>" +
            "- Average position size relative to account balance</p>" +
            
            "<h3>3. Trade Management Risk (30% weight)</h3>" +
            "<p>- Risk/Reward ratio<br>" +
            "- Win rate consistency<br>" +
            "- Average holding time<br>" +
            "- Trading frequency</p>" +
            
            "<h2>Calculation Formula:</h2>" +
            "<p>Risk Score = (0.4 � Drawdown Risk) + (0.3 � Position Risk) + " +
            "(0.3 � Trade Management Risk)</p>" +
            
            "<h2>Risk Level Classification:</h2>" +
            "<p>0-20: Very Low Risk<br>" +
            "21-40: Low Risk<br>" +
            "41-60: Moderate Risk<br>" +
            "61-80: High Risk<br>" +
            "81-100: Very High Risk</p>" +
            
            "<h2>Important Considerations:</h2>" +
            "<p>- The score is calculated using historical data and may not predict future risk<br>" +
            "- Market conditions and volatility can affect the risk level<br>" +
            "- Regular monitoring and adjustment of risk parameters is recommended</p>" +
            "</body></html>";
            
        JLabel explanationLabel = new JLabel(explanation);
        explanationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Add explanation to a scroll pane
        JScrollPane scrollPane = new JScrollPane(explanationLabel);
        scrollPane.setPreferredSize(new Dimension(450, 400));
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(scrollPane);
        
        // Add OK button
        JButton okButton = new JButton("OK");
        okButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        okButton.addActionListener(e -> dispose());
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(okButton);
        
        // Add panel to dialog
        add(mainPanel);
        
        // Configure dialog
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(parent);
    }
}