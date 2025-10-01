package com.hospital.hospitalmanagementsystem.service;

import com.hospital.hospitalmanagementsystem.model.*;
import com.hospital.hospitalmanagementsystem.repository.PaymentPlanRepository;
import com.hospital.hospitalmanagementsystem.repository.PaymentPlanInstallmentRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
            // FIX: Correctly handle the Optional
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

            // ... (rest of the method is the same)
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

            // This line will now work correctly
            invoiceService.updateInvoiceStatus(invoiceId, Invoice.InvoiceStatus.PENDING);

            return savedPlan;
        } catch (Exception e) {
            throw new RuntimeException("Error creating payment plan: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public PaymentPlan getPaymentPlanById(Long planId) {
        return paymentPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Payment plan not found with ID: " + planId));
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

    public PaymentPlan updatePaymentPlanStatus(Long planId, String action, String notes) {
        PaymentPlan paymentPlan = getPaymentPlanById(planId);

        switch (action.toLowerCase()) {
            case "suspend":
                paymentPlan.setStatus(PaymentPlan.PaymentPlanStatus.SUSPENDED);
                break;
            case "resume":
                paymentPlan.setStatus(PaymentPlan.PaymentPlanStatus.ACTIVE);
                break;
            case "cancel":
                paymentPlan.setStatus(PaymentPlan.PaymentPlanStatus.CANCELLED);
                break;
            case "complete":
                paymentPlan.setStatus(PaymentPlan.PaymentPlanStatus.COMPLETED);
                // Mark invoice as paid
                invoiceService.updateInvoiceStatus(paymentPlan.getInvoice().getId(), Invoice.InvoiceStatus.PAID);
                break;
            default:
                throw new IllegalArgumentException("Invalid action: " + action);
        }

        paymentPlan.setNotes(notes);
        return paymentPlanRepository.save(paymentPlan);
    }

    // ========== INSTALLMENT CALCULATIONS ==========

    public BigDecimal calculateMonthlyPayment(BigDecimal totalAmount, int numberOfInstallments, BigDecimal interestRate) {
        if (interestRate.compareTo(BigDecimal.ZERO) == 0) {
            // No interest - simple division
            return totalAmount.divide(new BigDecimal(numberOfInstallments), 2, RoundingMode.HALF_UP);
        }

        // Calculate with compound interest using simple approach
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
        paymentPlan.setInstallments(installments);
    }

    // ========== STATISTICS AND QUERIES ==========

    @Transactional(readOnly = true)
    public int getActivePaymentPlansCount() {
        return paymentPlanRepository.findByStatus(PaymentPlan.PaymentPlanStatus.ACTIVE).size();
    }

    @Transactional(readOnly = true)
    public List<PaymentPlan> getActivePaymentPlans() {
        return paymentPlanRepository.findByStatus(PaymentPlan.PaymentPlanStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public int getOverduePaymentPlansCount() {
        LocalDate today = LocalDate.now();
        return paymentPlanRepository.findByStatusAndNextPaymentDateBefore(
                PaymentPlan.PaymentPlanStatus.ACTIVE, today
        ).size();
    }

    @Transactional(readOnly = true)
    public List<PaymentPlan> getOverduePaymentPlans() {
        LocalDate today = LocalDate.now();
        return paymentPlanRepository.findByStatusAndNextPaymentDateBefore(
                PaymentPlan.PaymentPlanStatus.ACTIVE, today
        );
    }

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