
package data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class DataManager
{
	private static final Logger LOGGER = Logger.getLogger(DataManager.class.getName());
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");
	private static final int EXPECTED_MIN_FIELDS = 11;
	
	private final Map<String, ProviderStats> signalProviderStats;
	
	public DataManager()
	{
		this.signalProviderStats = new HashMap<>();
	}
	
	public void loadData(String path)
	{
		LOGGER.info("Loading data from: " + path);
		File downloadDirectory = new File(path);
		if (downloadDirectory.exists() && downloadDirectory.isDirectory())
		{
			File[] files = downloadDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
			if (files != null)
			{
				LOGGER.info("Found " + files.length + " CSV files");
				for (File file : files)
				{
					processFile(file);
				}
			}
		}
		LOGGER.info("Loaded " + signalProviderStats.size() + " providers");
	}
	
	private void processFile(File file)
	{
		LOGGER.info("Starting to process file: " + file.getName());
		List<String> skippedLines = new ArrayList<>();
		
		try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8), 32768))
		{
			
			String line;
			boolean isHeader = true;
			ProviderStats stats = new ProviderStats();
			int lineCount = 0;
			int tradeCounts = 0;
			double initialBalance = 0.0;
			boolean foundFirstBalance = false;
			
			String[] data;
			while ((line = reader.readLine()) != null)
			{
				lineCount++;
				if (isHeader)
				{
					isHeader = false;
					continue;
				}
				
				line = line.trim();
				if (line.isEmpty())
					continue;
				
				data = line.split(";", -1);
				
				if (data.length < EXPECTED_MIN_FIELDS)
				{
					LOGGER.warning(String.format("Line %d in %s has insufficient fields (%d < %d): %s", lineCount,
							file.getName(), data.length, EXPECTED_MIN_FIELDS, line));
					skippedLines.add(line);
					continue;
				}
				
				if (data.length >= 2) {
				    String operationType = data[1].trim();
				    
				    // Prüfung für Initial Balance
				    if ("Initial Balance".equalsIgnoreCase(operationType)) {
				        try {
				            String balanceStr = data[data.length - 1].trim();
				            if (!balanceStr.isEmpty()) {
				                initialBalance = Double.parseDouble(balanceStr);
				                if (initialBalance >= 0) {
				                    stats.setInitialBalance(initialBalance);
				                    foundFirstBalance = true;
				                }
				            }
				        } catch (NumberFormatException e) {
				            // Skip silently
				        }
				        continue;
				    }
				    
				    // Filter für Balance/Credit/Commission - einfach überspringen
				    if ("Balance".equals(operationType) || 
				        "Credit".equals(operationType) || 
				        "Commission".equals(operationType)) {
				        continue; // Skip silently
				    }
				}
				
				try
				{
					if (data[0].trim().isEmpty() || data[1].trim().isEmpty() || data[2].trim().isEmpty()
							|| data[3].trim().isEmpty() || data[4].trim().isEmpty() || data[6].trim().isEmpty()
							|| data[7].trim().isEmpty())
					{
						
						LOGGER.warning("Missing or empty required fields in line " + lineCount + ": " + line);
						skippedLines.add(line);
						continue;
					}
					
					LocalDateTime openTime = LocalDateTime.parse(data[0].trim(), DATE_TIME_FORMATTER);
					String type = data[1].trim();
					double lots = Double.parseDouble(data[2].trim());
					String symbol = data[3].trim();
					double openPrice = Double.parseDouble(data[4].trim());
					LocalDateTime closeTime = LocalDateTime.parse(data[6].trim(), DATE_TIME_FORMATTER);
					double closePrice = Double.parseDouble(data[7].trim());
					
					double commission = 0.0;
					double swap = 0.0;
					double profit = 0.0;
					
					try
					{
						if (data.length > data.length - 3)
						{
							commission = data[data.length - 3].trim().isEmpty() ? 0.0
									: Double.parseDouble(data[data.length - 3].trim());
						}
						if (data.length > data.length - 2)
						{
							swap = data[data.length - 2].trim().isEmpty() ? 0.0
									: Double.parseDouble(data[data.length - 2].trim());
						}
						profit = Double.parseDouble(data[data.length - 1].trim());
					} catch (NumberFormatException e)
					{
						LOGGER.warning("Error parsing financial fields in line " + lineCount + ": " + line);
						skippedLines.add(line);
						continue;
					}
					
					stats.addTrade(openTime, closeTime, type, symbol, lots, openPrice, closePrice, commission, swap,
							profit);
					
					tradeCounts++;
					
				} catch (Exception e)
				{
					LOGGER.warning(String.format("Error processing line %d in file %s: %s%nError: %s", lineCount,
							file.getName(), line, e.getMessage()));
					skippedLines.add(line);
					continue;
				}
			}
			
			if (!stats.getProfits().isEmpty())
			{
				signalProviderStats.put(file.getName(), stats);
				LOGGER.info(String.format("Successfully processed %s: %d trades loaded, Initial Balance: %.2f",
						file.getName(), tradeCounts, initialBalance));
			}
			
		} catch (IOException e)
		{
			LOGGER.severe("Error reading file " + file.getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		LOGGER.info("Skipped lines in file " + file.getName() + ": " + skippedLines.size());
		skippedLines.forEach(line -> LOGGER.info("Skipped: " + line));
	}
	
	public Map<String, ProviderStats> getStats()
	{
		return signalProviderStats;
	}
}
