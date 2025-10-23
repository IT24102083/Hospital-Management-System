package com.hospital.hospitalmanagementsystem.service;

import com.hospital.hospitalmanagementsystem.model.*;
import com.hospital.hospitalmanagementsystem.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hospital.hospitalmanagementsystem.model.Patient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final MedicineRepository medicineRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final OrderRepository orderRepository;
    private final InvoiceService invoiceService;



    public PrescriptionService(PrescriptionRepository prescriptionRepository,
                               PrescriptionItemRepository prescriptionItemRepository,
                               MedicineRepository medicineRepository,
                               MedicalRecordRepository medicalRecordRepository,
                               OrderRepository orderRepository,
                               InvoiceService invoiceService) {
        this.prescriptionRepository = prescriptionRepository;
        this.prescriptionItemRepository = prescriptionItemRepository;
        this.medicineRepository = medicineRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.orderRepository = orderRepository;
        this.invoiceService = invoiceService;
    }

    /**
     * Creates a new prescription and sets its status to PENDING.
     * @param prescription The prescription object with instructions.
     * @param user The patient submitting the prescription.
     * @param filePath The path to the uploaded prescription file.
     */
    @Transactional
    public void createPrescription(Prescription prescription, User user, String filePath) {
        MedicalRecord medicalRecord = medicalRecordRepository.findByPatient_Id(user.getId())
                .orElseThrow(() -> new RuntimeException("Medical record not found for user: " + user.getUsername()));

        prescription.setMedicalRecord(medicalRecord);
        prescription.setStatus(Prescription.Status.PENDING);
        prescriptionRepository.save(prescription);
    }

    /**
     * Retrieves all prescriptions for a specific patient by finding all their medical records first.
     * @param user The patient whose prescriptions are to be retrieved.
     * @return A list of prescriptions.
     */
    public List<Prescription> getPrescriptionsForPatient(User user) {
        List<MedicalRecord> medicalRecords = medicalRecordRepository.findAllByPatient_Id(user.getId());
        if (medicalRecords.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return prescriptionRepository.findByMedicalRecordIn(medicalRecords);
    }

    /**
     * Retrieves all prescriptions that are pending fulfillment.
     * @return A list of pending prescriptions, ordered by date.
     */
    public List<Prescription> getPendingPrescriptions() {
        return prescriptionRepository.findByStatusOrderByPrescriptionDateAsc(Prescription.Status.PENDING);
    }

    /**
     * Retrieves a single prescription by its ID.
     * @param id The ID of the prescription.
     * @return An Optional containing the prescription if found.
     */
    public Optional<Prescription> getPrescriptionById(Long id) {
        return prescriptionRepository.findById(id);
    }

    /**
     * Saves a new prescription to the database, including all its items.
     * The operation is transactional to ensure data integrity.
     * @param prescription The prescription to be saved.
     * @return The saved Prescription entity with its generated ID.
     */
    @Transactional
    public Prescription savePrescription(Prescription prescription) {
        return prescriptionRepository.save(prescription);
    }

    /**
     * Retrieves all prescriptions issued by a specific doctor.
     * This relies on a custom @Query in the PrescriptionRepository.
     * @param doctor The doctor whose prescriptions are to be retrieved.
     * @return A list of prescriptions.
     */
    public List<Prescription> getPrescriptionsByDoctor(Doctor doctor) {
        return prescriptionRepository.findByDoctor(doctor);
    }

    public long getPatientPrescriptionCount(Patient patient) {
        return prescriptionRepository.countByPatient(patient);
    }

    /**
     * Fulfills a prescription by linking items to inventory, updating stock, and changing the status.
     * This operation is transactional; it will roll back if any step fails (e.g., insufficient stock).
     * @param prescriptionId The ID of the prescription to fulfill.
     * @param prescribedData A map from the web form containing the selected medicine for each item.
     */
    @Transactional
    public void fulfillPrescription(Long prescriptionId, Map<String, String> prescribedData) {
        // 1. Fetch Core Entities
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found with ID: " + prescriptionId));

        if (prescription.getStatus() != Prescription.Status.PENDING) {
            throw new IllegalStateException("Only PENDING prescriptions can be fulfilled. Current status: " + prescription.getStatus());
        }

        Patient patient = prescription.getMedicalRecord().getPatient();
        if (patient == null) {
            throw new IllegalStateException("Prescription is not linked to a valid patient.");
        }

        // 2. Create and Prepare a new Order
        Order newOrder = new Order();
        newOrder.setPatient(patient);
        newOrder.setOrderDate(LocalDateTime.now());
        newOrder.setStatus(Order.OrderStatus.PENDING_PAYMENT);
        newOrder.setItems(new ArrayList<>());
        BigDecimal totalOrderAmount = BigDecimal.ZERO;

        // 3. Process each prescription item
        for (PrescriptionItem pItem : prescription.getItems()) {
            String formInputKey = "item_" + pItem.getId() + "_medicineId";
            String medicineIdStr = prescribedData.get(formInputKey);

            if (medicineIdStr == null || medicineIdStr.trim().isEmpty()) {
                throw new IllegalArgumentException("No medicine selected for item: '" + pItem.getMedicineName() + "'");
            }

            Long medicineId = Long.parseLong(medicineIdStr);
            Medicine selectedMedicine = medicineRepository.findById(medicineId)
                    .orElseThrow(() -> new IllegalArgumentException("Medicine not found with ID: " + medicineId));

            // Check and update stock
            int requiredQuantity = pItem.getQuantity();
            if (selectedMedicine.getStock() < requiredQuantity) {
                throw new IllegalStateException("Insufficient stock for " + selectedMedicine.getName() +
                        ". Required: " + requiredQuantity + ", Available: " + selectedMedicine.getStock());
            }
            selectedMedicine.setStock(selectedMedicine.getStock() - requiredQuantity);

            // Create OrderItem
            BigDecimal lineTotal = selectedMedicine.getPrice().multiply(BigDecimal.valueOf(requiredQuantity));
            OrderItem orderItem = new OrderItem(null, newOrder, selectedMedicine, requiredQuantity, selectedMedicine.getPrice(), lineTotal);
            newOrder.getItems().add(orderItem);

            totalOrderAmount = totalOrderAmount.add(lineTotal);

            pItem.setMedicine(selectedMedicine); // Link prescription item to the actual medicine
        }

        // 4. Finalize and Save the Order
        newOrder.setTotalAmount(totalOrderAmount);
        Order savedOrder = orderRepository.save(newOrder);

        // 5. Create and Save the corresponding Invoice
        Invoice invoice = new Invoice();
        invoice.setPatient(patient);
        invoice.setIssueDate(LocalDate.now());
        invoice.setDueDate(LocalDate.now().plusDays(30)); // Due in 30 days
        invoice.setStatus(Invoice.InvoiceStatus.PENDING);
        invoice.setDescription("Pharmacy medication order from prescription #" + prescription.getId());

        List<InvoiceItem> invoiceItems = new ArrayList<>();
        for(OrderItem orderItem : savedOrder.getItems()){
            InvoiceItem invoiceItem = new InvoiceItem();
            invoiceItem.setInvoice(invoice);
            invoiceItem.setDescription(orderItem.getMedicine().getName());
            invoiceItem.setQuantity(orderItem.getQuantity());
            invoiceItem.setUnitPrice(orderItem.getPricePerItem());
            invoiceItem.setLineTotal(orderItem.getLineTotal());
            invoiceItems.add(invoiceItem);
        }
        invoice.setItems(invoiceItems);
        invoice.setSubtotal(totalOrderAmount);
        invoice.setTotal(totalOrderAmount);
        invoice.setBalanceDue(totalOrderAmount);

        Invoice savedInvoice = invoiceService.saveInvoice(invoice);

        // 6. Link Invoice back to the Order
        savedOrder.setInvoice(savedInvoice);
        orderRepository.save(savedOrder);

        // 7. Mark the prescription as fulfilled
        prescription.setStatus(Prescription.Status.FULFILLED);
        prescriptionRepository.save(prescription);
    }
}