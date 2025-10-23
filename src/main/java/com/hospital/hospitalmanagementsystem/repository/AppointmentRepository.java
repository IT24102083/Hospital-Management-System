package com.hospital.hospitalmanagementsystem.repository;

import com.hospital.hospitalmanagementsystem.model.Appointment;
import com.hospital.hospitalmanagementsystem.model.Doctor;
import com.hospital.hospitalmanagementsystem.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatient(Patient patient);
    List<Appointment> findByDoctor(Doctor doctor);
    List<Appointment> findByDoctorAndAppointmentDate(Doctor doctor, LocalDate date);
    List<Appointment> findByPatientAndAppointmentDate(Patient patient, LocalDate date);
    List<Appointment> findByAppointmentDate(LocalDate date);
    List<Appointment> findAllByOrderByAppointmentDateDescAppointmentTimeDesc();
}