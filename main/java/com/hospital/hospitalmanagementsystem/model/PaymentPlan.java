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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment_plans")
public class PaymentPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(unique = true, name = "plan_number")
    private String planNumber;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @NotNull
    @DecimalMin(value = "0.01", message = "Total amount must be greater than 0")
    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @NotNull
    @DecimalMin(value = "0.01", message = "Monthly payment must be greater than 0")
    @Column(name = "monthly_payment", precision = 10, scale = 2)
    private BigDecimal monthlyPayment;

    @NotNull
    @Min(value = 1, message = "Number of payments must be at least 1")
    @Column(name = "number_of_payments")
    private Integer numberOfPayments;

    @NotNull
    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private PaymentPlanStatus status = PaymentPlanStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private Payment.PaymentMethod paymentMethod;

    @Column(name = "auto_pay_enabled")
    private Boolean autoPayEnabled = false;

    @DecimalMin(value = "0.00", message = "Interest rate must be non-negative")
    @Column(name = "interest_rate", precision = 5, scale = 4)
    private BigDecimal interestRate = BigDecimal.ZERO;

    @Column(name = "payments_made")
    private Integer paymentsMade = 0;

    @DecimalMin(value = "0.00", message = "Amount paid must be non-negative")
    @Column(name = "amount_paid", precision = 10, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @DecimalMin(value = "0.00", message = "Remaining balance must be non-negative")
    @Column(name = "remaining_balance", precision = 10, scale = 2)
    private BigDecimal remainingBalance;

    @Column(name = "next_payment_date")
    private LocalDate nextPaymentDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "paymentPlan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PaymentPlanInstallment> installments = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enums
    public enum PaymentPlanStatus {
        ACTIVE("Active"),
        COMPLETED("Completed"),
        SUSPENDED("Suspended"),
        CANCELLED("Cancelled"),
        DEFAULTED("Defaulted");

        private final String displayName;

        PaymentPlanStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Inner classes for requests and statistics (replacing DTOs)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotNull
        @Min(value = 2)
        private Integer numberOfInstallments;
        @NotNull
        private LocalDate startDate;
        @DecimalMin(value = "0.00")
        private BigDecimal interestRate = BigDecimal.ZERO;
        @NotNull
        private String paymentMethod;
        private Boolean autoPayEnabled = false;
        private String notes;
        private String bankAccountNumber;
        private String routingNumber;
        private String cardToken;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Statistics {
        private long totalActivePlans;
        private long totalCompletedPlans;
        private long totalCancelledPlans;
        private long totalSuspendedPlans;
        private long overdueCount;
        private BigDecimal totalPlannedAmount = BigDecimal.ZERO;
        private BigDecimal totalAmountPaid = BigDecimal.ZERO;
        private BigDecimal totalRemainingBalance = BigDecimal.ZERO;
        private BigDecimal averageMonthlyPayment = BigDecimal.ZERO;
        private BigDecimal averagePlanAmount = BigDecimal.ZERO;
        private BigDecimal collectionRate = BigDecimal.ZERO;
        private BigDecimal completionRate = BigDecimal.ZERO;
        private BigDecimal defaultRate = BigDecimal.ZERO;
        private int averagePlanDuration;
        private BigDecimal expectedMonthlyCollections = BigDecimal.ZERO;
        private BigDecimal actualMonthlyCollections = BigDecimal.ZERO;
        private List<MonthlyCollectionData> monthlyTrends;
        private Map<String, Integer> plansByDuration;
        private Map<String, BigDecimal> plansByAmount;
        private Map<String, Integer> plansByStatus;
        private int plansAtRisk;
        private BigDecimal potentialLoss = BigDecimal.ZERO;
        private List<RiskAnalysisData> riskBreakdown;
        private LocalDate startDate;
        private LocalDate endDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyCollectionData {
        private String month;
        private int year;
        private BigDecimal expectedAmount;
        private BigDecimal actualAmount;
        private BigDecimal collectionRate;
        private int activePlans;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskAnalysisData {
        private String riskLevel;
        private int planCount;
        private BigDecimal totalAmount;
        private String criteria;
    }

    @Override
    public String toString() {
        return "PaymentPlan{" +
                "id=" + id +
                ", planNumber='" + planNumber + '\'' +
                ", invoiceId=" + (invoice != null ? invoice.getId() : null) +
                ", patientId=" + (patient != null ? patient.getId() : null) +
                ", totalAmount=" + totalAmount +
                ", monthlyPayment=" + monthlyPayment +
                ", numberOfPayments=" + numberOfPayments +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", status=" + status +
                ", paymentMethod=" + paymentMethod +
                ", autoPayEnabled=" + autoPayEnabled +
                ", interestRate=" + interestRate +
                ", paymentsMade=" + paymentsMade +
                ", amountPaid=" + amountPaid +
                ", remainingBalance=" + remainingBalance +
                ", nextPaymentDate=" + nextPaymentDate +
                ", notes='" + notes + '\'' +
                ", installmentsCount=" + (installments != null ? installments.size() : 0) +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}