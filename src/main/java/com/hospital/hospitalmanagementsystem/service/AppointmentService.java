package com.hospital.hospitalmanagementsystem.service;

import com.hospital.hospitalmanagementsystem.model.*;
import com.hospital.hospitalmanagementsystem.repository.AppointmentRepository;
import com.hospital.hospitalmanagementsystem.repository.DoctorAvailabilityRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Comparator;

/**
  Service for managing appointments
 */
@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final EmailService emailService;
    private final InvoiceService invoiceService;
    private static final Logger logger = LoggerFactory.getLogger(AppointmentService.class);
    private final ReportService reportService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            DoctorAvailabilityRepository availabilityRepository,
            EmailService emailService , InvoiceService invoiceService,
            ReportService reportService) {
        this.appointmentRepository = appointmentRepository;
        this.availabilityRepository = availabilityRepository;
        this.emailService = emailService;
        this.invoiceService = invoiceService;
        this.reportService = reportService;

    }

    @Transactional
    public Invoice  bookAppointment(Appointment appointment) {
        Doctor doctor = appointment.getDoctor();
        LocalDate date = appointment.getAppointmentDate();
        List<DoctorAvailability> availabilities = availabilityRepository.findByDoctorAndAvailableDate(doctor, date);

        if (availabilities.isEmpty() || availabilities.stream().noneMatch(DoctorAvailability::isAvailable)) {
            throw new RuntimeException("Doctor is not available on this date");
        }
        DoctorAvailability availability = availabilities.get(0);
        if (availability.getBookedAppointments() >= availability.getMaxAppointments()) {
            throw new RuntimeException("No more appointments available for this date");
        }
        availability.setBookedAppointments(availability.getBookedAppointments() + 1);
        availabilityRepository.save(availability);
        Appointment savedAppointment = appointmentRepository.save(appointment);
//        emailService.sendAppointmentConfirmationEmail(savedAppointment);

        // --- Create an invoice for the appointment ---
        Invoice invoice = new Invoice();
        invoice.setPatient(savedAppointment.getPatient());
        invoice.setAppointment(savedAppointment);
        invoice.setDescription("Consultation fee for Dr. " + doctor.getFirstName() + " " + doctor.getLastName());
        invoice.setIssueDate(LocalDate.now());
        invoice.setDueDate(LocalDate.now());

        BigDecimal consultationFee = BigDecimal.ZERO;
        try {
            // The Doctor model stores the fee as a String
            if (doctor.getConsultationFee() != null) {
                consultationFee = new BigDecimal(doctor.getConsultationFee());
            }
        } catch (NumberFormatException e) {
            // Handle cases where the fee is not a valid number
            System.err.println("Could not parse consultation fee for doctor ID " + doctor.getId());
        }

        invoice.setSubtotal(consultationFee);
        invoice.setTotal(consultationFee);
        invoice.setBalanceDue(consultationFee);

        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setDescription("Doctor Consultation");
        item.setQuantity(1);
        item.setUnitPrice(consultationFee);
        item.setLineTotal(consultationFee);

        List<InvoiceItem> items = new ArrayList<>();
        items.add(item);
        invoice.setItems(items);

        Invoice savedInvoice = invoiceService.saveInvoice(invoice);

        // --- Generate PDF and Send Email ---
        try {
            byte[] pdfInvoice = reportService.generateAppointmentInvoicePdf(savedInvoice);
            logger.info("PDF invoice generated for appointment #{}", savedAppointment.getId());

            // <<< 5. ADDED correct call with all 3 arguments >>>
            emailService.sendAppointmentConfirmationEmail(savedAppointment, savedInvoice, pdfInvoice);

        } catch (Exception e) {
            logger.error("Failed to generate or email invoice for appointment ID: {}. The appointment was still created.", savedAppointment.getId(), e);
        }

        return savedInvoice;

       // return appointmentRepository.save(appointment);


//        return savedAppointment;

    }

    /**
     * Saves or updates an appointment entity.
     * This method was added to allow updating an appointment's status.
     * @param appointment The appointment to save.
     * @return The saved appointment entity.
     */
    public Appointment saveAppointment(Appointment appointment) {
        return appointmentRepository.save(appointment);
    }

    public List<Appointment> getPatientAppointments(Patient patient) {
        return appointmentRepository.findByPatient(patient);
    }

    public List<Appointment> getDoctorAppointments(Doctor doctor) {
        return appointmentRepository.findByDoctor(doctor);
    }

    public List<Appointment> getDoctorAppointmentsByDate(Doctor doctor, LocalDate date) {
        return appointmentRepository.findByDoctorAndAppointmentDate(doctor, date);
    }

    public Optional<Appointment> getAppointmentById(Long id) {
        return appointmentRepository.findById(id);
    }

    @Transactional
    public void cancelAppointment(Long id) {
        appointmentRepository.findById(id).ifPresent(appointment -> {
            appointment.setStatus(Appointment.Status.CANCELLED);
            this.saveAppointment(appointment); // Use the new save method for consistency

            availabilityRepository.findByDoctorAndAvailableDate(appointment.getDoctor(), appointment.getAppointmentDate())
                    .stream().findFirst().ifPresent(availability -> {
                        availability.setBookedAppointments(Math.max(0, availability.getBookedAppointments() - 1));
                        availabilityRepository.save(availability);
                    });
        });
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    public List<Appointment> getUpcomingAppointments(Patient patient) {
        LocalDate today = LocalDate.now();
        return appointmentRepository.findByPatient(patient).stream()
                .filter(appointment ->
                        !appointment.getStatus().equals(Appointment.Status.CANCELLED) &&
                                !appointment.getAppointmentDate().isBefore(today)) // Refined date check
                .sorted((a1, a2) -> {
                    int dateComparison = a1.getAppointmentDate().compareTo(a2.getAppointmentDate());
                    return dateComparison != 0 ? dateComparison :
                            a1.getAppointmentTime().compareTo(a2.getAppointmentTime());
                })
                .collect(Collectors.toList());
    }

    public List<Appointment> getRecentAppointments(Patient patient) {
        LocalDate today = LocalDate.now();
        return appointmentRepository.findByPatient(patient).stream()
                .filter(appointment -> appointment.getAppointmentDate().isBefore(today))
                .sorted((a1, a2) -> {
                    int dateComparison = a2.getAppointmentDate().compareTo(a1.getAppointmentDate());
                    return dateComparison != 0 ? dateComparison :
                            a2.getAppointmentTime().compareTo(a1.getAppointmentTime());
                })
                .limit(5)
                .collect(Collectors.toList());
    }

    public List<Appointment> getUpcomingAppointmentsForNextWeek() {
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);
        return appointmentRepository.findAll().stream()
                .filter(appointment ->
                        !appointment.getStatus().equals(Appointment.Status.CANCELLED) &&
                                !appointment.getAppointmentDate().isBefore(today) && // Refined date check
                                appointment.getAppointmentDate().isBefore(nextWeek))
                .sorted((a1, a2) -> {
                    int dateComparison = a1.getAppointmentDate().compareTo(a2.getAppointmentDate());
                    return dateComparison != 0 ? dateComparison :
                            a1.getAppointmentTime().compareTo(a2.getAppointmentTime());
                })
                .collect(Collectors.toList());
    }

    /**
     * Finds appointments based on multiple optional criteria and sorts them in descending order.
     * @param patientName Part of the patient's name.
     * @param doctorName Part of the doctor's name.
     * @param date The specific date of the appointment.
     * @param status The status of the appointment.
     * @return A sorted and filtered list of appointments.
     */
    public List<Appointment> findFilteredAppointments(String patientName, String doctorName, LocalDate date, Appointment.Status status) {
        // Start with all appointments
        List<Appointment> appointments = appointmentRepository.findAll();

        // Filter by patient name if provided
        if (patientName != null && !patientName.trim().isEmpty()) {
            String lowerCasePatientName = patientName.toLowerCase();
            appointments = appointments.stream()
                    .filter(a -> (a.getPatient().getFirstName() + " " + a.getPatient().getLastName()).toLowerCase().contains(lowerCasePatientName))
                    .collect(Collectors.toList());
        }

        // Filter by doctor name if provided
        if (doctorName != null && !doctorName.trim().isEmpty()) {
            String lowerCaseDoctorName = doctorName.toLowerCase();
            appointments = appointments.stream()
                    .filter(a -> (a.getDoctor().getFirstName() + " " + a.getDoctor().getLastName()).toLowerCase().contains(lowerCaseDoctorName))
                    .collect(Collectors.toList());
        }

        // Filter by date if provided
        if (date != null) {
            appointments = appointments.stream()
                    .filter(a -> a.getAppointmentDate().equals(date))
                    .collect(Collectors.toList());
        }

        // Filter by status if provided
        if (status != null) {
            appointments = appointments.stream()
                    .filter(a -> a.getStatus() == status)
                    .collect(Collectors.toList());
        }

        // Sort the final list in descending order (newest first)
        return appointments.stream()
                .sorted(Comparator.comparing(Appointment::getAppointmentDate)
                        .thenComparing(Appointment::getAppointmentTime).reversed())
                .collect(Collectors.toList());
    }

    public List<Appointment> findFilteredAppointments(String patientName, String doctorName, LocalDate date) {
        List<Appointment> appointments = appointmentRepository.findAll();

        if (patientName != null && !patientName.trim().isEmpty()) {
            String lowerCasePatientName = patientName.toLowerCase();
            appointments = appointments.stream()
                    .filter(a -> (a.getPatient().getFirstName() + " " + a.getPatient().getLastName()).toLowerCase().contains(lowerCasePatientName))
                    .collect(Collectors.toList());
        }

        if (doctorName != null && !doctorName.trim().isEmpty()) {
            String lowerCaseDoctorName = doctorName.toLowerCase();
            appointments = appointments.stream()
                    .filter(a -> (a.getDoctor().getFirstName() + " " + a.getDoctor().getLastName()).toLowerCase().contains(lowerCaseDoctorName))
                    .collect(Collectors.toList());
        }

        if (date != null) {
            appointments = appointments.stream()
                    .filter(a -> a.getAppointmentDate().equals(date))
                    .collect(Collectors.toList());
        }

        return appointments;
    }

    public long countAppointments() {
        return appointmentRepository.count();
    }

    public List<Appointment> getAppointmentsByDate(LocalDate date) {
        return appointmentRepository.findByAppointmentDate(date);
    }
}