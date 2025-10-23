package com.hospital.hospitalmanagementsystem.service;

import com.hospital.hospitalmanagementsystem.model.Pharmacist;
import com.hospital.hospitalmanagementsystem.repository.PharmacistRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PharmacistService {

    private final PharmacistRepository pharmacistRepository;

    public PharmacistService(PharmacistRepository pharmacistRepository) {
        this.pharmacistRepository = pharmacistRepository;
    }

    public Optional<Pharmacist> findById(Long id) {
        return pharmacistRepository.findById(id);
    }

    public Pharmacist save(Pharmacist pharmacist) {
        return pharmacistRepository.save(pharmacist);
    }
}