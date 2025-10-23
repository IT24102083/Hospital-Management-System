package com.hospital.hospitalmanagementsystem.controller;

import com.hospital.hospitalmanagementsystem.model.Invoice;
import com.hospital.hospitalmanagementsystem.model.Patient;
import com.hospital.hospitalmanagementsystem.model.User;
import com.hospital.hospitalmanagementsystem.service.PatientService;
import com.hospital.hospitalmanagementsystem.service.PharmacyOrderService;
import com.hospital.hospitalmanagementsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class PharmacyController {

    @Autowired
    private PharmacyOrderService pharmacyOrderService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private UserService userService;

    // --- REMOVED ---
    // The @GetMapping("/pharmacy") method was removed from this controller
    // to resolve the "Ambiguous mapping" conflict with HomeController.
    // HomeController will now be solely responsible for displaying the initial pharmacy page.

    @PostMapping("/pharmacy/checkout")
    @PreAuthorize("hasRole('PATIENT')")
    @ResponseBody // Important: This endpoint returns data, not a view
    public ResponseEntity<?> processCheckout(@RequestBody List<PharmacyOrderService.CartItemDTO> cartItems, Principal principal) {
        try {
            // Get the logged-in patient
            User user = userService.getUserByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Patient patient = patientService.getPatientById(user.getId())
                    .orElseThrow(() -> new RuntimeException("Patient not found"));

            Invoice createdInvoice = pharmacyOrderService.createOrderAndInvoiceFromCart(patient, cartItems);

            // Return the URL for redirection
            String redirectUrl = "/patient/invoices/" + createdInvoice.getId() + "/pay";
            return ResponseEntity.ok(Map.of("redirectUrl", redirectUrl));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}