package utils;

import java.net.URL;
import javax.swing.ImageIcon;

/**
 * Zentrale Klasse zum Verwalten von Icons in der Anwendung
 */
public class IconManager {
    // Icon-Größen
    public static final int ICON_SMALL = 16;
    public static final int ICON_MEDIUM = 24;
    public static final int ICON_LARGE = 32;
    
    // Icons laden
    public static ImageIcon getIcon(String name, int size) {
        String path = "/icons/" + name + "_" + size + ".png";
        URL url = IconManager.class.getResource(path);
        if (url != null) {
            return new ImageIcon(url);
        } else {
            System.err.println("Icon nicht gefunden: " + path);
            return null;
        }
    }
    
    // Häufig verwendete Icons
    public static ImageIcon getChartIcon(int size) {
        return getIcon("chart", size);
    }
    
    public static ImageIcon getFilterIcon(int size) {
        return getIcon("filter", size);
    }
    
    public static ImageIcon getSearchIcon(int size) {
        return getIcon("search", size);
    }
    
    public static ImageIcon getExportIcon(int size) {
        return getIcon("export", size);
    }
    
    public static ImageIcon getSettingsIcon(int size) {
        return getIcon("settings", size);
    }
    
    public static ImageIcon getFavoriteIcon(int size, boolean selected) {
        return getIcon(selected ? "star_filled" : "star_empty", size);
    }
    
    public static ImageIcon getRefreshIcon(int size) {
        return getIcon("refresh", size);
    }
    
    public static ImageIcon getDatabaseIcon(int size) {
        return getIcon("database", size);
    }
    
    public static ImageIcon getCompareIcon(int size) {
        return getIcon("compare", size);
    }
    
    public static ImageIcon getListIcon(int size) {
        return getIcon("list", size);
    }
    
    public static ImageIcon getInfoIcon(int size) {
        return getIcon("info", size);
    }
    
    public static ImageIcon getDeleteIcon(int size) {
        return getIcon("delete", size);
    }
    
    public static ImageIcon getResetIcon(int size) {
        return getIcon("reset", size);
    }
}