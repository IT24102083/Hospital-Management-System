package com.hospital.hospitalmanagementsystem.controller;

import com.hospital.hospitalmanagementsystem.model.Appointment;
import com.hospital.hospitalmanagementsystem.model.Doctor;
import com.hospital.hospitalmanagementsystem.model.Patient;
import com.hospital.hospitalmanagementsystem.model.User;
import com.hospital.hospitalmanagementsystem.service.AppointmentService;
import com.hospital.hospitalmanagementsystem.service.DoctorService;
import com.hospital.hospitalmanagementsystem.service.PatientService;
import com.hospital.hospitalmanagementsystem.service.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/receptionist")
public class ReceptionistController {

    private final AppointmentService appointmentService;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final UserService userService;

    public ReceptionistController(AppointmentService appointmentService, PatientService patientService, DoctorService doctorService, UserService userService) {
        this.appointmentService = appointmentService;
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model,
                            @RequestParam(name = "view", required = false, defaultValue = "dashboard") String view,
                            @RequestParam(name = "id", required = false) Long id, // For editing appointments
                            @RequestParam(value = "patientName", required = false) String patientName,
                            @RequestParam(value = "doctorName", required = false) String doctorName,
                            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        model.addAttribute("currentView", view);

        switch (view) {
            case "register":
                if (!model.containsAttribute("patient")) {
                    model.addAttribute("patient", new Patient());
                }
                model.addAttribute("pageTitle", "Register New Patient");
                break;

            case "edit":
                Appointment appointment = appointmentService.getAppointmentById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid appointment Id:" + id));
                List<Doctor> doctors = doctorService.getAllDoctors();
                model.addAttribute("appointment", appointment);
                model.addAttribute("doctors", doctors);
                model.addAttribute("pageTitle", "Edit Appointment");
                break;

            default: // "dashboard" view
                List<Appointment> appointments = appointmentService.getAllAppointments();
                // Filtering logic remains the same...
                if (patientName != null && !patientName.isEmpty()) { /* ... filter by patient ... */ }
                if (doctorName != null && !doctorName.isEmpty()) { /* ... filter by doctor ... */ }
                if (date != null) { /* ... filter by date ... */ }

                model.addAttribute("appointments", appointments);
                model.addAttribute("patientName", patientName);
                model.addAttribute("doctorName", doctorName);
                model.addAttribute("date", date);
                model.addAttribute("pageTitle", "Receptionist Dashboard");
                break;
        }

        return "home/receptionist/dashboard"; // Always return the same template
    }
    @GetMapping("/patients/register")
    public String showRegisterPatientForm(Model model) {
        if (!model.containsAttribute("patient")) {
            model.addAttribute("patient", new Patient());
        }
        return "home/receptionist/register-patient";
    }

    @PostMapping("/patients/register")
    public String registerPatient(@Valid @ModelAttribute("patient") Patient patient, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.patient", result);
            redirectAttributes.addFlashAttribute("patient", patient);
            return "redirect:/receptionist/patients/register";
        }
        patient.setRole(User.Role.ROLE_PATIENT); // Set user role
        userService.saveUser(patient); // Save user with encoded password
        redirectAttributes.addFlashAttribute("successMessage", "Patient registered successfully!");
        return "redirect:/receptionist/dashboard";
    }

    @GetMapping("/appointments/edit/{id}")
    public String showEditAppointmentForm(@PathVariable("id") Long id, Model model) {
        Appointment appointment = appointmentService.getAppointmentById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid appointment Id:" + id));
        List<Doctor> doctors = doctorService.getAllDoctors();
        model.addAttribute("appointment", appointment);
        model.addAttribute("doctors", doctors);
        return "home/receptionist/edit-appointment";
    }

    @PostMapping("/appointments/edit")
    public String editAppointment(@Valid @ModelAttribute("appointment") Appointment appointment, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            // Handle errors, maybe redirect back to the edit form with error messages
            return "home/receptionist/edit-appointment";
        }
        appointmentService.saveAppointment(appointment); // Update the appointment
        redirectAttributes.addFlashAttribute("successMessage", "Appointment updated successfully!");
        return "redirect:/receptionist/dashboard";
    }
}