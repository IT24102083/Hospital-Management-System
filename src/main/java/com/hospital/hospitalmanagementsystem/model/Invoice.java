package com.hospital.hospitalmanagementsystem.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "invoices")
public class Invoice {

    // ... All of your existing fields and methods from Id to recalculateTotals() remain the same ...
    // --- Fields ---
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(unique = true, name = "invoice_number")
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Size(max = 500)
    private String description;

    @NotNull
    @Column(name = "issue_date")
    private LocalDate issueDate;

    @NotNull
    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "service_date")
    private LocalDate serviceDate;

    @NotNull
    @DecimalMin(value = "0.00", message = "Subtotal must be non-negative")
    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @DecimalMin(value = "0.00", message = "Tax must be non-negative")
    @Column(precision = 10, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;

    @DecimalMin(value = "0.00", message = "Discount must be non-negative")
    @Column(precision = 10, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.00", message = "Total must be non-negative")
    @Column(precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @DecimalMin(value = "0.00", message = "Amount paid must be non-negative")
    @Column(name = "amount_paid", precision = 10, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @DecimalMin(value = "0.00", message = "Balance due must be non-negative")
    @Column(name = "balance_due", precision = 10, scale = 2)
    private BigDecimal balanceDue = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status = InvoiceStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<InvoiceItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Payment> payments = new ArrayList<>();

    @OneToOne(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PaymentPlan paymentPlan;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // --- Helper Methods ---
    public void addItem(InvoiceItem item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
        item.setInvoice(this);
        recalculateTotals();
    }

    public void removeItem(InvoiceItem item) {
        if (this.items != null) {
            this.items.remove(item);
            item.setInvoice(null);
            recalculateTotals();
        }
    }

    public void recalculateTotals() {
        this.subtotal = getItems().stream()
                .map(InvoiceItem::getLineTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.total = this.subtotal.add(this.tax).subtract(this.discount);
        this.balanceDue = this.total.subtract(this.amountPaid);
        if (this.total.compareTo(BigDecimal.ZERO) < 0) this.total = BigDecimal.ZERO;
        if (this.balanceDue.compareTo(BigDecimal.ZERO) < 0) this.balanceDue = BigDecimal.ZERO;
    }


    // --- Enums and Inner DTO Classes ---

    public enum InvoiceStatus { DRAFT, PENDING, SENT, PAID, PARTIALLY_PAID, OVERDUE, CANCELLED, REFUNDED }

    // UPDATED: Removed @AllArgsConstructor since the class has no fields yet.
    @NoArgsConstructor
    public static class InvoiceStatistics {
        // You can add fields for statistics later, like totalBilled, totalCollected, etc.
    }

    // UPDATED: Added fields to this class, which resolves the constructor conflict.
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotNull
        private Long patientId;
        private Long appointmentId;
        private String description;
        private LocalDate dueDate;
        private List<InvoiceItemRequest> items;
    }

    // A helper DTO for the items within the CreateRequest
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceItemRequest {
        private String description;
        private int quantity;
        private BigDecimal unitPrice;
    }

    @Override
    public String toString() {
        return "Invoice{" +
                "id=" + id +
                ", invoiceNumber='" + invoiceNumber + '\'' +
                ", patientId=" + (patient != null ? patient.getId() : null) +
                ", total=" + total +
                ", status=" + status +
                '}';
    }
}