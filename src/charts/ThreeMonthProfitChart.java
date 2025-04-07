package charts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.TreeMap;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Month;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

public class ThreeMonthProfitChart extends ChartPanel {
    
    public ThreeMonthProfitChart(Map<String, Double> monthlyProfits, double equityDrawdown) {
        super(createChart(monthlyProfits, equityDrawdown));
        this.setMouseWheelEnabled(true);
        this.setDomainZoomable(true);
        this.setRangeZoomable(true);
    }
    
    private static JFreeChart createChart(Map<String, Double> monthlyProfits, double equityDrawdown) {
        TimeSeries series = new TimeSeries("3MPDD Verlauf");
        
        // Sortiere die Monate chronologisch
        TreeMap<String, Double> sortedProfits = new TreeMap<>(monthlyProfits);
        
        // Nur ein Punkt pro Monat
        for (Map.Entry<String, Double> entry : sortedProfits.entrySet()) {
            String currentMonth = entry.getKey();
            double mpdd = calculate3MPDD(sortedProfits, currentMonth, equityDrawdown);
            
            // Konvertiere YYYY/MM in Month-Objekt für JFreeChart
            String[] parts = currentMonth.split("/");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            series.add(new Month(month, year), mpdd);
        }
        
        TimeSeriesCollection dataset = new TimeSeriesCollection(series);
        
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            "3MPDD Verlauf",
            "Datum",
            "MPDD",
            dataset,
            true,
            true,
            false
        );
        
        // Formatierung des Charts
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // Linie und Punkte Formatierung
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, new Color(0, 100, 0));  // Dunkelgrün
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesShapesVisible(0, true);
        renderer.setDrawOutlines(true);
        renderer.setUseFillPaint(true);
        renderer.setSeriesLinesVisible(0, true);
        // Keine Interpolation zwischen den Punkten
        renderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(renderer);
        
        // X-Achse (Datum) Formatierung
        DateAxis dateAxis = (DateAxis) plot.getDomainAxis();
        dateAxis.setDateFormatOverride(new SimpleDateFormat("MM/yyyy"));
        
        return chart;
    }
   
    
    private static double calculate3MPDD(TreeMap<String, Double> profits, String currentMonth, double equityDrawdown) {
        if (equityDrawdown == 0.0) {
            return 0.0;  // Verhindert Division durch Null
        }
        
        // Berechne den Durchschnitt der letzten 3 Monate (ohne den aktuellen)
        String[] keys = profits.headMap(currentMonth, false).keySet().toArray(new String[0]);
        if (keys.length >= 3) {
            double sum = 0;
            // Nimm genau die letzten 3 Monate
            for (int i = keys.length - 1; i >= keys.length - 3; i--) {
                sum += profits.get(keys[i]);
            }
            double avgProfit = sum / 3;
            return avgProfit / equityDrawdown; // Berechne MPDD
        }
        
        return 0.0;  // Wenn nicht genug Monate verfügbar sind
    }
}