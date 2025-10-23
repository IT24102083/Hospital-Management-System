package com.hospital.hospitalmanagementsystem.controller;

import com.hospital.hospitalmanagementsystem.model.*;
import com.hospital.hospitalmanagementsystem.repository.MedicineRepository;
import com.hospital.hospitalmanagementsystem.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.beans.PropertyEditorSupport;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/doctor")
public class DoctorController {

    private final DoctorService doctorService;
    private final UserService userService;
    private final AppointmentService appointmentService;
    private final DoctorAvailabilityService availabilityService;
    private final MedicalRecordService medicalRecordService;
    private final PrescriptionService prescriptionService;
    private final MedicineRepository medicineRepository;

    public DoctorController(DoctorService doctorService,
                            UserService userService,
                            AppointmentService appointmentService,
                            DoctorAvailabilityService availabilityService,
                            MedicalRecordService  medicalRecordService,
                            PrescriptionService prescriptionService ,
                            MedicineRepository medicineRepository) {

        this.doctorService = doctorService;
        this.userService = userService;
        this.appointmentService = appointmentService;
        this.availabilityService = availabilityService;
        this.medicalRecordService = medicalRecordService;
        this.prescriptionService = prescriptionService;
        this.medicineRepository = medicineRepository;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(LocalTime.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                if (text != null && !text.isEmpty()) {
                    setValue(LocalTime.parse(text, DateTimeFormatter.ofPattern("HH:mm")));
                } else {
                    setValue(null);
                }
            }
            @Override
            public String getAsText() {
                if (getValue() != null) {
                    return DateTimeFormatter.ofPattern("HH:mm").format((LocalTime) getValue());
                }
                return "";
            }
        });
    }

//    @GetMapping("/profile")
//    public String viewProfile(Model model, Principal principal) {
//        if (principal == null) {
//            return "redirect:/login";
//        }
//        Optional<User> userOpt = userService.getUserByUsername(principal.getName());
//        if (userOpt.isPresent() && userOpt.get().getRole() == User.Role.ROLE_DOCTOR) {
//            doctorService.getDoctorById(userOpt.get().getId()).ifPresent(doctor -> model.addAttribute("doctor", doctor));
//        }
//        return "home/doctor/profile";
//    }
//
//    @GetMapping("/profile/edit")
//    public String editProfileForm(Model model, Principal principal) {
//        if (principal == null) {
//            return "redirect:/login";
//        }
//        Optional<User> userOpt = userService.getUserByUsername(principal.getName());
//        if (userOpt.isPresent() && userOpt.get().getRole() == User.Role.ROLE_DOCTOR) {
//            doctorService.getDoctorById(userOpt.get().getId()).ifPresent(doctor -> model.addAttribute("doctor", doctor));
//        }
//        return "home/doctor/edit-profile";
//    }

//    @PostMapping("/profile/edit")
//    public String updateProfile(@Valid @ModelAttribute("doctor") Doctor doctorDetails,
//                                BindingResult result, Principal principal) {
//        if (result.hasErrors()) {
//            return "home/doctor/edit-profile";
//        }
//        Optional<User> userOpt = userService.getUserByUsername(principal.getName());
//        if (userOpt.isPresent() && userOpt.get().getRole() == User.Role.ROLE_DOCTOR) {
//            doctorService.getDoctorById(userOpt.get().getId()).ifPresent(doctor -> {
//                doctor.setFirstName(doctorDetails.getFirstName());
//                doctor.setLastName(doctorDetails.getLastName());
//                doctor.setPhoneNumber(doctorDetails.getPhoneNumber());
//                doctor.setAddress(doctorDetails.getAddress());
//                doctor.setBiography(doctorDetails.getBiography());
//                doctorService.saveDoctor(doctor);
//            });
//            return "redirect:/doctor/profile?updated";
//        }
//        return "redirect:/login";
//    }

    @GetMapping("/profile")
    public String viewOrEditProfile(@RequestParam(name = "mode", required = false) String mode, Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        Optional<User> userOpt = userService.getUserByUsername(principal.getName());
        if (userOpt.isPresent() && userOpt.get().getRole() == User.Role.ROLE_DOCTOR) {
            doctorService.getDoctorById(userOpt.get().getId()).ifPresent(doctor -> {
                model.addAttribute("doctor", doctor);
                // Explicitly add a boolean flag to the model for edit mode
                model.addAttribute("editMode", "edit".equals(mode));
            });
            return "home/doctor/profile";
        }
        return "redirect:/login?error";
    }

    @PostMapping("/profile/edit")
    public String updateProfile(@ModelAttribute("doctor") Doctor doctorDetails,
                                BindingResult result, Principal principal, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("doctor", doctorDetails);
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.doctor", result);
            return "redirect:/doctor/profile?mode=edit&error=validation";
        }
        Optional<User> userOpt = userService.getUserByUsername(principal.getName());
        if (userOpt.isPresent() && userOpt.get().getRole() == User.Role.ROLE_DOCTOR) {
            doctorService.getDoctorById(userOpt.get().getId()).ifPresent(doctor -> {
                doctor.setFirstName(doctorDetails.getFirstName());
                doctor.setLastName(doctorDetails.getLastName());
                doctor.setPhoneNumber(doctorDetails.getPhoneNumber());
                doctor.setAddress(doctorDetails.getAddress());
                doctor.setBiography(doctorDetails.getBiography());
                doctor.setQualification(doctorDetails.getQualification());
                doctor.setLicenseNumber(doctorDetails.getLicenseNumber());
                doctor.setExperience(doctorDetails.getExperience());
                doctor.setConsultationFee(doctorDetails.getConsultationFee());
                doctorService.saveDoctor(doctor);
            });
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
            return "redirect:/doctor/profile";
        }
        return "redirect:/login";
    }

    @GetMapping("/appointments")
    public String viewAppointments(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        Optional<User> userOpt = userService.getUserByUsername(principal.getName());
        if (userOpt.isPresent() && userOpt.get() instanceof Doctor) {
            Doctor doctor = (Doctor) userOpt.get();
            model.addAttribute("doctor", doctor);
            model.addAttribute("appointments", appointmentService.getDoctorAppointments(doctor));
        }
        return "home/doctor/appointments";
    }

    @GetMapping("/availability")
    public String viewAvailability(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        Optional<User> userOpt = userService.getUserByUsername(principal.getName());
        if (userOpt.isPresent() && userOpt.get().getRole() == User.Role.ROLE_DOCTOR) {
            Optional<Doctor> doctorOpt = doctorService.getDoctorById(userOpt.get().getId());
            if (doctorOpt.isPresent()) {
                Doctor doctor = doctorOpt.get();
                model.addAttribute("doctor", doctor);
                List<DoctorAvailability> availabilities = availabilityService.getDoctorAvailabilities(doctor);
                model.addAttribute("availabilities", availabilities);
                model.addAttribute("newAvailability", new DoctorAvailability());
            }
        }
        return "home/doctor/availability";
    }

    @PostMapping("/availability/add")
    public String addAvailability(@Valid @ModelAttribute("newAvailability") DoctorAvailability availability,
                                  BindingResult result, Principal principal, Model model) {
        Optional<User> userOpt = userService.getUserByUsername(principal.getName());
        if (!userOpt.isPresent() || userOpt.get().getRole() != User.Role.ROLE_DOCTOR) {
            return "redirect:/login";
        }
        Doctor doctor = doctorService.getDoctorById(userOpt.get().getId()).orElse(null);

        if (result.hasErrors()) {
            model.addAttribute("doctor", doctor);
            model.addAttribute("availabilities", availabilityService.getDoctorAvailabilities(doctor));
            return "home/doctor/availability";
        }
        availability.setDoctor(doctor);
        availability.setBookedAppointments(0);
        availability.setAvailable(true);
        availabilityService.saveAvailability(availability);
        return "redirect:/doctor/availability?added";
    }

    @GetMapping("/availability/edit/{id}")
    public String editAvailabilityForm(@PathVariable Long id, Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        Optional<User> userOpt = userService.getUserByUsername(principal.getName());
        if (userOpt.isPresent() && userOpt.get().getRole() == User.Role.ROLE_DOCTOR) {
            Optional<DoctorAvailability> availabilityOpt = availabilityService.getAvailabilityById(id);
            if (availabilityOpt.isPresent() && availabilityOpt.get().getDoctor().getId().equals(userOpt.get().getId())) {
                model.addAttribute("availability", availabilityOpt.get());
                model.addAttribute("doctor", availabilityOpt.get().getDoctor());
                return "home/doctor/edit-availability";
            }
        }
        return "redirect:/doctor/availability?error";
    }

    @PostMapping("/availability/update")
    public String updateAvailability(@Valid @ModelAttribute("availability") DoctorAvailability availabilityDetails,
                                     BindingResult result, Principal principal, Model model) {
        Optional<User> userOpt = userService.getUserByUsername(principal.getName());
        if (!userOpt.isPresent() || userOpt.get().getRole() != User.Role.ROLE_DOCTOR) {
            return "redirect:/login";
        }
        if (result.hasErrors()) {
            model.addAttribute("doctor", doctorService.getDoctorById(userOpt.get().getId()).orElse(null));
            return "home/doctor/edit-availability";
        }

        Optional<DoctorAvailability> originalAvailabilityOpt = availabilityService.getAvailabilityById(availabilityDetails.getId());
        if (originalAvailabilityOpt.isPresent() && originalAvailabilityOpt.get().getDoctor().getId().equals(userOpt.get().getId())) {
            DoctorAvailability originalAvailability = originalAvailabilityOpt.get();
            originalAvailability.setAvailableDate(availabilityDetails.getAvailableDate());
            originalAvailability.setStartTime(availabilityDetails.getStartTime());
            originalAvailability.setEndTime(availabilityDetails.getEndTime());
            originalAvailability.setMaxAppointments(availabilityDetails.getMaxAppointments());
            availabilityService.saveAvailability(originalAvailability);
            return "redirect:/doctor/availability?updated";
        }
        return "redirect:/doctor/availability?error";
    }

    @GetMapping("/availability/delete/{id}")
    public String deleteAvailability(@PathVariable Long id, Principal principal) {
        Optional<User> userOpt = userService.getUserByUsername(principal.getName());
        if (userOpt.isPresent() && userOpt.get().getRole() == User.Role.ROLE_DOCTOR) {
            Optional<DoctorAvailability> availabilityOpt = availabilityService.getAvailabilityById(id);
            if (availabilityOpt.isPresent() && availabilityOpt.get().getDoctor().getId().equals(userOpt.get().getId())) {
                availabilityService.deleteAvailability(id);
                return "redirect:/doctor/availability?deleted";
            }
        }
        return "redirect:/doctor/availability";
    }

    @GetMapping("/appointments/date")
    public String getAppointmentsByDate(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                        Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        Optional<User> userOpt = userService.getUserByUsername(principal.getName());
        if (userOpt.isPresent() && userOpt.get().getRole() == User.Role.ROLE_DOCTOR) {
            Optional<Doctor> doctorOpt = doctorService.getDoctorById(userOpt.get().getId());
            if (doctorOpt.isPresent()) {
                Doctor doctor = doctorOpt.get();
                model.addAttribute("doctor", doctor);
                List<Appointment> appointments = appointmentService.getDoctorAppointmentsByDate(doctor, date);
                model.addAttribute("appointments", appointments);
                model.addAttribute("selectedDate", date);
            }
        }
        return "home/doctor/appointments-by-date";
    }

    @GetMapping("/records/create/{appointmentId}")
    public String showCreateRecordForm(@PathVariable Long appointmentId, Model model) {
        Optional<Appointment> appointmentOpt = appointmentService.getAppointmentById(appointmentId);
        if (appointmentOpt.isPresent()) {
            model.addAttribute("appointment", appointmentOpt.get());
            model.addAttribute("medicalRecord", new MedicalRecord());
            return "home/doctor/create-medical-record";
        }
        return "redirect:/dashboard?error=appointmentNotFound";
    }

    @PostMapping("/records/create/{appointmentId}")
    public String saveMedicalRecord(@PathVariable Long appointmentId,
                                    @Valid @ModelAttribute("medicalRecord") MedicalRecord medicalRecord,
                                    BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        Optional<Appointment> appointmentOpt = appointmentService.getAppointmentById(appointmentId);
        if (appointmentOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Appointment not found.");
            return "redirect:/dashboard";
        }
        Appointment appointment = appointmentOpt.get();
        if (result.hasErrors()) {
            model.addAttribute("appointment", appointment);
            return "home/doctor/create-medical-record";
        }
        medicalRecord.setAppointment(appointment);
        medicalRecord.setPatient(appointment.getPatient());
        medicalRecord.setDoctor(appointment.getDoctor());
        medicalRecordService.save(medicalRecord);
        appointment.setStatus(Appointment.Status.COMPLETED);
        appointmentService.saveAppointment(appointment);

        // NOTIFICATION: Switched to using RedirectAttributes for clearer success messages on the dashboard.
        redirectAttributes.addFlashAttribute("success", "Medical record created successfully!");
        return "redirect:/dashboard";
    }

    /**
     * Shows the form to create a new prescription. The {id} here is the appointmentId.
     */
    @GetMapping("/prescriptions/create/{id}")
    public String showCreatePrescriptionForm(@PathVariable("id") Long appointmentId, Model model, RedirectAttributes redirectAttributes) {
        Optional<Appointment> appointmentOpt = appointmentService.getAppointmentById(appointmentId);
        if (appointmentOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Appointment not found.");
            return "redirect:/dashboard";
        }

        // Get the appointment, which has the patient data loaded
        Appointment appointment = appointmentOpt.get();

        Optional<MedicalRecord> medicalRecordOpt = medicalRecordService.findByAppointment(appointment);
        if (medicalRecordOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "A medical record must be created for this appointment before issuing a prescription.");
            return "redirect:/dashboard";
        }

        model.addAttribute("medicalRecord", medicalRecordOpt.get());

        // FIX: Add the Patient object separately to the model to avoid a LazyInitializationException
        model.addAttribute("patient", appointment.getPatient());

        model.addAttribute("prescriptionForm", new PrescriptionForm());
        model.addAttribute("allMedicines", medicineRepository.findAll());
        return "home/doctor/issue-prescription";
    }

    /**
     * Saves the new prescription submitted from the form. The {id} here is the medicalRecordId.
     */
    @PostMapping("/prescriptions/create/{id}")
    public String savePrescription(@PathVariable("id") Long medicalRecordId,
                                   @ModelAttribute("prescriptionForm") PrescriptionForm prescriptionForm,
                                   RedirectAttributes redirectAttributes) {
        Optional<MedicalRecord> medicalRecordOpt = medicalRecordService.findById(medicalRecordId);
        if (medicalRecordOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Medical record not found.");
            return "redirect:/dashboard";
        }
        MedicalRecord medicalRecord = medicalRecordOpt.get();
        Prescription prescription = new Prescription();
        prescription.setMedicalRecord(medicalRecord);
        prescription.setInstructions(prescriptionForm.getInstructions());
        prescription.setStatus(Prescription.Status.PENDING);
        List<PrescriptionItem> validItems = prescriptionForm.getItems().stream()
                .filter(item -> item.getMedicineName() != null && !item.getMedicineName().trim().isEmpty())
                .collect(Collectors.toList());

        if (validItems.isEmpty()) {
            redirectAttributes.addFlashAttribute("formError", "Please add at least one medication.");
            return "redirect:/doctor/prescriptions/create/" + medicalRecord.getAppointment().getId();
        }
        validItems.forEach(item -> item.setPrescription(prescription));
        prescription.setItems(validItems);
        prescriptionService.savePrescription(prescription);

        redirectAttributes.addFlashAttribute("success", "Prescription issued successfully!");
        return "redirect:/dashboard";
    }

    @GetMapping("/records")
    public String listMedicalRecords(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        User user = userService.getUserByUsername(principal.getName()).orElse(null);
        if (user instanceof Doctor) {
            List<MedicalRecord> records = medicalRecordService.findByDoctor((Doctor) user);
            model.addAttribute("medicalRecords", records);
        }
        return "home/doctor/medical-records";
    }

    @GetMapping("/prescriptions")
    public String listPrescriptions(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        User user = userService.getUserByUsername(principal.getName()).orElse(null);
        if (user instanceof Doctor) {
            List<Prescription> prescriptions = prescriptionService.getPrescriptionsByDoctor((Doctor) user);
            model.addAttribute("prescriptions", prescriptions);
        }
        return "home/doctor/prescriptions";
    }

    public static class PrescriptionForm {
        private List<PrescriptionItem> items = new ArrayList<>();
        private String instructions;
        public List<PrescriptionItem> getItems() { return items; }
        public void setItems(List<PrescriptionItem> items) { this.items = items; }
        public String getInstructions() { return instructions; }
        public void setInstructions(String instructions) { this.instructions = instructions; }
    }


    @GetMapping("/records/edit/{id}")
    public String showEditRecordForm(@PathVariable Long id, Model model, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }
        User user = userService.getUserByUsername(principal.getName()).orElse(null);
        Optional<MedicalRecord> medicalRecordOpt = medicalRecordService.findById(id);

        if (medicalRecordOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Medical record not found.");
            return "redirect:/doctor/records";
        }

        MedicalRecord medicalRecord = medicalRecordOpt.get();
        // Security Check: Ensure the logged-in doctor is the one who created the record
        if (user == null || !medicalRecord.getDoctor().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("error", "You are not authorized to edit this record.");
            return "redirect:/doctor/records";
        }

        model.addAttribute("medicalRecord", medicalRecord);
        return "home/doctor/edit-medical-record";
    }

    @PostMapping("/records/update/{id}")
    public String updateMedicalRecord(@PathVariable Long id,
                                      @Valid @ModelAttribute("medicalRecord") MedicalRecord recordDetails,
                                      BindingResult result, Principal principal, RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }
        User user = userService.getUserByUsername(principal.getName()).orElse(null);
        Optional<MedicalRecord> originalRecordOpt = medicalRecordService.findById(id);

        if (originalRecordOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Medical record not found.");
            return "redirect:/doctor/records";
        }

        MedicalRecord originalRecord = originalRecordOpt.get();
        // Security Check
        if (user == null || !originalRecord.getDoctor().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("error", "You are not authorized to update this record.");
            return "redirect:/doctor/records";
        }

        if (result.hasErrors()) {
            // Re-populate model for the form
            recordDetails.setPatient(originalRecord.getPatient()); // Ensure patient data is not lost
            recordDetails.setAppointment(originalRecord.getAppointment()); // Ensure appointment data is not lost
            return "home/doctor/edit-medical-record";
        }

        originalRecord.setDiagnosis(recordDetails.getDiagnosis());
        originalRecord.setSymptoms(recordDetails.getSymptoms());
        originalRecord.setTreatment(recordDetails.getTreatment());
        originalRecord.setNotes(recordDetails.getNotes());
        // createdAt, patient, doctor, and appointment are not updated

        medicalRecordService.save(originalRecord);
        redirectAttributes.addFlashAttribute("success", "Medical record updated successfully!");
        return "redirect:/doctor/records";
    }


    @GetMapping("/records/delete/{id}")
    public String deleteMedicalRecord(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }
        User user = userService.getUserByUsername(principal.getName()).orElse(null);
        Optional<MedicalRecord> medicalRecordOpt = medicalRecordService.findById(id);

        if (medicalRecordOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Medical record not found.");
            return "redirect:/doctor/records";
        }

        MedicalRecord medicalRecord = medicalRecordOpt.get();
        // Security Check: Ensure the logged-in doctor is the one who created the record
        if (user == null || !medicalRecord.getDoctor().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("error", "You are not authorized to delete this record.");
            return "redirect:/doctor/records";
        }

        try {
            medicalRecordService.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Medical record deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not delete record. It may be linked to other items.");
        }

        return "redirect:/doctor/records";
    }
}

