package ui;

import javax.swing.*;
import java.awt.*;
import models.FilterCriteria;

public class FilterDialog extends JDialog {
    private FilterCriteria result = null;
    private static FilterCriteria lastCriteria = null;
    private final JTextField minTradeDaysField = new JTextField(10);
    private final JTextField minTradesField = new JTextField(10);
    private final JTextField minProfitField = new JTextField(10);
    private final JTextField minProfitFactorField = new JTextField(10);
    private final JTextField minWinRateField = new JTextField(10);
    private final JTextField maxDrawdownField = new JTextField(10);
    private final JTextField minTotalProfitField = new JTextField(10);
    private final JTextField maxConcurrentTradesField = new JTextField(10);
    private final JTextField maxConcurrentLotsField = new JTextField(10);
    private final JTextField maxDurationField = new JTextField(10);
    
    public FilterDialog(JFrame parent) {
        super(parent, "Filter Signal Providers", true);
        
        JPanel filterPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        int row = 0;
        
        // Min Trading Days
        gbc.gridx = 0; gbc.gridy = row++;
        filterPanel.add(new JLabel("Min. Trading Days:"), gbc);
        gbc.gridx = 1;
        filterPanel.add(minTradeDaysField, gbc);
        
        // Min Trades
        gbc.gridx = 0; gbc.gridy = row++;
        filterPanel.add(new JLabel("Min. Trades:"), gbc);
        gbc.gridx = 1;
        filterPanel.add(minTradesField, gbc);
        
        addField(filterPanel, gbc, row++, "Min. Profit:", minProfitField);
        addField(filterPanel, gbc, row++, "Min. Total Profit:", minTotalProfitField);
        addField(filterPanel, gbc, row++, "Min. Profit Factor:", minProfitFactorField);
        addField(filterPanel, gbc, row++, "Min. Win Rate (%):", minWinRateField);
        addField(filterPanel, gbc, row++, "Max. Drawdown (%):", maxDrawdownField);
        addField(filterPanel, gbc, row++, "Max Concurrent Trades:", maxConcurrentTradesField);
        addField(filterPanel, gbc, row++, "Max Concurrent Lots:", maxConcurrentLotsField);
        addField(filterPanel, gbc, row, "Max Duration (h):", maxDurationField);

        // Button Panel
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        
        okButton.addActionListener(e -> {
            if (validateAndSetResult()) {
                lastCriteria = result;
                dispose();
            }
        });
        
        cancelButton.addActionListener(e -> {
            result = null;
            dispose();
        });
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        // Layout
        setLayout(new BorderLayout());
        add(filterPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        restoreLastValues();
        
        pack();
        setLocationRelativeTo(parent);
    }
    
    private void addField(JPanel panel, GridBagConstraints gbc, int row, String label, JTextField field) {
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        panel.add(field, gbc);
    }
    
    private void restoreLastValues() {
        if (lastCriteria != null) {
            if (lastCriteria.getMinTradeDays() > 0) {
                minTradeDaysField.setText(String.valueOf(lastCriteria.getMinTradeDays()));
            }
            if (lastCriteria.getMinTrades() > 0) {
                minTradesField.setText(String.valueOf(lastCriteria.getMinTrades()));
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
            if (lastCriteria.getMaxDuration() < Long.MAX_VALUE) {
                maxDurationField.setText(String.valueOf(lastCriteria.getMaxDuration()));
            }
        }
    }
    
    private boolean validateAndSetResult() {
        try {
            FilterCriteria criteria = new FilterCriteria();
            
            if (!minTradeDaysField.getText().isEmpty()) {
                criteria.setMinTradeDays(Integer.parseInt(minTradeDaysField.getText().trim()));
            }
            
            if (!minTradesField.getText().isEmpty()) {
                criteria.setMinTrades(Integer.parseInt(minTradesField.getText().trim()));
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
            if (!maxDurationField.getText().isEmpty()) {
                criteria.setMaxDuration(Long.parseLong(maxDurationField.getText().trim()));
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
        return result;
    }
}