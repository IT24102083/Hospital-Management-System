package com.hospital.hospitalmanagementsystem.controller;

import com.hospital.hospitalmanagementsystem.model.*;
import com.hospital.hospitalmanagementsystem.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort; // <-- ADDED IMPORT
import org.springframework.data.web.PageableDefault; // <-- ADDED IMPORT
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/accountant")
public class AccountantDashboardController {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentPlanService paymentPlanService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private DoctorService doctorService;

    // ========== BANK SLIP VERIFICATION ENDPOINTS ==========

    @GetMapping("/bank-slips")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public String showBankSlipsForVerification(Model model, Pageable pageable) {
        try {
            Page<Payment> pendingBankSlips = paymentService.getPendingBankSlipsPaginated(pageable);

            model.addAttribute("pendingBankSlips", pendingBankSlips);
            model.addAttribute("totalPending", pendingBankSlips.getTotalElements());

            return "home/accountant/bank-slip-list";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load bank slips: " + e.getMessage());
            return "home/error";
        }
    }

    @GetMapping("/bank-slips/{id}")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public String showBankSlipDetails(@PathVariable Long id, Model model) {
        try {
            Payment payment = paymentService.getPaymentById(id)
                    .orElseThrow(() -> new RuntimeException("Payment not found with ID: " + id));
            Invoice invoice = payment.getInvoice();

            if (payment.getPaymentMethod() != Payment.PaymentMethod.BANK_TRANSFER) {
                throw new IllegalArgumentException("This payment is not a bank transfer");
            }

            model.addAttribute("payment", payment);
            model.addAttribute("invoice", invoice);

            return "home/accountant/bank-slip-verification";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load bank slip details: " + e.getMessage());
            return "home/error";
        }
    }

    // Add this NEW endpoint to your AccountantDashboardController
    @GetMapping("/bank-slips/{id}/view")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public ResponseEntity<byte[]> viewBankSlip(@PathVariable Long id, HttpServletResponse response) {
        try {
            ResponseEntity<byte[]> result = paymentService.viewBankSlipFile(id);

            // Add headers to allow iframe embedding
            response.setHeader("X-Frame-Options", "SAMEORIGIN");
            response.setHeader("Content-Security-Policy", "frame-ancestors 'self'");

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    // Keep your existing download endpoint unchanged for the download button
    @GetMapping("/bank-slips/{id}/download")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public ResponseEntity<byte[]> downloadBankSlip(@PathVariable Long id) {
        try {
            return paymentService.downloadBankSlipFile(id);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/bank-slips/{id}/verify")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public String processBankSlipVerification(@PathVariable Long id,
                                              @RequestParam String verificationAction,
                                              @RequestParam(required = false) BigDecimal verifiedAmount,
                                              @RequestParam(required = false) String verificationNotes,
                                              RedirectAttributes redirectAttributes) {
        try {
            switch (verificationAction) {
                case "approve":
                    // FIXED: Approve and update database
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
                                "Please enter a valid verified amount.");
                        return "redirect:/accountant/bank-slips/" + id;
                    }
                    paymentService.approvePartialBankSlipPayment(id, verifiedAmount, verificationNotes);
                    redirectAttributes.addFlashAttribute("success",
                            "Partial payment of $" + verifiedAmount + " approved successfully.");
                    break;
            }

            return "redirect:/accountant/bank-slips";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error processing verification: " + e.getMessage());
            return "redirect:/accountant/bank-slips/" + id;
        }
    }

    // ========== INVOICE MANAGEMENT ==========

    @GetMapping("/invoices/create")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public String showCreateInvoiceForm(Model model) {
        try {
            List<Patient> patients = patientService.getAllPatients();
            List<Doctor> doctors = doctorService.getAllDoctors();

            model.addAttribute("patients", patients);
            model.addAttribute("doctors", doctors);
            model.addAttribute("invoice", new Invoice());

            return "home/accountant/create-invoice";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load invoice creation form: " + e.getMessage());
            return "home/error";
        }
    }

    @PostMapping("/invoices/create")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createInvoice(@RequestBody Map<String, Object> invoiceData) {
        try {
            // Extract patient
            Long patientId = Long.valueOf(invoiceData.get("patientId").toString());
            Patient patient = patientService.getPatientById(patientId)
                    .orElseThrow(() -> new RuntimeException("Patient not found with ID: " + patientId));

            // Create invoice
            Invoice invoice = new Invoice();
            invoice.setPatient(patient);
            invoice.setInvoiceNumber(invoiceData.get("invoiceNumber").toString());
            invoice.setIssueDate(LocalDate.parse(invoiceData.get("issueDate").toString()));
            invoice.setDueDate(LocalDate.parse(invoiceData.get("dueDate").toString()));
            invoice.setDescription(invoiceData.get("description").toString());

            // Set amounts
            invoice.setSubtotal(new BigDecimal(invoiceData.get("subtotal").toString()));
            invoice.setTax(new BigDecimal(invoiceData.get("tax").toString()));
            invoice.setDiscount(new BigDecimal(invoiceData.get("discount").toString()));
            invoice.setTotal(new BigDecimal(invoiceData.get("total").toString()));
            invoice.setAmountPaid(BigDecimal.ZERO);
            invoice.setBalanceDue(new BigDecimal(invoiceData.get("total").toString()));
            invoice.setStatus(Invoice.InvoiceStatus.PENDING);

            // Save invoice first to get ID
            Invoice savedInvoice = invoiceService.saveInvoice(invoice);

            // Add invoice items
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) invoiceData.get("items");

            for (Map<String, Object> itemData : items) {
                InvoiceItem item = new InvoiceItem();
                item.setInvoice(savedInvoice);
                item.setDescription(itemData.get("description").toString());
                item.setQuantity(Integer.valueOf(itemData.get("quantity").toString()));
                item.setUnitPrice(new BigDecimal(itemData.get("unitPrice").toString()));
                item.setLineTotal(new BigDecimal(itemData.get("lineTotal").toString()));

                if (itemData.containsKey("itemNotes") && itemData.get("itemNotes") != null) {
                    item.setItemNotes(itemData.get("itemNotes").toString());
                }

                savedInvoice.getItems().add(item);
            }

            // Save with items
            savedInvoice = invoiceService.saveInvoice(savedInvoice);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("invoiceId", savedInvoice.getId());
            response.put("message", "Invoice created successfully!");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error creating invoice: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public String listInvoices(Model model,
                               @PageableDefault(size = 10, sort = "issueDate", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            Page<Invoice> invoices = invoiceService.getAllInvoicesPaginated(pageable);

            model.addAttribute("invoices", invoices);
            model.addAttribute("totalInvoices", invoices.getTotalElements());
            model.addAttribute("overdueInvoices", invoiceService.getOverdueInvoices().size());

            return "home/accountant/invoices-list";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load invoices: " + e.getMessage());
            return "home/error";
        }
    }

    @GetMapping("/invoices/{id}")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public String viewInvoiceDetails(@PathVariable Long id, Model model) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(id)
                    .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + id));

            List<Payment> payments = paymentService.getPaymentsByInvoiceId(id);

            model.addAttribute("invoice", invoice);
            model.addAttribute("payments", payments);

            return "home/billing/invoice-details";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load invoice details: " + e.getMessage());
            return "home/error";
        }
    }

    @GetMapping("/invoices/{id}/edit")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public String editInvoiceForm(@PathVariable Long id, Model model) {
        Invoice invoice = invoiceService.getInvoiceById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + id));
        model.addAttribute("invoice", invoice);
        return "home/accountant/edit-invoice";
    }

    @PostMapping("/invoices/{id}/edit")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public String updateInvoice(@PathVariable Long id,
                                @Valid @ModelAttribute("invoice") Invoice invoice,
                                BindingResult result,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (result.hasErrors()) {
            model.addAttribute("invoice", invoice);
            return "home/accountant/edit-invoice";
        }
        invoiceService.saveInvoice(invoice);
        redirectAttributes.addFlashAttribute("success", "Invoice updated successfully!");
        return "redirect:/accountant/invoices/" + id;
    }

    // ========== PAYMENT PLANS MANAGEMENT ==========

    @GetMapping("/payment-plans")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public String showPaymentPlans(Model model, Pageable pageable) {
        try {
            Page<PaymentPlan> paymentPlans = paymentPlanService.getAllPaymentPlansPaginated(pageable);
            int activeCount = paymentPlanService.getActivePaymentPlansCount();
            BigDecimal monthlyCollections = paymentPlanService.calculateMonthlyCollections();
            // FIXED: Changed method name to match service implementation
            int overdueCount = paymentPlanService.getOverdueInstallmentsCount();
            BigDecimal totalOutstanding = paymentPlanService.getTotalOutstandingAmount();

            model.addAttribute("paymentPlans", paymentPlans);
            model.addAttribute("activePlansCount", activeCount);
            model.addAttribute("monthlyCollections", monthlyCollections);
            model.addAttribute("overduePayments", overdueCount);
            model.addAttribute("totalOutstanding", totalOutstanding);

            return "home/accountant/payment-plan-management";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load payment plans: " + e.getMessage());
            return "home/error";
        }
    }

    @GetMapping("/payment-plans/{id}")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public String viewPaymentPlanDetails(@PathVariable Long id, Model model) {
        try {
            PaymentPlan paymentPlan = paymentPlanService.getPaymentPlanById(id)
                    .orElseThrow(() -> new RuntimeException("Payment plan not found with ID: " + id));

            model.addAttribute("paymentPlan", paymentPlan);
            model.addAttribute("paymentHistory", paymentPlanService.getPaymentHistory(id));

            return "home/accountant/payment-plan-management";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load payment plan details: " + e.getMessage());
            return "home/error";
        }
    }

    @PostMapping("/payment-plans/{id}/cancel")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public String cancelPaymentPlan(@PathVariable Long id,
                                    @RequestParam String cancellationReason,
                                    RedirectAttributes redirectAttributes) {
        try {
            // FIXED: Changed method to use the flexible status update method
            paymentPlanService.updatePaymentPlanStatus(id, "cancel", cancellationReason);
            redirectAttributes.addFlashAttribute("success", "Payment plan cancelled successfully.");
            return "redirect:/accountant/payment-plans";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error cancelling payment plan: " + e.getMessage());
            return "redirect:/accountant/payment-plans/" + id;
        }
    }

    @PostMapping("/payment-plans/{id}/adjust")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public String adjustPaymentPlan(@PathVariable Long id,
                                    @RequestParam Integer newDuration,
                                    @RequestParam BigDecimal newMonthlyAmount,
                                    @RequestParam String adjustmentReason,
                                    RedirectAttributes redirectAttributes) {
        try {
            paymentPlanService.adjustPaymentPlan(id, newDuration, newMonthlyAmount, adjustmentReason);
            redirectAttributes.addFlashAttribute("success", "Payment plan adjusted successfully.");
            return "redirect:/accountant/payment-plans/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adjusting payment plan: " + e.getMessage());
            return "redirect:/accountant/payment-plans/" + id;
        }
    }

    // ========== FINANCIAL REPORTS ==========

    @GetMapping("/api/accountant/reports/financial")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getFinancialReport(
            @RequestParam String reportType,
            @RequestParam String range,
            @RequestParam String start,
            @RequestParam String end) {

        try {
            Map<String, Object> response = new HashMap<>();

            // Parse dates
            LocalDate startDate = LocalDate.parse(start);
            LocalDate endDate = LocalDate.parse(end);

            // Summary data
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalRevenue", paymentService.getTotalRevenue());
            summary.put("outstandingAmount", invoiceService.getTotalOutstandingBalance());
            summary.put("totalInvoices", invoiceService.getPaidInvoicesCount());
            summary.put("activePaymentPlans", paymentPlanService.getActivePaymentPlansCount());

            // Table data - generate based on report type
            List<Map<String, Object>> tableData = generateTableData(reportType, startDate, endDate);

            // Chart data
            Map<String, Object> chartData = generateChartData(reportType, startDate, endDate);

            response.put("summary", summary);
            response.put("tableData", tableData);
            response.put("chartData", chartData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(new HashMap<>());
        }
    }

    private List<Map<String, Object>> generateTableData(String reportType, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> tableData = new ArrayList<>();

        // Get all payments and invoices in the date range
        List<Payment> payments = paymentService.getPaymentsByDateRange(startDate, endDate);
        List<Invoice> invoices = invoiceService.getInvoicesByDateRange(startDate, endDate);

        // Group by month
        Map<String, List<Payment>> paymentsByMonth = payments.stream()
                .collect(Collectors.groupingBy(p -> p.getPaymentDate().format(DateTimeFormatter.ofPattern("MMMM yyyy"))));

        Map<String, List<Invoice>> invoicesByMonth = invoices.stream()
                .collect(Collectors.groupingBy(i -> i.getIssueDate().format(DateTimeFormatter.ofPattern("MMMM yyyy"))));

        // Combine all months
        Set<String> allMonths = new TreeSet<>();
        allMonths.addAll(paymentsByMonth.keySet());
        allMonths.addAll(invoicesByMonth.keySet());

        for (String month : allMonths) {
            Map<String, Object> row = new HashMap<>();

            List<Payment> monthPayments = paymentsByMonth.getOrDefault(month, new ArrayList<>());
            List<Invoice> monthInvoices = invoicesByMonth.getOrDefault(month, new ArrayList<>());

            BigDecimal revenue = monthInvoices.stream()
                    .map(Invoice::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal paymentsReceived = monthPayments.stream()
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal outstanding = monthInvoices.stream()
                    .map(Invoice::getBalanceDue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);



            BigDecimal collectionRate = revenue.compareTo(BigDecimal.ZERO) > 0
                    ? paymentsReceived.divide(revenue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100))
                    : BigDecimal.ZERO;

            row.put("period", month);
            row.put("revenue", revenue);
            row.put("paymentsReceived", paymentsReceived);
            row.put("outstanding", outstanding);
            row.put("collectionRate", collectionRate.setScale(1, RoundingMode.HALF_UP));

            tableData.add(row);
        }

        return tableData;
    }

    private Map<String, Object> generateChartData(String reportType, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> chartData = new HashMap<>();

        // Revenue chart data
        List<Payment> payments = paymentService.getPaymentsByDateRange(startDate, endDate);
        Map<String, BigDecimal> revenueByMonth = payments.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getPaymentDate().format(DateTimeFormatter.ofPattern("MMM")),
                        Collectors.reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)
                ));

        Map<String, Object> revenueChart = new HashMap<>();
        revenueChart.put("labels", new ArrayList<>(revenueByMonth.keySet()));
        revenueChart.put("data", new ArrayList<>(revenueByMonth.values()));

        // Payment methods chart data
        Map<String, Long> paymentMethodCounts = payments.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getPaymentMethod().toString(),
                        Collectors.counting()
                ));

        Map<String, Object> paymentMethodsChart = new HashMap<>();
        paymentMethodsChart.put("labels", new ArrayList<>(paymentMethodCounts.keySet()));
        paymentMethodsChart.put("data", new ArrayList<>(paymentMethodCounts.values()));

        chartData.put("revenue", revenueChart);
        chartData.put("paymentMethods", paymentMethodsChart);

        return chartData;
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

    @GetMapping("/pos-terminal")
    public String showPOSTerminal() {
        return "home/accountant/pos-terminal";
    }
}