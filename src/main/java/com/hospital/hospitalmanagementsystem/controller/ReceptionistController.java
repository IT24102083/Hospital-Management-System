package com.hospital.hospitalmanagementsystem.controller;

import com.hospital.hospitalmanagementsystem.model.Appointment;
import com.hospital.hospitalmanagementsystem.model.Doctor;
import com.hospital.hospitalmanagementsystem.model.Patient;
import com.hospital.hospitalmanagementsystem.model.User;
import com.hospital.hospitalmanagementsystem.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Comparator;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/receptionist")
public class ReceptionistController {

    private final AppointmentService appointmentService;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final UserService userService;
    private final DoctorAvailabilityService doctorAvailabilityService;

    public ReceptionistController(AppointmentService appointmentService, PatientService patientService, DoctorService doctorService, UserService userService, DoctorAvailabilityService doctorAvailabilityService) {
        this.appointmentService = appointmentService;
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.userService = userService;
        this.doctorAvailabilityService = doctorAvailabilityService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal,
                            @RequestParam(name = "view", required = false, defaultValue = "dashboard") String view,
                            @RequestParam(name = "id", required = false) Long id,
                            @RequestParam(value = "patientName", required = false) String patientName,
                            @RequestParam(value = "doctorName", required = false) String doctorName,
                            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        Optional<User> userOpt = userService.getUserByUsername(principal.getName());
        userOpt.ifPresent(user -> model.addAttribute("user", user));
        model.addAttribute("currentView", view);

        switch (view) {
            case "register":
                if (!model.containsAttribute("patient")) {
                    model.addAttribute("patient", new Patient());
                }
                model.addAttribute("pageTitle", "Register New Patient");
                break;

            case "book":
                if (!model.containsAttribute("appointment")) {
                    model.addAttribute("appointment", new Appointment());
                }
                model.addAttribute("patients", patientService.getAllPatients());
                model.addAttribute("doctors", doctorService.getAllDoctors());
                model.addAttribute("pageTitle", "Book New Appointment");
                break;

            case "edit":
                Appointment appointment = appointmentService.getAppointmentById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid appointment Id:" + id));
                model.addAttribute("appointment", appointment);
                model.addAttribute("doctors", doctorService.getAllDoctors());
                model.addAttribute("pageTitle", "Edit Appointment");
                break;

            default: // "dashboard" view
                List<Appointment> appointments = appointmentService.findFilteredAppointments(patientName, doctorName, date);

                appointments.sort(Comparator.comparing(Appointment::getAppointmentDate).reversed()
                        .thenComparing(Appointment::getAppointmentTime).reversed());

                model.addAttribute("appointments", appointments);
                model.addAttribute("patientName", patientName);
                model.addAttribute("doctorName", doctorName);
                model.addAttribute("date", date);
                model.addAttribute("pageTitle", "Receptionist Dashboard");
                model.addAttribute("patientCount", patientService.countPatients());
                model.addAttribute("appointmentCount", appointmentService.countAppointments());
                model.addAttribute("todaysAppointments", appointmentService.getAppointmentsByDate(LocalDate.now()));
                break;
        }

        return "home/receptionist/dashboard";
    }

    @PostMapping("/appointments/book")
    public String bookAppointment(@Valid @ModelAttribute("appointment") Appointment appointment, BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("patients", patientService.getAllPatients());
            model.addAttribute("doctors", doctorService.getAllDoctors());
            // Return to the dashboard template but keep the view as 'book'
            model.addAttribute("currentView", "book");
            model.addAttribute("pageTitle", "Book New Appointment");
            return "home/receptionist/dashboard";
        }
        try {
            appointmentService.bookAppointment(appointment);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment booked successfully!");
            return "redirect:/receptionist/dashboard";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            // Redirect back to the booking view on error
            return "redirect:/receptionist/dashboard?view=book";
        }
    }

    @GetMapping("/doctors/{id}/availability")
    @ResponseBody
    public ResponseEntity<List<LocalDate>> getDoctorAvailability(@PathVariable Long id) {
        Doctor doctor = doctorService.getDoctorById(id)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        List<LocalDate> availableDates = doctorAvailabilityService.getDoctorAvailabilityDates(doctor);
        return ResponseEntity.ok(availableDates);
    }

    @PostMapping("/patients/register")
    public String registerPatient(@Valid @ModelAttribute("patient") Patient patient, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.patient", result);
            redirectAttributes.addFlashAttribute("patient", patient);
            return "redirect:/receptionist/dashboard?view=register";
        }
        patient.setRole(User.Role.ROLE_PATIENT);
        userService.saveUser(patient);
        redirectAttributes.addFlashAttribute("successMessage", "Patient registered successfully!");
        return "redirect:/receptionist/dashboard";
    }

    @PostMapping("/appointments/edit")
    public String editAppointment(@Valid @ModelAttribute("appointment") Appointment appointment, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.appointment", result);
            redirectAttributes.addFlashAttribute("appointment", appointment);
            return "redirect:/receptionist/dashboard?view=edit&id=" + appointment.getId();
        }
        appointmentService.saveAppointment(appointment);
        redirectAttributes.addFlashAttribute("successMessage", "Appointment updated successfully!");
        return "redirect:/receptionist/dashboard";
    }
}