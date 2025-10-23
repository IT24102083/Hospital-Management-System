package com.hospital.hospitalmanagementsystem.controller;


import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.DocumentException;
import com.hospital.hospitalmanagementsystem.service.PaymentService; // Assuming you have a PaymentService
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.WebContext;
import org.xhtmlrenderer.pdf.ITextRenderer;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import com.hospital.hospitalmanagementsystem.model.*;
import com.hospital.hospitalmanagementsystem.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.validation.FieldError;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.time.LocalDate;

import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping("/patient")
public class PatientController {
    private static final Logger logger = LoggerFactory.getLogger(PatientController.class);

    private final PatientService patientService;
    private final UserService userService;
    private final AppointmentService appointmentService;
    private final InvoiceService invoiceService;
    private final PrescriptionService prescriptionService;
    private final TemplateEngine templateEngine;
    private final PaymentService paymentService;

    @Autowired
    private MedicalRecordService medicalRecordService;

    @Value("${app.upload.prescriptions:uploads/prescriptions}")
    private String uploadDir;

    public PatientController(PatientService patientService,
                             UserService userService,
                             AppointmentService appointmentService,
                             InvoiceService invoiceService,
                             PrescriptionService prescriptionService,
                             TemplateEngine templateEngine,
                             PaymentService paymentService) {
        this.patientService = patientService;
        this.userService = userService;
        this.appointmentService = appointmentService;
        this.invoiceService = invoiceService;
        this.prescriptionService = prescriptionService;
        this.templateEngine = templateEngine;
        this.paymentService = paymentService;
    }

    // New endpoint to show the prescription upload form
    @GetMapping("/prescriptions/submit")
    public String submitPrescriptionForm(Model model) {
        model.addAttribute("prescription", new Prescription());
        return "home/patient/submit-prescription";
    }

    // New endpoint to handle prescription submission
    @PostMapping("/prescriptions/submit")
    public String handlePrescriptionSubmission(@Valid @ModelAttribute("prescription") Prescription prescription,
                                               BindingResult result,
                                               @RequestParam("prescriptionFile") MultipartFile file,
                                               Principal principal,
                                               RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }

        if (file.isEmpty()) {
            result.rejectValue("filePath", "error.prescription", "A prescription file is required.");
        }

        if (result.hasErrors()) {
            return "home/patient/submit-prescription";
        }

        try {
            Optional<User> userOpt = userService.getUserByUsername(principal.getName());
            if (userOpt.isPresent()) {
                String filePath = saveUploadedFile(file);
                prescriptionService.createPrescription(prescription, userOpt.get(), filePath);
                redirectAttributes.addFlashAttribute("successMessage", "Prescription submitted successfully!");
                return "redirect:/patient/prescriptions";
            }
            return "redirect:/login";
        } catch (Exception e) {
            logger.error("Error submitting prescription", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error submitting prescription: " + e.getMessage());
            return "redirect:/patient/prescriptions/submit";
        }
    }

    @GetMapping("/prescriptions")
    public String viewMyPrescriptions(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        try {
            Optional<User> userOpt = userService.getUserByUsername(principal.getName());
            if (userOpt.isPresent()) {
                // FIX: Get the patient object
                Optional<Patient> patientOpt = patientService.getPatientById(userOpt.get().getId());
                if (patientOpt.isPresent()) {
                    Patient patient = patientOpt.get();
                    List<Prescription> prescriptions = prescriptionService.getPrescriptionsForPatient(userOpt.get());
                    model.addAttribute("patient", patient);
                    model.addAttribute("prescriptions", prescriptions);
                }
            }
            return "home/patient/view-prescriptions";
        } catch (Exception e) {
            logger.error("Error fetching patient prescriptions", e);
            model.addAttribute("errorMessage", "Could not load prescriptions.");
            return "home/error";
        }
    }

    private String saveUploadedFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return null;
        }
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String uniqueFilename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/prescriptions/" + uniqueFilename;
    }

    /**
     * Patient Dashboard endpoint - shows overview of patient information
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        try {
            // For development/testing - using hardcoded data if no principal
            if (principal == null) {
                logger.warn("No principal found, using mock data");
                // Add mock data for development
                mockDashboardData(model);
                return "home/patient/dashboard";
            }

            Optional<User> userOpt = userService.getUserByUsername(principal.getName());
            if (userOpt.isPresent() && userOpt.get().getRole() == User.Role.ROLE_PATIENT) {
                Optional<Patient> patientOpt = patientService.getPatientById(userOpt.get().getId());
                if (patientOpt.isPresent()) {
                    Patient patient = patientOpt.get();
                    User user = userOpt.get();

                    List<Appointment> upcomingAppointments = new ArrayList<>();
                    List<Appointment> pastAppointments = new ArrayList<>();
                    List<Invoice> recentInvoices = new ArrayList<>();

                    long totalInvoiceCount = 0;
                    long prescriptionCount = 0;

                    try {
                        // Get upcoming and recent appointments - catch exceptions individually
                        upcomingAppointments = appointmentService.getUpcomingAppointments(patient);
                    } catch (Exception e) {
                        logger.error("Error fetching upcoming appointments", e);
                    }

                    try {
                        pastAppointments = appointmentService.getRecentAppointments(patient);
                    } catch (Exception e) {
                        logger.error("Error fetching past appointments", e);
                    }

                    try {
                        // Get recent invoices
                        recentInvoices = invoiceService.getRecentInvoices(patient, 3);
                    } catch (Exception e) {
                        logger.error("Error fetching recent invoices", e);
                    }

                    try {
                        prescriptionCount = prescriptionService.getPatientPrescriptionCount(patient);
                    } catch (Exception e) {
                        logger.error("Error fetching prescription count for dashboard", e);
                    }

                    try {
                        totalInvoiceCount = invoiceService.getPatientInvoiceCount(patient);
                    } catch (Exception e) {
                        logger.error("Error fetching total invoice count", e);
                    }


                    // Add all data to model
                    model.addAttribute("patient", patient);
                    model.addAttribute("upcomingAppointments", upcomingAppointments);
                    model.addAttribute("pastAppointments", pastAppointments);
                    model.addAttribute("recentInvoices", recentInvoices);
                    model.addAttribute("prescriptionCount", prescriptionCount);
                    model.addAttribute("totalInvoiceCount", totalInvoiceCount);
                }
            }

            // Add current date and login info for dashboard display
            model.addAttribute("currentDate", LocalDate.now());
            model.addAttribute("currentUser", "IT24102083");
            model.addAttribute("currentDateTime", "2025-08-11 05:39:58");

            return "home/patient/dashboard";

        } catch (Exception e) {
            logger.error("Error in patient dashboard", e);
            model.addAttribute("errorMessage", "An error occurred while loading the dashboard. Please try again later.");
            return "home/error";
        }
    }

    // Mock data for development/testing
    private void mockDashboardData(Model model) {
        model.addAttribute("patient", new Patient());
        model.addAttribute("upcomingAppointments", new ArrayList<Appointment>());
        model.addAttribute("pastAppointments", new ArrayList<Appointment>());
        model.addAttribute("recentInvoices", new ArrayList<Invoice>());
        model.addAttribute("currentDate", LocalDate.now());
        model.addAttribute("currentUser", "IT24102083");
        model.addAttribute("currentDateTime", "2025-08-11 05:39:58");
    }

    /**
     * View patient profile
     */
    @GetMapping("/profile")
    public String viewProfile(Model model, Principal principal) {
        try {
            if (principal == null) {
                return "redirect:/login";
            }

            Optional<User> userOpt = userService.getUserByUsername(principal.getName());
            if (userOpt.isPresent() && userOpt.get().getRole() == User.Role.ROLE_PATIENT) {
                Optional<Patient> patientOpt = patientService.getPatientById(userOpt.get().getId());
                if (patientOpt.isPresent()) {
                    model.addAttribute("patient", patientOpt.get());
                }
            }

            // Add current date and login info
            model.addAttribute("currentUser", "IT24102083");
            model.addAttribute("currentDateTime", "2025-08-11 05:39:58");

            return "home/patient/profile";

        } catch (Exception e) {
            logger.error("Error viewing patient profile", e);
            model.addAttribute("errorMessage", "An error occurred while loading your profile. Please try again later.");
            return "home/error";
        }
    }

    /**
     * Handle patient profile update submission
     */
    @PostMapping("/profile/edit")
    public String updateProfile(@Valid @ModelAttribute("patient") Patient patientDetails,
                                BindingResult result,
                                @RequestParam(value = "newPassword", required = false) String newPassword,
                                @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
                                Principal principal, Model model, RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        // Password validation logic
        if (newPassword != null && !newPassword.isEmpty()) {
            if (!newPassword.equals(confirmPassword)) {
                result.addError(new FieldError("patient", "password", "Passwords do not match."));
            } else {
                // Set the password on the details object so it can be updated
                patientDetails.setPassword(newPassword);
            }
        } else {
            // Ensure password is null if empty, so it's not updated
            patientDetails.setPassword(null);
        }

        if (result.hasErrors()) {
            // If there are validation errors, return to the profile page to display them
            model.addAttribute("currentUser", "IT24102083");
            model.addAttribute("currentDateTime", "2025-08-11 05:39:58");
            return "home/patient/profile"; // Return to the same page
        }

        try {
            Optional<User> currentUserOpt = userService.getUserByUsername(principal.getName());
            if (currentUserOpt.isPresent()) {
                // Use the robust updateUser service method
                userService.updateUser(currentUserOpt.get().getId(), patientDetails);
                redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
                return "redirect:/patient/profile";
            }
            return "redirect:/login";

        } catch (Exception e) {
            logger.error("Error updating patient profile", e);
            model.addAttribute("errorMessage", "An error occurred while updating your profile.");
            // Pass the submitted data back to the form to avoid data loss
            model.addAttribute("patient", patientDetails);
            return "home/patient/profile";
        }
    }

    /**
     * View patient appointments
     */
    @GetMapping("/appointments")
    public String viewAppointments(Model model, Principal principal) {
        try {
            if (principal == null) {
                return "redirect:/login";
            }

            Optional<User> userOpt = userService.getUserByUsername(principal.getName());
            if (userOpt.isPresent() && userOpt.get().getRole() == User.Role.ROLE_PATIENT) {
                Optional<Patient> patientOpt = patientService.getPatientById(userOpt.get().getId());
                if (patientOpt.isPresent()) {
                    Patient patient = patientOpt.get();
                    List<Appointment> appointments = appointmentService.getPatientAppointments(patient);

                    // FIX: This line adds the patient's data to the page
                    model.addAttribute("patient", patient);

                    model.addAttribute("appointments", appointments);
                }
            }

            // Add current date and login info
            model.addAttribute("currentUser", "IT24102083");
            model.addAttribute("currentDateTime", "2025-08-11 05:39:58");

            return "home/patient/view-appointments";

        } catch (Exception e) {
            logger.error("Error viewing patient appointments", e);
            model.addAttribute("errorMessage", "An error occurred while loading your appointments. Please try again later.");
            return "home/error";
        }
    }

//    /**
//     * View patient invoices
//     */
//    @GetMapping("/invoices")
//    public String viewInvoices(Model model, Principal principal) {
//        try {
//            if (principal == null) {
//                return "redirect:/login";
//            }
//            Optional<User> userOpt = userService.getUserByUsername(principal.getName());
//            if (userOpt.isPresent() && userOpt.get().getRole() == User.Role.ROLE_PATIENT) {
//                Optional<Patient> patientOpt = patientService.getPatientById(userOpt.get().getId());
//                if (patientOpt.isPresent()) {
//                    Patient patient = patientOpt.get(); // FIX: Get the patient object
//                    List<Invoice> invoices = invoiceService.getPatientInvoices(patient);
//
//                    model.addAttribute("patient", patient);
//                    model.addAttribute("invoices", invoices);
//                }
//            }
//            model.addAttribute("currentUser", "IT24102083");
//            model.addAttribute("currentDateTime", "2025-08-11 05:39:58");
//
//            return "home/patient/invoices";
//
//        } catch (Exception e) {
//            logger.error("Error viewing patient invoices", e);
//            model.addAttribute("errorMessage", "An error occurred while loading your invoices. Please try again later.");
//            return "home/error";
//        }
//    }
//
//    /**
//     * View individual invoice details
//     */
//    @GetMapping("/invoices/{id}")
//    public String viewInvoiceDetails(@PathVariable Long id, Model model, Principal principal) {
//        try {
//            if (principal == null) {
//                return "redirect:/login";
//            }
//
//            Optional<User> userOpt = userService.getUserByUsername(principal.getName());
//            if (userOpt.isPresent() && userOpt.get().getRole() == User.Role.ROLE_PATIENT) {
//                Optional<Invoice> invoiceOpt = invoiceService.getInvoiceById(id);
//                if (invoiceOpt.isPresent() && invoiceOpt.get().getPatient().getId().equals(userOpt.get().getId())) {
//                    model.addAttribute("invoice", invoiceOpt.get());
//
//                    // Add current date and login info
//                    model.addAttribute("currentUser", "IT24102083");
//                    model.addAttribute("currentDateTime", "2025-08-11 05:39:58");
//
//                    return "home/patient/invoice-details";
//                }
//            }
//
//            return "redirect:/patient/invoices";
//
//        } catch (Exception e) {
//            logger.error("Error viewing invoice details", e);
//            model.addAttribute("errorMessage", "An error occurred while loading the invoice details. Please try again later.");
//            return "home/error";
//        }
//    }

    /**
     * MODIFIED: View patient payment history with pagination and summary cards.
     * This now serves the patient-payment-history.html page.
     */
    @GetMapping("/invoices")
    public String viewPaymentHistory(Model model,
                                     Principal principal,
                                     @PageableDefault(size = 10, sort = "issueDate") Pageable pageable) {
        try {
            if (principal == null) {
                return "redirect:/login";
            }
            Optional<User> userOpt = userService.getUserByUsername(principal.getName());
            if (userOpt.isPresent() && userOpt.get().getRole() == User.Role.ROLE_PATIENT) {
                User user = userOpt.get();
                // Assuming Patient ID is the same as User ID, which is the pattern in this controller
                Optional<Patient> patientOpt = patientService.getPatientById(user.getId());
                if (patientOpt.isPresent()) {
                    Patient patient = patientOpt.get();

                    // 1. Fetch paginated invoices using the updated service method
                    Page<Invoice> invoicesPage = invoiceService.getPatientInvoicesPaginated(patient, pageable);

                    // 2. Get summary data for the UI cards
                    Map<String, Object> summaryData = invoiceService.getPatientInvoiceSummary(patient);

                    // 3. Add all required data to the model
                    model.addAttribute("user", user); // For the navbar welcome message
                    model.addAttribute("invoices", invoicesPage); // The paginated list for the table
                    model.addAllAttributes(summaryData); // Adds totalPaidAmount, outstandingAmount, etc.

                    // 4. Return the new view name
                    return "home/patient/patient-payment-history";
                }
            }
            // If user or patient is not found, redirect
            return "redirect:/patient/dashboard";

        } catch (Exception e) {
            logger.error("Error viewing patient payment history", e);
            model.addAttribute("errorMessage", "An error occurred while loading your payment history.");
            return "home/error";
        }
    }

    /**
     * MODIFIED: View individual invoice details, pointing to the new template path.
     */
    @GetMapping("/invoices/{id}")
    public String viewInvoiceDetails(@PathVariable Long id, Model model, Principal principal) {
        try {
            if (principal == null) {
                return "redirect:/login";
            }

            Optional<User> userOpt = userService.getUserByUsername(principal.getName());
            if (userOpt.isPresent() && userOpt.get().getRole() == User.Role.ROLE_PATIENT) {
                Optional<Invoice> invoiceOpt = invoiceService.getInvoiceById(id);
                // Security check: ensure the invoice belongs to the logged-in patient
                if (invoiceOpt.isPresent() && invoiceOpt.get().getPatient().getId().equals(userOpt.get().getId())) {
                    model.addAttribute("invoice", invoiceOpt.get());

                    // Add current date and login info (optional, can be removed if not needed in the new view)
                    model.addAttribute("currentUser", "IT24102083");
                    model.addAttribute("currentDateTime", "2025-08-11 05:39:58");

                    // Point to the new details view file path
                    return "home/patient/invoice-details";
                }
            }
            // If invoice not found or doesn't belong to the user, redirect
            return "redirect:/patient/invoices";

        } catch (Exception e) {
            logger.error("Error viewing invoice details", e);
            model.addAttribute("errorMessage", "An error occurred while loading the invoice details.");
            return "home/error";
        }
    }


    /**
     * View appointment details
     */
    @GetMapping("/appointment/{id}")
    public String viewAppointmentDetails(@PathVariable Long id, Model model, Principal principal) {
        try {
            if (principal == null) {
                return "redirect:/login";
            }

            Optional<User> userOpt = userService.getUserByUsername(principal.getName());
            if (userOpt.isPresent() && userOpt.get().getRole() == User.Role.ROLE_PATIENT) {
                Optional<Appointment> appointmentOpt = appointmentService.getAppointmentById(id);
                if (appointmentOpt.isPresent() && appointmentOpt.get().getPatient().getId().equals(userOpt.get().getId())) {
                    model.addAttribute("appointment", appointmentOpt.get());

                    // Add current date and login info
                    model.addAttribute("currentUser", "IT24102083");
                    model.addAttribute("currentDateTime", "2025-08-11 05:39:58");

                    return "patient/appointment-details";
                }
            }

            return "redirect:/patient/appointments";

        } catch (Exception e) {
            logger.error("Error viewing appointment details", e);
            model.addAttribute("errorMessage", "An error occurred while loading the appointment details. Please try again later.");
            return "home/error";
        }
    }

    /**
     * Cancel an appointment
     */
    @PostMapping("/appointment/{id}/cancel")
    public String cancelAppointment(@PathVariable Long id, Principal principal, Model model) {
        try {
            if (principal == null) {
                return "redirect:/login";
            }

            Optional<User> userOpt = userService.getUserByUsername(principal.getName());
            if (userOpt.isPresent() && userOpt.get().getRole() == User.Role.ROLE_PATIENT) {
                Optional<Appointment> appointmentOpt = appointmentService.getAppointmentById(id);
                if (appointmentOpt.isPresent() && appointmentOpt.get().getPatient().getId().equals(userOpt.get().getId())) {
                    appointmentService.cancelAppointment(id);
                    return "redirect:/patient/appointments?cancelled";
                }
            }

            return "redirect:/patient/appointments";

        } catch (Exception e) {
            logger.error("Error cancelling appointment", e);
            model.addAttribute("errorMessage", "An error occurred while cancelling the appointment. Please try again later.");
            return "home/error";
        }
    }

    /**
     * View patient medical records
     */
    @GetMapping("/records")
    public String viewMyMedicalRecords(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        try {
            // Get the User object from the principal's username
            Optional<User> userOpt = userService.getUserByUsername(principal.getName());
            if (userOpt.isPresent()) {
                // Get the Patient object using the user's ID
                Optional<Patient> patientOpt = patientService.getPatientById(userOpt.get().getId());
                if (patientOpt.isPresent()) {
                    Patient patient = patientOpt.get();
                    // Find medical records for this patient using the service
                    List<MedicalRecord> records = medicalRecordService.findByPatient(patient);

                    model.addAttribute("patient", patient);
                    model.addAttribute("medicalRecords", records);
                    // Return the path to the new HTML view we will create next
                    return "home/patient/view-records";
                }
            }
            // If user or patient is not found, redirect to the dashboard
            return "redirect:/dashboard";
        } catch (Exception e) {
            // You should add logging here
            // logger.error("Error fetching medical records for patient", e);
            model.addAttribute("errorMessage", "Could not load medical records due to an error.");
            return "home/error";
        }
    }

    /**
     * Finds the latest completed payment for an invoice, generates a QR code with its details,
     * and displays the receipt as a web page. This is called when the user clicks "Receipt".
     */
    @GetMapping("/invoices/{invoiceId}/receipt")
    public String showReceiptPage(@PathVariable Long invoiceId, Model model, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }

        try {
            // Find the most recent completed payment for this invoice
            Payment payment = paymentService.getPaymentsByInvoiceId(invoiceId).stream()
                    .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No completed payment found for invoice ID: " + invoiceId));

            Invoice invoice = invoiceService.getInvoiceById(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + invoiceId));

            // Security check
            if (!invoice.getPatient().getId().equals(payment.getPatient().getId())) {
                throw new SecurityException("Access Denied.");
            }

            model.addAttribute("invoice", invoice);
            model.addAttribute("payment", payment);
            model.addAttribute("patient", invoice.getPatient());

            // --- QR Code Generation ---
            try {
                String qrCodeData = "Receipt for Invoice: " + invoice.getInvoiceNumber() + "\n" +
                        "Patient: " + invoice.getPatient().getFirstName() + " " + invoice.getPatient().getLastName() + "\n" +
                        "Amount Paid: $" + payment.getAmount() + "\n" +
                        "Transaction ID: " + payment.getTransactionId() + "\n" +
                        "Date: " + payment.getPaymentDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

                QRCodeWriter qrCodeWriter = new QRCodeWriter();
                BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeData, BarcodeFormat.QR_CODE, 200, 200);

                ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
                byte[] pngData = pngOutputStream.toByteArray();

                String qrCodeBase64 = Base64.getEncoder().encodeToString(pngData);
                model.addAttribute("qrCodeImage", qrCodeBase64);
            } catch (Exception e) {
                logger.error("Error generating QR code for receipt", e);
                model.addAttribute("qrCodeImage", null); // Ensure it's null on failure
            }

            return "home/billing/receipt-generator";

        } catch (Exception e) {
            logger.error("Could not find or display a receipt for invoice {}: {}", invoiceId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "No completed payment receipt was found for that invoice.");
            return "redirect:/patient/invoices";
        }
    }

    /**
     * This method handles the PDF download request, called by the "Download PDF" button on the receipt page.
     * It uses the XHTML-compliant 'receipt-generator.html' template to create the PDF.
     */
    @GetMapping("/invoices/{invoiceId}/receipt-pdf/{paymentId}")
    public ResponseEntity<byte[]> downloadReceiptPdf(@PathVariable Long invoiceId,
                                                     @PathVariable Long paymentId,
                                                     HttpServletRequest request,
                                                     HttpServletResponse response) {
        Invoice invoice = invoiceService.getInvoiceById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + invoiceId));

        Payment payment = paymentService.getPaymentById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));

        // 1. Prepare the data for the template
        Map<String, Object> data = new HashMap<>();
        data.put("invoice", invoice);
        data.put("payment", payment);
        data.put("patient", invoice.getPatient());

        // --- Generate QR Code ---
        try {
            String qrCodeData = "Receipt for Invoice: " + invoice.getInvoiceNumber() + "\n" + "Amount Paid: $" + payment.getAmount();
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeData, BarcodeFormat.QR_CODE, 200, 200);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            data.put("qrCodeImage", Base64.getEncoder().encodeToString(pngOutputStream.toByteArray()));
        } catch (Exception e) {
            data.put("qrCodeImage", null);
        }

        // 2. Load the CSS content from the local file
        String cssContent = "";
        try {
            ClassPathResource resource = new ClassPathResource("static/css/receipt-styles.css");
            byte[] cssData = FileCopyUtils.copyToByteArray(resource.getInputStream());
            cssContent = new String(cssData, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Could not load receipt-styles.css for PDF generation. The PDF will be unstyled.", e);
        }

        // 3. Add the CSS and a PDF flag to the data map for Thymeleaf to use
        data.put("pdfCss", cssContent);
        data.put("isPdfGeneration", true);

        // 4. Process the template using a WebContext
        WebContext context = new WebContext(request, response, request.getServletContext());
        context.setVariables(data);
        String htmlContent = templateEngine.process("home/billing/receipt-generator", context);

        // 5. Generate PDF from the self-contained HTML
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);

            byte[] pdfBytes = outputStream.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "HealthFirst_Receipt_" + invoice.getInvoiceNumber() + ".pdf";
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return ResponseEntity.ok().headers(headers).body(pdfBytes);

        } catch (Exception e) {
            logger.error("Error generating PDF for invoiceId: " + invoiceId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
