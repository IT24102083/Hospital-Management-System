package com.hospital.hospitalmanagementsystem.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    @NotNull(message = "Please select a doctor.")
    private Doctor doctor;

    @NotNull(message = "Please select a date.")
    private LocalDate appointmentDate;

    @NotNull(message = "Please select a time.")
    @DateTimeFormat(pattern = "HH:mm:ss", fallbackPatterns = { "HH:mm" })
    private LocalTime appointmentTime;

    private String reason;

    @Enumerated(EnumType.STRING)
    private Status status = Status.SCHEDULED;

    private String notes;

    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL)
    private List<Payment> payments;

    @OneToOne(mappedBy = "appointment", cascade = CascadeType.ALL)
    private MedicalRecord medicalRecord;

    public enum Status {
        SCHEDULED, COMPLETED, CANCELLED, NO_SHOW
    }

    @Override
    public String toString() {
        return "Appointment{" +
                "id=" + id +
                ", patientId=" + (patient != null ? patient.getId() : null) +
                ", doctorId=" + (doctor != null ? doctor.getId() : null) +
                ", appointmentDate=" + appointmentDate +
                ", appointmentTime=" + appointmentTime +
                ", reason='" + reason + '\'' +
                ", status=" + status +
                ", notes='" + notes + '\'' +
                '}';
    }
}