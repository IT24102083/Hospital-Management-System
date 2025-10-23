package com.hospital.hospitalmanagementsystem.controller;

import com.hospital.hospitalmanagementsystem.model.Medicine;
import com.hospital.hospitalmanagementsystem.model.Prescription;
import com.hospital.hospitalmanagementsystem.model.User;
import com.hospital.hospitalmanagementsystem.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
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
import java.time.LocalDate;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/pharmacist")
public class PharmacistController {

    private final UserService userService;
    private final MedicineService medicineService;
    private final PrescriptionService prescriptionService;
    private final PharmacistService pharmacistService;
    private final ReportService reportService;
    private final EmailService emailService;

    @Value("${app.upload.dir:uploads/medicines}")
    private String uploadDir;

    public PharmacistController(UserService userService,
                                MedicineService medicineService,
                                PrescriptionService prescriptionService,
                                PharmacistService pharmacistService,
                                ReportService reportService,
                                EmailService emailService ) {
        this.userService = userService;
        this.medicineService = medicineService;
        this.prescriptionService = prescriptionService;
        this.pharmacistService = pharmacistService;
        this.reportService = reportService;
        this.emailService = emailService;
    }

    /**
     * Displays the dashboard with pending prescriptions.
     * Added code to fetch pending prescriptions and add them to the model.
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        try {
            BigDecimal totalInventoryValue = BigDecimal.ZERO;
            int totalMedicines = 0;
            int lowStockCount = 0;
            int expiredCount = 0;

            List<Medicine> medicines = medicineService.getAllMedicines();
            totalMedicines = medicines.size();

            LocalDate today = LocalDate.now();
            for (Medicine medicine : medicines) {
                if (medicine.getPrice() != null && medicine.getStock() > 0) {
                    BigDecimal itemValue = medicine.getPrice().multiply(BigDecimal.valueOf(medicine.getStock()));
                    totalInventoryValue = totalInventoryValue.add(itemValue);
                }

                if (medicine.getStock() <= 10) {
                    lowStockCount++;
                }

                if (medicine.getExpiryDate() != null && medicine.getExpiryDate().isBefore(today)) {
                    expiredCount++;
                }
            }

            // Get the pending prescriptions
            List<Prescription> pendingPrescriptions = prescriptionService.getPendingPrescriptions();

            model.addAttribute("totalInventoryValue", totalInventoryValue);
            model.addAttribute("medicineCount", totalMedicines);
            model.addAttribute("criticalCount", lowStockCount + expiredCount);
            model.addAttribute("pendingPrescriptionsCount", pendingPrescriptions.size());
            model.addAttribute("pendingPrescriptions", pendingPrescriptions);
            model.addAttribute("medicines", medicines);
            model.addAttribute("today", today);
            model.addAttribute("thirtyDaysFromNow", today.plusDays(30));

            addCommonAttributes(model);
            return "home/pharmacist/dashboard";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error loading dashboard: " + e.getMessage());
            addCommonAttributes(model);
            return "home/pharmacist/dashboard";
        }
    }

    /**
     * Displays a list of all pending prescriptions.
     */
    @GetMapping("/prescriptions")
    public String viewPendingPrescriptions(Model model) {
        model.addAttribute("prescriptions", prescriptionService.getPendingPrescriptions());
        addCommonAttributes(model);
        // FIXED: Changed to return the correct list view
        return "home/pharmacist/prescriptions-list";
    }

    /**
     * Displays the details of a single prescription to be fulfilled.
     */
    @GetMapping("/prescriptions/{id}")
    public String viewPrescriptionForFulfillment(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Prescription> prescriptionOpt = prescriptionService.getPrescriptionById(id);
        if (prescriptionOpt.isPresent()) {
            model.addAttribute("prescription", prescriptionOpt.get());
            model.addAttribute("medicines", medicineService.getAllMedicines());
            addCommonAttributes(model);
            // This view shows a single prescription for fulfillment
            return "home/pharmacist/fulfill-prescriptions";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Prescription not found with ID: " + id);
            return "redirect:/pharmacist/prescriptions";
        }
    }

    /**
     * Processes the fulfillment of a prescription.
     */
    @PostMapping("/prescriptions/fulfill/{id}")
    public String fulfillPrescription(@PathVariable Long id, @RequestParam Map<String, String> prescribedData, RedirectAttributes redirectAttributes) {
        if (!prescriptionService.getPrescriptionById(id).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot fulfill. Prescription not found.");
            return "redirect:/pharmacist/prescriptions";
        }

        try {
            prescriptionService.fulfillPrescription(id, prescribedData);
            redirectAttributes.addFlashAttribute("successMessage", "Prescription has been successfully fulfilled.");
            return "redirect:/pharmacist/prescriptions";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation Error: " + e.getMessage());
            return "redirect:/pharmacist/prescriptions/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred while fulfilling the prescription.");
            return "redirect:/pharmacist/prescriptions/" + id;
        }
    }

    /**
     * Displays the form to create a new medicine.
     */
    @GetMapping("/medicines/create")
    public String addMedicineForm(Model model) {
        model.addAttribute("medicine", new Medicine());
        addCommonAttributes(model);
        return "home/pharmacist/create_medicine";
    }

    // Add this method inside your MedicineController class
    @PostMapping("/{id}/update-stock")
    @ResponseBody
    public ResponseEntity<?> updateStock(@PathVariable Long id, @RequestBody Integer newStock) {
        try {
            if (newStock < 0) {
                return ResponseEntity.badRequest().body("{\"success\": false, \"message\": \"Stock cannot be negative.\"}");
            }
            Medicine updatedMedicine = medicineService.updateMedicineStock(id, newStock);
            return ResponseEntity.ok(updatedMedicine);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/medicines-table")
    public String getMedicinesTableFragment(Model model,
                                            @RequestParam(value = "search", required = false) String search,
                                            @RequestParam(value = "category", required = false) String category) {

        // This logic is the same as in the main dashboard method
        List<Medicine> allMedicines = medicineService.getAllMedicines();
        List<Medicine> tableMedicines = allMedicines;

        if (search != null && !search.trim().isEmpty()) {
            String lowerCaseSearch = search.toLowerCase();
            tableMedicines = allMedicines.stream()
                    .filter(m -> m.getName().toLowerCase().contains(lowerCaseSearch) ||
                            (m.getBrand() != null && m.getBrand().toLowerCase().contains(lowerCaseSearch)))
                    .collect(Collectors.toList());
        } else if (category != null && !category.trim().isEmpty()) {
            tableMedicines = allMedicines.stream()
                    .filter(m -> m.getCategory().equalsIgnoreCase(category))
                    .collect(Collectors.toList());
        }

        model.addAttribute("medicines", tableMedicines);
        model.addAttribute("today", LocalDate.now()); // Add 'today' for expiry date logic in the fragment

        // This tells Thymeleaf to only render the 'medicinesTableBody' fragment
        // from the specified template.
        return "home/pharmacist/dashboard :: medicinesTableBody";
    }

    @GetMapping("/dashboard-stats")
    @ResponseBody
    public ResponseEntity<?> getDashboardStats() {
        try {
            List<Medicine> allMedicines = medicineService.getAllMedicines();
            List<Medicine> activeMedicines = allMedicines.stream()
                    .filter(Medicine::isActive)
                    .collect(Collectors.toList());

            BigDecimal totalInventoryValue = BigDecimal.ZERO;
            int lowStockCount = 0;
            int expiredCount = 0;
            LocalDate today = LocalDate.now();

            for (Medicine medicine : activeMedicines) {
                if (medicine.getPrice() != null && medicine.getStock() > 0) {
                    BigDecimal itemValue = medicine.getPrice().multiply(BigDecimal.valueOf(medicine.getStock()));
                    totalInventoryValue = totalInventoryValue.add(itemValue);
                }
                if (medicine.getStock() <= 10) {
                    lowStockCount++;
                }
                if (medicine.getExpiryDate() != null && medicine.getExpiryDate().isBefore(today)) {
                    expiredCount++;
                }
            }

            List<Prescription> pendingPrescriptions = prescriptionService.getPendingPrescriptions();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalInventoryValue", totalInventoryValue);
            stats.put("medicineCount", activeMedicines.size());
            stats.put("criticalCount", lowStockCount + expiredCount);
            stats.put("pendingPrescriptionsCount", pendingPrescriptions.size());

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            // Log the exception for debugging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"success\": false, \"message\": \"Error fetching dashboard stats\"}");
        }
    }

    /**
     * Handles the submission of the new medicine form.
     */
    @PostMapping("/medicines/create")
    public String addMedicine(@Valid @ModelAttribute("medicine") Medicine medicine,
                              BindingResult result,
                              @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                              @RequestParam(value = "imageUrl", required = false) String imageUrl,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        if (result.hasErrors()) {
            addCommonAttributes(model);
            return "home/pharmacist/create_medicine";
        }
        try {
            String imagePath = handleImageUpload(imageFile, imageUrl);
            if (imagePath != null && !imagePath.trim().isEmpty()) {
                medicine.setImage(imagePath);
            }
            if (medicine.getRequiresPrescription() == null) {
                medicine.setRequiresPrescription(false);
            }
            medicineService.saveMedicine(medicine);
            redirectAttributes.addFlashAttribute("successMessage", "Medicine added successfully!");
            return "redirect:/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error adding medicine: " + e.getMessage());
            return "redirect:/pharmacist/medicines/create";
        }
    }

    /**
     * Handles image file upload or URL selection.
     */
    private String handleImageUpload(MultipartFile imageFile, String imageUrl) throws IOException {
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            return imageUrl.trim();
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = StringUtils.cleanPath(imageFile.getOriginalFilename());
            String fileExtension = "";
            if (originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            Path targetPath = uploadPath.resolve(uniqueFilename);
            Files.copy(imageFile.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/medicines/" + uniqueFilename;
        }
        return null;
    }

    @GetMapping("/profile")
    public String viewProfile(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        try {
            User user = userService.getUserByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Pharmacist user not found"));

            // Use the "user" object directly, but name it "pharmacist" for the form
            model.addAttribute("pharmacist", user);
            addCommonAttributes(model);
            return "home/pharmacist/profile";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error loading profile: " + e.getMessage());
            return "home/error";
        }
    }

    // <<< ADDED: Endpoint to handle the profile update submission >>>
    @PostMapping("/profile/edit")
    public String updateProfile(@ModelAttribute("pharmacist") User userDetails,
                                RedirectAttributes redirectAttributes,
                                Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        try {
            User currentUser = userService.getUserByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Use the existing updateUser service which correctly handles all fields
            userService.updateUser(currentUser.getId(), userDetails);

            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
            return "redirect:/pharmacist/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating profile: " + e.getMessage());
            return "redirect:/pharmacist/profile";
        }
    }

    /**
     * Helper method to add common model attributes.
     */
    private void addCommonAttributes(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            Optional<User> userOpt = userService.getUserByUsername(auth.getName());
            userOpt.ifPresent(user -> model.addAttribute("user", user));
        }
    }

    /**
     * ADDED: Displays the new reports page for the pharmacist.
     */
    @GetMapping("/reports")
    public String showReportsPage(Model model) {
        addCommonAttributes(model);
        return "home/pharmacist/pharmacist-reports";
    }

    /**
     * ADDED: Handles the generation of reports for download or email.
     */
    @PostMapping("/reports/generate")
    public Object generateAndEmailReport(@RequestParam String reportType,
                                         @RequestParam String action,
                                         Principal principal,
                                         RedirectAttributes redirectAttributes) {
        try {
            byte[] pdfData;

            if ("inventory".equals(reportType)) {
                pdfData = reportService.generateInventoryReportPdf();
            } else {
                throw new IllegalArgumentException("Invalid report type specified.");
            }

            String fileName = "Inventory-Report-" + LocalDate.now() + ".pdf";

            if ("download".equals(action)) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDispositionFormData("attachment", fileName);
                return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);
            } else if ("email".equals(action)) {
                User user = userService.getUserByUsername(principal.getName())
                        .orElseThrow(() -> new RuntimeException("Current user not found."));

                emailService.sendReportByEmail(user.getEmail(), "Your Pharmacy Inventory Report", fileName, pdfData);

                redirectAttributes.addFlashAttribute("successMessage", "The report has been sent to your email address: " + user.getEmail());
                return "redirect:/pharmacist/reports";
            }

            throw new IllegalArgumentException("Invalid action specified.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error generating report: " + e.getMessage());
            return "redirect:/pharmacist/reports";
        }
    }
}