package utils;



import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoggerUtil {
    private static final Logger LOGGER = LogManager.getLogger(LoggerUtil.class);
    
    private LoggerUtil() {
        // Private constructor to prevent instantiation
    }
    
    public static void info(String message) {
        LOGGER.info(message);
    }
    
    public static void debug(String message) {
        LOGGER.debug(message);
    }
    
    public static void warn(String message) {
        LOGGER.warn(message);
    }
    
    public static void error(String message) {
        LOGGER.error(message);
    }
    
    public static void error(String message, Throwable throwable) {
        LOGGER.error(message, throwable);
    }
    
    public static boolean isDebugEnabled() {
        return LOGGER.isDebugEnabled();
    }
    
    public static boolean isInfoEnabled() {
        return LOGGER.isInfoEnabled();
    }
}