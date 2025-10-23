package com.hospital.hospitalmanagementsystem.controller;

import com.hospital.hospitalmanagementsystem.model.Doctor;
import com.hospital.hospitalmanagementsystem.model.User;
import com.hospital.hospitalmanagementsystem.service.DoctorService;
import com.hospital.hospitalmanagementsystem.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Controller
public class HomeController {

    private final DoctorService doctorService;
    private final UserService userService;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Hardcoded values as per requirements
    private static final String FIXED_DATETIME = "2025-08-11 19:20:23";
    private static final String FIXED_USER_ID = "IT24102083";

    public HomeController(DoctorService doctorService, UserService userService) {
        this.doctorService = doctorService;
        this.userService = userService;
    }

    @GetMapping("/")
    public String home(Model model) {
        List<Doctor> doctors = doctorService.getAllDoctors();
        model.addAttribute("doctors", doctors);
        addCommonAttributes(model);
        return "home/index";
    }

    @GetMapping("/about")
    public String about(Model model) {
        addCommonAttributes(model);
        return "home/about";
    }

    @GetMapping("/pharmacy")
    public String pharmacy(Model model) {
        // Add authentication info for the pharmacy page
        addCommonAttributes(model);

        // Flag to indicate we're on the pharmacy page
        model.addAttribute("pharmacyActive", true);

        return "home/pharmacy";
    }

    @GetMapping("/services")
    public String services(Model model) {
        addCommonAttributes(model);
        return "home/services";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        addCommonAttributes(model);
        return "home/contact";
    }

    @GetMapping("/login")
    public String login(Model model) {
        addCommonAttributes(model);
        return "home/login";
    }

    @GetMapping("/access-denied")
    public String accessDenied(Model model) {
        addCommonAttributes(model);
        return "home/access-denied";
    }

    @GetMapping("/goto-dashboard")
    public String redirectToDashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "redirect:/login";
        }

        Optional<User> userOpt = userService.getUserByUsername(auth.getName());
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            switch (user.getRole()) {
                case ROLE_PATIENT:
                    return "redirect:/patient/dashboard";
                case ROLE_DOCTOR:
                    return "redirect:/doctor/dashboard";
                case ROLE_ADMIN:
                    return "redirect:/admin/dashboard";
                case ROLE_PHARMACIST:
                    return "redirect:/pharmacist/dashboard";
                case ROLE_RECEPTIONIST:
                    return "redirect:/receptionist/dashboard";
                case ROLE_ACCOUNTANT:
                    return "redirect:/accountant/accountant-dashboard";
                default:
                    return "redirect:/"; // Default to main dashboard
            }
        }

        return "redirect:/";
    }

    /**
     * Add common attributes to the model for all views
     * Updated to explicitly include authentication status and user role
     *
     * @param model The model to add attributes to
     */
    private void addCommonAttributes(Model model) {
        // Use the fixed datetime as requested
        model.addAttribute("currentDateTime", FIXED_DATETIME);

        // Get current authenticated user if available
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() &&
                !"anonymousUser".equals(auth.getName());

        // Always use the fixed user ID when authenticated
        if (isAuthenticated) {
            // Get user details if available
            Optional<User> userOpt = userService.getUserByUsername(auth.getName());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                model.addAttribute("currentUser", FIXED_USER_ID);
                model.addAttribute("username", user.getUsername());
                model.addAttribute("firstName", user.getFirstName());
                model.addAttribute("lastName", user.getLastName());
                model.addAttribute("userRole", user.getRole().name());
            } else {
                // Fallback if user not found in database but authenticated
                model.addAttribute("currentUser", FIXED_USER_ID);

                // Extract role from authorities
                Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
                if (!authorities.isEmpty()) {
                    model.addAttribute("userRole", authorities.iterator().next().getAuthority());
                }
            }
        } else {
            // Guest user
            model.addAttribute("currentUser", "Guest");
        }

        // Explicitly add authentication status for templates to check
        model.addAttribute("isAuthenticated", isAuthenticated);
    }
}