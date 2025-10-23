package com.hospital.hospitalmanagementsystem.repository;

import com.hospital.hospitalmanagementsystem.model.Invoice;
import com.hospital.hospitalmanagementsystem.model.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByPatient(Patient patient);

    List<Invoice> findByStatus(Invoice.InvoiceStatus status);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByPatientOrderByIssueDateDesc(Patient patient);

    // ADDED: Paginated query for patient invoices, sorted by issue date descending
    Page<Invoice> findByPatientOrderByIssueDateDesc(Patient patient, Pageable pageable);

    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.paymentPlan WHERE i.patient = :patient")
    List<Invoice> findByPatientWithPaymentPlan(@Param("patient") Patient patient);

    long countByPatient(Patient patient);

    List<Invoice> findByIssueDateBetween(LocalDate startDate, LocalDate endDate);

}