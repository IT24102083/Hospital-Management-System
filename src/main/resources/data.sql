-- Create database if it doesn't exist
CREATE DATABASE IF NOT EXISTS hospital_management;
USE hospital_management;

-- Sample users (passwords are 'password' encrypted with BCrypt)
INSERT INTO users (username, password, first_name, last_name, email, phone_number, address, active, role)
VALUES
('admin', '$2a$10$3I9q1c3uWHnAgCJkZOKW0uuXQjjC8Ql6/Tc66W6VhMiZjY.f/qD6W', 'Admin', 'User', 'admin@hospital.com', '1234567890', '123 Admin St', 1, 'ROLE_ADMIN'),
('doctor1', '$2a$10$3I9q1c3uWHnAgCJkZOKW0uuXQjjC8Ql6/Tc66W6VhMiZjY.f/qD6W', 'John', 'Smith', 'john.smith@hospital.com', '1234567891', '456 Doctor Ave', 1, 'ROLE_DOCTOR'),
('doctor2', '$2a$10$3I9q1c3uWHnAgCJkZOKW0uuXQjjC8Ql6/Tc66W6VhMiZjY.f/qD6W', 'Sarah', 'Johnson', 'sarah.johnson@hospital.com', '1234567892', '789 Doctor Blvd', 1, 'ROLE_DOCTOR'),
('patient1', '$2a$10$3I9q1c3uWHnAgCJkZOKW0uuXQjjC8Ql6/Tc66W6VhMiZjY.f/qD6W', 'Bob', 'Williams', 'bob.williams@email.com', '1234567893', '101 Patient Rd', 1, 'ROLE_PATIENT'),
('patient2', '$2a$10$3I9q1c3uWHnAgCJkZOKW0uuXQjjC8Ql6/Tc66W6VhMiZjY.f/qD6W', 'Emily', 'Davis', 'emily.davis@email.com', '1234567894', '202 Patient St', 1, 'ROLE_PATIENT'),
('pharmacist1', '$2a$10$3I9q1c3uWHnAgCJkZOKW0uuXQjjC8Ql6/Tc66W6VhMiZjY.f/qD6W', 'Michael', 'Brown', 'michael.brown@hospital.com', '1234567895', '303 Pharmacy Ln', 1, 'ROLE_PHARMACIST'),
('receptionist1', '$2a$10$3I9q1c3uWHnAgCJkZOKW0uuXQjjC8Ql6/Tc66W6VhMiZjY.f/qD6W', 'Lisa', 'Taylor', 'lisa.taylor@hospital.com', '1234567896', '404 Reception Dr', 1, 'ROLE_RECEPTIONIST');

-- Sample doctors
INSERT INTO doctors (user_id, specialization, qualification, license_number, experience, biography, consultation_fee)
VALUES 
(2, 'Cardiology', 'MD, FACC', 'MD12345', '15 years', 'Dr. Smith is a board-certified cardiologist with over 15 years of experience in diagnosing and treating heart conditions.', '150.00'),
(3, 'Pediatrics', 'MD, FAAP', 'MD67890', '10 years', 'Dr. Johnson specializes in pediatric care and has been helping children stay healthy for over a decade.', '120.00');

-- Sample patients
INSERT INTO patients (user_id, date_of_birth, gender, blood_group, emergency_contact_name, emergency_contact_number, allergies, medical_history)
VALUES 
(4, '1985-06-15', 'Male', 'O+', 'Jane Williams', '9876543210', 'Penicillin', 'Hypertension'),
(5, '1990-11-23', 'Female', 'A-', 'Mark Davis', '9876543211', 'None', 'Asthma');

-- Sample doctor availability
INSERT INTO doctor_availability (doctor_id, available_date, day_of_week, start_time, end_time, max_appointments, booked_appointments, available)
VALUES 
(2, '2025-08-10', 'MONDAY', '09:00:00', '17:00:00', 8, 0, 1),
(2, '2025-08-11', 'TUESDAY', '09:00:00', '17:00:00', 8, 0, 1),
(2, '2025-08-12', 'WEDNESDAY', '09:00:00', '17:00:00', 8, 0, 1),
(3, '2025-08-10', 'MONDAY', '10:00:00', '18:00:00', 10, 0, 1),
(3, '2025-08-11', 'TUESDAY', '10:00:00', '18:00:00', 10, 0, 1),
(3, '2025-08-13', 'THURSDAY', '10:00:00', '18:00:00', 10, 0, 1);

-- Sample medicines
INSERT INTO medicines (name, category, description, price, stock, dosage_form, manufacturer, expiry_date, requires_prescription, storage_instructions)
VALUES 
('Amoxicillin', 'Antibiotics', 'Broad-spectrum antibiotic used to treat bacterial infections', 15.99, 100, 'Capsule', 'MediPharma', '2026-12-31', 1, 'Store in a cool, dry place'),
('Lisinopril', 'Antihypertensive', 'Used to treat high blood pressure and heart failure', 25.50, 75, 'Tablet', 'HealthDrug', '2026-10-15', 1, 'Keep away from moisture'),
('Paracetamol', 'Analgesic', 'Pain reliever and fever reducer', 5.99, 200, 'Tablet', 'ReliefMed', '2027-05-20', 0, 'Store at room temperature'),
('Cetirizine', 'Antihistamine', 'Used to relieve allergy symptoms', 8.75, 150, 'Tablet', 'AllerCare', '2026-08-10', 0, 'Keep container tightly closed'),
('Insulin', 'Hormone', 'Used to treat diabetes', 120.00, 50, 'Injectable', 'DiabCare', '2026-03-15', 1, 'Refrigerate at 2-8°C');

-- Sample appointments
INSERT INTO appointments (patient_id, doctor_id, appointment_date, appointment_time, reason, status, notes)
VALUES 
(4, 2, '2025-08-15', '10:30:00', 'Chest pain', 'SCHEDULED', 'First consultation'),
(5, 3, '2025-08-16', '14:00:00', 'Regular checkup', 'SCHEDULED', 'Annual pediatric assessment');

-- Update doctor availability for booked appointments
UPDATE doctor_availability 
SET booked_appointments = booked_appointments + 1
WHERE doctor_id = 2 AND available_date = '2025-08-15';

UPDATE doctor_availability 
SET booked_appointments = booked_appointments + 1
WHERE doctor_id = 3 AND available_date = '2025-08-16';

-- Sample invoices
INSERT INTO invoices (patient_id, invoice_number, issue_date, due_date, subtotal, tax, discount, total, status)
VALUES 
(4, 'INV-ABC12345', '2025-08-01', '2025-08-31', 150.00, 15.00, 0.00, 165.00, 'PENDING'),
(5, 'INV-DEF67890', '2025-08-02', '2025-08-31', 120.00, 12.00, 10.00, 122.00, 'PAID');

-- Sample invoice items
INSERT INTO invoice_items (invoice_id, description, quantity, unit_price, amount, item_type)
VALUES 
(1, 'Cardiology Consultation', 1, 150.00, 150.00, 'CONSULTATION'),
(2, 'Pediatric Consultation', 1, 120.00, 120.00, 'CONSULTATION');

-- Sample payments
INSERT INTO payments (invoice_id, appointment_id, amount, payment_method, transaction_id, status, payment_date)
VALUES 
(2, 2, 122.00, 'CREDIT_CARD', 'TXN123456789', 'COMPLETED', '2025-08-02 15:30:00');

-- Sample medical records
INSERT INTO medical_records (patient_id, doctor_id, appointment_id, diagnosis, symptoms, treatment, notes)
VALUES 
(5, 3, 2, 'Common Cold', 'Runny nose, sore throat, mild fever', 'Rest, fluids, over-the-counter cold medicine', 'Patient showing good recovery. Follow-up in 1 week if symptoms persist.');

-- Sample prescription
INSERT INTO prescriptions (medical_record_id, prescription_date, instructions, status, fulfilled)
VALUES 
(1, '2025-08-02 15:45:00', 'Take with food. Complete the full course.', 'ACTIVE', 0);

-- Sample prescription items
INSERT INTO prescription_items (prescription_id, medicine_id, dosage, frequency, duration, special_instructions, quantity)
VALUES 
(1, 3, '500mg', 'Twice daily', '5 days', 'Take after meals', 10),
(1, 4, '10mg', 'Once daily', '5 days', 'Take before bedtime', 5);