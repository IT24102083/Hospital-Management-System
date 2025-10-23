package com.hospital.hospitalmanagementsystem.service;

import com.hospital.hospitalmanagementsystem.model.Medicine;
import com.hospital.hospitalmanagementsystem.repository.MedicineRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class MedicineService {

    private final MedicineRepository medicineRepository;

    public MedicineService(MedicineRepository medicineRepository) {
        this.medicineRepository = medicineRepository;
    }

    public List<Medicine> getAllMedicines() {
        return medicineRepository.findAllByActiveTrue();
    }

    public Page<Medicine> getAllMedicinesPaged(Pageable pageable) {
        return medicineRepository.findAll(pageable);
    }

    public Optional<Medicine> getMedicineById(Long id) {
        return medicineRepository.findById(id);
    }

    public Medicine saveMedicine(Medicine medicine) {
        return medicineRepository.save(medicine);
    }

    public void deactivateMedicine(Long id) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Medicine not found with id: " + id));
        medicine.setActive(false);
        medicineRepository.save(medicine);
    }

    public void reactivateMedicine(Long id) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Medicine not found with id: " + id));
        medicine.setActive(true);
        medicineRepository.save(medicine);
    }

    public void deleteMedicine(Long id) {
        medicineRepository.deleteById(id);
    }

    public List<Medicine> getMedicinesByCategory(String category) {
        return medicineRepository.findByCategoryAndActiveTrue(category);
    }

    public List<Medicine> getExpiredMedicines() {
        return medicineRepository.findByExpiryDateBefore(LocalDate.now());
    }

    public List<Medicine> getLowStockMedicines(int minStock) {
        return medicineRepository.findByStockLessThan(minStock);
    }

    public List<Medicine> searchMedicinesByName(String name) {
        return medicineRepository.findByNameContainingIgnoreCase(name);
    }

    // Fixed method - now passing a single parameter to match the repository method
    public List<Medicine> searchMedicines(String term) {
        return medicineRepository.findByNameContainingOrGeneric_nameContainingOrBrandContaining(term);
    }

    public Page<Medicine> searchMedicines(String query, Pageable pageable) {
        return medicineRepository.findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(query, query, pageable);
    }

    public Medicine updateMedicineStock(Long id, Integer newStock) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Medicine not found with id: " + id));
        medicine.setStock(newStock);
        return medicineRepository.save(medicine);
    }
}