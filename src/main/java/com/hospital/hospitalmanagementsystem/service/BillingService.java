package com.hospital.hospitalmanagementsystem.service;

import com.hospital.hospitalmanagementsystem.model.Invoice;
import com.hospital.hospitalmanagementsystem.model.Payment;
import com.hospital.hospitalmanagementsystem.model.Patient;

import java.util.List;
import java.util.Optional;

public interface BillingService {
    Optional<Invoice> findInvoiceById(Long id);
    List<Invoice> findInvoicesByPatient(Patient patient);
    Invoice saveInvoice(Invoice invoice);
    Payment savePayment(Payment payment);
    Optional<Payment> findPaymentById(Long id);
}
