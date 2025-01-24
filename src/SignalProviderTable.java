import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import data.DataManager;
import ui.MainFrame;
import utils.MqlAnalyserConf;

public class SignalProviderTable {
    private static final Logger LOGGER = Logger.getLogger(SignalProviderTable.class.getName());
    private final MainFrame mainFrame;
    private final DataManager dataManager;
    private final MqlAnalyserConf config;
    
    public static void main(String[] args) {
        String rootPath = args.length > 0 ? args[0] : "c:\\tmp\\mql5";
        SwingUtilities.invokeLater(() -> new SignalProviderTable(rootPath).start());
    }
    
    public SignalProviderTable(String rootPathStr) {
        this.config = new MqlAnalyserConf(rootPathStr);
        this.dataManager = new DataManager();
        
        LOGGER.info("Starting application...");
        String downloadPath = config.getDownloadPath();
        LOGGER.info("Loading data from: " + downloadPath);
        
        try {
            dataManager.loadData(downloadPath);
        } catch (Exception e) {
            LOGGER.severe("Error loading data: " + e.getMessage());
            throw new RuntimeException("Failed to initialize application", e);
        }
        
        this.mainFrame = new MainFrame(dataManager, rootPathStr, config);
    }
    
    public void start() {
        try {
            mainFrame.display();
        } catch (Exception e) {
            LOGGER.severe("Error displaying main frame: " + e.getMessage());
        }
    }
}
