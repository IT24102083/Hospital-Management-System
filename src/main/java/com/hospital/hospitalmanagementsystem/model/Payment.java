package com.hospital.hospitalmanagementsystem.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(unique = true)
    private String transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @NotNull
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than 0")
    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.PENDING;

    @CreationTimestamp
    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Additional fields for bank transfer verification
    @Column(name = "reference_number")
    private String referenceNumber;

    @Column(name = "bank_slip_path")
    private String bankSlipPath;

    @Column(name = "verification_notes", columnDefinition = "TEXT")
    private String verificationNotes;

    @Column(name = "verified_by")
    private String verifiedBy;

    @Column(name = "verification_date")
    private LocalDateTime verificationDate;

    // Enums
    public enum PaymentMethod {
        CREDIT_CARD("Credit Card"),
        DEBIT_CARD("Debit Card"),
        BANK_TRANSFER("Bank Transfer"),
        CASH("Cash"),
        CHECK("Check"),
        INSURANCE("Insurance");

        private final String displayName;

        PaymentMethod(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum PaymentStatus {
        PENDING("Pending"),
        COMPLETED("Completed"),
        FAILED("Failed"),
        CANCELLED("Cancelled"),
        REFUNDED("Refunded");

        private final String displayName;

        PaymentStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Inner classes for statistics and requests (replacing DTOs)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Statistics {
        private long totalCount;
        private long successfulCount;
        private long failedCount;
        private long pendingCount;
        private long refundedCount;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private BigDecimal averageAmount = BigDecimal.ZERO;
        private BigDecimal largestPayment = BigDecimal.ZERO;
        private BigDecimal smallestPayment = BigDecimal.ZERO;
        private Map<String, PaymentMethodStats> paymentMethodBreakdown;
        private BigDecimal processingSuccessRate = BigDecimal.ZERO;
        private int averageProcessingTimeMinutes;
        private int totalBankSlipsVerified;
        private int pendingVerifications;
        private BigDecimal totalRefunds = BigDecimal.ZERO;
        private int refundCount;
        private BigDecimal averageRefundAmount = BigDecimal.ZERO;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethodStats {
        private String method;
        private long count;
        private BigDecimal totalAmount;
        private BigDecimal averageAmount;
        private BigDecimal successRate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotNull
        private Long invoiceId;
        @NotNull
        @DecimalMin(value = "0.01")
        private BigDecimal amount;
        @NotNull
        private String paymentMethod;
        private String cardNumber;
        private String cardName;
        private String cardExpiry;
        private String cardCvc;
        private String referenceNumber;
        private String bankName;
        private String billingAddress;
        private String billingCity;
        private String billingZip;
        private String notes;
        private String cardToken;
        private Boolean saveCard = false;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerificationRequest {
        @NotNull
        private String verificationAction; // "approve", "reject", "partial"
        @DecimalMin(value = "0.01")
        private BigDecimal verifiedAmount;
        private String verificationNotes;
        @NotNull
        private Long verifierId;
        private String verifierName;
        private String bankReference;
        private String actualBankName;
        private String transferDate;
        private String rejectionReason;
        private String rejectionCategory;
        private Boolean requiresManagerApproval = false;
        private String managerComments;
    }

    @Override
    public String toString() {
        return "Payment{" +
                "id=" + id +
                ", transactionId='" + transactionId + '\'' +
                ", invoiceId=" + (invoice != null ? invoice.getId() : null) +
                ", patientId=" + (patient != null ? patient.getId() : null) +
                ", amount=" + amount +
                ", paymentMethod=" + paymentMethod +
                ", status=" + status +
                ", paymentDate=" + paymentDate +
                ", notes='" + notes + '\'' +
                '}';
    }
}