package com.hospital.hospitalmanagementsystem.controller;

import com.hospital.hospitalmanagementsystem.model.*;
import com.hospital.hospitalmanagementsystem.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final DoctorAvailabilityService availabilityService;
    private final UserService userService;

    public AppointmentController(AppointmentService appointmentService,
                                 PatientService patientService,
                                 DoctorService doctorService,
                                 DoctorAvailabilityService availabilityService,
                                 UserService userService) {
        this.appointmentService = appointmentService;
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.availabilityService = availabilityService;
        this.userService = userService;
    }

    @GetMapping("/list")
    public String listAppointments(Model model,
                                   @RequestParam(value = "patientName", required = false) String patientName,
                                   @RequestParam(value = "doctorName", required = false) String doctorName,
                                   @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                   @RequestParam(value = "status", required = false) Appointment.Status status) {

        // Fetch filtered and sorted appointments
        List<Appointment> appointments = appointmentService.findFilteredAppointments(patientName, doctorName, date, status);

        model.addAttribute("appointments", appointments);

        // Add filter parameters back to the model to keep them in the form
        model.addAttribute("patientName", patientName);
        model.addAttribute("doctorName", doctorName);
        model.addAttribute("date", date);
        model.addAttribute("status", status);

        // Add lists for dropdowns
        model.addAttribute("statuses", Appointment.Status.values());

        return "home/appointment/appointment-list"; // Return the new view
    }

    @GetMapping("/book")
    public String showBookingForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            return "redirect:/login?error";
        }
        Long userId = userOpt.get().getId();

        Patient patient = patientService.getPatientById(userId).orElse(null);
        model.addAttribute("patient", patient);

        model.addAttribute("appointment", new Appointment());
        model.addAttribute("doctors", doctorService.getAllDoctors());

        return "home/patient/book-appointment";
    }

    @GetMapping("/availability/{doctorId}")
    @ResponseBody
    // FIX: Method now filters out already booked time slots for better accuracy
    public List<LocalTime> getDoctorAvailability(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        Optional<Doctor> doctorOpt = doctorService.getDoctorById(doctorId);
        if (doctorOpt.isEmpty()) {
            return Collections.emptyList();
        }

        Doctor doctor = doctorOpt.get();
        List<DoctorAvailability> availabilities = availabilityService.getDoctorAvailabilitiesByDate(doctor, date);

        if (availabilities.isEmpty() || !availabilities.get(0).isAvailable()) {
            return Collections.emptyList();
        }

        DoctorAvailability dayAvailability = availabilities.get(0);

        List<Appointment> bookedAppointments = appointmentService.getDoctorAppointmentsByDate(doctor, date);
        Set<LocalTime> bookedTimes = bookedAppointments.stream()
                .map(Appointment::getAppointmentTime)
                .collect(Collectors.toSet());

        // 2. Generate all possible time slots for the doctor's shift
        LocalTime startTime = dayAvailability.getStartTime();
        LocalTime endTime = dayAvailability.getEndTime();
        long slotDuration = 30; // Assuming 30-minute slots

        return Stream.iterate(startTime, time -> time.plusMinutes(slotDuration))
                .limit((endTime.toSecondOfDay() - startTime.toSecondOfDay()) / (slotDuration * 60))
                .filter(slot -> !bookedTimes.contains(slot))
                .collect(Collectors.toList());
    }

    @PostMapping("/book")
    public String bookAppointment(@Valid @ModelAttribute("appointment") Appointment appointment,
                                  BindingResult result, Model model) {

        if (result.hasErrors()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            userService.getUserByUsername(username).ifPresent(user ->
                    patientService.getPatientById(user.getId()).ifPresent(patient -> model.addAttribute("patient", patient))
            );
            model.addAttribute("doctors", doctorService.getAllDoctors());
            return "home/patient/book-appointment";
        }

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();

            Optional<User> userOpt = userService.getUserByUsername(username);
            if (userOpt.isEmpty()) {
                return "redirect:/login";
            }
            Long userId = userOpt.get().getId();
            Patient patient = patientService.getPatientById(userId).orElse(null);

            if (patient == null) {
                return "redirect:/login";
            }

            appointment.setPatient(patient);
            appointment.setStatus(Appointment.Status.SCHEDULED);

            Invoice createdInvoice = appointmentService.bookAppointment(appointment);

//            appointmentService.bookAppointment(appointment);

            return "redirect:/patient/invoices/" + createdInvoice.getId() + "/pay";

//            return "redirect:/dashboard?appointmentBooked";

        } catch (Exception e) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            userService.getUserByUsername(username).ifPresent(user ->
                    patientService.getPatientById(user.getId()).ifPresent(patient -> model.addAttribute("patient", patient))
            );
            model.addAttribute("error", e.getMessage());
            model.addAttribute("doctors", doctorService.getAllDoctors());
            return "home/patient/book-appointment";
        }
    }

    @GetMapping("/cancel/{id}")
    public String cancelAppointment(@PathVariable Long id) {
        appointmentService.cancelAppointment(id);
        return "redirect:/dashboard?appointmentCancelled";
    }

    @GetMapping("/available-dates/{doctorId}")
    @ResponseBody
    public List<LocalDate> getDoctorAvailableDates(@PathVariable Long doctorId) {
        Doctor doctor = doctorService.getDoctorById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        return availabilityService.getDoctorAvailabilityDates(doctor);
    }

    @GetMapping("/manage")
    public String manageAppointments(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"))) {
            userService.getUserByUsername(username).ifPresent(user ->
                    doctorService.getDoctorById(user.getId()).ifPresent(doctor ->
                            model.addAttribute("appointments", appointmentService.getDoctorAppointments(doctor))
                    )
            );
            return "home/doctor/manage-appointments";
        } else if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_RECEPTIONIST"))) {
            model.addAttribute("appointments", appointmentService.getAllAppointments());
            model.addAttribute("patients", patientService.getAllPatients());
            model.addAttribute("doctors", doctorService.getAllDoctors());
            return "receptionist/manage-appointments";
        }
        return "redirect:/dashboard";


    }
}