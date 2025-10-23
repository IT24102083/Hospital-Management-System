package com.hospital.hospitalmanagementsystem.service;

import com.hospital.hospitalmanagementsystem.model.Doctor;
import com.hospital.hospitalmanagementsystem.model.Patient;
import com.hospital.hospitalmanagementsystem.model.User;
import com.hospital.hospitalmanagementsystem.observer.UserActivityManager;
import com.hospital.hospitalmanagementsystem.repository.DoctorRepository;
import com.hospital.hospitalmanagementsystem.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminUserService {

    private final UserService userService;
    private final UserActivityManager userActivityManager;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    public AdminUserService(UserService userService, UserActivityManager userActivityManager, DoctorRepository doctorRepository, PatientRepository patientRepository) {
        this.userService = userService;
        this.userActivityManager = userActivityManager;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    // Pass-through method for reading data
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    public User saveUser(User user) {

        try {
            // FIX 1: Convert the Enum to a String using .name()
            String role = user.getRole().name();

            User userToSave;

            if ("ROLE_DOCTOR".equals(role)) {
                Doctor newDoctor = new Doctor();
                newDoctor.setUsername(user.getUsername());
                newDoctor.setPassword(user.getPassword());
                newDoctor.setEmail(user.getEmail());
                newDoctor.setRole(user.getRole());
                newDoctor.setFirstName(user.getFirstName());
                newDoctor.setLastName(user.getLastName());
                newDoctor.setAddress(user.getAddress());
                newDoctor.setPhoneNumber(user.getPhoneNumber());
                newDoctor.setActive(user.isActive());

                userToSave = newDoctor;

            } else if ("ROLE_PATIENT".equals(role)) {
                Patient newPatient = new Patient();
                newPatient.setUsername(user.getUsername());
                newPatient.setPassword(user.getPassword());
                newPatient.setEmail(user.getEmail());
                newPatient.setRole(user.getRole());
                newPatient.setFirstName(user.getFirstName());
                newPatient.setLastName(user.getLastName());
                newPatient.setAddress(user.getAddress());
                newPatient.setPhoneNumber(user.getPhoneNumber());
                newPatient.setActive(user.isActive());

                userToSave = newPatient;
            } else {
                userToSave = user;
            }
            User savedUser = userService.saveUser(userToSave);

            // This line notifies observers (your existing logic)
            userActivityManager.notifyObservers("USER_CREATED", savedUser);
            return savedUser;

        } catch (Exception e) {
            // FIX 4: Never leave a catch block empty!
            e.printStackTrace();
            // You might want to throw the exception here
            throw new RuntimeException("Error saving user: " + e.getMessage(), e);
        }
    }

    public User updateUser(Long id, User userDetails) {
        User updatedUser = userService.updateUser(id, userDetails);
        // After updating, notify observers
        userActivityManager.notifyObservers("USER_UPDATED", updatedUser);
        return updatedUser;
    }

    public void deleteUser(Long id) {
        // We must fetch the user before deleting to have its details for the log
        userService.getUserById(id).ifPresent(user -> {
            userService.deleteUser(id);
            // After deleting, notify observers
            userActivityManager.notifyObservers("USER_DELETED", user);
        });
    }
}