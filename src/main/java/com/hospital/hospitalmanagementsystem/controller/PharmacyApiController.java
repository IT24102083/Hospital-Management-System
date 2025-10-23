package com.hospital.hospitalmanagementsystem.controller;

import com.hospital.hospitalmanagementsystem.model.Medicine;
import com.hospital.hospitalmanagementsystem.service.MedicineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/medicines")
public class PharmacyApiController {

    private final MedicineService medicineService;

    public PharmacyApiController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    // Get all medicines
    @GetMapping
    public List<Medicine> getAllMedicines() {
        return medicineService.getAllMedicines();
    }

    // Get medicine by ID
    @GetMapping("/{id}")
    public ResponseEntity<Medicine> getMedicineById(@PathVariable Long id) {
        Optional<Medicine> medicine = medicineService.getMedicineById(id);
        return medicine.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Update medicine stock
    @PutMapping("/{id}/stock")
    public ResponseEntity<Medicine> updateMedicineStock(
            @PathVariable Long id,
            @RequestBody Integer newStock) {

        Optional<Medicine> medicineOpt = medicineService.getMedicineById(id);
        if (medicineOpt.isPresent()) {
            Medicine medicine = medicineOpt.get();
            medicine.setStock(newStock);
            medicineService.saveMedicine(medicine);
            return ResponseEntity.ok(medicine);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Search medicines by name, generic name, or brand
    @GetMapping("/search")
    public List<Medicine> searchMedicines(@RequestParam String term) {
        return medicineService.searchMedicines(term);
    }

    // Get medicines by category
    @GetMapping("/category/{category}")
    public List<Medicine> getMedicinesByCategory(@PathVariable String category) {
        return medicineService.getMedicinesByCategory(category);
    }
}