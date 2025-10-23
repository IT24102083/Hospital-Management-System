package com.hospital.hospitalmanagementsystem.service;

import com.hospital.hospitalmanagementsystem.model.Appointment;
import com.hospital.hospitalmanagementsystem.model.Doctor;
import com.hospital.hospitalmanagementsystem.model.MedicalRecord;
import com.hospital.hospitalmanagementsystem.model.Patient;

import java.util.List;
import java.util.Optional;

public interface MedicalRecordService {
    Optional<MedicalRecord> findById(Long id);
    List<MedicalRecord> findByPatient(Patient patient);
    List<MedicalRecord> findByDoctor(Doctor doctor);
    MedicalRecord save(MedicalRecord medicalRecord);

    /**
     * NOTIFICATION: Added this method to the service interface.
     * This allows the DoctorController to find a medical record associated with a specific appointment.
     * @param appointment The appointment to search by.
     * @return An Optional containing the medical record if found.
     */
    Optional<MedicalRecord> findByAppointment(Appointment appointment);

    /**
     * Counts the number of medical records for a given patient.
     * @param patient The patient.
     * @return The count of medical records.
     */
    long countByPatient(Patient patient);

    void deleteById(Long id);

}

