package ui;

import javax.swing.*;
import java.awt.*;
import models.FilterCriteria;

public class FilterDialog extends JDialog {
    private FilterCriteria result = null;
    private static FilterCriteria lastCriteria = null;
    private final JTextField minTradeDaysField = new JTextField(10);
    private final JTextField minProfitField = new JTextField(10);
    private final JTextField minProfitFactorField = new JTextField(10);
    private final JTextField minWinRateField = new JTextField(10);
    private final JTextField maxDrawdownField = new JTextField(10);
    private final JTextField minTotalProfitField = new JTextField(10);
    private final JTextField maxConcurrentTradesField = new JTextField(10);
    private final JTextField maxConcurrentLotsField = new JTextField(10);
    
    public FilterDialog(JFrame parent) {
        super(parent, "Filter Signal Providers", true);
        
        // Panel für die Filter-Kriterien
        JPanel filterPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Zeile 1: Minimum Trade Days
        gbc.gridx = 0; gbc.gridy = 0;
        filterPanel.add(new JLabel("Min. Trade Days:"), gbc);
        gbc.gridx = 1;
        filterPanel.add(minTradeDaysField, gbc);
        
        // Zeile 2: Minimum Profit
        gbc.gridx = 0; gbc.gridy = 1;
        filterPanel.add(new JLabel("Min. Profit:"), gbc);
        gbc.gridx = 1;
        filterPanel.add(minProfitField, gbc);
        
        // Zeile 3: Minimum Total Profit
        gbc.gridx = 0; gbc.gridy = 2;
        filterPanel.add(new JLabel("Min. Total Profit:"), gbc);
        gbc.gridx = 1;
        filterPanel.add(minTotalProfitField, gbc);
        
        // Zeile 4: Minimum Profit Factor
        gbc.gridx = 0; gbc.gridy = 3;
        filterPanel.add(new JLabel("Min. Profit Factor:"), gbc);
        gbc.gridx = 1;
        filterPanel.add(minProfitFactorField, gbc);
        
        // Zeile 5: Minimum Win Rate
        gbc.gridx = 0; gbc.gridy = 4;
        filterPanel.add(new JLabel("Min. Win Rate (%):"), gbc);
        gbc.gridx = 1;
        filterPanel.add(minWinRateField, gbc);
        
        // Zeile 6: Maximum Drawdown
        gbc.gridx = 0; gbc.gridy = 5;
        filterPanel.add(new JLabel("Max. Drawdown (%):"), gbc);
        gbc.gridx = 1;
        filterPanel.add(maxDrawdownField, gbc);
        
        // Zeile 7: Max Concurrent Trades
        gbc.gridx = 0; gbc.gridy = 6;
        filterPanel.add(new JLabel("Max Concurrent Trades:"), gbc);
        gbc.gridx = 1;
        filterPanel.add(maxConcurrentTradesField, gbc);
        
        // Zeile 8: Max Concurrent Lots
        gbc.gridx = 0; gbc.gridy = 7;
        filterPanel.add(new JLabel("Max Concurrent Lots:"), gbc);
        gbc.gridx = 1;
        filterPanel.add(maxConcurrentLotsField, gbc);
        
        // Button Panel
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        
        okButton.addActionListener(e -> {
            if (validateAndSetResult()) {
                lastCriteria = result;  // Speichern der letzten Werte
                dispose();
            }
        });
        
        cancelButton.addActionListener(e -> {
            result = null;
            dispose();
        });
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        // Main Layout
        setLayout(new BorderLayout());
        add(filterPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Letzte Werte setzen, falls vorhanden
        restoreLastValues();
        
        pack();
        setLocationRelativeTo(parent);
    }
    
    private void restoreLastValues() {
        if (lastCriteria != null) {
            if (lastCriteria.getMinTradeDays() > 0) {
                minTradeDaysField.setText(String.valueOf(lastCriteria.getMinTradeDays()));
            }
            if (lastCriteria.getMinProfit() > 0) {
                minProfitField.setText(String.valueOf(lastCriteria.getMinProfit()));
            }
            if (lastCriteria.getMinTotalProfit() > 0) {
                minTotalProfitField.setText(String.valueOf(lastCriteria.getMinTotalProfit()));
            }
            if (lastCriteria.getMinProfitFactor() > 0) {
                minProfitFactorField.setText(String.valueOf(lastCriteria.getMinProfitFactor()));
            }
            if (lastCriteria.getMinWinRate() > 0) {
                minWinRateField.setText(String.valueOf(lastCriteria.getMinWinRate()));
            }
            if (lastCriteria.getMaxDrawdown() < 100) {
                maxDrawdownField.setText(String.valueOf(lastCriteria.getMaxDrawdown()));
            }
            if (lastCriteria.getMaxConcurrentTrades() < Integer.MAX_VALUE) {
                maxConcurrentTradesField.setText(String.valueOf(lastCriteria.getMaxConcurrentTrades()));
            }
            if (lastCriteria.getMaxConcurrentLots() < Double.MAX_VALUE) {
                maxConcurrentLotsField.setText(String.valueOf(lastCriteria.getMaxConcurrentLots()));
            }
        }
    }
    
    private boolean validateAndSetResult() {
        try {
            FilterCriteria criteria = new FilterCriteria();
            
            if (!minTradeDaysField.getText().isEmpty()) {
                criteria.setMinTradeDays(Integer.parseInt(minTradeDaysField.getText().trim()));
            }
            
            if (!minProfitField.getText().isEmpty()) {
                criteria.setMinProfit(Double.parseDouble(minProfitField.getText().trim()));
            }
            
            if (!minTotalProfitField.getText().isEmpty()) {
                criteria.setMinTotalProfit(Double.parseDouble(minTotalProfitField.getText().trim()));
            }
            
            if (!minProfitFactorField.getText().isEmpty()) {
                criteria.setMinProfitFactor(Double.parseDouble(minProfitFactorField.getText().trim()));
            }
            
            if (!minWinRateField.getText().isEmpty()) {
                criteria.setMinWinRate(Double.parseDouble(minWinRateField.getText().trim()));
            }
            
            if (!maxDrawdownField.getText().isEmpty()) {
                criteria.setMaxDrawdown(Double.parseDouble(maxDrawdownField.getText().trim()));
            }
            
            if (!maxConcurrentTradesField.getText().isEmpty()) {
                criteria.setMaxConcurrentTrades(Integer.parseInt(maxConcurrentTradesField.getText().trim()));
            }
            
            if (!maxConcurrentLotsField.getText().isEmpty()) {
                criteria.setMaxConcurrentLots(Double.parseDouble(maxConcurrentLotsField.getText().trim()));
            }
            
            result = criteria;
            return true;
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                "Please enter valid numbers for all fields.",
                "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    
    public FilterCriteria showDialog() {
        setVisible(true);
        return result;  // Will be null if cancelled
    }
}