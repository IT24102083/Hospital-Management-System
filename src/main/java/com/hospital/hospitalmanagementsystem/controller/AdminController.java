package com.hospital.hospitalmanagementsystem.controller;

import com.hospital.hospitalmanagementsystem.model.User;
import com.hospital.hospitalmanagementsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;

    @Autowired
    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public String adminDashboard(
            @RequestParam(name = "view", required = false, defaultValue = "dashboard") String view,
            @RequestParam(name = "id", required = false) Long id,
            Model model) {

        // Always add totalUsers and the list of users to the model
        model.addAttribute("totalUsers", userService.getAllUsers().size());
        model.addAttribute("users", userService.getAllUsers()); // <-- MOVED THIS LINE

        switch (view) {
            // The 'users' case is now redundant for data loading but kept for view logic
            case "users":
                model.addAttribute("currentView", "dashboard"); // Set to dashboard to show combined view
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
        userService.saveUser(user);
        redirectAttributes.addFlashAttribute("successMessage", "User added successfully!");
        return "redirect:/admin/dashboard"; // Redirect to the main combined view
    }

    @PostMapping("/users/update/{id}")
    public String updateUser(@PathVariable("id") Long id, @Valid @ModelAttribute("user") User user, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.user", result);
            redirectAttributes.addFlashAttribute("user", user);
            return "redirect:/admin/dashboard?view=editUser&id=" + id;
        }
        userService.updateUser(id, user);
        redirectAttributes.addFlashAttribute("successMessage", "User updated successfully!");
        return "redirect:/admin/dashboard"; // Redirect to the main combined view
    }

    @GetMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        userService.deleteUser(id);
        redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully!");
        return "redirect:/admin/dashboard"; // Redirect to the main combined view
    }
}