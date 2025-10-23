package com.hospital.hospitalmanagementsystem.service;

import com.hospital.hospitalmanagementsystem.model.*;
import com.hospital.hospitalmanagementsystem.repository.PaymentPlanInstallmentRepository;
import com.hospital.hospitalmanagementsystem.repository.PaymentPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PaymentPlanService {

    @Autowired
    private PaymentPlanRepository paymentPlanRepository;

    @Autowired
    private PaymentPlanInstallmentRepository installmentRepository;

    @Autowired
    private InvoiceService invoiceService;

    // ========== PAYMENT PLAN CRUD ==========

    public PaymentPlan createPaymentPlan(Long invoiceId, PaymentPlan.CreateRequest request) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + invoiceId));

            if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
                throw new IllegalStateException("Cannot create payment plan for a paid invoice");
            }

            PaymentPlan paymentPlan = new PaymentPlan();
            paymentPlan.setInvoice(invoice);
            paymentPlan.setPatient(invoice.getPatient());
            paymentPlan.setPlanNumber(generatePlanNumber());
            paymentPlan.setTotalAmount(invoice.getBalanceDue());
            paymentPlan.setNumberOfPayments(request.getNumberOfInstallments());
            paymentPlan.setInterestRate(request.getInterestRate() != null ? request.getInterestRate() : BigDecimal.ZERO);
            paymentPlan.setStartDate(request.getStartDate());
            paymentPlan.setStatus(PaymentPlan.PaymentPlanStatus.ACTIVE);
            paymentPlan.setNotes(request.getNotes());

            BigDecimal monthlyPayment = calculateMonthlyPayment(
                    paymentPlan.getTotalAmount(),
                    paymentPlan.getNumberOfPayments(),
                    paymentPlan.getInterestRate()
            );
            paymentPlan.setMonthlyPayment(monthlyPayment);

            paymentPlan.setNextPaymentDate(request.getStartDate());
            paymentPlan.setEndDate(request.getStartDate().plusMonths(request.getNumberOfInstallments() - 1));

            paymentPlan.setAmountPaid(BigDecimal.ZERO);
            paymentPlan.setRemainingBalance(paymentPlan.getTotalAmount());
            paymentPlan.setPaymentsMade(0);

            PaymentPlan savedPlan = paymentPlanRepository.save(paymentPlan);
            createInstallmentSchedule(savedPlan);

            invoiceService.updateInvoiceStatus(invoiceId, Invoice.InvoiceStatus.PARTIALLY_PAID);

            return savedPlan;
        } catch (Exception e) {
            throw new RuntimeException("Error creating payment plan: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<PaymentPlan> getPaymentPlanById(Long planId) {
        return paymentPlanRepository.findById(planId);
    }

    @Transactional(readOnly = true)
    public List<PaymentPlan> getPaymentPlansByPatientId(Long patientId) {
        return paymentPlanRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    @Transactional(readOnly = true)
    public Page<PaymentPlan> getAllPaymentPlansPaginated(Pageable pageable) {
        return paymentPlanRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<PaymentPlan> getPaymentPlansByStatus(PaymentPlan.PaymentPlanStatus status, Pageable pageable) {
        return paymentPlanRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }

    // ========== PAYMENT PLAN MANAGEMENT ==========

    public PaymentPlan updatePaymentPlanStatus(Long planId, String action, String notes) {
        PaymentPlan paymentPlan = paymentPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Payment plan not found with ID: " + planId));

        switch (action.toLowerCase()) {
            case "suspend":
                paymentPlan.setStatus(PaymentPlan.PaymentPlanStatus.SUSPENDED);
                break;
            case "resume":
                paymentPlan.setStatus(PaymentPlan.PaymentPlanStatus.ACTIVE);
                break;
            case "cancel":
                paymentPlan.setStatus(PaymentPlan.PaymentPlanStatus.CANCELLED);
                // Cancel all pending installments
                installmentRepository.findByPaymentPlanIdAndStatus(planId, PaymentPlanInstallment.InstallmentStatus.PENDING)
                        .forEach(installment -> {
                            installment.setStatus(PaymentPlanInstallment.InstallmentStatus.CANCELLED);
                            installmentRepository.save(installment);
                        });
                break;
            case "complete":
                paymentPlan.setStatus(PaymentPlan.PaymentPlanStatus.COMPLETED);
                // Mark the original invoice as paid
                invoiceService.updateInvoiceStatus(paymentPlan.getInvoice().getId(), Invoice.InvoiceStatus.PAID);
                break;
            default:
                throw new IllegalArgumentException("Invalid action: " + action);
        }

        paymentPlan.setNotes(notes);
        return paymentPlanRepository.save(paymentPlan);
    }

    public void adjustPaymentPlan(Long planId, Integer newDuration, BigDecimal newMonthlyAmount, String adjustmentReason) {
        PaymentPlan paymentPlan = paymentPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Payment plan not found with ID: " + planId));

        paymentPlan.setNumberOfPayments(newDuration);
        paymentPlan.setMonthlyPayment(newMonthlyAmount);
        paymentPlan.setNotes(paymentPlan.getNotes() + " | Adjusted: " + adjustmentReason);
        paymentPlan.setEndDate(paymentPlan.getStartDate().plusMonths(newDuration - 1));

        // Delete existing installments and recreate the schedule
        installmentRepository.deleteByPaymentPlanId(planId);
        createInstallmentSchedule(paymentPlan);

        paymentPlanRepository.save(paymentPlan);
    }

    // ========== INSTALLMENT MANAGEMENT ==========

    public BigDecimal calculateMonthlyPayment(BigDecimal totalAmount, int numberOfInstallments, BigDecimal interestRate) {
        if (interestRate.compareTo(BigDecimal.ZERO) == 0) {
            return totalAmount.divide(new BigDecimal(numberOfInstallments), 2, RoundingMode.HALF_UP);
        }
        BigDecimal totalWithInterest = totalAmount.multiply(BigDecimal.ONE.add(interestRate));
        return totalWithInterest.divide(new BigDecimal(numberOfInstallments), 2, RoundingMode.HALF_UP);
    }

    private void createInstallmentSchedule(PaymentPlan paymentPlan) {
        List<PaymentPlanInstallment> installments = new ArrayList<>();
        LocalDate paymentDate = paymentPlan.getStartDate();

        for (int i = 1; i <= paymentPlan.getNumberOfPayments(); i++) {
            PaymentPlanInstallment installment = new PaymentPlanInstallment();
            installment.setPaymentPlan(paymentPlan);
            installment.setInstallmentNumber(i);
            installment.setDueDate(paymentDate);
            installment.setAmount(paymentPlan.getMonthlyPayment());
            installment.setStatus(PaymentPlanInstallment.InstallmentStatus.PENDING);
            installment.setAmountPaid(BigDecimal.ZERO);
            installment.setReminderSent(false);
            installments.add(installment);
            paymentDate = paymentDate.plusMonths(1);
        }
        installmentRepository.saveAll(installments);
    }

    @Transactional(readOnly = true)
    public List<PaymentPlanInstallment> getPaymentHistory(Long planId) {
        return installmentRepository.findByPaymentPlanIdOrderByInstallmentNumberAsc(planId);
    }

    // ========== STATISTICS AND QUERIES ==========

    @Transactional(readOnly = true)
    public int getActivePaymentPlansCount() {
        return (int) paymentPlanRepository.countByStatus(PaymentPlan.PaymentPlanStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<PaymentPlan> getActivePaymentPlans() {
        return paymentPlanRepository.findByStatus(PaymentPlan.PaymentPlanStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public int getOverdueInstallmentsCount() {
        LocalDate today = LocalDate.now();
        return (int) installmentRepository.countOverdueInstallments(today);
    }

    @Transactional(readOnly = true)
    public List<PaymentPlan> getOverduePaymentPlans() {
        LocalDate today = LocalDate.now();
        return paymentPlanRepository.findByStatusAndNextPaymentDateBefore(
                PaymentPlan.PaymentPlanStatus.ACTIVE, today
        );
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateMonthlyCollections() {
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);
        BigDecimal expectedCollections = paymentPlanRepository.getExpectedMonthlyCollections(startOfMonth, endOfMonth);
        return expectedCollections != null ? expectedCollections : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalOutstandingAmount() {
        Object[] stats = paymentPlanRepository.getActivePaymentPlanStatistics();
        if (stats != null && stats.length > 3 && stats[3] != null) {
            return (BigDecimal) stats[3];
        }
        return BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalPlannedRevenue() {
        Object[] stats = paymentPlanRepository.getActivePaymentPlanStatistics();
        if (stats != null && stats.length > 1 && stats[1] != null) {
            return (BigDecimal) stats[1];
        }
        return BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal getOnTimePaymentRate() {
        long totalInstallments = installmentRepository.count();
        if (totalInstallments == 0) return BigDecimal.ZERO;
        long paidOnTime = installmentRepository.countByPaymentPlanIdAndStatus(null, PaymentPlanInstallment.InstallmentStatus.PAID);
        return BigDecimal.valueOf(paidOnTime).divide(BigDecimal.valueOf(totalInstallments), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

    /**
     * ADDED: Restored this method which was missing after the merge.
     * It is required by the PaymentController.
     */
    @Transactional(readOnly = true)
    public LocalDate getFirstPaymentDate(PaymentPlan plan) {
        return plan.getStartDate();
    }

    // ========== UTILITY METHODS ==========

    private String generatePlanNumber() {
        String prefix = "PLAN";
        String year = String.valueOf(LocalDate.now().getYear());
        String sequence = String.format("%06d", paymentPlanRepository.count() + 1);
        return prefix + year + sequence;
    }
}