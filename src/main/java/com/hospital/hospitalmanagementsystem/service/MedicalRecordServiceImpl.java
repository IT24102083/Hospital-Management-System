package com.hospital.hospitalmanagementsystem.service;

import com.hospital.hospitalmanagementsystem.model.Appointment;
import com.hospital.hospitalmanagementsystem.model.Doctor;
import com.hospital.hospitalmanagementsystem.model.MedicalRecord;
import com.hospital.hospitalmanagementsystem.model.Patient;
import com.hospital.hospitalmanagementsystem.repository.MedicalRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;

    public MedicalRecordServiceImpl(MedicalRecordRepository medicalRecordRepository) {
        this.medicalRecordRepository = medicalRecordRepository;
    }

    @Override
    public Optional<MedicalRecord> findById(Long id) {
        return medicalRecordRepository.findById(id);
    }

    @Override
    public List<MedicalRecord> findByPatient(Patient patient) {
        return medicalRecordRepository.findByPatient(patient);
    }

    @Override
    public MedicalRecord save(MedicalRecord medicalRecord) {
        return medicalRecordRepository.save(medicalRecord);
    }

    @Override
    public List<MedicalRecord> findByDoctor(Doctor doctor) {
        return medicalRecordRepository.findByDoctor(doctor);
    }

    @Override
    public Optional<MedicalRecord> findByAppointment(Appointment appointment) {
        return medicalRecordRepository.findByAppointment(appointment);
    }

    @Override
    public long countByPatient(Patient patient) {
        return medicalRecordRepository.countByPatient(patient);
    }

    @Override
    public void deleteById(Long id) {
        // The JpaRepository already provides this method
        medicalRecordRepository.deleteById(id);
    }
}

