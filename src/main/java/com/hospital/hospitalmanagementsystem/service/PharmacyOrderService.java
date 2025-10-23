package com.hospital.hospitalmanagementsystem.service;

import com.hospital.hospitalmanagementsystem.model.*;
import com.hospital.hospitalmanagementsystem.repository.MedicineRepository;
import com.hospital.hospitalmanagementsystem.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PharmacyOrderService {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private MedicineRepository medicineRepository;
    @Autowired
    private InvoiceService invoiceService;
    @Autowired
    private ReportService reportService;
    @Autowired
    private EmailService emailService;

    private static final Logger logger = LoggerFactory.getLogger(PharmacyOrderService.class);


    // A simple DTO class to receive cart data from the frontend
    public static class CartItemDTO {
        public Long medicineId;
        public int quantity;
    }

    @Transactional
    public Invoice createOrderAndInvoiceFromCart(Patient patient, List<CartItemDTO> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart cannot be empty.");
        }

        // 1. Create the Order
        Order order = new Order();
        order.setPatient(patient);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(Order.OrderStatus.PENDING_PAYMENT);
        order.setItems(new ArrayList<>());

        BigDecimal totalAmount = BigDecimal.ZERO;

        // 2. Process each item in the cart
        for (CartItemDTO itemDTO : cartItems) {
            Medicine medicine = medicineRepository.findById(itemDTO.medicineId)
                    .orElseThrow(() -> new RuntimeException("Medicine not found with ID: " + itemDTO.medicineId));

            // ** CRITICAL: Check and reduce stock **
            if (medicine.getStock() < itemDTO.quantity) {
                throw new RuntimeException("Insufficient stock for " + medicine.getName());
            }
            medicine.setStock(medicine.getStock() - itemDTO.quantity);
            medicineRepository.save(medicine);

            // Create OrderItem
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setMedicine(medicine);
            orderItem.setQuantity(itemDTO.quantity);
            orderItem.setPricePerItem(medicine.getPrice());
            BigDecimal lineTotal = medicine.getPrice().multiply(BigDecimal.valueOf(itemDTO.quantity));
            orderItem.setLineTotal(lineTotal);

            order.getItems().add(orderItem);
            totalAmount = totalAmount.add(lineTotal);
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        logger.info("New order #{} created for patient {}", savedOrder.getId(), patient.getFirstName());

        // 3. Generate an Invoice for this order
        Invoice invoice = new Invoice();
        invoice.setPatient(patient);
        invoice.setDescription("Pharmacy Order - #" + savedOrder.getId());
        invoice.setIssueDate(LocalDate.now());
        invoice.setDueDate(LocalDate.now().plusDays(7));
        invoice.setSubtotal(totalAmount);
        invoice.setTax(BigDecimal.ZERO);
        invoice.setDiscount(BigDecimal.ZERO);
        invoice.setTotal(totalAmount);
        invoice.setBalanceDue(totalAmount);
        invoice.setStatus(Invoice.InvoiceStatus.PENDING);

        // Create a corresponding list of InvoiceItems
        List<InvoiceItem> invoiceItems = new ArrayList<>();
        for (OrderItem orderItem : savedOrder.getItems()) {
            InvoiceItem invoiceItem = new InvoiceItem();

            // Set the parent invoice on the child item to satisfy the NOT NULL constraint.
            invoiceItem.setInvoice(invoice);

            invoiceItem.setDescription(orderItem.getMedicine().getName());
            invoiceItem.setQuantity(orderItem.getQuantity());
            invoiceItem.setUnitPrice(orderItem.getPricePerItem());
            invoiceItem.setLineTotal(orderItem.getLineTotal());
            invoiceItems.add(invoiceItem);
        }
        // Set the list of items on the invoice
        invoice.setItems(invoiceItems);

        // Save the invoice (which will also save the items due to CascadeType.ALL)
//        Invoice createdInvoice = invoiceService.saveInvoice(invoice);

        Invoice createdInvoice = invoiceService.createInvoice(invoice);


        // Link invoice back to order and save
        savedOrder.setInvoice(createdInvoice);
        orderRepository.save(savedOrder);

        try {
            byte[] pdfReport = reportService.generateOrderReceiptPdf(savedOrder);
            logger.info("PDF receipt generated for order #{}", savedOrder.getId());
            emailService.sendOrderConfirmationWithAttachment(savedOrder, pdfReport);
        } catch (Exception e) {
            logger.error("Failed to generate or email receipt for order ID: {}. The order was still created.", savedOrder.getId(), e);
        }

        return createdInvoice;
    }


}