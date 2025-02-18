package components;



import java.awt.BorderLayout;
import javax.swing.JPanel;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebView;

public class WebViewPanel extends JPanel {
    private JFXPanel jfxPanel;
    private WebView webView;
    
    public WebViewPanel() {
        setLayout(new BorderLayout());
        
        // Initialize JavaFX Panel
        jfxPanel = new JFXPanel();
        add(jfxPanel, BorderLayout.CENTER);
        
        // Initialize JavaFX components
        Platform.runLater(() -> {
            webView = new WebView();
            // Zoom auf 50% setzen
            webView.setZoom(0.8);
            Scene scene = new Scene(webView);
            jfxPanel.setScene(scene);
        });
    }
    
    public void loadURL(String url) {
        if (url == null || url.isEmpty()) {
            return;
        }
        
        Platform.runLater(() -> {
            try {
                webView.getEngine().load(url);
            } catch (Exception e) {
                System.err.println("Error loading URL: " + e.getMessage());
            }
        });
    }
    
    public void clear() {
        Platform.runLater(() -> {
            webView.getEngine().loadContent("");
        });
    }
}