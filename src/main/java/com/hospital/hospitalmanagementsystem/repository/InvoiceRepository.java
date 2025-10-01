package com.hospital.hospitalmanagementsystem.repository;

import com.hospital.hospitalmanagementsystem.model.Invoice;
import com.hospital.hospitalmanagementsystem.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional; // Import Optional

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByPatient(Patient patient);

    List<Invoice> findByStatus(Invoice.InvoiceStatus status);

    // FIX: Changed return type from Invoice to Optional<Invoice> for null-safety
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    // This method supports sorting by issueDate in the database
    List<Invoice> findByPatientOrderByIssueDateDesc(Patient patient);
}