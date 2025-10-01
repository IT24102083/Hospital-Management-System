package com.hospital.hospitalmanagementsystem.controller;

import com.hospital.hospitalmanagementsystem.model.*;
import com.hospital.hospitalmanagementsystem.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/patient")
public class PatientController {
    private static final Logger logger = LoggerFactory.getLogger(PatientController.class);

    private final PatientService patientService;
    private final UserService userService;
    private final AppointmentService appointmentService;
    private final InvoiceService invoiceService;
    private final PrescriptionService prescriptionService; // Added service

    @Value("${app.upload.prescriptions:uploads/prescriptions}")
    private String uploadDir;

    public PatientController(PatientService patientService,
                             UserService userService,
                             AppointmentService appointmentService,
                             InvoiceService invoiceService,
                             PrescriptionService prescriptionService) { // Injected service
        this.patientService = patientService;
        this.userService = userService;
        this.appointmentService = appointmentService;
        this.invoiceService = invoiceService;
        this.prescriptionService = prescriptionService;
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

    // New endpoint for patients to view their prescriptions
    @GetMapping("/prescriptions")
    public String viewMyPrescriptions(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        try {
            Optional<User> userOpt = userService.getUserByUsername(principal.getName());
            if (userOpt.isPresent()) {
                List<Prescription> prescriptions = prescriptionService.getPrescriptionsForPatient(userOpt.get());
                model.addAttribute("prescriptions", prescriptions);
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

    private Patient getPatientFromPrincipal(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("User must be logged in to perform this action.");
        }
        User user = userService.getUserByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database."));
        return patientService.getPatientById(user.getId())
                .orElseThrow(() -> new RuntimeException("Patient profile not found for the current user. Please contact support."));
    }

    /**
     * Patient Dashboard endpoint - shows overview of patient information
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        try {
            Patient patient = getPatientFromPrincipal(principal);

            // Fetch all necessary data
            List<Invoice> allInvoices = invoiceService.getPatientInvoices(patient);
            List<Appointment> upcomingAppointments = appointmentService.getUpcomingAppointments(patient);
            List<Prescription> prescriptions = prescriptionService.getPrescriptionsForPatient(patient);
            List<Invoice> recentInvoices = allInvoices.stream().limit(3).collect(Collectors.toList());

            // Add all data to the model
            model.addAttribute("patient", patient);
            model.addAttribute("upcomingAppointments", upcomingAppointments);
            model.addAttribute("invoiceCount", allInvoices.size());
            model.addAttribute("prescriptionCount", prescriptions.size());
            model.addAttribute("recentInvoices", recentInvoices);

            // Add current date and login info
            model.addAttribute("currentDate", LocalDate.now());
            model.addAttribute("currentUser", "IT24102083");
            model.addAttribute("currentDateTime", "2025-10-01 21:06:27");

            return "home/patient/dashboard";

        } catch (Exception e) {
            logger.error("Error in patient dashboard for user {}: {}", (principal != null ? principal.getName() : "UNKNOWN"), e.getMessage());
            model.addAttribute("errorMessage", "An error occurred while loading the dashboard: " + e.getMessage());
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

            return "patient/profile";

        } catch (Exception e) {
            logger.error("Error viewing patient profile", e);
            model.addAttribute("errorMessage", "An error occurred while loading your profile. Please try again later.");
            return "home/error";
        }
    }

    /**
     * Display form for editing patient profile
     */
    @GetMapping("/profile/edit")
    public String editProfileForm(Model model, Principal principal) {
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

            return "patient/edit-profile";

        } catch (Exception e) {
            logger.error("Error loading edit profile form", e);
            model.addAttribute("errorMessage", "An error occurred while loading the edit form. Please try again later.");
            return "home/error";
        }
    }

    /**
     * Handle patient profile update submission
     */
    @PostMapping("/profile/edit")
    public String updateProfile(@Valid @ModelAttribute("patient") Patient patientDetails,
                                BindingResult result, Principal principal, Model model) {
        try {
            if (result.hasErrors()) {
                // Add current date and login info on validation error
                model.addAttribute("currentUser", "IT24102083");
                model.addAttribute("currentDateTime", "2025-08-11 05:39:58");
                return "patient/edit-profile";
            }

            if (principal == null) {
                return "redirect:/login";
            }

            Optional<User> userOpt = userService.getUserByUsername(principal.getName());
            if (userOpt.isPresent() && userOpt.get().getRole() == User.Role.ROLE_PATIENT) {
                Optional<Patient> patientOpt = patientService.getPatientById(userOpt.get().getId());
                if (patientOpt.isPresent()) {
                    Patient patient = patientOpt.get();

                    // Update fields that are allowed to be edited by the patient
                    patient.setFirstName(patientDetails.getFirstName());
                    patient.setLastName(patientDetails.getLastName());
                    patient.setPhoneNumber(patientDetails.getPhoneNumber());
                    patient.setAddress(patientDetails.getAddress());
                    patient.setEmergencyContactName(patientDetails.getEmergencyContactName());
                    patient.setEmergencyContactNumber(patientDetails.getEmergencyContactNumber());
                    patient.setAllergies(patientDetails.getAllergies());

                    patientService.savePatient(patient);
                    return "redirect:/patient/profile?updated";
                }
            }

            return "redirect:/login";

        } catch (Exception e) {
            logger.error("Error updating patient profile", e);
            model.addAttribute("errorMessage", "An error occurred while updating your profile. Please try again later.");
            return "home/error";
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
                    List<Appointment> appointments = appointmentService.getPatientAppointments(patientOpt.get());
                    model.addAttribute("appointments", appointments);
                }
            }

            // Add current date and login info
            model.addAttribute("currentUser", "IT24102083");
            model.addAttribute("currentDateTime", "2025-08-11 05:39:58");

            return "home/patient/book-appointment";

        } catch (Exception e) {
            logger.error("Error viewing patient appointments", e);
            model.addAttribute("errorMessage", "An error occurred while loading your appointments. Please try again later.");
            return "home/error";
        }
    }

    /**
     * View patient invoices
     */
    @GetMapping("/invoices")
    public String viewInvoices(Model model, Principal principal) {
        try {
            if (principal == null) {
                return "redirect:/login";
            }

            Optional<User> userOpt = userService.getUserByUsername(principal.getName());
            if (userOpt.isPresent() && userOpt.get().getRole() == User.Role.ROLE_PATIENT) {
                Optional<Patient> patientOpt = patientService.getPatientById(userOpt.get().getId());
                if (patientOpt.isPresent()) {
                    List<Invoice> invoices = invoiceService.getPatientInvoices(patientOpt.get());
                    model.addAttribute("invoices", invoices);
                }
            }

            // Add current date and login info
            model.addAttribute("currentUser", "IT24102083");
            model.addAttribute("currentDateTime", "2025-08-11 05:39:58");

            return "home/patient/invoices";

        } catch (Exception e) {
            logger.error("Error viewing patient invoices", e);
            model.addAttribute("errorMessage", "An error occurred while loading your invoices. Please try again later.");
            return "home/error";
        }
    }

    /**
     * View individual invoice details
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
                if (invoiceOpt.isPresent() && invoiceOpt.get().getPatient().getId().equals(userOpt.get().getId())) {
                    model.addAttribute("invoice", invoiceOpt.get());

                    // Add current date and login info
                    model.addAttribute("currentUser", "IT24102083");
                    model.addAttribute("currentDateTime", "2025-08-11 05:39:58");

                    return "home/patient/invoice-details";
                }
            }

            return "redirect:/patient/invoices";

        } catch (Exception e) {
            logger.error("Error viewing invoice details", e);
            model.addAttribute("errorMessage", "An error occurred while loading the invoice details. Please try again later.");
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

                    return "home/patient/appointment-details";
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
}