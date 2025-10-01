package com.hospital.hospitalmanagementsystem.service;

import com.hospital.hospitalmanagementsystem.model.*;
import com.hospital.hospitalmanagementsystem.repository.MedicineRepository;
import com.hospital.hospitalmanagementsystem.repository.PrescriptionItemRepository;
import com.hospital.hospitalmanagementsystem.repository.PrescriptionRepository;
import com.hospital.hospitalmanagementsystem.repository.MedicalRecordRepository; // Ensure you have this repository
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final MedicineRepository medicineRepository;
    private final MedicalRecordRepository medicalRecordRepository;

    public PrescriptionService(PrescriptionRepository prescriptionRepository,
                               PrescriptionItemRepository prescriptionItemRepository,
                               MedicineRepository medicineRepository,
                               MedicalRecordRepository medicalRecordRepository) {
        this.prescriptionRepository = prescriptionRepository;
        this.prescriptionItemRepository = prescriptionItemRepository;
        this.medicineRepository = medicineRepository;
        this.medicalRecordRepository = medicalRecordRepository;
    }

    /**
     * Creates a new prescription and sets its status to PENDING.
     * @param prescription The prescription object with instructions.
     * @param user The patient submitting the prescription.
     * @param filePath The path to the uploaded prescription file.
     */
    @Transactional
    public void createPrescription(Prescription prescription, User user, String filePath) {
        // **FIXED**: Changed to findByPatient_Id
        MedicalRecord medicalRecord = medicalRecordRepository.findByPatient_Id(user.getId())
                .orElseThrow(() -> new RuntimeException("Medical record not found for user: " + user.getUsername()));

        prescription.setMedicalRecord(medicalRecord);
        prescription.setStatus(Prescription.Status.PENDING); // Set initial status
        // prescription.setFilePath(filePath); // Uncomment if you add a filePath field to Prescription

        prescriptionRepository.save(prescription);
    }

    /**
     * Retrieves all prescriptions for a specific patient.
     * @param user The patient whose prescriptions are to be retrieved.
     * @return A list of prescriptions.
     */
    public List<Prescription> getPrescriptionsForPatient(User user) {
        // **FIXED**: Changed to findByPatient_Id
        MedicalRecord medicalRecord = medicalRecordRepository.findByPatient_Id(user.getId())
                .orElseThrow(() -> new RuntimeException("Medical record not found for user: " + user.getUsername()));
        return prescriptionRepository.findByMedicalRecord(medicalRecord);
    }

    /**
     * Retrieves all prescriptions that are pending fulfillment.
     * @return A list of pending prescriptions, ordered by date.
     */
    public List<Prescription> getPendingPrescriptions() {
        return prescriptionRepository.findByStatusOrderByPrescriptionDateAsc(Prescription.Status.PENDING);
    }

    /**
     * Retrieves a single prescription by its ID.
     * @param id The ID of the prescription.
     * @return An Optional containing the prescription if found.
     */
    public Optional<Prescription> getPrescriptionById(Long id) {
        return prescriptionRepository.findById(id);
    }

    /**
     * Saves a new prescription to the database, including all its items.
     * The operation is transactional to ensure data integrity.
     * @param prescription The prescription to be saved.
     * @return The saved Prescription entity with its generated ID.
     */
    @Transactional
    public Prescription savePrescription(Prescription prescription) {
        return prescriptionRepository.save(prescription);
    }

    /**
     * Retrieves all prescriptions issued by a specific doctor.
     * This relies on a custom @Query in the PrescriptionRepository.
     * @param doctor The doctor whose prescriptions are to be retrieved.
     * @return A list of prescriptions.
     */
    public List<Prescription> getPrescriptionsByDoctor(Doctor doctor) {
        return prescriptionRepository.findByDoctor(doctor);
    }

    /**
     * Fulfills a prescription by linking items to inventory, updating stock, and changing the status.
     * This operation is transactional; it will roll back if any step fails (e.g., insufficient stock).
     * @param prescriptionId The ID of the prescription to fulfill.
     * @param prescribedData A map from the web form containing the selected medicine for each item.
     */
    @Transactional
    public void fulfillPrescription(Long prescriptionId, Map<String, String> prescribedData) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found with ID: " + prescriptionId));

        if (prescription.getStatus() != Prescription.Status.PENDING) {
            throw new IllegalStateException("Only PENDING prescriptions can be fulfilled. Current status: " + prescription.getStatus());
        }

        for (PrescriptionItem item : prescription.getItems()) {
            String formInputKey = "item_" + item.getId() + "_medicineId";
            String medicineIdStr = prescribedData.get(formInputKey);

            if (medicineIdStr == null || medicineIdStr.trim().isEmpty()) {
                throw new IllegalArgumentException("No medicine selected for item: '" + item.getMedicineName() + "'");
            }

            Long medicineId = Long.parseLong(medicineIdStr);
            Medicine selectedMedicine = medicineRepository.findById(medicineId)
                    .orElseThrow(() -> new IllegalArgumentException("Medicine not found with ID: " + medicineId));

            // Check stock levels
            int requiredQuantity = item.getQuantity();
            if (selectedMedicine.getStock() < requiredQuantity) {
                throw new IllegalStateException("Insufficient stock for " + selectedMedicine.getName() +
                        ". Required: " + requiredQuantity + ", Available: " + selectedMedicine.getStock());
            }

            // Update stock
            selectedMedicine.setStock(selectedMedicine.getStock() - requiredQuantity);
            medicineRepository.save(selectedMedicine);

            // Link the selected medicine to the prescription item and save
            item.setMedicine(selectedMedicine);
            prescriptionItemRepository.save(item);
        }

        // Mark the entire prescription as fulfilled
        prescription.setStatus(Prescription.Status.FULFILLED);
        prescriptionRepository.save(prescription);
    }
}