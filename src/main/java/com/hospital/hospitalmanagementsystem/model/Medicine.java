package com.hospital.hospitalmanagementsystem.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "medicines")
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "Medicine name is required")
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "generic_name")
    private String generic_name;

    @Column(name = "brand")
    private String brand;

    @Column(name = "category")
    private String category;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock cannot be negative")
    @Column(name = "stock", nullable = false)
    private Integer stock;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "dosage")
    private String dosage;

    @Column(name = "form")
    private String form;

    @Column(name = "manufacturer")
    private String manufacturer;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "prescription_required", columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean prescription_required = false;

    @Column(name = "side_effects", columnDefinition = "TEXT")
    private String side_effects;

    @Column(name = "contraindications", columnDefinition = "TEXT")
    private String contraindications;

    @Column(name = "image")
    private String image;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "dosage_form")
    private String dosageForm;

    @Column(name = "requires_prescription", columnDefinition = "BIT DEFAULT 0")
    private Boolean requiresPrescription = false;

    @Column(name = "storage_instructions")
    private String storageInstructions;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods for field compatibility
    public String getGenericName() {
        return this.generic_name;
    }

    public void setGenericName(String genericName) {
        this.generic_name = genericName;
    }

    public String getSideEffects() {
        return this.side_effects;
    }

    public void setSideEffects(String sideEffects) {
        this.side_effects = sideEffects;
    }

    // Fix for compilation error - Add the missing method
    public boolean isRequiresPrescription() {
        if (this.requiresPrescription != null) {
            return this.requiresPrescription;
        }
        if (this.prescription_required != null) {
            return this.prescription_required;
        }
        return false;
    }

    public void setRequiresPrescription(boolean requiresPrescription) {
        this.requiresPrescription = requiresPrescription;
        this.prescription_required = requiresPrescription;
    }

    public Boolean getPrescriptionRequired() {
        return this.prescription_required;
    }

    public void setPrescriptionRequired(Boolean prescriptionRequired) {
        this.prescription_required = prescriptionRequired;
        this.requiresPrescription = prescriptionRequired;
    }

    public Boolean getRequiresPrescription() {
        return this.requiresPrescription;
    }

    @PrePersist
    public void prePersist() {
        if (this.prescription_required == null) {
            this.prescription_required = false;
        }
        if (this.requiresPrescription == null) {
            this.requiresPrescription = false;
        }
        if (this.stock == null) {
            this.stock = 0;
        }
    }
}