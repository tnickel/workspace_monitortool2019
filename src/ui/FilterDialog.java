package ui;



import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.EmptyBorder;
import models.FilterCriteria;
import components.MainTable;

public class FilterDialog extends JDialog {
    private final MainTable mainTable;
    private final JSpinner minTradeDaysSpinner;
    private final JSpinner minProfitSpinner;
    private final JSpinner minProfitFactorSpinner;
    private final JSpinner minWinRateSpinner;
    private final JSpinner maxDrawdownSpinner;
    
    public FilterDialog(JFrame parent, MainTable mainTable) {
        super(parent, "Filter Strategies", true);
        this.mainTable = mainTable;
        
        // Spinner-Modelle mit sinnvollen Bereichen und Schrittgrößen
        SpinnerNumberModel tradeDaysModel = new SpinnerNumberModel(0, 0, 10000, 1);
        SpinnerNumberModel profitModel = new SpinnerNumberModel(0.0, -100000.0, 100000.0, 100.0);
        SpinnerNumberModel profitFactorModel = new SpinnerNumberModel(0.0, 0.0, 100.0, 0.1);
        SpinnerNumberModel winRateModel = new SpinnerNumberModel(0.0, 0.0, 100.0, 1.0);
        SpinnerNumberModel drawdownModel = new SpinnerNumberModel(100.0, 0.0, 100.0, 1.0);
        
        // Spinner initialisieren
        minTradeDaysSpinner = new JSpinner(tradeDaysModel);
        minProfitSpinner = new JSpinner(profitModel);
        minProfitFactorSpinner = new JSpinner(profitFactorModel);
        minWinRateSpinner = new JSpinner(winRateModel);
        maxDrawdownSpinner = new JSpinner(drawdownModel);
        
        // Editor für bessere Darstellung der Dezimalzahlen
        JSpinner.NumberEditor profitEditor = new JSpinner.NumberEditor(minProfitSpinner, "#,##0.00");
        minProfitSpinner.setEditor(profitEditor);
        
        JSpinner.NumberEditor profitFactorEditor = new JSpinner.NumberEditor(minProfitFactorSpinner, "#,##0.00");
        minProfitFactorSpinner.setEditor(profitFactorEditor);
        
        JSpinner.NumberEditor winRateEditor = new JSpinner.NumberEditor(minWinRateSpinner, "#,##0.00");
        minWinRateSpinner.setEditor(winRateEditor);
        
        JSpinner.NumberEditor drawdownEditor = new JSpinner.NumberEditor(maxDrawdownSpinner, "#,##0.00");
        maxDrawdownSpinner.setEditor(drawdownEditor);
        
        initializeUI();
        setupKeyBindings();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        
        // Hauptpanel mit GridBagLayout für die Filterkriterien
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Filterkriterien hinzufügen
        addFilterCriterion(mainPanel, gbc, 0, "Minimum Trade Days:", minTradeDaysSpinner, 
            "Minimum number of trading days required");
        addFilterCriterion(mainPanel, gbc, 1, "Minimum Total Profit:", minProfitSpinner, 
            "Minimum total profit in account currency");
        addFilterCriterion(mainPanel, gbc, 2, "Minimum Profit Factor:", minProfitFactorSpinner, 
            "Minimum ratio of gross profit to gross loss");
        addFilterCriterion(mainPanel, gbc, 3, "Minimum Win Rate (%):", minWinRateSpinner, 
            "Minimum percentage of winning trades");
        addFilterCriterion(mainPanel, gbc, 4, "Maximum Drawdown (%):", maxDrawdownSpinner, 
            "Maximum allowed drawdown percentage");
        
        // Button-Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton applyButton = new JButton("Apply");
        JButton resetButton = new JButton("Reset");
        JButton cancelButton = new JButton("Cancel");
        
        applyButton.setMnemonic(KeyEvent.VK_A);
        resetButton.setMnemonic(KeyEvent.VK_R);
        cancelButton.setMnemonic(KeyEvent.VK_C);
        
        applyButton.addActionListener(e -> applyFilter());
        resetButton.addActionListener(e -> resetFilter());
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(resetButton);
        buttonPanel.add(applyButton);
        buttonPanel.add(cancelButton);
        
        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Dialog-Eigenschaften
        setSize(400, 300);
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getRootPane().setDefaultButton(applyButton);
    }
    
    private void addFilterCriterion(JPanel panel, GridBagConstraints gbc, 
            int row, String label, JComponent component, String tooltip) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.3;
        JLabel labelComponent = new JLabel(label);
        labelComponent.setToolTipText(tooltip);
        panel.add(labelComponent, gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        component.setToolTipText(tooltip);
        panel.add(component, gbc);
    }
    
    private void setupKeyBindings() {
        // ESC zum Schließen
        getRootPane().registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }
    
    private void applyFilter() {
        try {
            FilterCriteria criteria = new FilterCriteria(
                (Integer) minTradeDaysSpinner.getValue(),
                (Double) minProfitSpinner.getValue(),
                (Double) minProfitFactorSpinner.getValue(),
                (Double) minWinRateSpinner.getValue(),
                (Double) maxDrawdownSpinner.getValue()
            );
            
            mainTable.applyFilter(criteria);
            dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error applying filter: " + e.getMessage(),
                "Filter Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void resetFilter() {
        minTradeDaysSpinner.setValue(0);
        minProfitSpinner.setValue(0.0);
        minProfitFactorSpinner.setValue(0.0);
        minWinRateSpinner.setValue(0.0);
        maxDrawdownSpinner.setValue(100.0);
        mainTable.clearFilter();
    }
}