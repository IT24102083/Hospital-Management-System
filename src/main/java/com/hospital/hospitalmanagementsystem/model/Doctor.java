package com.hospital.hospitalmanagementsystem.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.Pattern;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "doctors")
@PrimaryKeyJoinColumn(name = "user_id")
public class Doctor extends User {

    private String specialization;

    @Pattern(regexp = "^[a-zA-Z.,\\s]*$", message = "Qualification must not contain numbers")
    private String qualification;

    private String licenseNumber;

    private String experience;

    private String biography;

    @Pattern(regexp = "^[0-9.]*$", message = "Consultation fee must contain only numbers")
    private String consultationFee;

    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Appointment> appointments = new ArrayList<>();

    @OneToMany(mappedBy = "doctor") // FIX: Changed "mappedby" to "mappedBy"
    @JsonIgnore
    private List<DoctorAvailability> availabilities = new ArrayList<>();

    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<MedicalRecord> medicalRecords = new ArrayList<>();

    @Override
    public String toString() {
        return "Doctor{" +
                "id=" + getId() +
                ", firstName='" + getFirstName() + '\'' +
                ", lastName='" + getLastName() + '\'' +
                ", specialization='" + specialization + '\'' +
                '}';
    }
}