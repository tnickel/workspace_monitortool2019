package models;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import data.ProviderStats;

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
}