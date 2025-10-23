package com.hospital.hospitalmanagementsystem.controller;

// Add these imports for logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hospital.hospitalmanagementsystem.model.Medicine;
import com.hospital.hospitalmanagementsystem.model.User;
import com.hospital.hospitalmanagementsystem.service.MedicineService;
import com.hospital.hospitalmanagementsystem.service.UserService;
import org.springframework.http.HttpStatus;
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

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/medicines")
public class MedicineController {

    // Add this logger instance for debugging
    private static final Logger LOGGER = LoggerFactory.getLogger(MedicineController.class);

    private final MedicineService medicineService;
    private final UserService userService;
    private final String uploadDir = "uploads/medicines";

    public MedicineController(MedicineService medicineService, UserService userService) {
        this.medicineService = medicineService;
        this.userService = userService;
    }

    @GetMapping("/edit/{id}")
    public String editMedicineForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Medicine> medicineOpt = medicineService.getMedicineById(id);
        if (medicineOpt.isPresent()) {
            model.addAttribute("medicine", medicineOpt.get());
            addCommonAttributes(model);
            return "home/pharmacist/edit_medicine";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Medicine not found!");
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/edit/{id}")
    public String updateMedicine(@PathVariable Long id,
                                 @Valid @ModelAttribute("medicine") Medicine medicineDetails,
                                 BindingResult result,
                                 @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                 @RequestParam(value = "imageUrl", required = false) String imageUrl,
                                 @RequestParam(value = "imageOption", required = false) String imageOption,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (result.hasErrors()) {
            addCommonAttributes(model);
            model.addAttribute("medicine", medicineDetails);
            return "home/pharmacist/edit_medicine";
        }
        Optional<Medicine> medicineOpt = medicineService.getMedicineById(id);
        if (!medicineOpt.isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Medicine not found!");
            return "redirect:/dashboard";
        }

        try {
            Medicine existingMedicine = medicineOpt.get();
            // Update all fields from the form
            existingMedicine.setName(medicineDetails.getName());
            existingMedicine.setGenericName(medicineDetails.getGenericName());
            existingMedicine.setBrand(medicineDetails.getBrand());
            existingMedicine.setCategory(medicineDetails.getCategory());
            existingMedicine.setPrice(medicineDetails.getPrice());
            existingMedicine.setStock(medicineDetails.getStock());
            existingMedicine.setDescription(medicineDetails.getDescription());
            existingMedicine.setDosage(medicineDetails.getDosage());
            existingMedicine.setForm(medicineDetails.getForm());
            existingMedicine.setDosageForm(medicineDetails.getDosageForm());
            existingMedicine.setManufacturer(medicineDetails.getManufacturer());
            existingMedicine.setExpiryDate(medicineDetails.getExpiryDate());
            existingMedicine.setRequiresPrescription(medicineDetails.isRequiresPrescription());
            existingMedicine.setSideEffects(medicineDetails.getSideEffects());
            existingMedicine.setContraindications(medicineDetails.getContraindications());
            existingMedicine.setStorageInstructions(medicineDetails.getStorageInstructions());

            // Handle image based on user's choice
            if ("upload".equals(imageOption) && imageFile != null && !imageFile.isEmpty()) {
                existingMedicine.setImage(handleImageUpload(imageFile));
            } else if ("url".equals(imageOption) && imageUrl != null && !imageUrl.trim().isEmpty()) {
                existingMedicine.setImage(imageUrl.trim());
            }

            medicineService.saveMedicine(existingMedicine);
            redirectAttributes.addFlashAttribute("successMessage", "Medicine updated successfully!");
            return "redirect:/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating medicine: " + e.getMessage());
            return "redirect:/medicines/edit/" + id;
        }
    }

    @PostMapping("/deactivate/{id}")
    @ResponseBody
    public ResponseEntity<?> deactivateMedicineAjax(@PathVariable Long id) {
        try {
            medicineService.deactivateMedicine(id);
            return ResponseEntity.ok().body("{\"success\": true, \"message\": \"Medicine deactivated successfully\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"success\": false, \"message\": \"Error deactivating medicine: " + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/reactivate/{id}")
    @ResponseBody
    public ResponseEntity<?> reactivateMedicineAjax(@PathVariable Long id) {
        try {
            medicineService.reactivateMedicine(id);
            return ResponseEntity.ok().body("{\"success\": true, \"message\": \"Medicine reactivated successfully\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"success\": false, \"message\": \"Error reactivating medicine: " + e.getMessage() + "\"}");
        }
    }

    // ADDED: Endpoint to handle quick stock updates from the dashboard
    @PostMapping("/{id}/update-stock")
    @ResponseBody
    public ResponseEntity<?> updateStock(@PathVariable Long id, @RequestBody Integer newStock) {
        LOGGER.info("Received request to update stock for medicine ID: {} to new stock: {}", id, newStock);
        try {
            if (newStock < 0) {
                LOGGER.warn("Attempted to set negative stock for medicine ID: {}", id);
                return ResponseEntity.badRequest().body("{\"success\": false, \"message\": \"Stock cannot be negative.\"}");
            }
            Medicine updatedMedicine = medicineService.updateMedicineStock(id, newStock);
            LOGGER.info("Successfully updated stock for medicine ID: {}", id);
            return ResponseEntity.ok(updatedMedicine);
        } catch (Exception e) {
            // This will log any error that occurs
            LOGGER.error("Error updating stock for medicine ID: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    private String handleImageUpload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String uniqueFilename = UUID.randomUUID().toString() + "." + StringUtils.getFilenameExtension(file.getOriginalFilename());
        Path targetPath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/medicines/" + uniqueFilename;
    }

    private void addCommonAttributes(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            Optional<User> userOpt = userService.getUserByUsername(auth.getName());
            userOpt.ifPresent(user -> model.addAttribute("user", user));
        }
    }
}