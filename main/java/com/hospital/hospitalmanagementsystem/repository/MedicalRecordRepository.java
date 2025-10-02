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
    Optional<MedicalRecord> findByPatient_Id(Long patientId);

    /**
     * Finds a medical record by the associated Appointment.
     * This is a more direct way to link records to a specific consultation.
     * @param appointment The Appointment entity.
     * @return An Optional containing the MedicalRecord if found.
     */
    Optional<MedicalRecord> findByAppointment(Appointment appointment);
}