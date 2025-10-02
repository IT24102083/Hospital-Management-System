////package com.hospital.hospitalmanagementsystem.model;
////
////import lombok.AllArgsConstructor;
////import lombok.Data;
////import lombok.NoArgsConstructor;
////
////import javax.persistence.*;
////import java.math.BigDecimal;
////
////@Data
////@NoArgsConstructor
////@AllArgsConstructor
////@Entity
////@Table(name = "invoice_items")
////public class InvoiceItem {
////
////    @Id
////    @GeneratedValue(strategy = GenerationType.IDENTITY)
////    private Long id;
////
////    @ManyToOne
////    @JoinColumn(name = "invoice_id", nullable = false)
////    private Invoice invoice;
////
////    private String description;
////
////    private int quantity;
////
////    private BigDecimal unitPrice;
////
////    private BigDecimal amount;
////
////    @Enumerated(EnumType.STRING)
////    private ItemType itemType;
////
////    public enum ItemType {
////        CONSULTATION, MEDICINE, TEST, PROCEDURE, OTHER
////    }
////}
//
//package com.hospital.hospitalmanagementsystem.model;
//
//import javax.persistence.*;
//import javax.validation.constraints.DecimalMin;
//import javax.validation.constraints.Min;
//import javax.validation.constraints.NotNull;
//import javax.validation.constraints.Size;
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "invoice_items")
//public class InvoiceItem {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "invoice_id", nullable = false)
//    private Invoice invoice;
//
//    @NotNull
//    @Size(min = 1, max = 255, message = "Description must be between 1 and 255 characters")
//    private String description;
//
//    @Size(max = 100)
//    @Column(name = "service_code")
//    private String serviceCode;
//
//    @NotNull
//    @Min(value = 1, message = "Quantity must be at least 1")
//    private Integer quantity;
//
//    @NotNull
//    @DecimalMin(value = "0.00", message = "Unit price must be non-negative")
//    @Column(name = "unit_price", precision = 10, scale = 2)
//    private BigDecimal unitPrice;
//
//    @NotNull
//    @DecimalMin(value = "0.00", message = "Line total must be non-negative")
//    @Column(name = "line_total", precision = 10, scale = 2)
//    private BigDecimal lineTotal;
//
//    @Size(max = 500)
//    @Column(name = "item_notes")
//    private String itemNotes;
//
//    @Column(name = "created_at")
//    private LocalDateTime createdAt;
//
//    @Column(name = "updated_at")
//    private LocalDateTime updatedAt;
//
//    // Constructors
//    public InvoiceItem() {}
//
//    public InvoiceItem(Invoice invoice, String description, Integer quantity, BigDecimal unitPrice) {
//        this.invoice = invoice;
//        this.description = description;
//        this.quantity = quantity;
//        this.unitPrice = unitPrice;
//        calculateLineTotal();
//    }
//
//    // Utility methods
//    public void calculateLineTotal() {
//        if (quantity != null && unitPrice != null) {
//            this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
//        } else {
//            this.lineTotal = BigDecimal.ZERO;
//        }
//    }
//
//    @PrePersist
//    protected void onCreate() {
//        createdAt = LocalDateTime.now();
//        updatedAt = LocalDateTime.now();
//        calculateLineTotal();
//    }
//
//    @PreUpdate
//    protected void onUpdate() {
//        updatedAt = LocalDateTime.now();
//        calculateLineTotal();
//    }
//
//    // Getters and Setters
//    public Long getId() { return id; }
//    public void setId(Long id) { this.id = id; }
//
//    public Invoice getInvoice() { return invoice; }
//    public void setInvoice(Invoice invoice) { this.invoice = invoice; }
//
//    public String getDescription() { return description; }
//    public void setDescription(String description) { this.description = description; }
//
//    public String getServiceCode() { return serviceCode; }
//    public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }
//
//    public Integer getQuantity() { return quantity; }
//    public void setQuantity(Integer quantity) {
//        this.quantity = quantity;
//        calculateLineTotal();
//    }
//
//    public BigDecimal getUnitPrice() { return unitPrice; }
//    public void setUnitPrice(BigDecimal unitPrice) {
//        this.unitPrice = unitPrice;
//        calculateLineTotal();
//    }
//
//    public BigDecimal getLineTotal() { return lineTotal; }
//    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
//
//    public String getItemNotes() { return itemNotes; }
//    public void setItemNotes(String itemNotes) { this.itemNotes = itemNotes; }
//
//    public LocalDateTime getCreatedAt() { return createdAt; }
//    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
//
//    public LocalDateTime getUpdatedAt() { return updatedAt; }
//    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
//}

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
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "invoice_items")
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @NotNull
    @Size(min = 1, max = 255, message = "Description must be between 1 and 255 characters")
    private String description;

    @Size(max = 100)
    @Column(name = "service_code")
    private String serviceCode;

    @NotNull
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull
    @DecimalMin(value = "0.00", message = "Unit price must be non-negative")
    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @NotNull
    @DecimalMin(value = "0.00", message = "Line total must be non-negative")
    @Column(name = "line_total", precision = 10, scale = 2)
    private BigDecimal lineTotal;

    @Size(max = 500)
    @Column(name = "item_notes")
    private String itemNotes;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Override
    public String toString() {
        return "InvoiceItem{" +
                "id=" + id +
                ", invoiceId=" + (invoice != null ? invoice.getId() : null) +
                ", description='" + description + '\'' +
                ", serviceCode='" + serviceCode + '\'' +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", lineTotal=" + lineTotal +
                ", itemNotes='" + itemNotes + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}