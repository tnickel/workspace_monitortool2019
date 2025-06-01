package reports;

import java.util.List;
import java.util.logging.Logger;

/**
 * Klasse für die Integration von PDF-Links in HTML-Reports
 */
public class HtmlPdfIntegrator {
    private static final Logger LOGGER = Logger.getLogger(HtmlPdfIntegrator.class.getName());
    
    private final PdfManager pdfManager;
    
    /**
     * Konstruktor für den HtmlPdfIntegrator
     * 
     * @param pdfManager Der PdfManager für PDF-Operationen
     */
    public HtmlPdfIntegrator(PdfManager pdfManager) {
        this.pdfManager = pdfManager;
    }
    
    /**
     * Generiert HTML-Code für PDF-Links eines Providers
     * 
     * @param providerId Die Provider-ID
     * @return HTML-String mit PDF-Links oder leer String wenn keine PDFs vorhanden
     */
    public String generatePdfLinksHtml(String providerId) {
        List<String> pdfFiles = pdfManager.findPdfsForProvider(providerId);
        
        if (pdfFiles.isEmpty()) {
            return ""; // Keine PDFs gefunden
        }
        
        StringBuilder htmlBuilder = new StringBuilder();
        
        // PDF-Sektion beginnen
        htmlBuilder.append("<div class=\"pdf-section\">\n");
        htmlBuilder.append("<h3>Zusätzliche Analysen</h3>\n");
        htmlBuilder.append("<div class=\"pdf-links\">\n");
        
        // Für jede PDF-Datei einen Button erstellen
        for (String pdfFileName : pdfFiles) {
            String displayName = pdfManager.getDisplayNameForPdf(pdfFileName);
            String relativePath = pdfManager.getRelativePdfPath(pdfFileName);
            
            htmlBuilder.append("<div class=\"pdf-link-container\">\n");
            htmlBuilder.append("<a href=\"").append(relativePath).append("\" ");
            htmlBuilder.append("target=\"_blank\" class=\"pdf-button\">");
            htmlBuilder.append("<span class=\"pdf-icon\">📄</span> ");
            htmlBuilder.append(displayName);
            htmlBuilder.append("</a>\n");
            htmlBuilder.append("</div>\n");
        }
        
        htmlBuilder.append("</div>\n"); // Ende pdf-links
        htmlBuilder.append("</div>\n"); // Ende pdf-section
        
        LOGGER.info("PDF-Links für Provider " + providerId + " generiert: " + pdfFiles.size() + " PDFs");
        return htmlBuilder.toString();
    }
    
    /**
     * Generiert CSS-Styles für PDF-Links
     * 
     * @return CSS-String für PDF-Styling
     */
    public String generatePdfCss() {
        return 
            "        /* PDF-Sektion Styling */\n" +
            "        .pdf-section {\n" +
            "            background-color: #f0f8ff;\n" +
            "            padding: 15px;\n" +
            "            border-radius: 5px;\n" +
            "            margin-bottom: 20px;\n" +
            "            border-left: 4px solid #3498db;\n" +
            "        }\n" +
            "        .pdf-section h3 {\n" +
            "            margin-top: 0;\n" +
            "            color: #2c3e50;\n" +
            "            border-bottom: 2px solid #3498db;\n" +
            "            padding-bottom: 5px;\n" +
            "        }\n" +
            "        .pdf-links {\n" +
            "            display: flex;\n" +
            "            flex-wrap: wrap;\n" +
            "            gap: 10px;\n" +
            "            margin-top: 15px;\n" +
            "        }\n" +
            "        .pdf-link-container {\n" +
            "            flex: 0 0 auto;\n" +
            "        }\n" +
            "        .pdf-button {\n" +
            "            display: inline-block;\n" +
            "            background: linear-gradient(135deg, #3498db, #2980b9);\n" +
            "            color: white;\n" +
            "            padding: 10px 15px;\n" +
            "            text-decoration: none;\n" +
            "            border-radius: 6px;\n" +
            "            font-weight: bold;\n" +
            "            font-size: 14px;\n" +
            "            transition: all 0.3s ease;\n" +
            "            box-shadow: 0 2px 4px rgba(0,0,0,0.2);\n" +
            "            border: 1px solid #2980b9;\n" +
            "        }\n" +
            "        .pdf-button:hover {\n" +
            "            background: linear-gradient(135deg, #2980b9, #1f639a);\n" +
            "            transform: translateY(-2px);\n" +
            "            box-shadow: 0 4px 8px rgba(0,0,0,0.3);\n" +
            "            text-decoration: none;\n" +
            "        }\n" +
            "        .pdf-button:active {\n" +
            "            transform: translateY(0);\n" +
            "            box-shadow: 0 2px 4px rgba(0,0,0,0.2);\n" +
            "        }\n" +
            "        .pdf-icon {\n" +
            "            margin-right: 5px;\n" +
            "            font-size: 16px;\n" +
            "        }\n" +
            "        \n" +
            "        /* Responsive Design für PDF-Links */\n" +
            "        @media (max-width: 768px) {\n" +
            "            .pdf-links {\n" +
            "                flex-direction: column;\n" +
            "            }\n" +
            "            .pdf-button {\n" +
            "                width: 100%;\n" +
            "                text-align: center;\n" +
            "            }\n" +
            "        }\n";
    }
    
    /**
     * Generiert kompakte PDF-Links für das Inhaltsverzeichnis
     * 
     * @param providerId Die Provider-ID
     * @return HTML-String mit kompakten PDF-Info oder leerer String
     */
    public String generateTocPdfInfo(String providerId) {
        List<String> pdfFiles = pdfManager.findPdfsForProvider(providerId);
        
        if (pdfFiles.isEmpty()) {
            return "";
        }
        
        // Kompakte Anzeige für das Inhaltsverzeichnis
        if (pdfFiles.size() == 1) {
            return " <span class=\"toc-pdf-info\">[1 PDF]</span>";
        } else {
            return " <span class=\"toc-pdf-info\">[" + pdfFiles.size() + " PDFs]</span>";
        }
    }
    
    /**
     * Generiert CSS für Inhaltsverzeichnis PDF-Info
     * 
     * @return CSS-String für TOC PDF-Styling
     */
    public String generateTocPdfCss() {
        return 
            "        /* TOC PDF-Info Styling */\n" +
            "        .toc-pdf-info {\n" +
            "            color: #3498db;\n" +
            "            font-size: 0.9em;\n" +
            "            font-weight: normal;\n" +
            "            background-color: #ecf0f1;\n" +
            "            padding: 2px 6px;\n" +
            "            border-radius: 3px;\n" +
            "            margin-left: 5px;\n" +
            "        }\n";
    }
    
    /**
     * Kopiert alle PDFs für eine Liste von Provider-IDs und gibt HTML-Status zurück
     * 
     * @param providerIds Liste der Provider-IDs
     * @return HTML-String mit Kopier-Status-Information
     */
    public String copyPdfsAndGenerateStatus(List<String> providerIds) {
        java.util.Map<String, List<String>> copiedPdfs = pdfManager.copyPdfsForProviders(providerIds);
        
        if (copiedPdfs.isEmpty()) {
            return ""; // Keine PDFs gefunden
        }
        
        StringBuilder statusBuilder = new StringBuilder();
        statusBuilder.append("<!-- PDF-Kopier-Status:\n");
        
        int totalPdfs = 0;
        for (java.util.Map.Entry<String, List<String>> entry : copiedPdfs.entrySet()) {
            String providerId = entry.getKey();
            List<String> pdfs = entry.getValue();
            totalPdfs += pdfs.size();
            
            statusBuilder.append("Provider ").append(providerId).append(": ");
            statusBuilder.append(pdfs.size()).append(" PDFs kopiert\n");
        }
        
        statusBuilder.append("Gesamt: ").append(totalPdfs).append(" PDF-Dateien kopiert\n");
        statusBuilder.append("-->\n");
        
        LOGGER.info("PDFs für " + providerIds.size() + " Provider kopiert. Gesamt: " + totalPdfs + " PDFs");
        return statusBuilder.toString();
    }
    
    /**
     * Prüft, ob für einen Provider PDF-Dateien verfügbar sind
     * 
     * @param providerId Die Provider-ID
     * @return true wenn PDFs vorhanden sind, false sonst
     */
    public boolean hasPdfsForProvider(String providerId) {
        return pdfManager.hasPdfsForProvider(providerId);
    }
}