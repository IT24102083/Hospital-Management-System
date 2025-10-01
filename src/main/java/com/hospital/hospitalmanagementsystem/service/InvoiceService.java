package com.hospital.hospitalmanagementsystem.service;

import com.hospital.hospitalmanagementsystem.model.Invoice;
import com.hospital.hospitalmanagementsystem.model.Patient;
import com.hospital.hospitalmanagementsystem.repository.InvoiceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Service for managing invoices
 *
 * Current User's Login: IT24102083
 * Current Date and Time (UTC - YYYY-MM-DD HH:MM:SS formatted): 2025-08-11 05:00:22
 */
@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public Invoice saveInvoice(Invoice invoice) {
        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isEmpty()) {
            invoice.setInvoiceNumber(generateInvoiceNumber());
        }
        return invoiceRepository.save(invoice);
    }

    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    public Optional<Invoice> getInvoiceById(Long id) {
        return invoiceRepository.findById(id);
    }

    public List<Invoice> getPatientInvoices(Patient patient) {
        return invoiceRepository.findByPatient(patient);
    }

    public List<Invoice> getInvoicesByStatus(Invoice.InvoiceStatus status) {
        return invoiceRepository.findByStatus(status);
    }

    public Invoice getInvoiceByNumber(String invoiceNumber) {
        // FIX: Handle the Optional returned by the repository to prevent a new error
        return invoiceRepository.findByInvoiceNumber(invoiceNumber).orElse(null);
    }

    public void deleteInvoice(Long id) {
        invoiceRepository.deleteById(id);
    }

    private String generateInvoiceNumber() {
        return "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public void updateInvoiceStatus(Long invoiceId, Invoice.InvoiceStatus newStatus) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + invoiceId));

        invoice.setStatus(newStatus);
        invoiceRepository.save(invoice);
    }

    /**
     * Get recent invoices for a patient with limit
     * @param patient the patient
     * @param limit maximum number of invoices to return
     * @return list of recent invoices
     */
    public List<Invoice> getRecentInvoices(Patient patient, int limit) {
        // Using findByPatient and sorting in memory since repository method is missing
        return invoiceRepository.findByPatientOrderByIssueDateDesc(patient).stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    public void applyPayment(Long invoiceId, BigDecimal paymentAmount) {
        // 1. Find the invoice or throw an error if it doesn't exist
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + invoiceId));

        // 2. Update the paid amount and balance due
        BigDecimal newAmountPaid = invoice.getAmountPaid().add(paymentAmount);
        BigDecimal newBalanceDue = invoice.getTotal().subtract(newAmountPaid);

        invoice.setAmountPaid(newAmountPaid);
        invoice.setBalanceDue(newBalanceDue);

        // 3. Update the invoice status based on the new balance
        if (newBalanceDue.compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setStatus(Invoice.InvoiceStatus.PAID);
            invoice.setBalanceDue(BigDecimal.ZERO); // Ensure balance isn't negative
        } else {
            invoice.setStatus(Invoice.InvoiceStatus.PARTIALLY_PAID);
        }

        // 4. Save the updated invoice
        invoiceRepository.save(invoice);
    }

    public BigDecimal getTotalOutstandingBalance() {
        return invoiceRepository.findAll().stream()
                // Filter for invoices that have a balance
                .filter(invoice ->
                        invoice.getStatus() == Invoice.InvoiceStatus.PENDING ||
                                invoice.getStatus() == Invoice.InvoiceStatus.PARTIALLY_PAID ||
                                invoice.getStatus() == Invoice.InvoiceStatus.OVERDUE)

                .map(Invoice::getBalanceDue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Invoice> getOverdueInvoices() {
        LocalDate today = LocalDate.now();
        return invoiceRepository.findAll().stream()
                // Filter for unpaid invoices where the due date is before today
                .filter(invoice ->
                        (invoice.getStatus() == Invoice.InvoiceStatus.PENDING ||
                                invoice.getStatus() == Invoice.InvoiceStatus.PARTIALLY_PAID)
                                && invoice.getDueDate().isBefore(today))
                .collect(Collectors.toList());
    }
}