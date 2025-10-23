package com.hospital.hospitalmanagementsystem.service.external;

import com.hospital.hospitalmanagementsystem.model.Invoice;
import com.hospital.hospitalmanagementsystem.model.Payment;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeBankSlip(MultipartFile file, Long paymentId);
    byte[] getBankSlipFile(String fileName);
    byte[] generateInvoicePDF(Invoice invoice);
    byte[] generateReceiptPDF(Payment payment);
}