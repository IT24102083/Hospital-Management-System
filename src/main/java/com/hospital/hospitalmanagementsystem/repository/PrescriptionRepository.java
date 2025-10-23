package com.hospital.hospitalmanagementsystem.repository;

import com.hospital.hospitalmanagementsystem.model.MedicalRecord;
import com.hospital.hospitalmanagementsystem.model.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.hospital.hospitalmanagementsystem.model.Doctor;
import com.hospital.hospitalmanagementsystem.model.Patient;

import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    List<Prescription> findByMedicalRecord(MedicalRecord medicalRecord);

    /**
     * Finds all prescriptions with a given status, ordered by the prescription date.
     * This replaces the old findByFulfilledFalse() method to match the updated Prescription entity.
     * @param status The status to search for (e.g., PENDING).
     * @return A list of prescriptions matching the status.
     */
    List<Prescription> findByStatusOrderByPrescriptionDateAsc(Prescription.Status status);

    @Query("SELECT p FROM Prescription p WHERE p.medicalRecord.doctor = :doctor")
    List<Prescription> findByDoctor(@Param("doctor") Doctor doctor);

    // In PrescriptionRepository.java
    List<Prescription> findByMedicalRecordIn(List<MedicalRecord> medicalRecords);
    long countByMedicalRecordIn(List<MedicalRecord> medicalRecords);

    @Query("SELECT count(p) FROM Prescription p WHERE p.medicalRecord.patient = :patient")
    long countByPatient(@Param("patient") Patient patient);

}