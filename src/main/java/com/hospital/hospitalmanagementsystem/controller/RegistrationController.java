package com.hospital.hospitalmanagementsystem.controller;

import com.hospital.hospitalmanagementsystem.model.Patient;
import com.hospital.hospitalmanagementsystem.model.User;
import com.hospital.hospitalmanagementsystem.service.PatientService;
import com.hospital.hospitalmanagementsystem.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Controller
public class RegistrationController {

    private final UserService userService;
    private final PatientService patientService;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public RegistrationController(UserService userService, PatientService patientService) {
        this.userService = userService;
        this.patientService = patientService;
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("patient", new Patient());

        // Add footer information
        addCommonAttributes(model);

        return "home/register";
    }

    @PostMapping("/register")
    public String registerPatient(@Valid @ModelAttribute("patient") Patient patient,
                                  BindingResult result, Model model) {

        // Add footer information in case of validation errors
        addCommonAttributes(model);

        if (result.hasErrors()) {
            return "home/register";
        }

        if (userService.existsByUsername(patient.getUsername())) {
            result.rejectValue("username", "error.patient", "Username is already taken");
            return "home/register";
        }

        if (userService.existsByEmail(patient.getEmail())) {
            result.rejectValue("email", "error.patient", "Email is already in use");
            return "home/register";
        }

        patient.setRole(User.Role.ROLE_PATIENT);
        Patient savedPatient = patientService.savePatient(patient);

        return "redirect:/login?registered";
    }

    /**
     * Adds common attributes needed for the page footer
     */
    private void addCommonAttributes(Model model) {
        // Get current UTC time formatted as required
        String currentDateTime = LocalDateTime.now(ZoneOffset.UTC)
                .format(DATE_TIME_FORMATTER);
        model.addAttribute("currentDateTime", currentDateTime);

        // Get current authenticated user if available
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = "Guest";

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            currentUser = auth.getName();
        }

        model.addAttribute("currentUser", currentUser);
    }
}