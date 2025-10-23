package com.hospital.hospitalmanagementsystem.service;

import com.hospital.hospitalmanagementsystem.model.Appointment;
import com.hospital.hospitalmanagementsystem.model.Invoice;
import com.hospital.hospitalmanagementsystem.model.Order;
import com.hospital.hospitalmanagementsystem.model.Payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    public EmailService(JavaMailSender javaMailSender, TemplateEngine templateEngine) {
        this.javaMailSender = javaMailSender;
        this.templateEngine = templateEngine;
    }

    /**
     * Sends an appointment confirmation email asynchronously.
     * @param appointment The appointment that was booked.
     */
    @Async
    public void sendAppointmentConfirmationEmail(Appointment appointment, Invoice invoice, byte[] pdfInvoice) {
        if (appointment.getPatient() == null || appointment.getPatient().getEmail() == null) {
            logger.warn("Cannot send confirmation email. Patient or patient email is null for appointment ID: {}", appointment.getId());
            return;
        }

        try {
            Context context = new Context();
            context.setVariable("patientName", appointment.getPatient().getFirstName());
            context.setVariable("doctorName", "Dr. " + appointment.getDoctor().getFirstName() + " " + appointment.getDoctor().getLastName());
            context.setVariable("appointmentDate", appointment.getAppointmentDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            context.setVariable("appointmentTime", appointment.getAppointmentTime().format(DateTimeFormatter.ofPattern("hh:mm a")));
            context.setVariable("reason", appointment.getReason());

            String htmlContent = templateEngine.process("home/emails/appointment-confirmation-email", context);

            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(appointment.getPatient().getEmail());
            helper.setSubject("Your Appointment Confirmation & Invoice From HealthFirst");
            helper.setText(htmlContent, true);
            helper.setFrom("noreply@healthfirst.com", "HealthFirst Hospital");

            // Attach the generated PDF invoice
            helper.addAttachment("Invoice-" + invoice.getInvoiceNumber() + ".pdf", new ByteArrayResource(pdfInvoice));

            javaMailSender.send(mimeMessage);
            logger.info("Appointment confirmation email with invoice sent to {}", appointment.getPatient().getEmail());

        } catch (Exception e) {
            logger.error("Failed to send appointment confirmation email for appointment ID: {}", appointment.getId(), e);
        }
    }

    @Async
    public void sendOrderConfirmationWithAttachment(Order order, byte[] pdf) {
        if (order.getPatient() == null || order.getPatient().getEmail() == null) {
            logger.warn("Cannot send order confirmation. Patient or email is null for order ID: {}", order.getId());
            return;
        }

        try {
            Context context = new Context();
            context.setVariable("patientName", order.getPatient().getFirstName());
            context.setVariable("orderId", order.getId());
            context.setVariable("totalAmount", order.getTotalAmount());

            String htmlContent = templateEngine.process("home/emails/order-confirmation-email", context);

            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(order.getPatient().getEmail());
            helper.setSubject("Your HealthFirst Pharmacy Order Confirmation #" + order.getId());
            helper.setText(htmlContent, true);
            helper.setFrom("pharmacy@healthfirst.com", "HealthFirst Pharmacy");

            helper.addAttachment("Receipt-" + order.getId() + ".pdf", new ByteArrayResource(pdf));

            javaMailSender.send(mimeMessage);
            logger.info("Order confirmation email with receipt sent to {}", order.getPatient().getEmail());

        } catch (Exception e) {
            logger.error("Failed to send order confirmation email for order ID: {}", order.getId(), e);
        }
    }

    /**
     * ADDED: Sends an email with a generic PDF attachment, like a report.
     * @param toEmail The recipient's email address.
     * @param subject The subject of the email.
     * @param reportName The desired name for the attached PDF file.
     * @param pdfData The byte array of the PDF to be attached.
     */
    @Async
    public void sendReportByEmail(String toEmail, String subject, String reportName, byte[] pdfData) {
        if (toEmail == null || toEmail.isEmpty()) {
            logger.warn("Cannot send report email. Recipient email is null or empty.");
            return;
        }

        try {
            Context context = new Context();
            context.setVariable("reportDate", LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));

            String htmlContent = templateEngine.process("home/emails/report-email-template", context);

            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom("reports@healthfirst.com", "HealthFirst Reports");

            // Attach the generated PDF report
            helper.addAttachment(reportName, new ByteArrayResource(pdfData));

            javaMailSender.send(mimeMessage);
            logger.info("Report email sent successfully to {}", toEmail);

        } catch (Exception e) {
            logger.error("Failed to send report email to {}", toEmail, e);
        }
    }

    /**
     * ADDED: Sends a payment confirmation email for both full and partial payments.
     * This email includes a PDF receipt of the transaction.
     * @param payment The completed payment transaction.
     * @param pdfReceipt A byte array of the generated PDF receipt.
     */
    @Async
    public void sendPaymentConfirmationEmail(Payment payment, byte[] pdfReceipt) {
        if (payment.getPatient() == null || payment.getPatient().getEmail() == null) {
            logger.warn("Cannot send payment confirmation email. Patient or patient email is null for payment ID: {}", payment.getId());
            return;
        }

        try {
            Invoice invoice = payment.getInvoice();

            Context context = new Context();
            context.setVariable("patientName", payment.getPatient().getFirstName());
            context.setVariable("invoiceNumber", invoice.getInvoiceNumber());
            context.setVariable("paymentAmount", payment.getAmount());
            context.setVariable("balanceDue", invoice.getBalanceDue());
            context.setVariable("paymentDate", payment.getPaymentDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            context.setVariable("transactionId", payment.getTransactionId());
            context.setVariable("statusMessage", invoice.getBalanceDue().compareTo(BigDecimal.ZERO) > 0
                    ? "A partial payment has been successfully applied to your invoice."
                    : "Your invoice has been paid in full. Thank you!");

            String htmlContent = templateEngine.process("home/emails/payment-confirmation-email", context);

            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(payment.getPatient().getEmail());
            helper.setSubject("Payment Confirmation for Invoice " + invoice.getInvoiceNumber());
            helper.setText(htmlContent, true);
            helper.setFrom("billing@healthfirst.com", "HealthFirst Billing");

            // Attach the generated PDF receipt
            helper.addAttachment("Receipt-" + payment.getTransactionId() + ".pdf", new ByteArrayResource(pdfReceipt));

            javaMailSender.send(mimeMessage);
            logger.info("Payment confirmation email with receipt sent to {}", payment.getPatient().getEmail());

        } catch (Exception e) {
            logger.error("Failed to send payment confirmation email for payment ID: {}", payment.getId(), e);
        }
    }

}