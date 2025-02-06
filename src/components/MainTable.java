package components;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.JTable;
import javax.swing.ToolTipManager;
import javax.swing.table.TableRowSorter;

import data.DataManager;
import data.ProviderStats;
import models.FilterCriteria;
import models.HighlightTableModel;
import renderers.HighlightRenderer;
import renderers.RiskScoreRenderer;
import ui.DetailFrame;
import utils.HtmlParser;

public class MainTable extends JTable {
   private final HighlightTableModel model;
   private final HighlightRenderer renderer;
   private final RiskScoreRenderer riskRenderer;
   private final DataManager dataManager;
   private String rootPath;
   private FilterCriteria currentFilter;
   private Consumer<String> statusUpdateCallback;
   private final HtmlParser htmlParser;
   
   public MainTable(DataManager dataManager, String downloadPath) {
       this.dataManager = dataManager;
       this.rootPath = downloadPath;
       this.model = new HighlightTableModel(rootPath);
       this.renderer = new HighlightRenderer();
       this.riskRenderer = new RiskScoreRenderer();
       this.htmlParser = new HtmlParser(rootPath);
       initialize();
       setupMouseListener();
       setupModelListener();
   }
   
   private void initialize() {
       setModel(model);
       setRowSorter(new TableRowSorter<>(model));
       
       ToolTipManager.sharedInstance().registerComponent(this);
       
       for (int i = 0; i < getColumnCount(); i++) {
           if (i == 13) {
               getColumnModel().getColumn(i).setCellRenderer(riskRenderer);
           } else {
               getColumnModel().getColumn(i).setCellRenderer(renderer);
           }
       }
       
       model.populateData(dataManager.getStats());
   }
   
   private void setupMouseListener() {
       addMouseListener(new MouseAdapter() {
           @Override
           public void mouseClicked(MouseEvent e) {
               if (e.getClickCount() == 2) {
                   int row = getSelectedRow();
                   if (row != -1) {
                       row = convertRowIndexToModel(row);
                       String providerName = (String) model.getValueAt(row, 1);
                       ProviderStats stats = dataManager.getStats().get(providerName);
                       String providerId = providerName.substring(providerName.lastIndexOf("_") + 1).replace(".csv", "");
                       
                       if (stats != null) {
                           DetailFrame detailFrame = new DetailFrame(providerName, stats, providerId, htmlParser);
                           detailFrame.setVisible(true);
                       }
                   }
               }
           }
       });
   }
   
   private void setupModelListener() {
       model.addTableModelListener(e -> updateStatus());
   }
   
   public void setStatusUpdateCallback(Consumer<String> callback) {
       this.statusUpdateCallback = callback;
   }
   
   @Override
   public String getToolTipText(MouseEvent e) {
       int row = rowAtPoint(e.getPoint());
       int col = columnAtPoint(e.getPoint());
       
       if (row >= 0 && col >= 0) {
           row = convertRowIndexToModel(row);
           col = convertColumnIndexToModel(col);
           
           if (col == 5) { // 3MProfProz column
               String providerName = (String) model.getValueAt(row, 1);
               return getThreeMonthProfitCalculationToolTip(providerName);
           } else if (col == 3) { // Original 3MonProfit tooltip
               String providerName = (String) model.getValueAt(row, 1);
               ProviderStats stats = dataManager.getStats().get(providerName);
               return getThreeMonthTradesToolTip(stats);
           }
       }
       return null;
   }
   
   private String getThreeMonthProfitCalculationToolTip(String providerName) {
	    // Hole die letzten drei Profite vom HtmlParser
	    List<String> profits = htmlParser.getLastThreeMonthsDetails(providerName);
	    
	    if (profits.isEmpty()) {
	        return "Keine Profitdaten verfügbar";
	    }

	    // Berechne die Summe
	    double sum = profits.stream()
	        .mapToDouble(s -> {
	            String valueStr = s.split(":")[1].trim()
	                             .replace("%", "")
	                             .replace(",", ".");  // Ersetze Komma durch Punkt
	            return Double.parseDouble(valueStr);
	        })
	        .sum();
	    
	    // Erstelle den Tooltip mit HTML-Formatierung
	    StringBuilder tooltip = new StringBuilder("<html><b>3-Monats Profit Berechnung:</b><br><br>");
	    
	    // Füge jeden Monat einzeln hinzu
	    profits.forEach(profit -> tooltip.append(profit).append("<br>"));
	    
	    // Füge die Berechnung hinzu
	    tooltip.append("<br>Summe: ").append(String.format("%.2f%%", sum))
	           .append("<br>Durchschnitt: ").append(String.format("%.2f%%", sum / profits.size()))
	           .append(" (").append(profits.size()).append(" Monate)")
	           .append("</html>");
	    
	    return tooltip.toString();
	}

   private String getThreeMonthTradesToolTip(ProviderStats stats) {
       LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
       
       long tradesCount = stats.getTrades().stream()
           .filter(trade -> trade.getCloseTime().isAfter(threeMonthsAgo))
           .count();
           
       LocalDateTime firstTradeDate = stats.getTrades().stream()
           .filter(trade -> trade.getCloseTime().isAfter(threeMonthsAgo))
           .map(trade -> trade.getCloseTime())
           .min(LocalDateTime::compareTo)
           .orElse(null);
           
       LocalDateTime lastTradeDate = stats.getTrades().stream()
           .filter(trade -> trade.getCloseTime().isAfter(threeMonthsAgo))
           .map(trade -> trade.getCloseTime())
           .max(LocalDateTime::compareTo)
           .orElse(null);
           
       DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
       
       return String.format("<html><b>3-Monats Übersicht:</b><br>" +
                          "Anzahl Trades: %d<br>" +
                          "Erster Trade: %s<br>" +
                          "Letzter Trade: %s</html>",
                          tradesCount,
                          firstTradeDate != null ? firstTradeDate.format(formatter) : "N/A",
                          lastTradeDate != null ? lastTradeDate.format(formatter) : "N/A");
   }
   
   public String getStatusText() {
       int totalProviders = dataManager.getStats().size();
       int visibleProviders = model.getRowCount();
       
       StringBuilder status = new StringBuilder()
           .append(String.format("%d/%d Signal Providers", visibleProviders, totalProviders));
           
       if (currentFilter != null) {
           status.append(" (filtered)");
       }

       return status.toString();
   }
   
   public void updateStatus() {
       if (statusUpdateCallback != null) {
           statusUpdateCallback.accept(getStatusText());
       }
   }
   
   public void highlightSearchText(String text) {
       renderer.setSearchText(text);
       repaint();
   }
   
   public void clearHighlight() {
       renderer.setSearchText("");
       repaint();
   }
   
   public boolean findAndSelectNext(String searchText, int[] currentIndex) {
       int startRow = currentIndex[0] + 1;
       
       for (int row = startRow; row < getRowCount(); row++) {
           for (int col = 0; col < getColumnCount(); col++) {
               Object value = getValueAt(row, col);
               if (value != null && value.toString().toLowerCase().contains(searchText)) {
                   currentIndex[0] = row;
                   scrollRectToVisible(getCellRect(row, 0, true));
                   setRowSelectionInterval(row, row);
                   return true;
               }
           }
       }
       return false;
   }
   
   public void applyFilter(FilterCriteria criteria) {
       this.currentFilter = criteria;
       refreshTableData();
   }
   
   public void resetFilter() {
       this.currentFilter = null;
       refreshTableData();
   }
   
   private void refreshTableData() {
       if (currentFilter == null) {
           model.populateData(dataManager.getStats());
       } else {
           Map<String, ProviderStats> filteredStats = dataManager.getStats().entrySet().stream()
               .filter(entry -> currentFilter.matches(entry.getValue()))
               .collect(Collectors.toMap(
                   Map.Entry::getKey,
                   Map.Entry::getValue
               ));
           model.populateData(filteredStats);
       }
       updateStatus();
   }
   
   public Map<String, ProviderStats> getCurrentProviderStats() {
       if (currentFilter == null) {
           return dataManager.getStats();
       }
       return dataManager.getStats().entrySet().stream()
           .filter(entry -> currentFilter.matches(entry.getValue()))
           .collect(Collectors.toMap(
               Map.Entry::getKey,
               Map.Entry::getValue
           ));
   }
}