//package com.hospital.hospitalmanagementsystem.service;
//
//import com.hospital.hospitalmanagementsystem.model.Payment;
//import com.hospital.hospitalmanagementsystem.model.PaymentPlan;
//import com.hospital.hospitalmanagementsystem.model.Invoice;
//import com.hospital.hospitalmanagementsystem.model.Patient;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.mail.SimpleMailMessage;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.mail.javamail.MimeMessageHelper;
//import org.springframework.stereotype.Service;
//import org.thymeleaf.TemplateEngine;
//import org.thymeleaf.context.Context;
//
//import javax.mail.MessagingException;
//import javax.mail.internet.MimeMessage;
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//
//@Service
//public class NotificationService {
//
//    @Autowired
//    private JavaMailSender mailSender;
//
//    @Autowired
//    private TemplateEngine templateEngine;
//
//    private final String FROM_EMAIL = "noreply@healthfirst.com";
//    private final String BILLING_EMAIL = "billing@healthfirst.com";
//
//    // Payment notifications
//    public void sendPaymentConfirmation(Patient patient, Payment payment) {
//        try {
//            Context context = new Context();
//            context.setVariable("patient", patient);
//            context.setVariable("payment", payment);
//            context.setVariable("invoice", payment.getInvoice());
//
//            String subject = "Payment Confirmation - " + payment.getReceiptNumber();
//            String htmlContent = templateEngine.process("email/payment-confirmation", context);
//
//            sendHtmlEmail(patient.getEmail(), subject, htmlContent);
//        } catch (Exception e) {
//            // Log error but don't throw exception to avoid breaking payment flow
//            System.err.println("Failed to send payment confirmation email: " + e.getMessage());
//        }
//    }
//
//    public void sendBankTransferAcknowledgment(Patient patient, Payment payment) {
//        try {
//            Context context = new Context();
//            context.setVariable("patient", patient);
//            context.setVariable("payment", payment);
//            context.setVariable("invoice", payment.getInvoice());
//
//            String subject = "Bank Transfer Receipt Received - " + payment.getInvoice().getInvoiceNumber();
//            String htmlContent = templateEngine.process("email/bank-transfer-acknowledgment", context);
//
//            sendHtmlEmail(patient.getEmail(), subject, htmlContent);
//        } catch (Exception e) {
//            System.err.println("Failed to send bank transfer acknowledgment: " + e.getMessage());
//        }
//    }
//
//    public void sendPaymentVerificationSuccess(Patient patient, Payment payment) {
//        try {
//            Context context = new Context();
//            context.setVariable("patient", patient);
//            context.setVariable("payment", payment);
//            context.setVariable("invoice", payment.getInvoice());
//
//            String subject = "Payment Verified - " + payment.getInvoice().getInvoiceNumber();
//            String htmlContent = templateEngine.process("email/payment-verification-success", context);
//
//            sendHtmlEmail(patient.getEmail(), subject, htmlContent);
//        } catch (Exception e) {
//            System.err.println("Failed to send payment verification success email: " + e.getMessage());
//        }
//    }
//
//    public void sendPaymentVerificationRejection(Patient patient, Payment payment, String reason) {
//        try {
//            Context context = new Context();
//            context.setVariable("patient", patient);
//            context.setVariable("payment", payment);
//            context.setVariable("invoice", payment.getInvoice());
//            context.setVariable("reason", reason);
//
//            String subject = "Payment Verification Issue - " + payment.getInvoice().getInvoiceNumber();
//            String htmlContent = templateEngine.process("email/payment-verification-rejection", context);
//
//            sendHtmlEmail(patient.getEmail(), subject, htmlContent);
//        } catch (Exception e) {
//            System.err.println("Failed to send payment verification rejection email: " + e.getMessage());
//        }
//    }
//
//    public void sendPartialPaymentVerification(Patient patient, Payment payment) {
//        try {
//            Context context = new Context();
//            context.setVariable("patient", patient);
//            context.setVariable("payment", payment);
//            context.setVariable("invoice", payment.getInvoice());
//
//            String subject = "Partial Payment Verified - " + payment.getInvoice().getInvoiceNumber();
//            String htmlContent = templateEngine.process("email/partial-payment-verification", context);
//
//            sendHtmlEmail(patient.getEmail(), subject, htmlContent);
//        } catch (Exception e) {
//            System.err.println("Failed to send partial payment verification email: " + e.getMessage());
//        }
//    }
//
//    public void sendRefundNotification(Patient patient, Payment refundPayment) {
//        try {
//            Context context = new Context();
//            context.setVariable("patient", patient);
//            context.setVariable("refundPayment", refundPayment);
//            context.setVariable("invoice", refundPayment.getInvoice());
//
//            String subject = "Refund Processed - " + refundPayment.getTransactionId();
//            String htmlContent = templateEngine.process("email/refund-notification", context);
//
//            sendHtmlEmail(patient.getEmail(), subject, htmlContent);
//        } catch (Exception e) {
//            System.err.println("Failed to send refund notification: " + e.getMessage());
//        }
//    }
//
//    // Invoice notifications
//    public void sendInvoiceNotification(Patient patient, Invoice invoice) {
//        try {
//            Context context = new Context();
//            context.setVariable("patient", patient);
//            context.setVariable("invoice", invoice);
//
//            String subject = "New Invoice - " + invoice.getInvoiceNumber();
//            String htmlContent = templateEngine.process("email/new-invoice", context);
//
//            sendHtmlEmail(patient.getEmail(), subject, htmlContent);
//        } catch (Exception e) {
//            System.err.println("Failed to send invoice notification: " + e.getMessage());
//        }
//    }
//
//    public void sendInvoiceReminder(Patient patient, Invoice invoice) {
//        try {
//            Context context = new Context();
//            context.setVariable("patient", patient);
//            context.setVariable("invoice", invoice);
//
//            String subject = "Payment Reminder - " + invoice.getInvoiceNumber();
//            String htmlContent = templateEngine.process("email/invoice-reminder", context);
//
//            sendHtmlEmail(patient.getEmail(), subject, htmlContent);
//        } catch (Exception e) {
//            System.err.println("Failed to send invoice reminder: " + e.getMessage());
//        }
//    }
//
//    public void sendOverdueNotification(Patient patient, Invoice invoice) {
//        try {
//            Context context = new Context();
//            context.setVariable("patient", patient);
//            context.setVariable("invoice", invoice);
//
//            String subject = "OVERDUE: Payment Required - " + invoice.getInvoiceNumber();
//            String htmlContent = templateEngine.process("email/overdue-notification", context);
//
//            sendHtmlEmail(patient.getEmail(), subject, htmlContent);
//        } catch (Exception e) {
//            System.err.println("Failed to send overdue notification: " + e.getMessage());
//        }
//    }
//
//    public void sendInvoiceCancellationNotification(Patient patient, Invoice invoice, String reason) {
//        try {
//            Context context = new Context();
//            context.setVariable("patient", patient);
//            context.setVariable("invoice", invoice);
//            context.setVariable("reason", reason);
//
//            String subject = "Invoice Cancelled - " + invoice.getInvoiceNumber();
//            String htmlContent = templateEngine.process("email/invoice-cancellation", context);
//
//            sendHtmlEmail(patient.getEmail(), subject, htmlContent);
//        } catch (Exception e) {
//            System.err.println("Failed to send invoice cancellation notification: " + e.getMessage());
//        }
//    }
//
//    public void sendDiscountNotification(Patient patient, Invoice invoice, BigDecimal discountAmount, String reason) {
//        try {
//            Context context = new Context();
//            context.setVariable("patient", patient);
//            context.setVariable("invoice", invoice);
//            context.setVariable("discountAmount", discountAmount);
//            context.setVariable("reason", reason);
//
//            String subject = "Discount Applied - " + invoice.getInvoiceNumber();
//            String htmlContent = templateEngine.process("email/discount-notification", context);
//
//            sendHtmlEmail(patient.getEmail(), subject, htmlContent);
//        } catch (Exception e) {
//            System.err.println("Failed to send discount notification: " + e.getMessage());
//        }
//    }
//
//    // Payment plan notifications
//    public void sendPaymentPlanConfirmation(Patient patient, PaymentPlan paymentPlan) {
//        try {
//            Context context = new Context();
//            context.setVariable("patient", patient);
//            context.setVariable("paymentPlan", paymentPlan);
//            context.setVariable("invoice", paymentPlan.getInvoice());
//
//            String subject = "Payment Plan Confirmed - " + paymentPlan.getPlanNumber();
//            String htmlContent = templateEngine.process("email/payment-plan-confirmation", context);
//
//            sendHtmlEmail(patient.getEmail(), subject, htmlContent);
//        } catch (Exception e) {
//            System.err.println("Failed to send payment plan confirmation: " + e.getMessage());
//        }
//    }
//
//    public void sendPaymentPlanPaymentConfirmation(Patient patient, PaymentPlan paymentPlan, Payment payment) {
//        try {
//            Context context = new Context();
//            context.setVariable("patient", patient);
//            context.setVariable("paymentPlan", paymentPlan);
//            context.setVariable("payment", payment);
//            context.setVariable("invoice", paymentPlan.getInvoice());
//
//            String subject = "Payment Plan Payment Received - " + paymentPlan.getPlanNumber();
//            String htmlContent = templateEngine.process("email/payment-plan-payment-confirmation", context);
//
//            sendHtmlEmail(patient.getEmail(), subject, htmlContent);
//        } catch (Exception e) {
//            System.err.println("Failed to send payment plan payment confirmation: " + e.getMessage());
//        }
//    }
//
//    public void sendPaymentReminder(Patient patient, PaymentPlan paymentPlan) {
//        try {
//            Context context = new Context();
//            context.setVariable("patient", patient);
//            context.setVariable("paymentPlan", paymentPlan);
//            context.setVariable("invoice", paymentPlan.getInvoice());
//
//            String subject = "Payment Due Today - " + paymentPlan.getPlanNumber();
//            String htmlContent = templateEngine.process("email/payment-plan-reminder", context);
//
//            sendHtmlEmail(patient.getEmail(), subject, htmlContent);
//        } catch (Exception e) {
//            System.err.println("Failed to send payment reminder: " + e.getMessage());
//        }
//    }
//
//    public void sendPaymentDueReminder(Patient patient, PaymentPlan paymentPlan) {
//        try {
//            Context context = new Context();
//            context.setVariable("patient", patient);
//            context.setVariable("paymentPlan", paymentPlan);
//            context.setVariable("invoice", paymentPlan.getInvoice());
//
//            String subject = "Payment Due Soon - " + paymentPlan.getPlanNumber();
//            String htmlContent = templateEngine.process("email/payment-plan-due-reminder", context);
//
//            sendHtmlEmail(patient.getEmail(), subject, htmlContent);
//        } catch (Exception e) {
//            System.err.println("Failed to send payment due reminder: " + e.getMessage());
//        }
//    }
//
//    public void sendOverduePaymentNotification(Patient patient, PaymentPlan paymentPlan) {
//        try {
//            Context context = new Context();
//            context.setVariable("patient", patient);
//            context.setVariable("paymentPlan", paymentPlan);
//            context.setVariable("invoice", paymentPlan.getInvoice());
//
//            String subject = "OVERDUE: Payment Plan Payment - " + paymentPlan.getPlanNumber();
//            String htmlContent = templateEngine.process("email/payment-plan-overdue", context);
//
//            sendHtmlEmail(patient.getEmail(), subject, htmlContent);
//        } catch (Exception e) {
//            System.err.println("Failed to send overdue payment notification: " + e.getMessage());
//        }
//    }
//
//    public void sendPaymentPlanSuspensionNotification(Patient patient, PaymentPlan paymentPlan, String reason) {
//        try {
//            Context context = new Context();
//            context.setVariable("patient", patient);
//            context.setVariable("paymentPlan", paymentPlan);
//            context.setVariable("reason", reason);
//
//            String subject = "Payment Plan Suspended - " + paymentPlan.getPlanNumber();
//            String htmlContent = templateEngine.process("email/payment-plan-suspension", context);
//
//            sendHtmlEmail(patient.getEmail(), subject, htmlContent);
//        } catch (Exception e) {
//            System.err.println("Failed to send payment plan suspension notification: " + e.getMessage());
//        }
//    }
//
//    public void sendPaymentPlanReactivationNotification(Patient patient, PaymentPlan paymentPlan) {
//        try {
//            Context context = new Context();
//            context.setVariable("patient", patient);
//            context.setVariable("paymentPlan", paymentPlan);
//
//            String subject = "Payment Plan Reactivated - " + paymentPlan.getPlanNumber();
//            String htmlContent = templateEngine.process("email/payment-plan-reactivation", context);
//
//            sendHtmlEmail(patient.getEmail(), subject, htmlContent);
//        } catch (Exception e) {
//            System.err.println("Failed to send payment plan reactivation notification: " + e.getMessage());
//        }
//    }
//
//    public void sendPaymentPlanCancellationNotification(Patient patient, PaymentPlan paymentPlan, String reason) {
//        try {
//            Context context = new Context();
//            context.setVariable("patient", patient);
//            context.setVariable("paymentPlan", paymentPlan);
//            context.setVariable("reason", reason);
//
//            String subject = "Payment Plan Cancelled - " + paymentPlan.getPlanNumber();
//            String htmlContent = templateEngine.process("email/payment-plan-cancellation", context);
//
//            sendHtmlEmail(patient.getEmail(), subject, htmlContent);
//        } catch (Exception e) {
//            System.err.println("Failed to send payment plan cancellation notification: " + e.getMessage());
//        }
//    }
//
//    public void sendAutoPayFailureNotification(Patient patient, PaymentPlan paymentPlan, String errorMessage) {
//        try {
//            Context context = new Context();
//            context.setVariable("patient", patient);
//            context.setVariable("paymentPlan", paymentPlan);
//            context.setVariable("errorMessage", errorMessage);
//
//            String subject = "Auto-Pay Failed - " + paymentPlan.getPlanNumber();
//            String htmlContent = templateEngine.process("email/autopay-failure", context);
//
//            sendHtmlEmail(patient.getEmail(), subject, htmlContent);
//        } catch (Exception e) {
//            System.err.println("Failed to send auto-pay failure notification: " + e.getMessage());
//        }
//    }
//
//    // Staff notifications
//    public void notifyAccountantsForVerification(Payment payment) {
//        try {
//            Context context = new Context();
//            context.setVariable("payment", payment);
//            context.setVariable("invoice", payment.getInvoice());
//            context.setVariable("patient", payment.getPatient());
//
//            String subject = "Bank Transfer Verification Required - " + payment.getTransactionId();
//            String htmlContent = templateEngine.process("email/verification-required", context);
//
//            sendHtmlEmail(BILLING_EMAIL, subject, htmlContent);
//        } catch (Exception e) {
//            System.err.println("Failed to notify accountants for verification: " + e.getMessage());
//        }
//    }
//
//    // Utility methods
//    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
//        MimeMessage message = mailSender.createMimeMessage();
//        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
//
//        helper.setFrom(FROM_EMAIL);
//        helper.setTo(to);
//        helper.setSubject(subject);
//        helper.setText(htmlContent, true);
//
//        mailSender.send(message);
//    }
//
//    private void sendSimpleEmail(String to, String subject, String text) {
//        try {
//            SimpleMailMessage message = new SimpleMailMessage();
//            message.setFrom(FROM_EMAIL);
//            message.setTo(to);
//            message.setSubject(subject);
//            message.setText(text);
//
//            mailSender.send(message);
//        } catch (Exception e) {
//            System.err.println("Failed to send simple email: " + e.getMessage());
//        }
//    }
//}

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
            Payment payment = paymentService.getPaymentById(paymentId);
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
            PaymentPlan plan = paymentPlanService.getPaymentPlanById(planId);
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