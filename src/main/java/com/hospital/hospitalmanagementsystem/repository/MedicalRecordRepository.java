package com.hospital.hospitalmanagementsystem.repository;

import com.hospital.hospitalmanagementsystem.model.Appointment;
import com.hospital.hospitalmanagementsystem.model.Doctor;
import com.hospital.hospitalmanagementsystem.model.MedicalRecord;
import com.hospital.hospitalmanagementsystem.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    List<MedicalRecord> findByPatient(Patient patient);
    List<MedicalRecord> findByDoctor(Doctor doctor);
    // In MedicalRecordRepository.java
    Optional<MedicalRecord> findByPatient_Id(Long patientId);
    List<MedicalRecord> findAllByPatient_Id(Long patientId);

    /**
     * Finds a medical record by the associated Appointment.
     * This is a more direct way to link records to a specific consultation.
     * @param appointment The Appointment entity.
     * @return An Optional containing the MedicalRecord if found.
     */
    Optional<MedicalRecord> findByAppointment(Appointment appointment);

    /**
     * Counts all medical records associated with a specific patient.
     * This is more efficient than fetching the entire list.
     * @param patient The patient entity to count records for.
     * @return The total number of medical records for the patient.
     */
    long countByPatient(Patient patient);
}