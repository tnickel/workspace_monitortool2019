package charts;
import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import data.DataManager;
import ui.MainFrame;

public class SignalProviderTable {
    private static final Logger LOGGER = Logger.getLogger(SignalProviderTable.class.getName());
    private final MainFrame mainFrame;
    private final DataManager dataManager;
    private final Path rootPath;
    
    public static void main(String[] args) {
        String rootPath = args.length > 0 ? args[0] : "c:\\tmp\\mql5";
        SwingUtilities.invokeLater(() -> new SignalProviderTable(rootPath).start());
    }
    
    public SignalProviderTable(String rootPathStr) {
        this.rootPath = Paths.get(rootPathStr);
        this.dataManager = new DataManager();
        
        Path downloadPath = rootPath.resolve("download");
        LOGGER.info("Starting application...");
        LOGGER.info("Loading data from: " + downloadPath);
        
        try {
            dataManager.loadData(downloadPath.toString());
        } catch (Exception e) {
            LOGGER.severe("Error loading data: " + e.getMessage());
            throw new RuntimeException("Failed to initialize application", e);
        }
        
        this.mainFrame = new MainFrame(dataManager);
    }
    
    public void start() {
        try {
            mainFrame.display();
        } catch (Exception e) {
            LOGGER.severe("Error displaying main frame: " + e.getMessage());
        }
    }
}