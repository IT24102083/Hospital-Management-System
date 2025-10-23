package com.hospital.hospitalmanagementsystem.service;

import com.hospital.hospitalmanagementsystem.exception.PaymentProcessingException;
import com.hospital.hospitalmanagementsystem.model.*;
import com.hospital.hospitalmanagementsystem.repository.PaymentRepository;
import com.hospital.hospitalmanagementsystem.service.external.FileStorageService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate; // <-- ADDED IMPORT
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private com.hospital.hospitalmanagementsystem.repository.InvoiceRepository invoiceRepository;

    // ========== PAYMENT PROCESSING ==========

    // FIXED: Card payment with complete database updates
    @Transactional
    public Payment processCardPayment(Payment.CreateRequest request) {
        try {
            System.out.println("PaymentService: Processing card payment for invoice ID: " + request.getInvoiceId());

            Invoice invoice = invoiceService.getInvoiceById(request.getInvoiceId())
                    .orElseThrow(() -> new RuntimeException("Invoice not found"));

            System.out.println("Invoice found - Balance Due: " + invoice.getBalanceDue());

            validatePaymentAmount(invoice, request.getAmount());

            // Create payment record
            Payment payment = createBasePayment(invoice, request.getAmount(), Payment.PaymentMethod.CREDIT_CARD);

            // Simulate card processing (90% success for demo)
            boolean paymentSuccess = simulateCardPayment(request);

            System.out.println("Payment simulation result: " + paymentSuccess);

            if (paymentSuccess) {
                payment.setStatus(Payment.PaymentStatus.COMPLETED);
                payment.setNotes("Card payment processed successfully");

                // CRITICAL: Save payment first
                payment = paymentRepository.save(payment);
                System.out.println("Payment saved with ID: " + payment.getId());

                // CRITICAL: Update invoice with payment
                invoiceService.applyPayment(invoice.getId(), request.getAmount());
                System.out.println("Invoice updated");

                // Flush to ensure database update
                paymentRepository.flush();

                return payment;
            } else {
                payment.setStatus(Payment.PaymentStatus.FAILED);
                payment.setNotes("Card payment failed");
                return paymentRepository.save(payment);
            }
        } catch (Exception e) {
            System.err.println("Error processing card payment: " + e.getMessage());
            e.printStackTrace();
            throw new PaymentProcessingException("Card payment failed: " + e.getMessage(), e);
        }
    }

    // FIXED: Bank transfer with complete database updates
    public Payment processBankTransferPayment(Long invoiceId, BigDecimal amount, String referenceNumber,
                                              MultipartFile receiptFile, String transferNotes) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Invoice not found"));

            validatePaymentAmount(invoice, amount);

            // Create payment record
            Payment payment = createBasePayment(invoice, amount, Payment.PaymentMethod.BANK_TRANSFER);
            payment.setStatus(Payment.PaymentStatus.PENDING); // Needs verification
            payment.setReferenceNumber(referenceNumber);

            // Build notes
            StringBuilder notes = new StringBuilder("Bank transfer - Reference: " + referenceNumber);
            if (transferNotes != null && !transferNotes.isEmpty()) {
                notes.append(" | Notes: ").append(transferNotes);
            }

            // CRITICAL: Save payment FIRST to get the ID
            payment.setNotes(notes.toString());
            payment = paymentRepository.save(payment);

            // Store bank slip file AFTER saving payment (so we have the ID)
            if (receiptFile != null && !receiptFile.isEmpty()) {
                try {
                    String fileName = fileStorageService.storeBankSlip(receiptFile, payment.getId());
                    payment.setBankSlipPath(fileName);
                    payment.setNotes(payment.getNotes() + " | Bank slip uploaded: " + fileName);

                    // Update payment with file path
                    payment = paymentRepository.save(payment);

                    System.out.println("Bank slip saved for payment ID: " + payment.getId());

                } catch (IllegalArgumentException e) {
                    // Validation error from FileStorageService
                    payment.setNotes(payment.getNotes() + " | Bank slip upload failed: " + e.getMessage());
                    payment = paymentRepository.save(payment);
                    throw new RuntimeException("File validation failed: " + e.getMessage(), e);
                } catch (Exception e) {
                    // Other errors
                    payment.setNotes(payment.getNotes() + " | Bank slip upload failed: " + e.getMessage());
                    payment = paymentRepository.save(payment);
                    System.err.println("Failed to store bank slip: " + e.getMessage());
                }
            } else {
                // No file uploaded - this should be caught by controller validation
                payment.setNotes(payment.getNotes() + " | WARNING: No bank slip uploaded");
                payment = paymentRepository.save(payment);
            }

            return payment;

        } catch (Exception e) {
            throw new PaymentProcessingException("Bank transfer failed: " + e.getMessage(), e);
        }
    }

    public Payment processPOSPayment(String customerType, String patientSearch, String customerName,
                                     String customerPhone, String paymentType, String invoiceNumber,
                                     String paymentDescription, BigDecimal amount, String paymentMethod) {
        try {
            Payment payment = new Payment();
            payment.setAmount(amount);
            payment.setPaymentMethod(Payment.PaymentMethod.valueOf(paymentMethod));
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setTransactionId(generateTransactionId());
            // payment.setReceiptNumber(generateReceiptNumber()); // TODO: Add `receiptNumber` field and setter to the Payment model class.
            payment.setNotes("POS Payment: " + paymentDescription);
            payment.setPaymentDate(LocalDateTime.now());

            // Handle invoice linking if provided
            if (invoiceNumber != null && !invoiceNumber.isEmpty()) {
                try {
                    // First try to find by invoice ID
                    Invoice invoice = null;
                    try {
                        invoice = invoiceService.getInvoiceById(Long.valueOf(invoiceNumber))
                                .orElse(null);
                    } catch (NumberFormatException e) {
                        // Not a numeric ID, try by invoice number
                        invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber).orElse(null);
                    }

                    if (invoice != null) {
                        payment.setInvoice(invoice);
                        payment.setPatient(invoice.getPatient());
                        invoiceService.applyPayment(invoice.getId(), amount);
                    } else {
                        payment.setNotes(payment.getNotes() + " | Invoice " + invoiceNumber + " not found" +
                                " | Customer: " + customerName + " | Phone: " + customerPhone);
                    }
                } catch (Exception e) {
                    payment.setNotes(payment.getNotes() + " | Customer: " + customerName);
                }
            }

            return paymentRepository.save(payment);
        } catch (Exception e) {
            throw new PaymentProcessingException("POS payment failed: " + e.getMessage(), e);
        }
    }

    // ========== BANK SLIP VERIFICATION ==========

    // FIXED: Bank slip approval with database updates
    public Payment approveBankSlipPayment(Long paymentId, String verificationNotes) {
        Payment payment = getPaymentById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));

        if (payment.getPaymentMethod() != Payment.PaymentMethod.BANK_TRANSFER) {
            throw new IllegalArgumentException("Payment is not a bank transfer");
        }

        // Update payment status
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setVerificationNotes(verificationNotes);
        payment.setVerificationDate(LocalDateTime.now());

        // CRITICAL: Save payment first
        payment = paymentRepository.save(payment);

        // CRITICAL: Update invoice
        if (payment.getInvoice() != null) {
            invoiceService.applyPayment(payment.getInvoice().getId(), payment.getAmount());
        }

        return payment;
    }

    public Payment rejectBankSlipPayment(Long paymentId, String rejectionReason) {
        Payment payment = getPaymentById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));

        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setVerificationNotes(rejectionReason);
        payment.setVerificationDate(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    public Payment approvePartialBankSlipPayment(Long paymentId, BigDecimal verifiedAmount, String verificationNotes) {
        Payment payment = getPaymentById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));

        if (verifiedAmount.compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("Verified amount cannot exceed payment amount");
        }

        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setAmount(verifiedAmount);
        payment.setVerificationNotes(verificationNotes);
        payment.setVerificationDate(LocalDateTime.now());

        // CRITICAL: Save payment first
        payment = paymentRepository.save(payment);

        // CRITICAL: Update invoice with partial payment
        if (payment.getInvoice() != null) {
            invoiceService.applyPayment(payment.getInvoice().getId(), verifiedAmount);
        }

        return payment;
    }

    // ========== PAYMENT QUERIES ==========

    @Transactional(readOnly = true)
    public List<Payment> getPendingBankSlipVerifications() {
        return paymentRepository.findByPaymentMethodAndStatus(
                Payment.PaymentMethod.BANK_TRANSFER,
                Payment.PaymentStatus.PENDING
        );
    }

    @Transactional(readOnly = true)
    public Page<Payment> getPendingBankSlipsPaginated(Pageable pageable) {
        return paymentRepository.findByPaymentMethodAndStatusOrderByPaymentDateDesc(
                Payment.PaymentMethod.BANK_TRANSFER,
                Payment.PaymentStatus.PENDING,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId);
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByInvoiceId(Long invoiceId) {
        return paymentRepository.findByInvoiceIdOrderByPaymentDateDesc(invoiceId);
    }

    /**
     * ADDED: Method to get payments by date range for reporting.
     */
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        // Assumes a corresponding method exists in PaymentRepository
        return paymentRepository.findByPaymentDateBetween(startDateTime, endDateTime);
    }

    @Transactional(readOnly = true)
    public List<Payment> getRecentPOSTransactions(int limit) {
        return paymentRepository.findTop10ByOrderByPaymentDateDesc();
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenue() {
        return paymentRepository.findByStatus(Payment.PaymentStatus.COMPLETED)
                .stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    // ========== NOTIFICATION METHODS ==========

    public int sendBankSlipReminders() {
        List<Payment> pendingPayments = getPendingBankSlipVerifications();
        int remindersSent = 0;

        for (Payment payment : pendingPayments) {
            if (payment.getPaymentDate().isBefore(LocalDateTime.now().minusHours(24))) {
                try {
                    // Add reminder logic here
                    remindersSent++;
                } catch (Exception e) {
                    System.err.println("Failed to send reminder for payment " + payment.getId());
                }
            }
        }

        return remindersSent;
    }

    public void contactPatientRegardingBankSlip(Long paymentId, String contactReason, String message) {
        Payment payment = getPaymentById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));

        if (payment.getInvoice() != null && payment.getInvoice().getPatient() != null) {
            payment.setNotes(payment.getNotes() + " | Contact made: " + contactReason + " - " + message);
            paymentRepository.save(payment);
        }
    }

    // ========== DOCUMENT HANDLING ==========

    public ResponseEntity<byte[]> downloadBankSlipFile(Long paymentId) {
        try {
            Payment payment = getPaymentById(paymentId)
                    .orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));

            if (payment.getBankSlipPath() == null || payment.getBankSlipPath().isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            byte[] fileData = fileStorageService.getBankSlipFile(payment.getBankSlipPath());
            // FIXED: Replaced call to a non-existent method with a safe fallback.
            // TODO: Implement a `getContentType` method in FileStorageService for proper MIME type handling.
            String contentType = "application/octet-stream";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));

            // Default to attachment to ensure download since content type is generic.
            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename(payment.getBankSlipPath())
                            .build()
            );

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileData);

        } catch (Exception e) {
            System.err.println("Error downloading bank slip: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    public ResponseEntity<byte[]> viewBankSlipFile(Long paymentId) throws IOException {
        try {
            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));

            if (payment.getBankSlipPath() == null || payment.getBankSlipPath().isEmpty()) {
                throw new RuntimeException("No bank slip found for this payment");
            }

            // Use FileStorageService
            byte[] fileData = fileStorageService.getBankSlipFile(payment.getBankSlipPath());
            // FIXED: Replaced call to a non-existent method with a safe fallback.
            // TODO: Implement a `getContentType` method in FileStorageService for proper MIME type handling.
            String contentType = "application/octet-stream";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(fileData.length);
            // INLINE instead of ATTACHMENT
            headers.setContentDisposition(
                    ContentDisposition.inline()
                            .filename(payment.getBankSlipPath())
                            .build()
            );

            return new ResponseEntity<>(fileData, headers, HttpStatus.OK);

        } catch (Exception e) {
            System.err.println("Error viewing bank slip: " + e.getMessage());
            throw new IOException("Failed to view bank slip: " + e.getMessage(), e);
        }
    }

    // ========== HELPER METHODS ==========

    private Payment createBasePayment(Invoice invoice, BigDecimal amount, Payment.PaymentMethod paymentMethod) {
        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setPatient(invoice.getPatient());
        payment.setAmount(amount);
        payment.setPaymentMethod(paymentMethod);
        payment.setTransactionId(generateTransactionId());
        // payment.setReceiptNumber(generateReceiptNumber()); // TODO: Add `receiptNumber` field and setter to the Payment model class.
        payment.setPaymentDate(LocalDateTime.now());
        return payment;
    }

    private void validatePaymentAmount(Invoice invoice, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        if (amount.compareTo(invoice.getBalanceDue()) > 0) {
            throw new IllegalArgumentException("Payment amount cannot exceed invoice balance");
        }
    }

    private String generateTransactionId() {
        return "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private String generateReceiptNumber() {
        return "RCP-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private boolean simulateCardPayment(Payment.CreateRequest request) {
        // 90% success rate for demo
        return Math.random() > 0.1;
    }
}