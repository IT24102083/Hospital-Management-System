package com.hospital.hospitalmanagementsystem.controller;

import com.hospital.hospitalmanagementsystem.model.Invoice;
import com.hospital.hospitalmanagementsystem.model.Payment;
import com.hospital.hospitalmanagementsystem.service.InvoiceService;
import com.hospital.hospitalmanagementsystem.service.PaymentService;
import com.hospital.hospitalmanagementsystem.service.PaymentPlanService;
import com.hospital.hospitalmanagementsystem.service.PatientService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
public class AccountantDashboardController {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentPlanService paymentPlanService;

    @Autowired
    private PatientService patientService;

    // ========== POS TERMINAL ENDPOINTS ==========

    @GetMapping("/accountant/pos")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public String showPOSTerminal(Model model) {
        try {
            // UPDATED: Removed the argument from the method call to match the service.
            List<Payment> recentTransactions = paymentService.getRecentPOSTransactions();
            List<com.hospital.hospitalmanagementsystem.model.Patient> patients = patientService.getAllPatients();

            model.addAttribute("recentTransactions", recentTransactions);
            model.addAttribute("patients", patients);

            return "home/accountant/pos-terminal";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load POS terminal: " + e.getMessage());
            return "home/error";
        }
    }

    @PostMapping("/accountant/pos/process-payment")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public String processPOSPayment(@RequestParam String customerType,
                                    @RequestParam(required = false) String patientSearch,
                                    @RequestParam(required = false) String customerName,
                                    @RequestParam(required = false) String customerPhone,
                                    @RequestParam String paymentType,
                                    @RequestParam(required = false) String invoiceNumber,
                                    @RequestParam String paymentDescription,
                                    @RequestParam BigDecimal amount,
                                    @RequestParam String paymentMethod,
                                    RedirectAttributes redirectAttributes) {
        try {
            Payment payment = paymentService.processPOSPayment(
                    customerType, patientSearch, customerName, customerPhone,
                    paymentType, invoiceNumber, paymentDescription, amount, paymentMethod);

            redirectAttributes.addFlashAttribute("success",
                    "Payment of $" + amount + " processed successfully! Transaction ID: " + payment.getTransactionId());

            return "redirect:/accountant/pos";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Payment processing failed: " + e.getMessage());
            // FIX: Corrected the redirect path to match the GET mapping
            return "redirect:/accountant/pos";
        }
    }

    // ========== BANK SLIP VERIFICATION ENDPOINTS ==========

    @GetMapping("/accountant/bank-slips")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public String showBankSlipsForVerification(Model model, Pageable pageable) {
        try {
            Page<Payment> pendingBankSlips = paymentService.getPendingBankSlipsPaginated(pageable);
            model.addAttribute("pendingBankSlips", pendingBankSlips);
            return "home/accountant/bank-slip-verification";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load bank slips: " + e.getMessage());
            return "home/error";
        }
    }

    @GetMapping("/accountant/bank-slips/{id}")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public String showBankSlipDetails(@PathVariable Long id, Model model) {
        try {
            Payment payment = paymentService.getPaymentById(id);
            Invoice invoice = payment.getInvoice();

            if (payment.getPaymentMethod() != Payment.PaymentMethod.BANK_TRANSFER) {
                throw new IllegalArgumentException("This payment is not a bank transfer");
            }

            model.addAttribute("payment", payment);
            model.addAttribute("invoice", invoice);

            // FIX: This view should probably be for details, not the main list
            return "home/accountant/bank-slip-details";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load bank slip details: " + e.getMessage());
            return "home/error";
        }
    }

    @PostMapping("/accountant/bank-slips/{id}/verify")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public String processBankSlipVerification(@PathVariable Long id,
                                              @RequestParam String verificationAction,
                                              @RequestParam(required = false) BigDecimal verifiedAmount,
                                              @RequestParam(required = false) String verificationNotes,
                                              RedirectAttributes redirectAttributes) {
        try {
            switch (verificationAction) {
                case "approve":
                    paymentService.approveBankSlipPayment(id, verificationNotes);
                    redirectAttributes.addFlashAttribute("success",
                            "Bank slip approved successfully! Payment has been processed.");
                    break;

                case "reject":
                    paymentService.rejectBankSlipPayment(id, verificationNotes);
                    redirectAttributes.addFlashAttribute("success",
                            "Bank slip rejected. Patient will be notified.");
                    break;

                case "partial":
                    if (verifiedAmount == null || verifiedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                        redirectAttributes.addFlashAttribute("error",
                                "Please enter a valid verified amount for partial payment.");
                        return "redirect:/accountant/bank-slips/" + id;
                    }

                    paymentService.approvePartialBankSlipPayment(id, verifiedAmount, verificationNotes);
                    redirectAttributes.addFlashAttribute("success",
                            "Partial payment of $" + verifiedAmount + " approved successfully.");
                    break;

                default:
                    redirectAttributes.addFlashAttribute("error", "Invalid verification action selected.");
                    return "redirect:/accountant/bank-slips/" + id;
            }

            return "redirect:/accountant/bank-slips";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error processing verification: " + e.getMessage());
            return "redirect:/accountant/bank-slips/" + id;
        }
    }

    @GetMapping("/accountant/bank-slips/{id}/download")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public ResponseEntity<byte[]> downloadBankSlip(@PathVariable Long id) {
        try {
            return paymentService.downloadBankSlipFile(id);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ========== AJAX ENDPOINTS ==========

    @GetMapping("/api/dashboard/stats")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalRevenue", paymentService.getTotalRevenue());
            stats.put("outstandingAmount", invoiceService.getTotalOutstandingBalance());
            stats.put("activePaymentPlans", paymentPlanService.getActivePaymentPlansCount());
            stats.put("pendingVerifications", paymentService.getPendingBankSlipVerifications().size());
            stats.put("totalInvoices", invoiceService.getAllInvoices().size());
            stats.put("overdueInvoices", invoiceService.getOverdueInvoices().size());

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/accountant/pos/clear-transaction")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public String clearPOSTransaction(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("info", "Transaction cleared.");
        return "redirect:/accountant/pos";
    }

    @PostMapping("/accountant/bank-slips/send-bulk-reminders")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public String sendBulkReminders(RedirectAttributes redirectAttributes) {
        try {
            int remindersSent = paymentService.sendBankSlipReminders();
            redirectAttributes.addFlashAttribute("success",
                    "Reminders sent to " + remindersSent + " patients with pending bank slips.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Failed to send reminders: " + e.getMessage());
        }

        return "redirect:/accountant/bank-slips";
    }

    @PostMapping("/accountant/bank-slips/{id}/contact-patient")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public String contactPatient(@PathVariable Long id,
                                 @RequestParam String contactReason,
                                 @RequestParam String message,
                                 RedirectAttributes redirectAttributes) {
        try {
            paymentService.contactPatientRegardingBankSlip(id, contactReason, message);
            redirectAttributes.addFlashAttribute("success", "Patient contacted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to contact patient: " + e.getMessage());
        }

        return "redirect:/accountant/bank-slips/" + id;
    }
}