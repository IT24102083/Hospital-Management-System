package com.hospital.hospitalmanagementsystem.controller;

import com.hospital.hospitalmanagementsystem.model.*;
import com.hospital.hospitalmanagementsystem.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
/**
 * Controller for handling dashboard requests for all user roles
 */
@Controller
@RequestMapping("/dashboard")
public class DashboardController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserService userService;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final AppointmentService appointmentService;
    private final MedicineService medicineService;
    private final InvoiceService invoiceService;
    private final PrescriptionService prescriptionService;
    private final PaymentService paymentService;
    private final PaymentPlanService paymentPlanService;

    public DashboardController(UserService userService, PatientService patientService,
                               DoctorService doctorService, AppointmentService appointmentService,
                               MedicineService medicineService, InvoiceService invoiceService,
                               PrescriptionService prescriptionService, PaymentService paymentService,
                               PaymentPlanService paymentPlanService) {
        this.userService = userService;
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.appointmentService = appointmentService;
        this.medicineService = medicineService;
        this.invoiceService = invoiceService;
        this.prescriptionService = prescriptionService;
        this.paymentService = paymentService;
        this.paymentPlanService = paymentPlanService;
    }

    /**
     * Main dashboard endpoint - routes to appropriate dashboard based on user role
     * Now checks session for authenticated user
     */
    @GetMapping
    public String dashboard(Model model, HttpServletRequest request,
                            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedDate) {
        try {
            // Get session and check if user is authenticated
            HttpSession session = request.getSession(false);
            if (session == null) {
                logger.warn("No session found, redirecting to login");
                return "redirect:/login";
            }

            // Get authenticated user from Spring Security context
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                logger.warn("Unauthenticated user attempting to access dashboard");
                return "redirect:/login";
            }

            String username = auth.getName();
            logger.info("User {} accessing dashboard", username);

            // Store username in session if not already there
            if (session.getAttribute("username") == null) {
                session.setAttribute("username", username);
                session.setAttribute("loginTime", LocalDateTime.now().format(DATE_TIME_FORMATTER));
            }

            Optional<User> userOpt = userService.getUserByUsername(username);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                model.addAttribute("user", user);

                // Add common attributes for all dashboards
                addCommonAttributes(model);

                // Store user role in session
                if (session.getAttribute("userRole") == null) {
                    session.setAttribute("userRole", user.getRole().name());
                }

                // Route based on user role
                switch (user.getRole()) {
                    case ROLE_PATIENT:
                        return handlePatientDashboard(model, user.getId());
                    case ROLE_DOCTOR:
                        return handleDoctorDashboard(model, user.getId(), selectedDate);
                    case ROLE_ADMIN:
                        return handleAdminDashboard(model);
                    case ROLE_PHARMACIST:
                        return handlePharmacistDashboard(model);
                    case ROLE_RECEPTIONIST:
                        return handleReceptionistDashboard(model);
                    case ROLE_ACCOUNTANT:
                        return handleAccountantDashboard(model);
                    default:
                        logger.warn("Unknown role {} for user {}", user.getRole(), username);
                        return "redirect:/";
                }
            } else {
                logger.warn("User {} not found in database", username);
                return "redirect:/login";
            }
        } catch (Exception e) {
            logger.error("Error accessing dashboard", e);
            model.addAttribute("errorMessage", "An error occurred while loading the dashboard. Please try again later.");
            addCommonAttributes(model);
            return "home/error";
        }
    }

    /**
     * Add common attributes needed across all dashboard templates
     * Updated to use the provided timestamp format and user login
     */
    private void addCommonAttributes(Model model) {
        model.addAttribute("currentDate", LocalDate.now());
        model.addAttribute("currentUser", "IT24102083");
        model.addAttribute("currentDateTime", "2025-08-11 19:14:04");
        model.addAttribute("dashboardActive", true); // For navigation highlighting
    }

    /**
     * Handle patient dashboard view
     */
    private String handlePatientDashboard(Model model, Long userId) {
        try {
            Optional<Patient> patientOpt = patientService.getPatientById(userId);

            if (patientOpt.isPresent()) {
                Patient patient = patientOpt.get();

                // Get patient appointments and upcoming appointments
                List<Appointment> upcomingAppointments = new ArrayList<>();
                List<Appointment> pastAppointments = new ArrayList<>();
                List<Invoice> recentInvoices = new ArrayList<>();

                try {
                    upcomingAppointments = appointmentService.getUpcomingAppointments(patient);
                } catch (Exception e) {
                    logger.error("Error fetching upcoming appointments for patient {}", userId, e);
                }

                try {
                    pastAppointments = appointmentService.getRecentAppointments(patient);
                } catch (Exception e) {
                    logger.error("Error fetching past appointments for patient {}", userId, e);
                }

                try {
                    recentInvoices = invoiceService.getRecentInvoices(patient, 3);
                } catch (Exception e) {
                    logger.error("Error fetching invoices for patient {}", userId, e);
                }

                // Add all data to model
                model.addAttribute("patient", patient);
                model.addAttribute("upcomingAppointments", upcomingAppointments);
                model.addAttribute("pastAppointments", pastAppointments);
                model.addAttribute("recentInvoices", recentInvoices);
                model.addAttribute("pageTitle", "Patient Dashboard");
            } else {
                logger.warn("Patient with ID {} not found", userId);
                model.addAttribute("errorMessage", "Patient profile not found");
            }

            return "home/patient/dashboard"; // Changed from patient-dashboard to dashboard to match template naming
        } catch (Exception e) {
            logger.error("Error in patient dashboard for user {}", userId, e);
            model.addAttribute("errorMessage", "An error occurred while loading the patient dashboard.");
            return "home/error";
        }
    }

    /**
     * Handle doctor dashboard view
     */
    private String handleDoctorDashboard(Model model, Long userId, LocalDate date) {
        try {
            Optional<Doctor> doctorOpt = doctorService.getDoctorById(userId);
            if (doctorOpt.isPresent()) {
                Doctor doctor = doctorOpt.get();

                // If no date is passed, default to today. Otherwise, use the selected date.
                LocalDate dateToFetch = (date == null) ? LocalDate.now() : date;

                List<Appointment> appointmentsForDate = new ArrayList<>();
                List<Appointment> upcomingAppointments = new ArrayList<>();
                int patientCount = 0;

                try {
                    // FIX: Changed LocalDate.now() to use the dateToFetch variable
                    appointmentsForDate = appointmentService.getDoctorAppointmentsByDate(doctor, dateToFetch);
                } catch (Exception e) {
                    // FIX: Updated the error log to be more descriptive
                    logger.error("Error fetching appointments for doctor {} on date {}", userId, dateToFetch, e);
                }

                try {
                    upcomingAppointments = appointmentService.getDoctorAppointments(doctor);
                } catch (Exception e) {
                    logger.error("Error fetching upcoming appointments for doctor {}", userId, e);
                }

                // Add all data to model
                model.addAttribute("doctor", doctor);
                model.addAttribute("todaysAppointments", appointmentsForDate); // Use the same variable name for the template
                model.addAttribute("upcomingAppointments", upcomingAppointments);
                model.addAttribute("patientCount", patientCount);
                model.addAttribute("selectedDate", dateToFetch); // Pass the selected date back to the view
                model.addAttribute("pageTitle", "Doctor Dashboard");
            } else {
                logger.warn("Doctor with ID {} not found", userId);
                model.addAttribute("errorMessage", "Doctor profile not found");
            }

            return "home/doctor/dashboard";
        } catch (Exception e) {
            logger.error("Error in doctor dashboard for user {}", userId, e);
            model.addAttribute("errorMessage", "An error occurred while loading the doctor dashboard.");
            return "home/error";
        }
    }

    /**
     * Handle admin dashboard view
     */
    private String handleAdminDashboard(Model model) {
        try {
            List<Patient> patients = new ArrayList<>();
            List<Doctor> doctors = new ArrayList<>();
            List<Appointment> appointments = new ArrayList<>();

            try {
                patients = patientService.getAllPatients();
            } catch (Exception e) {
                logger.error("Error fetching patients for admin dashboard", e);
            }

            try {
                doctors = doctorService.getAllDoctors();
            } catch (Exception e) {
                logger.error("Error fetching doctors for admin dashboard", e);
            }

            try {
                appointments = appointmentService.getAllAppointments();
            } catch (Exception e) {
                logger.error("Error fetching appointments for admin dashboard", e);
            }

            model.addAttribute("patients", patients);
            model.addAttribute("patientCount", patients.size());
            model.addAttribute("doctors", doctors);
            model.addAttribute("doctorCount", doctors.size());
            model.addAttribute("appointments", appointments);
            model.addAttribute("appointmentCount", appointments.size());
            model.addAttribute("pageTitle", "Admin Dashboard");

            return "home/admin/dashboard"; // Changed from admin-dashboard to dashboard to match template naming
        } catch (Exception e) {
            logger.error("Error in admin dashboard", e);
            model.addAttribute("errorMessage", "An error occurred while loading the admin dashboard.");
            return "home/error";
        }
    }

    /**
     * Handle pharmacist dashboard view
     */

    private String handlePharmacistDashboard(Model model) {
        try {
            List<Medicine> medicines = new ArrayList<>();
            List<Medicine> lowStockMedicines = new ArrayList<>();
            List<Medicine> expiredMedicines = new ArrayList<>();
            List<Prescription> pendingPrescriptions = new ArrayList<>();

            try {
                medicines = medicineService.getAllMedicines();
            } catch (Exception e) {
                logger.error("Error fetching medicines for pharmacist dashboard", e);
            }

            try {
                lowStockMedicines = medicineService.getLowStockMedicines(10);
            } catch (Exception e) {
                logger.error("Error fetching low stock medicines for pharmacist dashboard", e);
            }

            try {
                expiredMedicines = medicineService.getExpiredMedicines();
            } catch (Exception e) {
                logger.error("Error fetching expired medicines for pharmacist dashboard", e);
            }

            try {
                // Fetch the actual pending prescriptions from the service
                pendingPrescriptions = prescriptionService.getPendingPrescriptions();
            } catch (Exception e) {
                logger.error("Error fetching pending prescriptions for pharmacist dashboard", e);
            }

            // Add the count to the model so the HTML template can display it
            model.addAttribute("pendingPrescriptionsCount", pendingPrescriptions.size());

            model.addAttribute("medicines", medicines);
            model.addAttribute("medicineCount", medicines.size());
            model.addAttribute("lowStockMedicines", lowStockMedicines);
            model.addAttribute("expiredMedicines", expiredMedicines);
            model.addAttribute("pageTitle", "Pharmacist Dashboard");
            model.addAttribute("prescriptions", pendingPrescriptions);

            return "home/pharmacist/dashboard";
        } catch (Exception e) {
            logger.error("Error in pharmacist dashboard", e);
            model.addAttribute("errorMessage", "An error occurred while loading the pharmacist dashboard.");
            return "home/error";
        }
    }

    /**
     * Handle receptionist dashboard view
     */
    private String handleReceptionistDashboard(Model model) {
        try {
            List<Appointment> appointments = new ArrayList<>();
            List<Appointment> todaysAppointments = new ArrayList<>();
            List<Patient> patients = new ArrayList<>();

            try {
                appointments = appointmentService.getAllAppointments();
                // Filter today's appointments
                final LocalDate today = LocalDate.now();
                todaysAppointments = appointments.stream()
                        .filter(apt -> apt.getAppointmentDate().equals(today))
                        .collect(Collectors.toList()); // Fixed line
            } catch (Exception e) {
                logger.error("Error fetching appointments for receptionist dashboard", e);
            }

            try {
                patients = patientService.getAllPatients();
            } catch (Exception e) {
                logger.error("Error fetching patients for receptionist dashboard", e);
            }

            model.addAttribute("appointments", appointments);
            model.addAttribute("todaysAppointments", todaysAppointments);
            model.addAttribute("patients", patients);
            model.addAttribute("patientCount", patients.size());
            model.addAttribute("appointmentCount", appointments.size());
            model.addAttribute("pageTitle", "Receptionist Dashboard");

            return "home/receptionist/dashboard"; // Changed from receptionist-dashboard to dashboard to match template naming
        } catch (Exception e) {
            logger.error("Error in receptionist dashboard", e);
            model.addAttribute("errorMessage", "An error occurred while loading the receptionist dashboard.");
            return "home/error";
        }
    }

    /**
     * Handle accountant dashboard view
     */
    private String handleAccountantDashboard(Model model) {
        try {
            BigDecimal totalRevenue = paymentService.getTotalRevenue();
            BigDecimal outstandingAmount = invoiceService.getTotalOutstandingBalance();
            int activePaymentPlans = paymentPlanService.getActivePaymentPlansCount();
            int pendingVerifications = paymentService.getPendingBankSlipVerifications().size();

            model.addAttribute("totalRevenue", totalRevenue);
            model.addAttribute("outstandingAmount", outstandingAmount);
            model.addAttribute("activePaymentPlans", activePaymentPlans);
            model.addAttribute("pendingVerifications", pendingVerifications);
            model.addAttribute("pageTitle", "Accountant Dashboard");

            return "home/accountant/accountant-dashboard";
        } catch (Exception e) {
            logger.error("Error in accountant dashboard", e);
            model.addAttribute("errorMessage", "An error occurred while loading the accountant dashboard.");
            return "home/error";
        }
    }


    /**
     * Method to check if the user has a valid session
     * @return true if the session is valid, false otherwise
     */
    private boolean hasValidSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute("username") != null;
    }

    /**
     * Method to handle session timeout
     */
    @GetMapping("/session-expired")
    public String sessionExpired(Model model) {
        model.addAttribute("message", "Your session has expired. Please login again.");
        return "redirect:/login?expired=true";
    }
}