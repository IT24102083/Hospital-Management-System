package com.hospital.hospitalmanagementsystem.service;

import com.hospital.hospitalmanagementsystem.model.Invoice;
import com.hospital.hospitalmanagementsystem.model.Order;
import com.hospital.hospitalmanagementsystem.model.Patient;
import com.hospital.hospitalmanagementsystem.repository.InvoiceItemRepository;
import com.hospital.hospitalmanagementsystem.repository.InvoiceRepository;
import com.hospital.hospitalmanagementsystem.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing invoices, combining functionalities from multiple sources.
 *
 * Current User's Login: IT24102083
 * Current Date and Time (UTC - YYYY-MM-DD HH:MM:SS formatted): 2025-08-11 05:00:22
 */
@Service
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final OrderRepository orderRepository;

    public InvoiceService(InvoiceRepository invoiceRepository, InvoiceItemRepository invoiceItemRepository, OrderRepository orderRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.orderRepository = orderRepository;
    }

    // ========== CRUD OPERATIONS ==========

    public Invoice createInvoice(Invoice invoice) {
        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isEmpty()) {
            invoice.setInvoiceNumber(generateInvoiceNumber());
        }

        // Calculate totals before saving
        calculateInvoiceTotals(invoice);

        // Set initial status if not provided
        if (invoice.getStatus() == null) {
            invoice.setStatus(Invoice.InvoiceStatus.PENDING);
        }

        return invoiceRepository.save(invoice);
    }

    public Invoice saveInvoice(Invoice invoice) {
        // Recalculate totals to ensure data integrity
        calculateInvoiceTotals(invoice);
        return invoiceRepository.save(invoice);
    }

    public Invoice updateInvoice(Long id, Invoice updatedInvoice) {
        Invoice existing = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + id));

        existing.setDescription(updatedInvoice.getDescription());
        existing.setDueDate(updatedInvoice.getDueDate());
        existing.setNotes(updatedInvoice.getNotes());

        // Recalculate if financial details changed
        if (!Objects.equals(existing.getSubtotal(), updatedInvoice.getSubtotal())) {
            existing.setSubtotal(updatedInvoice.getSubtotal());
            existing.setTax(updatedInvoice.getTax());
            existing.setDiscount(updatedInvoice.getDiscount());
            calculateInvoiceTotals(existing);
        }

        return invoiceRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public Optional<Invoice> getInvoiceById(Long id) {
        return invoiceRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Invoice> getAllInvoicesPaginated(Pageable pageable) {
        return invoiceRepository.findAll(pageable);
    }

    public void deleteInvoice(Long id) {
        invoiceRepository.deleteById(id);
    }

    // ========== PATIENT-SPECIFIC OPERATIONS ==========

    @Transactional(readOnly = true)
    public List<Invoice> getPatientInvoices(Patient patient) {
        // Combines standard invoices and those with payment plans for a full view
        List<Invoice> invoices = invoiceRepository.findByPatient(patient);
        invoices.addAll(invoiceRepository.findByPatientWithPaymentPlan(patient));
        return invoiceRepository.findByPatient(patient);
    }

    @Transactional(readOnly = true)
    public List<Invoice> getRecentInvoices(Patient patient, int limit) {
        return invoiceRepository.findByPatientOrderByIssueDateDesc(patient).stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getPatientInvoiceCount(Patient patient) {
        return invoiceRepository.countByPatient(patient);
    }

    // ========== STATUS & PAYMENT MANAGEMENT ==========

    public void updateInvoiceStatus(Long invoiceId, Invoice.InvoiceStatus newStatus) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + invoiceId));

        invoice.setStatus(newStatus);
        invoiceRepository.save(invoice);
    }

    public void applyPayment(Long invoiceId, BigDecimal paymentAmount) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + invoiceId));

        BigDecimal newAmountPaid = invoice.getAmountPaid().add(paymentAmount);
        BigDecimal newBalanceDue = invoice.getTotal().subtract(newAmountPaid);

        invoice.setAmountPaid(newAmountPaid);
        invoice.setBalanceDue(newBalanceDue.max(BigDecimal.ZERO)); // Ensure balance isn't negative

        boolean isNowPaid = false;
        if (newBalanceDue.compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setStatus(Invoice.InvoiceStatus.PAID);
            isNowPaid = true;
        } else {
            invoice.setStatus(Invoice.InvoiceStatus.PARTIALLY_PAID);
        }

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // If payment completes the invoice, update the associated order status
        if (isNowPaid) {
            orderRepository.findByInvoice(savedInvoice).ifPresent(order -> {
                order.setStatus(Order.OrderStatus.COMPLETED);
                orderRepository.save(order);
            });
        }
    }

    // ========== QUERIES & REPORTING ==========

    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesByStatus(Invoice.InvoiceStatus status) {
        return invoiceRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public Invoice getInvoiceByNumber(String invoiceNumber) {
        // Safely handles Optional to prevent errors if not found
        return invoiceRepository.findByInvoiceNumber(invoiceNumber).orElse(null);
    }

    /**
     * ADDED: Retrieves a list of invoices issued within a specific date range.
     * This method is required by the AccountantDashboardController for financial reporting.
     *
     * @param startDate The start of the date range (inclusive).
     * @param endDate   The end of the date range (inclusive).
     * @return A list of invoices.
     */
    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesByDateRange(LocalDate startDate, LocalDate endDate) {
        return invoiceRepository.findByIssueDateBetween(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<Invoice> getOverdueInvoices() {
        LocalDate today = LocalDate.now();
        return invoiceRepository.findAll().stream()
                .filter(invoice ->
                        invoice.getDueDate() != null &&
                                (invoice.getStatus() == Invoice.InvoiceStatus.PENDING ||
                                        invoice.getStatus() == Invoice.InvoiceStatus.PARTIALLY_PAID ||
                                        invoice.getStatus() == Invoice.InvoiceStatus.SENT) && // Includes SENT status
                                invoice.getDueDate().isBefore(today))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public int getPaidInvoicesCount() {
        return invoiceRepository.findByStatus(Invoice.InvoiceStatus.PAID).size();
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalOutstandingBalance() {
        return invoiceRepository.findAll().stream()
                .filter(invoice ->
                        invoice.getStatus() == Invoice.InvoiceStatus.PENDING ||
                                invoice.getStatus() == Invoice.InvoiceStatus.PARTIALLY_PAID ||
                                invoice.getStatus() == Invoice.InvoiceStatus.OVERDUE ||
                                invoice.getStatus() == Invoice.InvoiceStatus.SENT) // Comprehensive status check
                .map(Invoice::getBalanceDue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getAgingReport() {
        Map<String, BigDecimal> agingReport = new HashMap<>();
        LocalDate today = LocalDate.now();

        List<Invoice> unpaidInvoices = invoiceRepository.findAll().stream()
                .filter(invoice -> invoice.getBalanceDue().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

        BigDecimal current = BigDecimal.ZERO;
        BigDecimal thirtyDays = BigDecimal.ZERO;
        BigDecimal sixtyDays = BigDecimal.ZERO;
        BigDecimal ninetyDaysPlus = BigDecimal.ZERO;

        for (Invoice invoice : unpaidInvoices) {
            if (invoice.getDueDate() == null) continue;

            long daysPastDue = java.time.temporal.ChronoUnit.DAYS.between(invoice.getDueDate(), today);

            if (daysPastDue <= 0) {
                current = current.add(invoice.getBalanceDue());
            } else if (daysPastDue <= 30) {
                thirtyDays = thirtyDays.add(invoice.getBalanceDue());
            } else if (daysPastDue <= 60) {
                sixtyDays = sixtyDays.add(invoice.getBalanceDue());
            } else {
                ninetyDaysPlus = ninetyDaysPlus.add(invoice.getBalanceDue());
            }
        }

        agingReport.put("current", current);
        agingReport.put("30days", thirtyDays);
        agingReport.put("60days", sixtyDays);
        agingReport.put("90plus", ninetyDaysPlus);

        return agingReport;
    }

    // ========== HELPER METHODS ==========

    private void calculateInvoiceTotals(Invoice invoice) {
        // Ensures fields are not null before calculation
        if (invoice.getSubtotal() == null) invoice.setSubtotal(BigDecimal.ZERO);
        if (invoice.getTax() == null) invoice.setTax(BigDecimal.ZERO);
        if (invoice.getDiscount() == null) invoice.setDiscount(BigDecimal.ZERO);
        if (invoice.getAmountPaid() == null) invoice.setAmountPaid(BigDecimal.ZERO);

        BigDecimal total = invoice.getSubtotal()
                .add(invoice.getTax())
                .subtract(invoice.getDiscount());
        invoice.setTotal(total);

        BigDecimal balanceDue = total.subtract(invoice.getAmountPaid());
        invoice.setBalanceDue(balanceDue.max(BigDecimal.ZERO)); // Prevent negative balance
    }

    private String generateInvoiceNumber() {
        // Using a sequential, date-based invoice number for better readability
        String prefix = "INV";
        String year = String.valueOf(LocalDate.now().getYear());
        String month = String.format("%02d", LocalDate.now().getMonthValue());
        String sequence = String.format("%05d", invoiceRepository.count() + 1);
        return prefix + "-" + year + "-" + month + sequence;
    }

    /**
     * ADDED: Retrieves a paginated list of invoices for a specific patient.
     * This is used by the patient-facing payment history page.
     *
     * @param patient  The patient whose invoices to retrieve.
     * @param pageable Pagination and sorting information.
     * @return A page of invoices.
     */
    @Transactional(readOnly = true)
    public Page<Invoice> getPatientInvoicesPaginated(Patient patient, Pageable pageable) {
        return invoiceRepository.findByPatientOrderByIssueDateDesc(patient, pageable);
    }

    /**
     * ADDED: Calculates summary financial statistics for a patient's invoices.
     *
     * @param patient The patient to summarize.
     * @return A map containing the summary data for the UI cards.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPatientInvoiceSummary(Patient patient) {
        List<Invoice> allInvoices = invoiceRepository.findByPatient(patient);
        Map<String, Object> summary = new HashMap<>();

        BigDecimal totalPaid = allInvoices.stream()
                .map(Invoice::getAmountPaid)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal outstanding = allInvoices.stream()
                .map(Invoice::getBalanceDue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activePlans = allInvoices.stream()
                .filter(invoice -> invoice.getPaymentPlan() != null &&
                        (invoice.getStatus() == Invoice.InvoiceStatus.PARTIALLY_PAID ||
                                invoice.getStatus() == Invoice.InvoiceStatus.PENDING))
                .count();

        summary.put("totalPaidAmount", totalPaid);
        summary.put("outstandingAmount", outstanding);
        summary.put("activePaymentPlans", (int) activePlans);
        summary.put("totalInvoices", allInvoices.size());

        return summary;
    }

}