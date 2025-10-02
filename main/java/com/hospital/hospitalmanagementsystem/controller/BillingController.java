package com.hospital.hospitalmanagementsystem.controller;

import com.hospital.hospitalmanagementsystem.model.Invoice;
import com.hospital.hospitalmanagementsystem.model.Payment;
import com.hospital.hospitalmanagementsystem.service.BillingService;
import com.hospital.hospitalmanagementsystem.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    @Autowired
    private BillingService billingService;

    @Autowired
    private PatientService patientService;

    @GetMapping("/invoices/patient/{patientId}")
    @PreAuthorize("hasAnyRole('ROLE_ACCOUNTANT', 'ROLE_PATIENT')")
    public ResponseEntity<List<Invoice>> getInvoicesByPatient(@PathVariable Long patientId) {
        return patientService.getPatientById(patientId)
                .map(patient -> ResponseEntity.ok(billingService.findInvoicesByPatient(patient)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/invoices")
    @PreAuthorize("hasRole('ROLE_ACCOUNTANT')")
    public Invoice createInvoice(@RequestBody Invoice invoice) {
        return billingService.saveInvoice(invoice);
    }

    @PostMapping("/payments")
    @PreAuthorize("hasRole('ROLE_ACCOUNTANT')")
    public Payment createPayment(@RequestBody Payment payment) {
        return billingService.savePayment(payment);
    }
}