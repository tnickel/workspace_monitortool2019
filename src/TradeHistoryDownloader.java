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

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class TradeHistoryDownloader {

    public static void main(String[] args) {
        // Root-Verzeichnis und Konfigurationsverzeichnis festlegen
        String rootDirPath = "C:\\tmp\\mql5";
        String configDirPath = rootDirPath + "\\conf";
        String configFilePath = configDirPath + "\\conf.txt";

        // Erstelle das Konfigurationsverzeichnis, falls es nicht existiert
        File configDir = new File(configDirPath);
        if (!configDir.exists()) {
            configDir.mkdirs();
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
                e.printStackTrace();
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
                    e.printStackTrace();
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
        prefs.put("download.default_directory", downloadFilepath);

        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", prefs);

        // WebDriver mit Downloadpfad-Einstellungen initialisieren
        WebDriver driver = new ChromeDriver(options);

        try {
            // Login auf der Webseite durchführen
            System.out.println("\n--- Login-Prozess startet ---");
            System.out.println("Öffne Login-Seite...");
            driver.get("https://www.mql5.com/en/auth_login");

            // Wartezeit erhöhen, um sicherzustellen, dass die Login-Seite vollständig geladen ist
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));

            // Benutzername und Passwort eingeben
            System.out.println("Warte darauf, dass das Benutzername-Feld sichtbar wird...");
            WebElement usernameField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("Login")));
            WebElement passwordField = driver.findElement(By.id("Password"));

            // Login-Daten eingeben
            System.out.println("Gebe Benutzername und Passwort ein...");
            usernameField.sendKeys(username);
            passwordField.sendKeys(password);

            // Versuche, den Login-Button zu finden und zu klicken
            WebElement loginButton = null;

            System.out.println("Versuche, den Login-Button zu finden...");
            try {
                // Versuche den Login-Button über die ID zu finden
                loginButton = driver.findElement(By.id("loginSubmit"));
                System.out.println("Login-Button mit ID 'loginSubmit' gefunden.");
            } catch (Exception e) {
                System.out.println("Button mit ID 'loginSubmit' nicht gefunden, versuche anderen Selektor.");
            }

            if (loginButton == null) {
                try {
                    // Fallback: Versuche den Login-Button über die Klasse zu finden
                    loginButton = driver.findElement(By.cssSelector("input.button.button_yellow.qa-submit"));
                    System.out.println("Login-Button mit CSS-Selektor 'input.button.button_yellow.qa-submit' gefunden.");
                } catch (Exception e) {
                    System.out.println("Button mit CSS-Selektor 'input.button.button_yellow.qa-submit' nicht gefunden, konnte nicht einloggen.");
                    return; // Wenn alle Locator-Strategien fehlschlagen, wird das Programm beendet
                }
            }

            // Login-Button durch JavaScript klicken, falls er gefunden wurde
            if (loginButton != null) {
                System.out.println("Klicke den Login-Button...");
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", loginButton);
            } else {
                System.out.println("Login-Button konnte nicht gefunden werden.");
                return;
            }

            // Sicherstellen, dass der Login erfolgreich war
            try {
                System.out.println("Überprüfe, ob die URL sich nach dem Login ändert...");
                wait.until(ExpectedConditions.urlContains("/en"));
                System.out.println("Login erfolgreich: Die URL hat sich geändert.");
                Thread.sleep(2000); // Verzögerung von 2 Sekunden nach erfolgreichem Login
            } catch (Exception e) {
                System.out.println("Login war nicht erfolgreich oder die URL hat sich nicht geändert. Programm wird beendet.");
                return;
            }

            // Weiter mit dem nächsten Schritt: Root-Seite öffnen und Signal-Provider verarbeiten
            System.out.println("\n--- Verarbeitung der Signal-Provider startet ---");
            System.out.println("Öffne die Root-Seite für die Liste der Signal-Provider...");
            driver.get("https://www.mql5.com/en/signals/mt5/list");

            // Warten, bis die Liste der Signal-Provider geladen ist
            System.out.println("Warte darauf, dass die Liste der Signal-Provider geladen ist...");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.className("signal")));
            System.out.println("Liste der Signal-Provider geladen.");

            // Alle Links zu den Signal-Providern sammeln
            List<WebElement> providerLinks = driver.findElements(By.cssSelector(".signal a[href*='/signals/']"));
            System.out.println("Anzahl der gefundenen Signal-Provider: " + providerLinks.size());

            for (int i = 0; i < providerLinks.size(); i++) {
                // StaleElementReferenceException vermeiden, indem das Element erneut gesucht wird
                providerLinks = driver.findElements(By.cssSelector(".signal a[href*='/signals/']"));
                WebElement link = providerLinks.get(i);

                // URL und Namen des Signal-Providers extrahieren
                String providerUrl = link.getAttribute("href");
                String providerName = link.getText().trim();
                System.out.println("\nVerarbeite Signal-Provider: " + providerName);

                // Zum Signal-Provider navigieren
                driver.get(providerUrl);

                // Auf den "Trade History"-Tab klicken
                System.out.println("Öffne den 'Trade History'-Tab...");
                WebElement tradeHistoryTab = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[text()='Trading history']")));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", tradeHistoryTab);

                // Warten, bis der Downloadlink sichtbar ist
                System.out.println("Warte darauf, dass der 'Export to CSV'-Link sichtbar ist...");
                List<WebElement> exportLinks = driver.findElements(By.xpath("//*[text()='History']"));
                WebElement exportLink = exportLinks.get(exportLinks.size() - 1); // Nimm den letzten Link mit dem Text 'History'

                // Den Downloadlink anklicken
                System.out.println("Klicke den 'Export to CSV'-Link...");
                exportLink.click();

                // Warten, bis die Datei heruntergeladen wurde (angepasste Wartezeit)
                System.out.println("Warte darauf, dass die Datei heruntergeladen wird...");
                Thread.sleep(5000); // Diese Wartezeit anpassen, falls nötig

                // Datei vom Download-Ordner ins Zielverzeichnis verschieben und umbenennen
                File[] downloadedFiles = new File(downloadFilepath).listFiles((dir, name) -> name.endsWith(".csv"));
                if (downloadedFiles != null && downloadedFiles.length > 0) {
                    for (File downloadedFile : downloadedFiles) {
                        // Speichere die Originaldatei im Unterverzeichnis "download" mit dem Namen des Signal-Providers
                        File originalFile = new File(additionalDownloadPath, providerName.replaceAll("[\\\\/:*?\"<>|]", "_") + ".csv");
                        Files.copy(downloadedFile.toPath(), originalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                        // Zieldatei mit dem Namen des Signal-Providers speichern
                        File targetFile = new File(downloadFilepath, providerName.replaceAll("[\\\\/:*?\"<>|]", "_") + ".csv");
                        Files.move(downloadedFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("Datei für " + providerName + " heruntergeladen und gespeichert in: " + targetFile.getAbsolutePath());
                    }
                } else {
                    System.out.println("Die Datei wurde nicht gefunden, eventuell ist der Download fehlgeschlagen.");
                }

                // Zurück zur Root-Seite, um den nächsten Signal-Provider zu verarbeiten
                System.out.println("Zurück zur Root-Seite, um den nächsten Signal-Provider zu verarbeiten...");
                driver.get("https://www.mql5.com/en/signals/mt5/list");
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        } finally {
            // Browser schließen, aber Dateien nicht löschen
            System.out.println("\n--- Abschluss ---");
            System.out.println("Schließe den Browser...");
            if (driver != null) {
                try {
                    // Wartezeit, damit der Downloadprozess vollständig abgeschlossen wird
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                driver.quit();
            }
        }
    }
}
