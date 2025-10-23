package com.hospital.hospitalmanagementsystem.repository;

import com.hospital.hospitalmanagementsystem.model.Doctor;
import com.hospital.hospitalmanagementsystem.model.DoctorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, Long> {
    List<DoctorAvailability> findByDoctor(Doctor doctor);
    List<DoctorAvailability> findByDoctorAndAvailableDate(Doctor doctor, LocalDate date);
    List<DoctorAvailability> findByAvailableDateAndAvailable(LocalDate date, boolean available);

    @Query("SELECT da.availableDate FROM DoctorAvailability da WHERE da.doctor = :doctor AND da.available = true AND da.availableDate >= :startDate")
    List<LocalDate> findAvailableDatesByDoctor(@Param("doctor") Doctor doctor, @Param("startDate") LocalDate startDate);
}