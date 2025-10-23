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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "invoices")
@SequenceGenerator(name="invoice_seq", initialValue=10001, allocationSize=1)
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    @NotNull
//    @Column(unique = true, name = "invoice_number")
//    private String invoiceNumber;

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
    private BigDecimal subtotal;

    @DecimalMin(value = "0.00", message = "Tax must be non-negative")
    @Column(precision = 10, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;

    @DecimalMin(value = "0.00", message = "Discount must be non-negative")
    @Column(precision = 10, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.00", message = "Total must be non-negative")
    @Column(precision = 10, scale = 2)
    private BigDecimal total;

    @DecimalMin(value = "0.00", message = "Amount paid must be non-negative")
    @Column(name = "amount_paid", precision = 10, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @DecimalMin(value = "0.00", message = "Balance due must be non-negative")
    @Column(name = "balance_due", precision = 10, scale = 2)
    private BigDecimal balanceDue;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status = InvoiceStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
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

    @PostPersist
    public void generateInvoiceNumber() {
        if (this.invoiceNumber == null && this.id != null) {
            String prefix = "INV";
            String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            String sequence = String.format("%05d", this.id);
            this.invoiceNumber = prefix + "-" + datePart + "-" + sequence;
        }
    }

    // Enums
    public enum InvoiceStatus {
        DRAFT("Draft"),
        PENDING("Pending"),
        SENT("Sent"),
        PAID("Paid"),
        PARTIALLY_PAID("Partially Paid"),
        OVERDUE("Overdue"),
        CANCELLED("Cancelled"),
        REFUNDED("Refunded");

        private final String displayName;

        InvoiceStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Inner classes for statistics (replacing DTOs)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceStatistics {
        private int totalInvoices;
        private int paidInvoices;
        private int pendingInvoices;
        private int overdueInvoices;
        private int cancelledInvoices;
        private BigDecimal totalRevenue = BigDecimal.ZERO;
        private BigDecimal outstandingAmount = BigDecimal.ZERO;
        private BigDecimal averageInvoiceAmount = BigDecimal.ZERO;
        private BigDecimal totalDiscountsApplied = BigDecimal.ZERO;
        private BigDecimal collectionRate = BigDecimal.ZERO;
        private int averageDaysToPayment;
        private BigDecimal monthlyRecurringRevenue = BigDecimal.ZERO;
        private BigDecimal current = BigDecimal.ZERO;
        private BigDecimal thirtyDays = BigDecimal.ZERO;
        private BigDecimal sixtyDays = BigDecimal.ZERO;
        private BigDecimal ninetyDaysPlus = BigDecimal.ZERO;
        private BigDecimal totalTaxCollected = BigDecimal.ZERO;
        private int activePaymentPlans;
        private BigDecimal paymentPlanRevenue = BigDecimal.ZERO;
    }

    // Inner class for creating invoices (replacing InvoiceCreateRequest DTO)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotNull
        private Long patientId;
        @NotNull
        private String description;
        @NotNull
        private LocalDate issueDate;
        @NotNull
        private LocalDate dueDate;
        private BigDecimal subtotal;
        private BigDecimal tax;
        private BigDecimal discount;
        private String notes;
    }

    @Override
    public String toString() {
        return "Invoice{" +
                "id=" + id +
                ", invoiceNumber='" + invoiceNumber + '\'' +
                ", patientId=" + (patient != null ? patient.getId() : null) +
                ", appointmentId=" + (appointment != null ? appointment.getId() : null) +
                ", issueDate=" + issueDate +
                ", dueDate=" + dueDate +
                ", total=" + total +
                ", amountPaid=" + amountPaid +
                ", balanceDue=" + balanceDue +
                ", status=" + status +
                '}';
    }
}