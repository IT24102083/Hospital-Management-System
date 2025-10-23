package com.hospital.hospitalmanagementsystem.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment_plan_installments")
public class PaymentPlanInstallment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_plan_id", nullable = false)
    private PaymentPlan paymentPlan;

    @NotNull
    @Min(value = 1, message = "Installment number must be at least 1")
    @Column(name = "installment_number")
    private Integer installmentNumber;

    @NotNull
    @Column(name = "due_date")
    private LocalDate dueDate;

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @DecimalMin(value = "0.00", message = "Amount paid must be non-negative")
    @Column(name = "amount_paid", precision = 10, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    private InstallmentStatus status = InstallmentStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "reminder_sent")
    private Boolean reminderSent = false;

    @Column(name = "reminder_sent_date")
    private LocalDateTime reminderSentDate;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enums
    public enum InstallmentStatus {
        PENDING("Pending"),
        PAID("Paid"),
        OVERDUE("Overdue"),
        PARTIAL("Partial"),
        CANCELLED("Cancelled");

        private final String displayName;

        InstallmentStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Override
    public String toString() {
        return "PaymentPlanInstallment{" +
                "id=" + id +
                ", paymentPlanId=" + (paymentPlan != null ? paymentPlan.getId() : null) +
                ", installmentNumber=" + installmentNumber +
                ", dueDate=" + dueDate +
                ", amount=" + amount +
                ", amountPaid=" + amountPaid +
                ", paymentDate=" + paymentDate +
                ", status=" + status +
                ", paymentId=" + (payment != null ? payment.getId() : null) +
                ", notes='" + notes + '\'' +
                ", reminderSent=" + reminderSent +
                ", reminderSentDate=" + reminderSentDate +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}