import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class TradeHistoryDownloader {

    private static final Logger logger = LogManager.getLogger(TradeHistoryDownloader.class);

    public static void main(String[] args) {
        // Initialisiere Proxies, falls nötig
        String proxyHost = "your_proxy_host";
        String proxyPort = "your_proxy_port";
        System.setProperty("http.proxyHost", proxyHost);
        System.setProperty("http.proxyPort", proxyPort);
        System.setProperty("https.proxyHost", proxyHost);
        System.setProperty("https.proxyPort", proxyPort);
        // Überprüfen, ob die Log4j-Konfigurationsdatei vorhanden ist
        File log4jConfigFile = new File("D:\\git\\Monitortool\\workspace_monitortool2019\\MqlAnalyser\\src\\resources\\log4j2.xml");
        if (log4jConfigFile.exists()) {
            try {
                System.out.println("Versuche, die Log4j-Konfigurationsdatei zu laden...");
                Configurator.initialize(null, log4jConfigFile.getAbsolutePath());
                logger.info("Log4j-Konfigurationsdatei erfolgreich geladen.");
            } catch (Exception e) {
                System.err.println("Log4j2 Konfigurationsdatei konnte nicht geladen werden, verwende Standardkonfiguration.");
                e.printStackTrace();
                configureDefaultLogger();
            }
        } else {
            System.err.println("Log4j2 Konfigurationsdatei nicht gefunden, verwende Standardkonfiguration.");
            configureDefaultLogger();
        }

        logger.info("Logger erfolgreich initialisiert.");

        // Überprüfen, ob die Log-Ausgabe in Konsole und Datei funktioniert
        logger.info("Test-Log: Dies ist eine Testmeldung, um sicherzustellen, dass der Logger funktioniert.");
        logger.warn("Warnung: Überprüfen Sie, ob diese Warnmeldung im Logfile und in der Konsole erscheint.");
        logger.error("Fehler: Überprüfen Sie, ob diese Fehlermeldung im Logfile und in der Konsole erscheint.");

        // Root-Verzeichnis und Konfigurationsverzeichnis festlegen
        String rootDirPath = "C:\\tmp\\mql5";
        String configDirPath = rootDirPath + "\\conf";
        String configFilePath = configDirPath + "\\conf.txt";
        String logDirPath = rootDirPath + "\\logs";

        // Erstelle das Konfigurations- und Log-Verzeichnis, falls es nicht existiert
        File configDir = new File(configDirPath);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        File logDir = new File(logDirPath);
        if (!logDir.exists()) {
            boolean logDirCreated = logDir.mkdirs();
            if (logDirCreated) {
                logger.info("Log-Verzeichnis erstellt: " + logDirPath);
            } else {
                logger.error("Log-Verzeichnis konnte nicht erstellt werden: " + logDirPath);
            }
        } else {
            logger.info("Log-Verzeichnis existiert bereits: " + logDirPath);
        }

        // Login-Daten aus der Konfigurationsdatei lesen oder abfragen
        String username = "";
        String password = "";
        File configFile = new File(configFilePath);
        Properties props = new Properties();

        if (configFile.exists()) {
            try {
                props.load(Files.newBufferedReader(configFile.toPath()));
                username = props.getProperty("username");
                password = props.getProperty("password");
            } catch (IOException e) {
                logger.error("Fehler beim Laden der Konfigurationsdatei", e);
                return;
            }
        } else {
            // Login-Daten abfragen und in der Konfigurationsdatei speichern
            try (Scanner scanner = new Scanner(System.in)) {
                System.out.print("Benutzername: ");
                username = scanner.nextLine();
                System.out.print("Passwort: ");
                password = scanner.nextLine();

                props.setProperty("username", username);
                props.setProperty("password", password);

                try (FileWriter writer = new FileWriter(configFile)) {
                    props.store(writer, "Login-Konfiguration");
                } catch (IOException e) {
                    logger.error("Fehler beim Speichern der Konfigurationsdatei", e);
                    return;
                }
            }
        }

        // Pfad zum ChromeDriver angeben
        System.setProperty("webdriver.chrome.driver", "C:\\tools\\chromedriver.exe");

        // Downloadpfad konfigurieren
        String downloadFilepath = rootDirPath;
        String additionalDownloadPath = downloadFilepath + "\\download";
        // Erstelle das Download-Verzeichnis, falls es nicht existiert
        File downloadDir = new File(additionalDownloadPath);
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", additionalDownloadPath);

        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", prefs);

        // Setze User-Agent, um den Web-Zugriff realistischer zu machen
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

        // WebDriver mit Downloadpfad-Einstellungen initialisieren
        WebDriver driver = new ChromeDriver(options);

        try {
            // Überprüfen, ob der WebDriver initialisiert wurde
            if (driver == null) {
                logger.error("WebDriver konnte nicht initialisiert werden.");
                return;
            }

            // Login auf der Webseite durchführen
            logger.info("\n--- Login-Prozess startet ---");
            logger.info("Öffne Login-Seite...");
            driver.get("https://www.mql5.com/en/auth_login");

            // Wartezeit erhöhen, um sicherzustellen, dass die Login-Seite vollständig geladen ist
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));

            // Benutzername und Passwort eingeben
            logger.info("Warte darauf, dass das Benutzername-Feld sichtbar wird...");
            WebElement usernameField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("Login")));
            if (usernameField == null) {
                logger.error("Benutzername-Feld konnte nicht gefunden werden.");
                return;
            }
            WebElement passwordField = driver.findElement(By.id("Password"));
            if (passwordField == null) {
                logger.error("Passwort-Feld konnte nicht gefunden werden.");
                return;
            }

            // Login-Daten eingeben
            logger.info("Gebe Benutzername und Passwort ein...");
            usernameField.sendKeys(username);
            passwordField.sendKeys(password);

            // Versuche, den Login-Button zu finden und zu klicken
            WebElement loginButton = null;

            logger.info("Versuche, den Login-Button zu finden...");
            try {
                // Versuche den Login-Button über die ID zu finden
                loginButton = driver.findElement(By.id("loginSubmit"));
                logger.info("Login-Button mit ID 'loginSubmit' gefunden.");
            } catch (Exception e) {
                logger.info("Button mit ID 'loginSubmit' nicht gefunden, versuche anderen Selektor.");
            }

            if (loginButton == null) {
                try {
                    // Fallback: Versuche den Login-Button über die Klasse zu finden
                    loginButton = driver.findElement(By.cssSelector("input.button.button_yellow.qa-submit"));
                    logger.info("Login-Button mit CSS-Selektor 'input.button.button_yellow.qa-submit' gefunden.");
                } catch (Exception e) {
                    logger.error("Button mit CSS-Selektor 'input.button.button_yellow.qa-submit' nicht gefunden, konnte nicht einloggen.");
                    return; // Wenn alle Locator-Strategien fehlschlagen, wird das Programm beendet
                }
            }

            // Login-Button durch JavaScript klicken, falls er gefunden wurde
            if (loginButton != null) {
                logger.info("Klicke den Login-Button...");
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", loginButton);
            } else {
                logger.error("Login-Button konnte nicht gefunden werden.");
                return;
            }

            // Sicherstellen, dass der Login erfolgreich war
            try {
                logger.info("Überprüfe, ob die URL sich nach dem Login ändert...");
                wait.until(ExpectedConditions.urlContains("/en"));
                logger.info("Login erfolgreich: Die URL hat sich geändert.");
                Thread.sleep(getRandomWaitTime()); // Zufällige Wartezeit, um nicht als Bot erkannt zu werden
            } catch (Exception e) {
                logger.error("Login war nicht erfolgreich oder die URL hat sich nicht geändert. Programm wird beendet.");
                return;
            }

            // Weiter mit dem nächsten Schritt: Root-Seite öffnen und Signal-Provider verarbeiten
            logger.info("\n--- Verarbeitung der Signal-Provider startet ---");
            int currentPage = 1;
            boolean hasNextPage = true;
            while (hasNextPage) {
                // Öffne die Seite der aktuellen Signal-Provider-Liste
                String pageUrl = "https://www.mql5.com/en/signals/mt5/list/page" + currentPage;
                logger.info("Öffne die Seite: " + pageUrl);
                driver.get(pageUrl);

                // Warten, bis die Liste der Signal-Provider geladen ist
                logger.info("Warte darauf, dass die Liste der Signal-Provider geladen ist...");
                wait.until(ExpectedConditions.presenceOfElementLocated(By.className("signal")));
                logger.info("Liste der Signal-Provider geladen.");

                // Alle Links zu den Signal-Providern sammeln
                List<WebElement> providerLinks = driver.findElements(By.cssSelector(".signal a[href*='/signals/']"));
                if (providerLinks == null || providerLinks.isEmpty()) {
                    logger.warn("Keine Signal-Provider gefunden. Programm wird beendet.");
                    hasNextPage = false;
                    break;
                }
                logger.info("Anzahl der gefundenen Signal-Provider: " + providerLinks.size());

                int downloadCounter = 0;
                for (int i = 0; i < providerLinks.size(); i++) {
                    // StaleElementReferenceException vermeiden, indem das Element erneut gesucht wird
                    providerLinks = driver.findElements(By.cssSelector(".signal a[href*='/signals/']"));
                    if (i >= providerLinks.size()) {
                        logger.warn("Die Anzahl der Signal-Provider hat sich geändert. Überspringe aktuellen Index.");
                        continue;
                    }
                    WebElement link = providerLinks.get(i);

                    // URL und Namen des Signal-Providers extrahieren
                    String providerUrl = link.getAttribute("href");
                    String providerName = link.getText().trim();
                    logger.info("\nVerarbeite Signal-Provider: " + providerName);
                    logger.info("Signal-Provider URL: " + providerUrl);

                    // Zum Signal-Provider navigieren
                    driver.get(providerUrl);

                    // Auf den "Trade History"-Tab klicken
                    logger.info("Öffne den 'Trade History'-Tab...");
                    WebElement tradeHistoryTab = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[text()='Trading history']")));
                    if (tradeHistoryTab == null) {
                        logger.error("'Trade History'-Tab konnte nicht gefunden werden.");
                        continue;
                    }
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", tradeHistoryTab);

                    // Warten, bis der Downloadlink sichtbar ist
                    logger.info("Warte darauf, dass der 'Export to CSV'-Link sichtbar ist...");
                    List<WebElement> exportLinks = driver.findElements(By.xpath("//*[text()='History']"));
                    if (exportLinks == null || exportLinks.isEmpty()) {
                        logger.warn("'Export to CSV'-Link konnte nicht gefunden werden. Überspringe diesen Signal-Provider.");
                        continue;
                    }
                    WebElement exportLink = exportLinks.get(exportLinks.size() - 1); // Nimm den letzten Link mit dem Text 'History'

                    // Den Downloadlink anklicken
                    logger.info("Klicke den 'Export to CSV'-Link (komplette URL zum Download)...");
                    logger.info("Komplette URL zum CSV-Download: " + exportLink.getAttribute("href"));
                    exportLink.click();

                    // Warten, bis die Datei heruntergeladen wurde (angepasste Wartezeit)
                    logger.info("Warte darauf, dass die Datei heruntergeladen wird...");
                    Thread.sleep(getRandomWaitTime()); // Zufällige Wartezeit, um nicht als Bot erkannt zu werden

                    // Prüfen, ob die heruntergeladene Datei vorhanden ist
                    File downloadedFile = findDownloadedFile(additionalDownloadPath);
                    if (downloadedFile != null && downloadedFile.exists()) {
                        // Speichere die Datei mit dem Namen des Signal-Providers
                        File targetFile = new File(additionalDownloadPath, providerName.replaceAll("[\\/:*?\"<>|]", "_") + ".csv");
                        Files.move(downloadedFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        downloadCounter++;
                        logger.info("Datei für " + providerName + " heruntergeladen und gespeichert in: " + targetFile.getAbsolutePath());
                        logger.info("Anzahl der bisher heruntergeladenen Dateien: " + downloadCounter);
                    } else {
                        logger.warn("Die Datei wurde nicht gefunden, eventuell ist der Download fehlgeschlagen.");
                    }

                    // Zurück zur Root-Seite, um den nächsten Signal-Provider zu verarbeiten
                    logger.info("Zurück zur Seite der Signal-Provider...");
                    driver.get(pageUrl);
                }

                // Zur nächsten Seite wechseln
                currentPage++;
                logger.info("Wechsel zur nächsten Seite: " + currentPage);
            }
        } catch (InterruptedException | IOException e) {
            logger.error("Fehler während der Verarbeitung", e);
        } finally {
            // Browser schließen, aber Dateien nicht löschen
            logger.info("\n--- Abschluss ---");
            logger.info("Schließe den Browser...");
            if (driver != null) {
                try {
                    // Wartezeit, damit der Downloadprozess vollständig abgeschlossen wird
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    logger.error("Fehler beim Warten vor dem Schließen des Browsers", e);
                }
                driver.quit();
            }
        }
    }

    private static int getRandomWaitTime() {
        return (int) (Math.random() * (30000 - 10000)) + 10000; // Zufällige Wartezeit zwischen 10 und 30 Sekunden
    }

    private static void configureDefaultLogger() {
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        builder.setStatusLevel(org.apache.logging.log4j.Level.ERROR);
        builder.setConfigurationName("DefaultConfig");

        // Console Appender erstellen
        builder.add(builder.newAppender("Console", "CONSOLE")
                .add(builder.newLayout("PatternLayout")
                        .addAttribute("pattern", "%d{yyyy-MM-dd HH:mm:ss} [%t] %-5level %logger{36} - %msg%n")));

        // File Appender erstellen
        builder.add(builder.newAppender("LogToFile", "File")
                .addAttribute("fileName", "logs/application.log")
                .add(builder.newLayout("PatternLayout")
                        .addAttribute("pattern", "%d{yyyy-MM-dd HH:mm:ss} [%t] %-5level %logger{36} - %msg%n")));

        // Root Logger konfigurieren
        builder.add(builder.newRootLogger(org.apache.logging.log4j.Level.INFO)
                .add(builder.newAppenderRef("Console"))
                .add(builder.newAppenderRef("LogToFile")));

        Configurator.initialize(builder.build());
    }

    private static File findDownloadedFile(String downloadDirectory) {
        File dir = new File(downloadDirectory);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".csv"));
        if (files != null && files.length > 0) {
            return files[0];
        }
        return null;
    }
}
