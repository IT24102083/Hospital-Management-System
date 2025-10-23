package com.hospital.hospitalmanagementsystem.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "prescription_items")
public class PrescriptionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    /**
     * Stores the name of the medicine as written by the doctor.
     * This is what the pharmacist sees initially.
     */
    @Column(name = "medicine_name", nullable = false)
    private String medicineName;

    /**
     * The actual medicine from inventory, linked by the pharmacist during fulfillment.
     * It is nullable because it's empty when the prescription is first created.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id", nullable = true) // FIXED: Now nullable
    private Medicine medicine;

    private String dosage;

    private String frequency;

    private String duration;

    private String specialInstructions;

    private int quantity;
}