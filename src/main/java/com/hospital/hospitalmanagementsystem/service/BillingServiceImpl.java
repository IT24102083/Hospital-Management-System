package com.hospital.hospitalmanagementsystem.service;

import com.hospital.hospitalmanagementsystem.model.Invoice;
import com.hospital.hospitalmanagementsystem.model.Patient;
import com.hospital.hospitalmanagementsystem.model.Payment;
import com.hospital.hospitalmanagementsystem.repository.InvoiceRepository;
import com.hospital.hospitalmanagementsystem.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BillingServiceImpl implements BillingService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    @Autowired
    public BillingServiceImpl(InvoiceRepository invoiceRepository, PaymentRepository paymentRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public Optional<Invoice> findInvoiceById(Long id) {
        return invoiceRepository.findById(id);
    }

    @Override
    public List<Invoice> findInvoicesByPatient(Patient patient) {
        return invoiceRepository.findByPatient(patient);
    }

    @Override
    public Invoice saveInvoice(Invoice invoice) {
        return invoiceRepository.save(invoice);
    }

    @Override
    public Payment savePayment(Payment payment) {
        return paymentRepository.save(payment);
    }

    @Override
    public Optional<Payment> findPaymentById(Long id) {
        return paymentRepository.findById(id);
    }
}