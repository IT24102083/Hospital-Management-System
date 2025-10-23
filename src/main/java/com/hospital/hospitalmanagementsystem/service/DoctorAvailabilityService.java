package com.hospital.hospitalmanagementsystem.service;

import com.hospital.hospitalmanagementsystem.model.Doctor;
import com.hospital.hospitalmanagementsystem.model.DoctorAvailability;
import com.hospital.hospitalmanagementsystem.repository.DoctorAvailabilityRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class DoctorAvailabilityService {

    private final DoctorAvailabilityRepository availabilityRepository;

    public DoctorAvailabilityService(DoctorAvailabilityRepository availabilityRepository) {
        this.availabilityRepository = availabilityRepository;
    }

    public DoctorAvailability saveAvailability(DoctorAvailability availability) {
        return availabilityRepository.save(availability);
    }

    public List<DoctorAvailability> getDoctorAvailabilities(Doctor doctor) {
        return availabilityRepository.findByDoctor(doctor);
    }

    public List<DoctorAvailability> getDoctorAvailabilitiesByDate(Doctor doctor, LocalDate date) {
        return availabilityRepository.findByDoctorAndAvailableDate(doctor, date);
    }

    public List<DoctorAvailability> getAvailableDoctorsByDate(LocalDate date) {
        return availabilityRepository.findByAvailableDateAndAvailable(date, true);
    }

    public Optional<DoctorAvailability> getAvailabilityById(Long id) {
        return availabilityRepository.findById(id);
    }

    public void deleteAvailability(Long id) {
        availabilityRepository.deleteById(id);
    }

    public List<LocalDate> getDoctorAvailabilityDates(Doctor doctor) {
        // Only return available dates from today onwards
        return availabilityRepository.findAvailableDatesByDoctor(doctor, LocalDate.now());
    }
}