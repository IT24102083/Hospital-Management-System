package com.hospital.hospitalmanagementsystem.service;

import com.hospital.hospitalmanagementsystem.model.*;
import com.hospital.hospitalmanagementsystem.service.external.EmailService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {

    @Autowired
    private EmailService emailService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentPlanService paymentPlanService;

    // ========== EMAIL NOTIFICATIONS ==========

    public void sendInvoiceCreatedEmail(Long invoiceId) {
        try {
            // FIX: Correctly handle the Optional return type
            Invoice invoice = invoiceService.getInvoiceById(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + invoiceId));

            Patient patient = invoice.getPatient();

            String subject = "New Invoice Created - " + invoice.getInvoiceNumber();
            String message = buildInvoiceCreatedEmailContent(invoice);

            emailService.sendEmail(patient.getEmail(), subject, message);
        } catch (Exception e) {
            System.err.println("Failed to send invoice created email: " + e.getMessage());
        }
    }

    public void sendPaymentConfirmationEmail(Long paymentId) {
        try {
            Payment payment = paymentService.getPaymentById(paymentId)
                    .orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));
            Patient patient = payment.getPatient();

            String subject = "Payment Confirmation - Transaction #" + payment.getTransactionId();
            String message = buildPaymentConfirmationEmailContent(payment);

            emailService.sendEmail(patient.getEmail(), subject, message);
        } catch (Exception e) {
            System.err.println("Failed to send payment confirmation email: " + e.getMessage());
        }
    }

    public void sendPaymentPlanCreatedEmail(Long planId) {
        try {
            // FIXED: Correctly unwrapped the Optional<PaymentPlan>
            PaymentPlan plan = paymentPlanService.getPaymentPlanById(planId)
                    .orElseThrow(() -> new RuntimeException("Payment plan not found with ID: " + planId));
            Patient patient = plan.getPatient();

            String subject = "Payment Plan Created - " + plan.getPlanNumber();
            String message = buildPaymentPlanCreatedEmailContent(plan);

            emailService.sendEmail(patient.getEmail(), subject, message);
        } catch (Exception e) {
            System.err.println("Failed to send payment plan created email: " + e.getMessage());
        }
    }

    // ========== EMAIL CONTENT BUILDERS ==========

    private String buildInvoiceCreatedEmailContent(Invoice invoice) {
        return "Dear " + invoice.getPatient().getFirstName() + ",\n\n" +
                "A new invoice has been created for your account.\n\n" +
                "Invoice Number: " + invoice.getInvoiceNumber() + "\n" +
                "Amount: $" + invoice.getTotal() + "\n" +
                "Due Date: " + invoice.getDueDate() + "\n\n" +
                "Please log in to your patient portal to view details and make payment.\n\n" +
                "Best regards,\nHospital Billing Department";
    }

    private String buildPaymentConfirmationEmailContent(Payment payment) {
        return "Dear " + payment.getPatient().getFirstName() + ",\n\n" +
                "Your payment has been successfully processed.\n\n" +
                "Payment Details:\n" +
                "Transaction ID: " + payment.getTransactionId() + "\n" +
                "Amount Paid: $" + payment.getAmount() + "\n" +
                "Payment Method: " + payment.getPaymentMethod() + "\n" +
                "Date: " + payment.getPaymentDate() + "\n\n" +
                "Thank you for your payment.\n\n" +
                "Best regards,\nHospital Billing Department";
    }

    private String buildPaymentPlanCreatedEmailContent(PaymentPlan plan) {
        return "Dear " + plan.getPatient().getFirstName() + ",\n\n" +
                "Your payment plan has been successfully created.\n\n" +
                "Payment Plan Details:\n" +
                "Plan Number: " + plan.getPlanNumber() + "\n" +
                "Total Amount: $" + plan.getTotalAmount() + "\n" +
                "Monthly Payment: $" + plan.getMonthlyPayment() + "\n" +
                "Number of Payments: " + plan.getNumberOfPayments() + "\n" +
                "First Payment Due: " + plan.getStartDate() + "\n\n" +
                "Thank you for setting up a payment plan with us.\n\n" +
                "Best regards,\nHospital Billing Department";
    }
}