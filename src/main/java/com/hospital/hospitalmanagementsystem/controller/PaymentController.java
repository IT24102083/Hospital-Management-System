package com.hospital.hospitalmanagementsystem.controller;

import com.hospital.hospitalmanagementsystem.model.*;
import com.hospital.hospitalmanagementsystem.service.InvoiceService;
import com.hospital.hospitalmanagementsystem.service.PatientService;
import com.hospital.hospitalmanagementsystem.service.PaymentService;
import com.hospital.hospitalmanagementsystem.service.PaymentPlanService;
import com.hospital.hospitalmanagementsystem.service.UserService;
import com.hospital.hospitalmanagementsystem.service.external.FileStorageService;
import com.hospital.hospitalmanagementsystem.service.EmailService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.util.Base64;


@Controller
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private UserService userService;

    @Autowired
    private PaymentPlanService paymentPlanService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private EmailService emailService;

    // FIXED: Unified patient lookup method
    private Patient getPatientFromPrincipal(Principal principal) {
        try {
            // First try to get by username (most common case)
            Optional<User> userOpt = userService.getUserByUsername(principal.getName());
            if (userOpt.isPresent() && userOpt.get().getRole() == User.Role.ROLE_PATIENT) {
                Optional<Patient> patientOpt = patientService.getPatientById(userOpt.get().getId());
                if (patientOpt.isPresent()) {
                    return patientOpt.get();
                }
            }

            // Fallback: try to find by email
            List<Patient> allPatients = patientService.getAllPatients();
            return allPatients.stream()
                    .filter(p -> p.getEmail() != null && p.getEmail().equals(principal.getName()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Patient account not found for user: " + principal.getName()));
        } catch (Exception e) {
            throw new RuntimeException("Unable to access patient account", e);
        }
    }

    // ========== PAYMENT PROCESSING ENDPOINTS ==========

    @GetMapping("/patient/invoices/{invoiceId}/pay")
    @PreAuthorize("hasRole('PATIENT')")
    public String showPaymentMethods(@PathVariable Long invoiceId, Model model, Principal principal) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + invoiceId));
            Patient patient = getPatientFromPrincipal(principal); // Use unified lookup

            validatePatientOwnership(invoice, patient);

            if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
                model.addAttribute("message", "This invoice has already been paid.");
                return "redirect:home//patient/invoices/" + invoiceId;
            }

            model.addAttribute("invoice", invoice);
            model.addAttribute("patient", patient);
            model.addAttribute("availablePaymentMethods", getAvailablePaymentMethods());

            return "home/billing/payment-method-selection";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load payment methods: " + e.getMessage());
            return "home/error";
        }
    }

    @GetMapping("/patient/invoices/{id}/pay/card")
    @PreAuthorize("hasRole('PATIENT')")
    public String showCardPaymentPage(@PathVariable Long id, Model model, Principal principal) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(id)
                    .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + id));
            Patient patient = getPatientFromPrincipal(principal); // Use unified lookup

            validatePatientOwnership(invoice, patient);

            model.addAttribute("invoice", invoice);
            model.addAttribute("patient", patient);
            model.addAttribute("paymentAmount", invoice.getBalanceDue());

            return "home/billing/card-payment-portal";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load card payment form: " + e.getMessage());
            return "home/error";
        }
    }

    @PostMapping("/patient/invoices/{id}/pay/card")
    @PreAuthorize("hasRole('PATIENT')")
    public String processCardPayment(@PathVariable Long id,
                                     @RequestParam String cardNumber,
                                     @RequestParam String cardName,
                                     @RequestParam String cardExpiry,
                                     @RequestParam String cardCvc,
                                     @RequestParam String billingAddress,
                                     @RequestParam String billingCity,
                                     @RequestParam String billingZip,
                                     @RequestParam(required = false) boolean saveCard,
                                     RedirectAttributes redirectAttributes,
                                     Principal principal) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(id)
                    .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + id));
            Patient patient = getPatientFromPrincipal(principal); // Use unified lookup

            validatePatientOwnership(invoice, patient);

            Payment.CreateRequest request = new Payment.CreateRequest();
            request.setInvoiceId(id);
            // NOTE: In a real system, you might pass the amount from the form
            // if you allow users to specify a partial payment amount.
            // Here we assume payment is for the full balance due.
            request.setAmount(invoice.getBalanceDue());
            request.setPaymentMethod(String.valueOf(Payment.PaymentMethod.CREDIT_CARD));
            request.setCardNumber(cardNumber);
            request.setCardName(cardName);
            request.setCardExpiry(cardExpiry);
            request.setCardCvc(cardCvc);

            Payment payment = paymentService.processCardPayment(request);

            if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
                redirectAttributes.addFlashAttribute("success", "Payment processed successfully!");

                try {
                    // Generate the receipt PDF using the existing service
                    byte[] pdfReceipt = fileStorageService.generateReceiptPDF(payment);

                    // Send the confirmation email with the receipt attached
                    emailService.sendPaymentConfirmationEmail(payment, pdfReceipt);

                } catch (Exception emailEx) {

                    // logger.error("Failed to send payment confirmation email for payment ID: {}", payment.getId(), emailEx);
                }

                String s = "payment-success";
                return "redirect:/patient/invoices/" + id + "/" + s + "?paymentId=" + payment.getId();
            } else {
                redirectAttributes.addFlashAttribute("error", "Payment failed. Please try again.");
                return "redirect:/patient/invoices/" + id + "/pay/card";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Payment processing error: " + e.getMessage());
            return "redirect:/patient/invoices/" + id + "/pay/card";
        }
    }

    @GetMapping("/patient/invoices/{id}/pay/bank-transfer")
    @PreAuthorize("hasRole('PATIENT')")
    public String showBankTransferPage(@PathVariable Long id, Model model, Principal principal) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(id)
                    .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + id));
            Patient patient = getPatientFromPrincipal(principal); // Use unified lookup

            validatePatientOwnership(invoice, patient);

            model.addAttribute("invoice", invoice);
            model.addAttribute("patient", patient);
            model.addAttribute("bankDetails", getBankAccountDetails());

            try {
                // 1. Create a string with the payment data
                // (This is a simple example; you can use a standardized format like UPI if needed)
                String qrCodeData = "Bank: HealthFirst Bank\n" +
                        "Account: 1234567890\n" +
                        "Amount: " + invoice.getBalanceDue() + "\n" +
                        "Reference: " + invoice.getInvoiceNumber();

                // 2. Generate the QR code image bytes
                QRCodeWriter qrCodeWriter = new QRCodeWriter();
                BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeData, BarcodeFormat.QR_CODE, 200, 200);

                ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
                byte[] pngData = pngOutputStream.toByteArray();

                // 3. Encode bytes to Base64 and add to the model
                String qrCodeBase64 = Base64.getEncoder().encodeToString(pngData);
                model.addAttribute("qrCodeImage", qrCodeBase64);

            } catch (Exception e) {
                model.addAttribute("qrCodeImage", null); // Handle error case
            }

            return "home/billing/bank-transfer-payment";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load bank transfer form: " + e.getMessage());
            return "home/error";
        }
    }

    @PostMapping("/patient/invoices/{id}/pay/bank-transfer")
    @PreAuthorize("hasRole('PATIENT')")
    public String processBankTransferPayment(@PathVariable Long id,
                                             @RequestParam BigDecimal amount,
                                             @RequestParam String referenceNumber,
                                             @RequestParam(required = false) MultipartFile receiptFile,
                                             @RequestParam(required = false) String transferNotes,
                                             RedirectAttributes redirectAttributes,
                                             Principal principal) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(id)
                    .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + id));
            Patient patient = getPatientFromPrincipal(principal); // Use unified lookup

            validatePatientOwnership(invoice, patient);

            Payment payment = paymentService.processBankTransferPayment(
                    id, amount, referenceNumber, receiptFile, transferNotes);

            redirectAttributes.addFlashAttribute("success",
                    "Bank transfer receipt uploaded successfully. Your payment is being verified.");
            return "redirect:/patient/invoices/" + id + "/bank-transfer-pending?paymentId=" + payment.getId();

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error processing bank transfer: " + e.getMessage());
            return "redirect:/patient/invoices/" + id + "/pay/bank-transfer";
        }
    }

    @GetMapping("/patient/invoices/{id}/payment-success")
    @PreAuthorize("hasRole('PATIENT')")
    public String showPaymentSuccess(@PathVariable Long id,
                                     @RequestParam Long paymentId,
                                     Model model,
                                     Principal principal) {
        try {
            Payment payment = paymentService.getPaymentById(paymentId)
                    .orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));
            Invoice invoice = payment.getInvoice();
            Patient patient = getPatientFromPrincipal(principal); // Use unified lookup

            if (!payment.getPatient().getId().equals(patient.getId())) {
                throw new org.springframework.security.access.AccessDeniedException("Access denied");
            }

            model.addAttribute("payment", payment);
            model.addAttribute("invoice", invoice);
            model.addAttribute("patient", patient);

            return "home/billing/payment-success";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load payment confirmation: " + e.getMessage());
            return "home/error";
        }
    }

    @GetMapping("/patient/invoices/{id}/bank-transfer-pending")
    @PreAuthorize("hasRole('PATIENT')")
    public String showBankTransferPending(@PathVariable Long id,
                                          @RequestParam Long paymentId,
                                          Model model,
                                          Principal principal) {
        try {
            Payment payment = paymentService.getPaymentById(paymentId)
                    .orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));
            Invoice invoice = payment.getInvoice();
            Patient patient = getPatientFromPrincipal(principal); // Use your existing helper method

            // Security check to ensure the logged-in patient owns this payment
            if (!payment.getPatient().getId().equals(patient.getId())) {
                // Deny access if the payment does not belong to the current user
                throw new org.springframework.security.access.AccessDeniedException("Access is denied to this payment record.");
            }

            model.addAttribute("payment", payment);
            model.addAttribute("invoice", invoice);
            model.addAttribute("patient", patient);

            // This should point to a new HTML file you will create
            return "home/billing/bank-transfer-pending";

        } catch (Exception e) {
            model.addAttribute("error", "Unable to load payment confirmation: " + e.getMessage());
            return "home/error";
        }
    }


    @GetMapping("/patient/invoices/{id}/receipt/view")
    @PreAuthorize("hasRole('PATIENT')")
    public String showReceipt(@PathVariable Long id,
                              @RequestParam(required = false) Long paymentId,
                              Model model,
                              Principal principal) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(id)
                    .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + id));
            Patient patient = getPatientFromPrincipal(principal); // Use unified lookup

            validatePatientOwnership(invoice, patient);

            Payment payment = null;
            if (paymentId != null) {
                payment = paymentService.getPaymentById(paymentId)
                        .orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));
            } else {
                List<Payment> payments = paymentService.getPaymentsByInvoiceId(id);
                payment = payments.stream()
                        .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
                        .findFirst()
                        .orElse(null);
            }

            if (payment == null) {
                model.addAttribute("error", "No payment found for this invoice");
                return "home/error";
            }

            model.addAttribute("payment", payment);
            model.addAttribute("invoice", invoice);
            model.addAttribute("patient", patient);

            return "home/billing/receipt-generator";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load receipt: " + e.getMessage());
            return "home/error";
        }
    }

    @GetMapping("/patient/invoices/{id}/receipt/download")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable Long id,
                                                  @RequestParam(required = false) Long paymentId,
                                                  Principal principal) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(id)
                    .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + id));
            Patient patient = getPatientFromPrincipal(principal); // Use unified lookup

            validatePatientOwnership(invoice, patient);

            Payment payment = null;
            if (paymentId != null) {
                payment = paymentService.getPaymentById(paymentId)
                        .orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));
            } else {
                List<Payment> payments = paymentService.getPaymentsByInvoiceId(id);
                payment = payments.stream()
                        .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
                        .findFirst()
                        .orElse(null);
            }

            if (payment == null) {
                return ResponseEntity.notFound().build();
            }

            byte[] pdfData = fileStorageService.generateReceiptPDF(payment);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    "receipt_" + generateReceiptNumber(payment) + ".pdf");

            return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== PAYMENT PLAN ENDPOINTS ==========

    @GetMapping("/patient/invoices/{id}/setup-payment-plan")
    @PreAuthorize("hasRole('PATIENT')")
    public String showPaymentPlanSetup(@PathVariable Long id, Model model, Principal principal) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(id)
                    .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + id));
            Patient patient = getPatientFromPrincipal(principal); // Use unified lookup

            validatePatientOwnership(invoice, patient);

            if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
                model.addAttribute("message", "This invoice has already been paid.");
                return "redirect:/patient/invoices/" + id;
            }

            model.addAttribute("invoice", invoice);
            model.addAttribute("patient", patient);
            model.addAttribute("paymentPlanRequest", new PaymentPlan.CreateRequest());
            model.addAttribute("maxInstallments", 12);

            return "home/billing/payment-plan-setup";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load payment plan setup: " + e.getMessage());
            return "home/error";
        }
    }

    @PostMapping("/patient/invoices/{id}/setup-payment-plan")
    @PreAuthorize("hasRole('PATIENT')")
    public String createPaymentPlan(@PathVariable Long id,
                                    @Valid @ModelAttribute PaymentPlan.CreateRequest request,
                                    RedirectAttributes redirectAttributes,
                                    Principal principal) {
        try {
            // Correctly handle the Optional return type
            Invoice invoice = invoiceService.getInvoiceById(id)
                    .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + id));

            Patient patient = getPatientFromPrincipal(principal);

            validatePatientOwnership(invoice, patient);

            PaymentPlan paymentPlan = paymentPlanService.createPaymentPlan(id, request);
            redirectAttributes.addFlashAttribute("success",
                    "Payment plan created successfully! Your first payment is due on " +
                            paymentPlanService.getFirstPaymentDate(paymentPlan));

            return "redirect:/patient/invoices/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating payment plan: " + e.getMessage());
            return "redirect:/patient/invoices/" + id + "/setup-payment-plan";
        }
    }

    // ========== HELPER METHODS ==========

    private void validatePatientOwnership(Invoice invoice, Patient patient) {
        if (!invoice.getPatient().getId().equals(patient.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied to this invoice");
        }
    }

    private List<String> getAvailablePaymentMethods() {
        return List.of("CREDIT_CARD", "DEBIT_CARD", "BANK_TRANSFER", "PAYPAL", "APPLE_PAY", "GOOGLE_PAY");
    }

    private Object getBankAccountDetails() {
        return new Object();
    }

    private String generateReceiptNumber(Payment payment) {
        return "RCP-" + payment.getId() + "-" + payment.getTransactionId().substring(Math.max(0, payment.getTransactionId().length() - 5));
    }


}