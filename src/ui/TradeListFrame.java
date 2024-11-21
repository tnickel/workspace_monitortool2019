package ui;



import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;

import components.TradeListTable;
import data.ProviderStats;

public class TradeListFrame extends JFrame {
    private final TradeListTable tradeTable;

    public TradeListFrame(String providerName, ProviderStats stats) {
        super("Trade List: " + providerName);
        this.tradeTable = new TradeListTable(stats);
        initializeUI();
        
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        // ESC-Taste zum Schließen
        KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        Action escapeAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(escapeKeyStroke, "ESCAPE");
        getRootPane().getActionMap().put("ESCAPE", escapeAction);
    }

    private void initializeUI() {
        setSize(800, 600);
        add(new JScrollPane(tradeTable));
        setLocationRelativeTo(null);
        
        // Toolbar mit Export-Button
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        
        JButton exportButton = new JButton("Export");
        exportButton.addActionListener(e -> exportTrades());
        
        toolBar.add(exportButton);
        add(toolBar, BorderLayout.NORTH);
    }
    
    private void exportTrades() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Trades");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setSelectedFile(new File("trades_export.csv"));
        
        FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV Files (*.csv)", "csv");
        fileChooser.setFileFilter(filter);
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getAbsolutePath() + ".csv");
            }
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.println("Date,Profit/Loss");
                
                for (int i = 0; i < tradeTable.getRowCount(); i++) {
                    String date = tradeTable.getValueAt(i, 0).toString();
                    String profit = tradeTable.getValueAt(i, 1).toString();
                    writer.println(date + "," + profit);
                }
                
                JOptionPane.showMessageDialog(this, 
                    "Export successful: " + file.getAbsolutePath(), 
                    "Export Complete", 
                    JOptionPane.INFORMATION_MESSAGE);
                    
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                    "Error exporting file: " + e.getMessage(),
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
}