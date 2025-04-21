import java.awt.Dimension;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import data.DataManager;
import ui.MainFrame;
import ui.SplashScreen;
import utils.ApplicationConstants;
import utils.MqlAnalyserConf;

public class SignalProviderTable {
    private static final Logger LOGGER = Logger.getLogger(SignalProviderTable.class.getName());
    private final MainFrame mainFrame;
    private final DataManager dataManager;
    private final MqlAnalyserConf config;
    
    public static void main(String[] args) {
        // Verwende die Konstante, aber prüfe auch Kommandozeilenargumente
        String rootPath = args.length > 0 ? args[0] : ApplicationConstants.ROOT_PATH;
        
        // Validiere den Pfad und korrigiere ihn, falls nötig
        rootPath = ApplicationConstants.validateRootPath(rootPath, "SignalProviderTable.main");
        
        // Splash-Screen anzeigen
        SplashScreen splash = new SplashScreen();
        splash.setVisible(true);
        
        // Starte Laden in einem separaten Thread
        SwingWorker<SignalProviderTable, Void> worker = new SwingWorker<SignalProviderTable, Void>() {
            @Override
            protected SignalProviderTable doInBackground() throws Exception {
                return new SignalProviderTable(rootPath, splash);
            }
            
            @Override
            protected void done() {
                try {
                    SignalProviderTable app = get();
                    app.start();
                    // Splash-Screen schließen
                    splash.dispose();
                } catch (Exception e) {
                    LOGGER.severe("Fehler beim Starten der Anwendung: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        
        worker.execute();
    }
    
    public SignalProviderTable(String rootPathStr, SplashScreen splash) {
        // Validiere den Pfad und korrigiere ihn, falls nötig
        rootPathStr = ApplicationConstants.validateRootPath(rootPathStr, "SignalProviderTable.constructor");
        
        this.config = new MqlAnalyserConf(rootPathStr);
        this.dataManager = new DataManager();
        
        // Callbacks für Fortschritt und Status setzen
        dataManager.setProgressCallback(progress -> {
            splash.setProgress(progress);
        });
        
        dataManager.setStatusCallback(status -> {
            splash.setStatus(status);
        });
        
        LOGGER.info("Starting application...");
        String downloadPath = config.getDownloadPath();
        LOGGER.info("Loading data from: " + downloadPath);
        
        splash.setStatus("Initialisiere...");
        
        try {
            dataManager.loadData(downloadPath);
        } catch (Exception e) {
            LOGGER.severe("Error loading data: " + e.getMessage());
            throw new RuntimeException("Failed to initialize application", e);
        }
        
        this.mainFrame = new MainFrame(dataManager, rootPathStr, config);
        
        // Hole aktuelle Größe und setze neue Breite (30% breiter)
        Dimension currentSize = mainFrame.getSize();
        int newWidth = (int)(currentSize.getWidth() * 1.3);
        mainFrame.setSize(newWidth, currentSize.height);
        mainFrame.setPreferredSize(new Dimension(newWidth, currentSize.height));
        
        // Zentriere das Fenster
        mainFrame.setLocationRelativeTo(null);
        
        // Setze Minimalgröße
        mainFrame.setMinimumSize(new Dimension(newWidth, currentSize.height));
    }
    
    public void start() {
        try {
            mainFrame.display();
        } catch (Exception e) {
            LOGGER.severe("Error displaying main frame: " + e.getMessage());
        }
    }
}