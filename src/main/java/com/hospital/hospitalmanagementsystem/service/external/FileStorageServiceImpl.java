package com.hospital.hospitalmanagementsystem.service.external;

import com.hospital.hospitalmanagementsystem.model.Invoice;
import com.hospital.hospitalmanagementsystem.model.Payment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final String UPLOAD_DIR = "uploads/bank-slips/";

    @Override
    public String storeBankSlip(MultipartFile file, Long paymentId) {
        try {
            // Create directory if it doesn't exist
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String fileName = "bankslip_" + paymentId + "_" + UUID.randomUUID().toString() +
                    getFileExtension(file.getOriginalFilename());

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store bank slip file", e);
        }
    }

    @Override
    public byte[] getBankSlipFile(String fileName) {
        try {
            Path filePath = Paths.get(UPLOAD_DIR).resolve(fileName);
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read bank slip file", e);
        }
    }

    @Override
    public byte[] generateInvoicePDF(Invoice invoice) {
        // Simplified PDF generation - you would use a library like iText or PDFBox
        String pdfContent = "Invoice PDF for " + invoice.getInvoiceNumber();
        return pdfContent.getBytes();
    }

    @Override
    public byte[] generateReceiptPDF(Payment payment) {
        // Simplified PDF generation - you would use a library like iText or PDFBox
        String pdfContent = "Receipt PDF for " + payment.getTransactionId();
        return pdfContent.getBytes();
    }

    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf("."));
        }
        return "";
    }
}