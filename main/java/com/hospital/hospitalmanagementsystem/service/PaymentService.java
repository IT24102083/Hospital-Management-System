package com.hospital.hospitalmanagementsystem.service;

import com.hospital.hospitalmanagementsystem.exception.PaymentProcessingException;
import com.hospital.hospitalmanagementsystem.model.*;
import com.hospital.hospitalmanagementsystem.repository.PaymentRepository;
import com.hospital.hospitalmanagementsystem.service.external.FileStorageService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    public Payment processCardPayment(Payment.CreateRequest request) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(request.getInvoiceId())
                    .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + request.getInvoiceId()));

            validatePaymentAmount(invoice, request.getAmount());

            Payment payment = createBasePayment(invoice, request.getAmount(), Payment.PaymentMethod.CREDIT_CARD);

            // Simulate card processing
            boolean paymentSuccess = simulateCardPayment(request);

            if (paymentSuccess) {
                payment.setStatus(Payment.PaymentStatus.COMPLETED);
                invoiceService.applyPayment(invoice.getId(), request.getAmount());
            } else {
                payment.setStatus(Payment.PaymentStatus.FAILED);
                payment.setNotes("Card payment failed - insufficient funds or invalid card");
            }

            return paymentRepository.save(payment);
        } catch (Exception e) {
            throw new PaymentProcessingException("Card payment processing failed: " + e.getMessage(), e);
        }
    }

    public Payment processBankTransferPayment(Long invoiceId, BigDecimal amount, String referenceNumber,
                                              MultipartFile receiptFile, String transferNotes) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + invoiceId));

            validatePaymentAmount(invoice, amount);

            Payment payment = createBasePayment(invoice, amount, Payment.PaymentMethod.BANK_TRANSFER);
            payment.setStatus(Payment.PaymentStatus.PENDING); // Needs verification
            payment.setReferenceNumber(referenceNumber);
            payment.setNotes("Bank transfer - Reference: " + referenceNumber +
                    (transferNotes != null ? " | Notes: " + transferNotes : ""));

            // Store bank slip file if provided
            if (receiptFile != null && !receiptFile.isEmpty()) {
                String fileName = fileStorageService.storeBankSlip(receiptFile, payment.getId());
                payment.setBankSlipPath(fileName);
                payment.setNotes(payment.getNotes() + " | Bank slip uploaded: " + fileName);
            }

            return paymentRepository.save(payment);
        } catch (Exception e) {
            throw new PaymentProcessingException("Bank transfer processing failed: " + e.getMessage(), e);
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
            payment.setNotes("POS Payment: " + paymentDescription);

            // Handle invoice linking if provided
            if (invoiceNumber != null && !invoiceNumber.isEmpty()) {
                Optional<Invoice> invoiceOpt = invoiceRepository.findByInvoiceNumber(invoiceNumber);
                if (invoiceOpt.isPresent()) {
                    Invoice invoice = invoiceOpt.get();
                    payment.setInvoice(invoice);
                    payment.setPatient(invoice.getPatient());
                    invoiceService.applyPayment(invoice.getId(), amount);
                } else {
                    // Invoice not found, treat as standalone payment
                    payment.setNotes(payment.getNotes() + " | Invoice number " + invoiceNumber + " not found. | Customer: " + customerName +
                            " | Phone: " + customerPhone);
                }
            }

            return paymentRepository.save(payment);
        } catch (Exception e) {
            throw new PaymentProcessingException("POS payment processing failed: " + e.getMessage(), e);
        }
    }

    // ========== BANK SLIP VERIFICATION ==========

    public List<Payment> getPendingBankSlipVerifications() {
        return paymentRepository.findByPaymentMethodAndStatus(
                Payment.PaymentMethod.BANK_TRANSFER,
                Payment.PaymentStatus.PENDING
        );
    }

    public Page<Payment> getPendingBankSlipsPaginated(Pageable pageable) {
        return paymentRepository.findByPaymentMethodAndStatusOrderByPaymentDateDesc(
                Payment.PaymentMethod.BANK_TRANSFER,
                Payment.PaymentStatus.PENDING,
                pageable
        );
    }

    public Payment approveBankSlipPayment(Long paymentId, String verificationNotes) {
        Payment payment = getPaymentById(paymentId);

        if (payment.getPaymentMethod() != Payment.PaymentMethod.BANK_TRANSFER) {
            throw new IllegalArgumentException("Payment is not a bank transfer");
        }

        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setVerificationNotes(verificationNotes);
        payment.setVerificationDate(LocalDateTime.now());

        if (payment.getInvoice() != null) {
            invoiceService.applyPayment(payment.getInvoice().getId(), payment.getAmount());
        }

        return paymentRepository.save(payment);
    }

    public Payment rejectBankSlipPayment(Long paymentId, String rejectionReason) {
        Payment payment = getPaymentById(paymentId);

        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setVerificationNotes(rejectionReason);
        payment.setVerificationDate(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    public Payment approvePartialBankSlipPayment(Long paymentId, BigDecimal verifiedAmount, String verificationNotes) {
        Payment payment = getPaymentById(paymentId);

        if (verifiedAmount.compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("Verified amount cannot exceed payment amount");
        }

        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setAmount(verifiedAmount); // Update to verified amount
        payment.setVerificationNotes(verificationNotes);
        payment.setVerificationDate(LocalDateTime.now());

        if (payment.getInvoice() != null) {
            invoiceService.applyPayment(payment.getInvoice().getId(), verifiedAmount);
        }

        return paymentRepository.save(payment);
    }

    public ResponseEntity<byte[]> downloadBankSlipFile(Long paymentId) {
        try {
            Payment payment = getPaymentById(paymentId);

            if (payment.getBankSlipPath() == null) {
                return ResponseEntity.notFound().build();
            }

            byte[] fileData = fileStorageService.getBankSlipFile(payment.getBankSlipPath());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", payment.getBankSlipPath());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileData);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ========== PAYMENT QUERIES ==========

    @Transactional(readOnly = true)
    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByInvoiceId(Long invoiceId) {
        return paymentRepository.findByInvoiceIdOrderByPaymentDateDesc(invoiceId);
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

    // ========== NOTIFICATION METHODS ==========

    public int sendBankSlipReminders() {
        List<Payment> pendingPayments = getPendingBankSlipVerifications();
        int remindersSent = 0;

        for (Payment payment : pendingPayments) {
            if (payment.getPaymentDate().isBefore(LocalDateTime.now().minusHours(24))) {
                try {
                    // NOTE: Add actual email/SMS reminder logic here
                    System.out.println("Sending reminder for payment ID: " + payment.getId());
                    remindersSent++;
                } catch (Exception e) {
                    System.err.println("Failed to send reminder for payment " + payment.getId());
                }
            }
        }

        return remindersSent;
    }

    public void contactPatientRegardingBankSlip(Long paymentId, String contactReason, String message) {
        Payment payment = getPaymentById(paymentId);

        if (payment.getInvoice() != null && payment.getInvoice().getPatient() != null) {
            // NOTE: Add actual notification logic here
            String contactNote = " | Contact made: " + contactReason + " - " + message;
            payment.setNotes(payment.getNotes() != null ? payment.getNotes() + contactNote : contactNote);
            paymentRepository.save(payment);
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
        return payment;
    }

    private void validatePaymentAmount(Invoice invoice, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        if (amount.compareTo(invoice.getBalanceDue()) > 0) {
            throw new IllegalArgumentException("Payment amount cannot exceed invoice balance due");
        }
    }

    private String generateTransactionId() {
        return "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private boolean simulateCardPayment(Payment.CreateRequest request) {
        // Simulate payment processing (90% success rate for demo)
        return Math.random() > 0.1;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }
}