package models;

import java.util.HashMap;
import java.util.Map;
import data.ProviderStats;
<<<<<<< HEAD

public class FilterCriteria {
    private int minTradeDays;
    private double minProfit;
    private double minTotalProfit;
    private double minProfitFactor;
    private double minWinRate;
    private double maxDrawdown;
    private int maxConcurrentTrades;
    private double maxConcurrentLots;

    public FilterCriteria() {
        this.minTradeDays = 0;
        this.minProfit = 0.0;
        this.minTotalProfit = 0.0;
        this.minProfitFactor = 0.0;
        this.minWinRate = 0.0;
        this.maxDrawdown = 100.0;
        this.maxConcurrentTrades = Integer.MAX_VALUE;
        this.maxConcurrentLots = Double.MAX_VALUE;
    }

    public int getMinTradeDays() {
        return minTradeDays;
    }

    public void setMinTradeDays(int minTradeDays) {
        this.minTradeDays = minTradeDays;
    }

    public double getMinProfit() {
        return minProfit;
    }

    public void setMinProfit(double minProfit) {
        this.minProfit = minProfit;
    }

    public double getMinTotalProfit() {
        return minTotalProfit;
    }

    public void setMinTotalProfit(double minTotalProfit) {
        this.minTotalProfit = minTotalProfit;
    }

    public double getMinProfitFactor() {
        return minProfitFactor;
    }

    public void setMinProfitFactor(double minProfitFactor) {
        this.minProfitFactor = minProfitFactor;
    }

    public double getMinWinRate() {
        return minWinRate;
    }

    public void setMinWinRate(double minWinRate) {
        this.minWinRate = minWinRate;
    }

    public double getMaxDrawdown() {
        return maxDrawdown;
    }

    public void setMaxDrawdown(double maxDrawdown) {
        this.maxDrawdown = maxDrawdown;
    }

    public int getMaxConcurrentTrades() {
        return maxConcurrentTrades;
    }

    public void setMaxConcurrentTrades(int maxConcurrentTrades) {
        this.maxConcurrentTrades = maxConcurrentTrades;
    }

    public double getMaxConcurrentLots() {
        return maxConcurrentLots;
    }

    public void setMaxConcurrentLots(double maxConcurrentLots) {
        this.maxConcurrentLots = maxConcurrentLots;
    }

    public boolean matches(ProviderStats stats) {
        // Berechne die Anzahl der Handelstage zwischen Start- und Enddatum
        long tradingDays = java.time.temporal.ChronoUnit.DAYS.between(
            stats.getStartDate(),
            stats.getEndDate().plusDays(1) // plusDays(1) um den letzten Tag einzuschließen
        );
        
        return tradingDays >= minTradeDays &&
               stats.getAverageProfitPerTrade() >= minProfit &&
               stats.getTotalProfit() >= minTotalProfit &&
               stats.getProfitFactor() >= minProfitFactor &&
               stats.getWinRate() >= minWinRate &&
               stats.getMaxDrawdownPercent() <= maxDrawdown &&
               stats.getMaxConcurrentTrades() <= maxConcurrentTrades &&
               stats.getMaxConcurrentLots() <= maxConcurrentLots;
    }

    @Override
    public String toString() {
        return String.format("FilterCriteria{minTradeDays=%d, minProfit=%.2f, minTotalProfit=%.2f, " +
                            "minProfitFactor=%.2f, minWinRate=%.2f, maxDrawdown=%.2f, " +
                            "maxConcurrentTrades=%d, maxConcurrentLots=%.2f}",
                            minTradeDays, minProfit, minTotalProfit,
                            minProfitFactor, minWinRate, maxDrawdown,
                            maxConcurrentTrades, maxConcurrentLots);
    }
=======
import java.io.*;

public class FilterCriteria
{
	private Map<Integer, FilterRange> columnFilters;
	private static final String SAVE_FILE = "filter_criteria.ser"; // Datei zum Speichern der Filterwerte
	
	public FilterCriteria()
	{
		this.columnFilters = new HashMap<>();
	}
	
	public void addFilter(int column, FilterRange range)
	{
		columnFilters.put(column, range);
	}
	
	public boolean matches(ProviderStats stats, Object[] rowData)
	{
		for (Map.Entry<Integer, FilterRange> entry : columnFilters.entrySet())
		{
			int column = entry.getKey();
			FilterRange range = entry.getValue();
			
			if (!range.matches(rowData[column]))
			{
				return false;
			}
		}
		return true;
	}
	
	public Map<Integer, FilterRange> getFilters()
	{
		return columnFilters;
	}
	
	public void setFilters(Map<Integer, FilterRange> filters)
	{
		this.columnFilters = filters;
	}
	
	public void saveFilters()
	{
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SAVE_FILE)))
		{
			oos.writeObject(columnFilters);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void loadFilters()
	{
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SAVE_FILE)))
		{
			columnFilters = (Map<Integer, FilterRange>) ois.readObject();
		} catch (IOException | ClassNotFoundException e)
		{
			columnFilters = new HashMap<>(); // Falls Datei nicht existiert
		}
	}
	
	public static class FilterRange implements Serializable
	{
		private static final long serialVersionUID = 1L;
		private final Double min;
		private final Double max;
		private final String textFilter;
		
		public FilterRange(Double min, Double max)
		{
			this.min = min;
			this.max = max;
			this.textFilter = null;
		}
		
		public FilterRange(String textFilter)
		{
			this.min = null;
			this.max = null;
			this.textFilter = textFilter;
		}
		
		public boolean matches(Object value)
		{
			if (value == null)
				return false;
			
			if (textFilter != null)
			{
				return value.toString().toLowerCase().contains(textFilter.toLowerCase());
			}
			
			try
			{
				double numValue = value instanceof Number ? ((Number) value).doubleValue()
						: Double.parseDouble(value.toString());
			
				if (min != null && numValue < min)
					return false;
				if (max != null && numValue > max)
					return false;
				return true;
			} catch (NumberFormatException e)
			{
				System.out.println("Number format exception for value: " + value);
				return false;
			}
		}
		
		public Double getMin()
		{
			return min;
		}
		
		public Double getMax()
		{
			return max;
		}
		
		public String getTextFilter()
		{
			return textFilter;
		}
	}
>>>>>>> feb2025
}