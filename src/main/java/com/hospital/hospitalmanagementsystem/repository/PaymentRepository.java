package com.hospital.hospitalmanagementsystem.repository;

import com.hospital.hospitalmanagementsystem.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Find by transaction ID
    Optional<Payment> findByTransactionId(String transactionId);

    // Find payments by invoice
    List<Payment> findByInvoiceIdOrderByPaymentDateDesc(Long invoiceId);

    // Find payments by patient
    List<Payment> findByPatientIdOrderByPaymentDateDesc(Long patientId);

    Page<Payment> findByPatientIdOrderByPaymentDateDesc(Long patientId, Pageable pageable);

    // Find by status
    List<Payment> findByStatus(Payment.PaymentStatus status);

    Page<Payment> findByStatusOrderByPaymentDateDesc(Payment.PaymentStatus status, Pageable pageable);

    // Find by payment method
    List<Payment> findByPaymentMethod(Payment.PaymentMethod paymentMethod);

    // Find by payment method and status
    List<Payment> findByPaymentMethodAndStatus(Payment.PaymentMethod paymentMethod, Payment.PaymentStatus status);

    Page<Payment> findByPaymentMethodAndStatusOrderByPaymentDateDesc(
            Payment.PaymentMethod paymentMethod, Payment.PaymentStatus status, Pageable pageable);

    // Find recent payments
    @Query("SELECT p FROM Payment p ORDER BY p.paymentDate DESC")
    List<Payment> findTop10ByOrderByPaymentDateDesc();

    // Find by date range
    List<Payment> findByPaymentDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    // Bank transfer specific queries
    @Query("SELECT p FROM Payment p WHERE p.paymentMethod = 'BANK_TRANSFER' AND p.status = 'PENDING' ORDER BY p.paymentDate ASC")
    List<Payment> findPendingBankTransfers();

    // Statistics queries
    @Query("SELECT COUNT(p), SUM(p.amount), AVG(p.amount) FROM Payment p WHERE p.paymentDate BETWEEN :startDate AND :endDate AND p.status = 'COMPLETED'")
    Object[] getPaymentStatistics(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT p.paymentMethod, COUNT(p), SUM(p.amount) FROM Payment p WHERE p.paymentDate BETWEEN :startDate AND :endDate AND p.status = 'COMPLETED' GROUP BY p.paymentMethod")
    List<Object[]> getPaymentMethodBreakdown(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED'")
    BigDecimal getTotalRevenue();

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE YEAR(p.paymentDate) = :year AND MONTH(p.paymentDate) = :month AND p.status = 'COMPLETED'")
    BigDecimal getMonthlyRevenue(@Param("year") int year, @Param("month") int month);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE DATE(p.paymentDate) = DATE(:date) AND p.status = 'COMPLETED'")
    BigDecimal getDailyRevenue(@Param("date") LocalDateTime date);

    // Search payments
    @Query("SELECT p FROM Payment p WHERE " +
            "p.transactionId LIKE %:searchTerm% OR " +
            "p.patient.firstName LIKE %:searchTerm% OR " +
            "p.patient.lastName LIKE %:searchTerm% OR " +
            "p.patient.email LIKE %:searchTerm%")
    List<Payment> searchPayments(@Param("searchTerm") String searchTerm);

    // Count methods
    long countByStatus(Payment.PaymentStatus status);

    long countByPaymentMethod(Payment.PaymentMethod paymentMethod);

    long countByPatientId(Long patientId);
}