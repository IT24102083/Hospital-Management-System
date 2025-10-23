//package com.hospital.hospitalmanagementsystem.service;
//
//import com.hospital.hospitalmanagementsystem.model.Order;
//import io.github.birddevelper.salmos.HtmlReportMaker;
//import io.github.birddevelper.salmos.setting.SummaryType;
//import org.springframework.stereotype.Service;
//
//import javax.sql.DataSource;
//import java.io.File;
//import java.nio.file.Files;
//import java.time.format.DateTimeFormatter;
//
//@Service
//public class ReportService {
//
//    private final DataSource dataSource;
//
//    public ReportService(DataSource dataSource) {
//        this.dataSource = dataSource;
//    }
//
//    public byte[] generateOrderReceiptPdf(Order order) throws Exception {
//
//        HtmlReportMaker hrm = new HtmlReportMaker(dataSource);
//
//        String sqlQuery = String.format(
//                "SELECT m.name AS 'Item', oi.quantity AS 'Qty', oi.price_per_item AS 'Unit Price', oi.line_total AS 'Total' " +
//                        "FROM order_items oi " +
//                        "JOIN medicines m ON oi.medicine_id = m.id " +
//                        "WHERE oi.order_id = %d",
//                order.getId()
//        );
//        hrm.setSqlQuery(sqlQuery);
//
//        hrm.addSummaryColumn("Total", SummaryType.SUM);
//        hrm.setSummaryDecimalPrecision(2);
//        hrm.setRowIndexVisible(true);
//
//        String tableHtml = hrm.generate();
//
//        String fullHtml = String.format(
//                "<html><head><style>" +
//                        "body { font-family: sans-serif; color: #333; }" +
//                        "h1 { color: #2563eb; text-align: center; }" +
//                        ".info { margin-bottom: 20px; border-bottom: 1px solid #eee; padding-bottom: 10px; }" +
//                        ".info p { margin: 0; }" +
//                        "table { width: 100%%; border-collapse: collapse; font-size: 12px; }" +
//                        "th, td { border: 1px solid #ccc; padding: 8px; text-align: left; }" +
//                        "th { background-color: #f2f2f2; }" +
//                        ".footer { text-align: right; margin-top: 20px; font-size: 14px; font-weight: bold; }" +
//                        "</style></head><body>" +
//                        "<h1>HealthFirst Pharmacy Receipt</h1>" +
//                        "<div class='info'>" +
//                        "<p><b>Order ID:</b> %s</p>" +
//                        "<p><b>Patient:</b> %s</p>" +
//                        "<p><b>Date:</b> %s</p>" +
//                        "</div>" +
//                        "%s" + // Placeholder for the table
//                        "<div class='footer'><p>Grand Total: $ %s</p></div>" +
//                        "</body></html>",
//                order.getId(),
//                order.getPatient().getFirstName() + " " + order.getPatient().getLastName(),
//                order.getOrderDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
//                tableHtml,
//                order.getTotalAmount()
//        );
//
//        File tempFile = File.createTempFile("receipt-", ".pdf");
//        try {
//            // <<< FIX: Changed the last 'null' to an empty string "" >>>
//            hrm.generatePDF(tempFile.getAbsolutePath(), null, "");
//
//            byte[] pdfBytes = Files.readAllBytes(tempFile.toPath());
//            return pdfBytes;
//        } finally {
//            if (tempFile != null) {
//                tempFile.delete();
//            }
//        }
//    }
//}

/* iText (html2pdf): We will use this powerful library to convert the final, complete HTML document into a PDF. */


package com.hospital.hospitalmanagementsystem.service;

import com.hospital.hospitalmanagementsystem.model.Invoice;
import com.hospital.hospitalmanagementsystem.model.Order;
import com.itextpdf.html2pdf.HtmlConverter; // iText dependency for PDF conversion
import io.github.birddevelper.salmos.HtmlReportMaker;
import io.github.birddevelper.salmos.setting.SummaryType;

import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;


@Service
public class ReportService {

    private final DataSource dataSource;


    public ReportService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public byte[] generateOrderReceiptPdf(Order order) throws Exception {

        // --- STEP 1: Use HtmlReportMaker to generate ONLY the HTML table ---
        HtmlReportMaker hrm = new HtmlReportMaker(dataSource);

        String sqlQuery = String.format(
                "SELECT m.name AS 'Item', oi.quantity AS 'Qty', oi.price_per_item AS 'Unit Price', oi.line_total AS 'Total' " +
                        "FROM order_items oi " +
                        "JOIN medicines m ON oi.medicine_id = m.id " +
                        "WHERE oi.order_id = %d",
                order.getId()
        );
        hrm.setSqlQuery(sqlQuery);
        hrm.addSummaryColumn("Total", SummaryType.SUM);
        hrm.setSummaryDecimalPrecision(2);
        hrm.setRowIndexVisible(true);

        String tableHtml = hrm.generate(); // This gets the <table>...</table> string from Salmos

        // --- STEP 2: Create a complete HTML document with a custom header ---
        String fullHtml = String.format(
                "<html><head><style>" +
                        "body { font-family: sans-serif; color: #333; }" +
                        "h1 { color: #2563eb; text-align: center; }" +
                        ".info { margin-bottom: 20px; border-bottom: 1px solid #eee; padding-bottom: 10px; }" +
                        ".info p { margin: 0; }" +
                        "table { width: 100%%; border-collapse: collapse; font-size: 12px; }" +
                        "th, td { border: 1px solid #ccc; padding: 8px; text-align: left; }" +
                        "th { background-color: #f2f2f2; }" +
                        ".footer { text-align: right; margin-top: 20px; font-size: 14px; font-weight: bold; }" +
                        "</style></head><body>" +
                        "<h1>HealthFirst Pharmacy Receipt</h1>" +
                        "<div class='info'>" +
                        "<p><b>Order ID:</b> %s</p>" +
                        "<p><b>Patient:</b> %s</p>" +
                        "<p><b>Date:</b> %s</p>" +
                        "</div>" +
                        "%s" + // Placeholder where the generated table will be inserted
                        "<div class='footer'><p>Grand Total: $ %s</p></div>" +
                        "</body></html>",
                order.getId(),
                order.getPatient().getFirstName() + " " + order.getPatient().getLastName(),
                order.getOrderDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                tableHtml, // Insert the table generated by Salmos
                order.getTotalAmount()
        );

        // --- STEP 3: Convert the final HTML string to a PDF using iText ---
        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(fullHtml, pdfOutputStream);

        return pdfOutputStream.toByteArray();
    }

    /**
     * MODIFIED: Generates a PDF invoice for a doctor's appointment using HtmlReportMaker and iText.
     */
    public byte[] generateAppointmentInvoicePdf(Invoice invoice) throws Exception {
        HtmlReportMaker hrm = new HtmlReportMaker(dataSource);

        // SQL query to fetch items for this specific invoice
        String sqlQuery = String.format(
                "SELECT description AS 'Description', quantity AS 'Qty', unit_price AS 'Unit Price', line_total AS 'Total' " +
                        "FROM invoice_items " +
                        "WHERE invoice_id = %d",
                invoice.getId()
        );
        hrm.setSqlQuery(sqlQuery);
        hrm.addSummaryColumn("Total", SummaryType.SUM);
        hrm.setSummaryDecimalPrecision(2);

        String tableHtml = hrm.generate();

        String fullHtml = String.format(
                "<html><head><style>" +
                        "body { font-family: sans-serif; color: #333; }" +
                        "h1 { color: #2563eb; text-align: center; }" +
                        ".info { margin-bottom: 20px; border-bottom: 1px solid #eee; padding-bottom: 10px; }" +
                        ".info p { margin: 0; }" +
                        "table { width: 100%%; border-collapse: collapse; font-size: 12px; }" +
                        "th, td { border: 1px solid #ccc; padding: 8px; text-align: left; }" +
                        "th { background-color: #f2f2f2; }" +
                        ".footer { text-align: right; margin-top: 20px; font-size: 14px; font-weight: bold; }" +
                        "</style></head><body>" +
                        "<h1>HealthFirst Hospital Invoice</h1>" +
                        "<div class='info'>" +
                        "<p><b>Invoice #:</b> %s</p>" +
                        "<p><b>Patient:</b> %s</p>" +
                        "<p><b>Date Issued:</b> %s</p>" +
                        "<p><b>Consulting Doctor:</b> Dr. %s %s</p>" +
                        "</div>" +
                        "%s" + // Placeholder for the table
                        "<div class='footer'><p>Total Due: $ %s</p></div>" +
                        "</body></html>",
                invoice.getInvoiceNumber(),
                invoice.getPatient().getFirstName() + " " + invoice.getPatient().getLastName(),
                invoice.getIssueDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                invoice.getAppointment().getDoctor().getFirstName(),
                invoice.getAppointment().getDoctor().getLastName(),
                tableHtml,
                invoice.getTotal()
        );

        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(fullHtml, pdfOutputStream);

        return pdfOutputStream.toByteArray();
    }

    /**
     * ADDED: Generates a PDF report for the current medicine inventory status.
     * The report includes stock levels, expiry dates, and a calculated status.
     * @return A byte array containing the generated PDF.
     * @throws Exception if there is an error during report generation.
     */
    public byte[] generateInventoryReportPdf() throws Exception {
        HtmlReportMaker hrm = new HtmlReportMaker(dataSource);

        // SQL query to fetch inventory details and calculate a dynamic status
        String sqlQuery = "SELECT " +
                "name AS 'Medicine Name', " +
                "category AS 'Category', " +
                "stock AS 'Stock', " +
                "price AS 'Unit Price', " +
                "expiry_date AS 'Expiry Date', " +
                "CASE " +
                "  WHEN expiry_date < CURDATE() THEN 'Expired' " +
                "  WHEN stock <= 10 THEN 'Low Stock' " +
                "  ELSE 'In Stock' " +
                "END AS 'Status' " +
                "FROM medicines WHERE is_active = true ORDER BY name ASC";

        hrm.setSqlQuery(sqlQuery);
        // We don't need a summary row for this report
        hrm.setRowIndexVisible(true);

        String tableHtml = hrm.generate();

        String fullHtml = String.format(
                "<html><head><style>" +
                        "body { font-family: sans-serif; color: #333; }" +
                        "h1 { color: #2563eb; text-align: center; }" +
                        ".info { margin-bottom: 20px; border-bottom: 1px solid #eee; padding-bottom: 10px; }" +
                        ".info p { margin: 0; }" +
                        "table { width: 100%%; border-collapse: collapse; font-size: 12px; }" +
                        "th, td { border: 1px solid #ccc; padding: 8px; text-align: left; }" +
                        "th { background-color: #f2f2f2; }" +
                        "</style></head><body>" +
                        "<h1>Pharmacy Inventory Status Report</h1>" +
                        "<div class='info'>" +
                        "<p><b>Report Generated On:</b> %s</p>" +
                        "</div>" +
                        "%s" + // Placeholder for the table
                        "</body></html>",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss")),
                tableHtml
        );

        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(fullHtml, pdfOutputStream);

        return pdfOutputStream.toByteArray();
    }
}