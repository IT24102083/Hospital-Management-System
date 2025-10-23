package com.hospital.hospitalmanagementsystem.repository;

import com.hospital.hospitalmanagementsystem.model.PaymentPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentPlanRepository extends JpaRepository<PaymentPlan, Long> {

    // Find by plan number
    Optional<PaymentPlan> findByPlanNumber(String planNumber);

    // Find by patient
    List<PaymentPlan> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    Page<PaymentPlan> findByPatientIdOrderByCreatedAtDesc(Long patientId, Pageable pageable);

    // Find by status
    List<PaymentPlan> findByStatus(PaymentPlan.PaymentPlanStatus status);

    Page<PaymentPlan> findByStatusOrderByCreatedAtDesc(PaymentPlan.PaymentPlanStatus status, Pageable pageable);

    // Find by invoice
    Optional<PaymentPlan> findByInvoiceId(Long invoiceId);

    // Find all ordered by date
    Page<PaymentPlan> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Find active payment plans
    @Query("SELECT pp FROM PaymentPlan pp WHERE pp.status = 'ACTIVE' ORDER BY pp.nextPaymentDate ASC")
    List<PaymentPlan> findActivePaymentPlans();

    // Find overdue payment plans
    List<PaymentPlan> findByStatusAndNextPaymentDateBefore(PaymentPlan.PaymentPlanStatus status, LocalDate date);

    // Find payment plans due soon
    @Query("SELECT pp FROM PaymentPlan pp WHERE pp.status = 'ACTIVE' AND pp.nextPaymentDate BETWEEN :startDate AND :endDate ORDER BY pp.nextPaymentDate ASC")
    List<PaymentPlan> findPaymentPlansDueSoon(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Find payment plans due today
    @Query("SELECT pp FROM PaymentPlan pp WHERE pp.status = 'ACTIVE' AND pp.nextPaymentDate = :date")
    List<PaymentPlan> findPaymentPlansDueToday(@Param("date") LocalDate date);

    // Find auto-pay plans ready for processing
    @Query("SELECT pp FROM PaymentPlan pp WHERE pp.status = 'ACTIVE' AND pp.autoPayEnabled = true AND pp.nextPaymentDate <= :date")
    List<PaymentPlan> findAutoPayPlansReady(@Param("date") LocalDate date);

    // Statistics queries
    @Query("SELECT COUNT(pp), SUM(pp.totalAmount), SUM(pp.amountPaid), SUM(pp.remainingBalance), AVG(pp.monthlyPayment) FROM PaymentPlan pp WHERE pp.status = 'ACTIVE'")
    Object[] getActivePaymentPlanStatistics();

    @Query("SELECT SUM(pp.monthlyPayment) FROM PaymentPlan pp WHERE pp.status = 'ACTIVE' AND pp.nextPaymentDate BETWEEN :startDate AND :endDate")
    BigDecimal getExpectedMonthlyCollections(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Search payment plans
    @Query("SELECT pp FROM PaymentPlan pp WHERE " +
            "pp.planNumber LIKE %:searchTerm% OR " +
            "pp.patient.firstName LIKE %:searchTerm% OR " +
            "pp.patient.lastName LIKE %:searchTerm% OR " +
            "pp.invoice.invoiceNumber LIKE %:searchTerm%")
    List<PaymentPlan> searchPaymentPlans(@Param("searchTerm") String searchTerm);

    // Count methods
    long countByStatus(PaymentPlan.PaymentPlanStatus status);

    long countByPatientId(Long patientId);

    @Query("SELECT COUNT(pp) FROM PaymentPlan pp WHERE pp.status = 'ACTIVE' AND pp.nextPaymentDate < :date")
    long countOverduePaymentPlans(@Param("date") LocalDate date);
}