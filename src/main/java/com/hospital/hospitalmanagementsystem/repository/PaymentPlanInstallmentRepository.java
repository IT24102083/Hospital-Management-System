package com.hospital.hospitalmanagementsystem.repository;

import com.hospital.hospitalmanagementsystem.model.PaymentPlanInstallment;
import com.hospital.hospitalmanagementsystem.model.PaymentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentPlanInstallmentRepository extends JpaRepository<PaymentPlanInstallment, Long> {

    // Find installments by payment plan
    List<PaymentPlanInstallment> findByPaymentPlanOrderByInstallmentNumberAsc(PaymentPlan paymentPlan);

    List<PaymentPlanInstallment> findByPaymentPlanIdOrderByInstallmentNumberAsc(Long paymentPlanId);

    // Find by status
    List<PaymentPlanInstallment> findByStatus(PaymentPlanInstallment.InstallmentStatus status);

    // Find by payment plan and status
    List<PaymentPlanInstallment> findByPaymentPlanAndStatus(PaymentPlan paymentPlan, PaymentPlanInstallment.InstallmentStatus status);

    List<PaymentPlanInstallment> findByPaymentPlanIdAndStatus(Long paymentPlanId, PaymentPlanInstallment.InstallmentStatus status);

    // Find by installment number
    Optional<PaymentPlanInstallment> findByPaymentPlanAndInstallmentNumber(PaymentPlan paymentPlan, Integer installmentNumber);

    Optional<PaymentPlanInstallment> findByPaymentPlanIdAndInstallmentNumber(Long paymentPlanId, Integer installmentNumber);

    // Find overdue installments
    List<PaymentPlanInstallment> findByDueDateBeforeAndStatusNot(LocalDate date, PaymentPlanInstallment.InstallmentStatus status);

    // Find installments due soon
    @Query("SELECT ppi FROM PaymentPlanInstallment ppi WHERE ppi.status = 'PENDING' AND ppi.dueDate BETWEEN :startDate AND :endDate ORDER BY ppi.dueDate ASC")
    List<PaymentPlanInstallment> findInstallmentsDueSoon(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Find installments due today
    @Query("SELECT ppi FROM PaymentPlanInstallment ppi WHERE ppi.status = 'PENDING' AND ppi.dueDate = :date")
    List<PaymentPlanInstallment> findInstallmentsDueToday(@Param("date") LocalDate date);

    // Find unpaid installments
    @Query("SELECT ppi FROM PaymentPlanInstallment ppi WHERE ppi.status IN ('PENDING', 'OVERDUE', 'PARTIAL') ORDER BY ppi.dueDate ASC")
    List<PaymentPlanInstallment> findUnpaidInstallments();

    // Find next installment for payment plan
    @Query("SELECT ppi FROM PaymentPlanInstallment ppi WHERE ppi.paymentPlan = :paymentPlan AND ppi.status IN ('PENDING', 'PARTIAL') ORDER BY ppi.installmentNumber ASC")
    List<PaymentPlanInstallment> findNextInstallments(@Param("paymentPlan") PaymentPlan paymentPlan);

    // Statistics queries
    @Query("SELECT SUM(ppi.amount) FROM PaymentPlanInstallment ppi WHERE ppi.paymentPlan.id = :paymentPlanId AND ppi.status = 'PAID'")
    BigDecimal getTotalPaidAmount(@Param("paymentPlanId") Long paymentPlanId);

    @Query("SELECT SUM(ppi.amount) FROM PaymentPlanInstallment ppi WHERE ppi.paymentPlan.id = :paymentPlanId AND ppi.status IN ('PENDING', 'OVERDUE', 'PARTIAL')")
    BigDecimal getTotalRemainingAmount(@Param("paymentPlanId") Long paymentPlanId);

    // Count methods
    long countByPaymentPlanId(Long paymentPlanId);

    long countByPaymentPlanIdAndStatus(Long paymentPlanId, PaymentPlanInstallment.InstallmentStatus status);

    @Query("SELECT COUNT(ppi) FROM PaymentPlanInstallment ppi WHERE ppi.dueDate < :date AND ppi.status IN ('PENDING', 'PARTIAL')")
    long countOverdueInstallments(@Param("date") LocalDate date);

    // Delete by payment plan
    void deleteByPaymentPlanId(Long paymentPlanId);
}