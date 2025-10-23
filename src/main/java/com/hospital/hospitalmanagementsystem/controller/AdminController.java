package com.hospital.hospitalmanagementsystem.controller;

import com.hospital.hospitalmanagementsystem.config.AppConfig; // Import Singleton
import com.hospital.hospitalmanagementsystem.model.Appointment;
import com.hospital.hospitalmanagementsystem.model.User;
import com.hospital.hospitalmanagementsystem.model.Doctor;
import com.hospital.hospitalmanagementsystem.service.AdminUserService; // Import  service
import com.hospital.hospitalmanagementsystem.service.AppointmentService;
import com.hospital.hospitalmanagementsystem.service.DoctorService;
import com.hospital.hospitalmanagementsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminUserService adminUserService; // USE THE NEW SERVICE
    private final UserService userService; // Keep for reading data if needed
    private final DoctorService doctorService;
    private final AppointmentService appointmentService;

    @Autowired
    public AdminController(AdminUserService adminUserService, UserService userService, DoctorService doctorService, AppointmentService appointmentService) {
        this.adminUserService = adminUserService;
        this.userService = userService;
        this.doctorService = doctorService;
        this.appointmentService = appointmentService;

    }

    @GetMapping("/dashboard")
    public String adminDashboard(
            @RequestParam(name = "view", required = false, defaultValue = "dashboard") String view,
            @RequestParam(name = "id", required = false) Long id,
            @RequestParam(value = "patientName", required = false) String patientName,
            @RequestParam(value = "doctorName", required = false) String doctorName,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "status", required = false) Appointment.Status status,
            Model model) {

        // Use the Singleton to get config data
        AppConfig appConfig = AppConfig.getInstance();
        model.addAttribute("appVersion", appConfig.getAppVersion());
        model.addAttribute("environment", appConfig.getEnvironment());
        model.addAttribute("totalUsers", adminUserService.getAllUsers().size());
        model.addAttribute("doctorCount", doctorService.getAllDoctors().size());
        model.addAttribute("appointmentCount", appointmentService.countAppointments());


        switch (view) {
            case "users":
                model.addAttribute("users", adminUserService.getAllUsers());
                model.addAttribute("currentView", "users");
                break;

            // ADDED: Case to handle the new doctor management view
            case "doctors":
                List<Doctor> doctors = doctorService.getAllDoctors();
                model.addAttribute("doctors", doctors);
                model.addAttribute("currentView", "doctors");
                break;

            case "appointments":
                List<Appointment> appointments = appointmentService.findFilteredAppointments(patientName, doctorName, date, status);
                model.addAttribute("appointments", appointments);

                // Pass filter values back to the view
                model.addAttribute("patientName", patientName);
                model.addAttribute("doctorName", doctorName);
                model.addAttribute("date", date);
                model.addAttribute("status", status);

                // Pass status enum values for the dropdown
                model.addAttribute("statuses", Appointment.Status.values());
                model.addAttribute("currentView", "appointments");
                break;

            case "addUser":
                if (!model.containsAttribute("user")) {
                    model.addAttribute("user", new User());
                }
                model.addAttribute("roles", User.Role.values());
                model.addAttribute("currentView", "userForm");
                model.addAttribute("formAction", "add");
                break;
            case "editUser":
                if (id != null && !model.containsAttribute("user")) {
                    User user = userService.getUserById(id)
                            .orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + id));
                    model.addAttribute("user", user);
                }
                model.addAttribute("roles", User.Role.values());
                model.addAttribute("currentView", "userForm");
                model.addAttribute("formAction", "edit");
                break;
            default:
                model.addAttribute("currentView", "dashboard");
                break;
        }
        return "home/admin/admin-dashboard";
    }

    @PostMapping("/users/add")
    public String addUser(@Valid @ModelAttribute("user") User user, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.user", result);
            redirectAttributes.addFlashAttribute("user", user);
            return "redirect:/admin/dashboard?view=addUser";
        }
        adminUserService.saveUser(user); // USE NEW SERVICE
        redirectAttributes.addFlashAttribute("successMessage", "User added successfully!");
        return "redirect:/admin/dashboard?view=users";
    }

    @PostMapping("/users/update/{id}")
    public String updateUser(@PathVariable("id") Long id, @ModelAttribute("user") User user, BindingResult result, RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.user", result);
            redirectAttributes.addFlashAttribute("user", user);
            return "redirect:/admin/dashboard?view=editUser&id=" + id;
        }

        userService.updateUser(id, user); // Now this line will be reached
        redirectAttributes.addFlashAttribute("successMessage", "User updated successfully!");
        return "redirect:/admin/dashboard?view=users";
    }

    @GetMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        adminUserService.deleteUser(id); // USE NEW SERVICE
        redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully!");
        return "redirect:/admin/dashboard?view=users";
    }
}