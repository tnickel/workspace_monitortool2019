# MQL5 Signal Provider Analyzer 📊

Ein umfassendes Java-Desktop-Tool zur professionellen Analyse und Bewertung von MQL5 Trading Signal Providern. Entwickelt für Trader und Investoren, die fundierte Entscheidungen bei der Auswahl von Signal-Anbietern treffen möchten.

## 🌟 Hauptfeatures

### 📈 Datenimport & Verarbeitung
- **CSV-Import**: Automatischer Import von MQL5 Handelsdaten im CSV-Format
- **Multi-Format-Unterstützung**: Erkennt automatisch MT4 und MQL5 Datenformate
- **Batch-Verarbeitung**: Gleichzeitige Analyse mehrerer Signal Provider
- **Echtzeit-Fortschrittsanzeige**: Visuelles Feedback während des Datenimports

### 📊 Umfangreiche Chart-Visualisierungen
- **Equity Drawdown Charts**: Detaillierte Darstellung des Risikos mit dynamischer Skalierung
- **Currency Pair Analysis**: Visualisierung offener Trades und Lots pro Währungspaar
- **Trade Stacking**: Zeitlicher Verlauf gleichzeitig offener Positionen
- **Efficiency Charts**: Wöchentliche Effizienzanalyse (Gewinn/Lotsize-Verhältnis)
- **Duration/Profit Charts**: Korrelation zwischen Handelsdauer und Profitabilität
- **Symbol Distribution**: Kreisdiagramme zur Währungspaar-Verteilung
- **Monthly Performance**: Monatliche Handelsaktivität und Performance-Übersicht
- **3MPDD Verlauf**: Historische Entwicklung des 3-Monats-Profit-Drawdown-Verhältnisses

### 🎯 Erweiterte Analysetools
- **Martingale-Erkennung**: Automatische Identifikation von Martingale-Handelsmustern
- **Risiko-Score-Berechnung**: Proprietärer Algorithmus zur Risikobewertung
- **Stabilitätsanalyse**: Bewertung der Handelskonsistenz
- **Drawdown-Analyse**: Maximaler Drawdown, 3-Monats-Drawdown, Equity Drawdown
- **Performance-Metriken**: Win-Rate, Profit Factor, durchschnittlicher Profit pro Trade

### 💾 Datenbank & Historie
- **H2 Embedded Database**: Persistente Speicherung historischer Daten
- **Automatische Backups**: Regelmäßige Sicherung wichtiger Daten
- **Historientracking**: Verfolgung von Änderungen über Zeit
- **Provider-Notizen**: Persönliche Anmerkungen zu jedem Signal Provider

### ⭐ Favoriten-Management
- **10 Kategorien**: Flexible Organisation mit farbcodierten Kategorien
- **Smart Filtering**: Schneller Zugriff auf favorisierte Provider
- **Bad Provider Liste**: Markierung unerwünschter Signal Provider
- **Kategorie-basierte Reports**: Separate Berichte für jede Favoriten-Kategorie

### 📑 Report-Generator
- **HTML-Export**: Professionelle Reports mit eingebetteten Charts
- **Batch-Reports**: Gleichzeitige Erstellung für mehrere Provider
- **Customizable**: Anpassbare Report-Inhalte und -Formate
- **Chart-Integration**: Automatische Einbindung aller relevanten Grafiken
- **Kategorie-Reports**: Spezielle Reports für Favoriten-Kategorien

### 🔍 Filter & Suche
- **Multi-Kriterien-Filter**: Filterung nach allen verfügbaren Metriken
- **Speicherbare Filter**: Wiederverwendbare Filtereinstellungen
- **Echtzeit-Suche**: Schnelle Suche mit Highlighting
- **Spalten-Konfiguration**: Ein-/Ausblenden von Tabellenspalten

### 🖥️ Benutzeroberfläche
- **Modernes Design**: Übersichtliche und intuitive Oberfläche
- **Responsive Tables**: Sortierbare und konfigurierbare Datentabellen
- **Tooltips**: Kontextsensitive Hilfe und detaillierte Informationen
- **Split-View**: Gleichzeitige Anzeige mehrerer Provider
- **Dark/Light Theme**: Anpassbares Farbschema

### 🔧 Technische Features
- **Multi-Threading**: Performante Verarbeitung großer Datenmengen
- **Cache-System**: Optimierte Ladezeiten durch intelligentes Caching
- **Fehlerbehandlung**: Robuste Fehlerbehandlung mit aussagekräftigen Meldungen
- **Logging**: Umfassendes Logging für Debugging und Analyse

## 🚀 Installation & Verwendung

### Systemanforderungen
- Java 11 oder höher
- Windows/Linux/MacOS
- Mindestens 4GB RAM empfohlen
- 500MB freier Speicherplatz

### Installation
1. Repository klonen
2. Mit Maven oder Gradle bauen
3. JAR-Datei ausführen

### Erste Schritte
1. MQL5 Signal Provider CSV-Dateien in den Download-Ordner kopieren
2. Anwendung starten und automatischen Import abwarten
3. Provider analysieren und favorisieren
4. Reports generieren

## 📁 Projektstruktur
```
src/
├── charts/          # Chart-Komponenten
├── components/      # UI-Komponenten
├── data/           # Datenmodelle
├── db/             # Datenbank-Manager
├── models/         # Geschäftslogik-Modelle
├── renderers/      # Tabellen-Renderer
├── reports/        # Report-Generator
├── services/       # Business Services
├── ui/             # Dialoge und Frames
└── utils/          # Hilfsklassen
```

## 🛠️ Verwendete Technologien
- **Java Swing**: Desktop-UI Framework
- **JFreeChart**: Chart-Bibliothek
- **H2 Database**: Embedded SQL-Datenbank
- **JavaFX WebView**: Web-Content Integration
- **Apache POI**: Excel-Export (optional)

## 📊 Unterstützte Metriken
- Total Trades & Win Rate
- Profit Factor & Total Profit
- Maximum/Average Drawdown
- 3/6/9/12 MPDD (Monthly Profit Drawdown Ratio)
- Concurrent Trades & Lots
- Trade Duration Analysis
- Symbol Distribution
- Monthly Performance

## 🤝 Beitragen
Contributions sind willkommen! Bitte erstellen Sie einen Pull Request mit einer detaillierten Beschreibung Ihrer Änderungen.

## 📄 Lizenz
Dieses Projekt ist unter der MIT-Lizenz lizenziert.

## 🎯 Zielgruppe
- Professionelle Trader
- Signal-Follower
- Investmentmanager
- Trading-Analysten
- Risikomanager

## 🌐 Sprachen
- Deutsche Benutzeroberfläche
- Englische Dokumentation
- Erweiterbar für weitere Sprachen

---

**Hinweis**: Dieses Tool dient ausschließlich zu Analysezwecken. Handelsentscheidungen sollten immer auf eigener Due Diligence basieren.
