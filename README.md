<div align="center">

# 🏥 HealthFirst Hospital Management System

### *Revolutionizing Healthcare Administration with Modern Technology*

[![Java](https://img.shields.io/badge/Java-11-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-2.7.14-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Maven](https://img.shields.io/badge/Maven-3.8+-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org/)

[![HTML5](https://img.shields.io/badge/HTML5-E34F26?style=for-the-badge&logo=html5&logoColor=white)](https://developer.mozilla.org/en-US/docs/Web/HTML)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-38B2AC?style=for-the-badge&logo=tailwind-css&logoColor=white)](https://tailwindcss.com/)
[![JavaScript](https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black)](https://developer.mozilla.org/en-US/docs/Web/JavaScript)
[![Thymeleaf](https://img.shields.io/badge/Thymeleaf-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white)](https://www.thymeleaf.org/)

[Features](#-key-features) • [Demo](#-live-demo) • [Installation](#-quick-start) • [Documentation](#-documentation) • [Contributing](#-contributing)

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Key Features](#-key-features)
- [Technology Stack](#-technology-stack)
- [System Architecture](#-system-architecture)
- [Quick Start](#-quick-start)
- [User Roles](#-user-roles--permissions)
- [Screenshots](#-screenshots)
- [API Documentation](#-api-documentation)
- [Configuration](#-configuration)
- [Testing](#-testing)
- [Deployment](#-deployment)
- [Contributing](#-contributing)
- [License](#-license)
- [Support](#-support)

---

## 🌟 Overview

**HealthFirst** is a comprehensive, enterprise-grade Hospital Management System designed to streamline healthcare operations. Built with modern Java technologies and best practices, it provides a complete solution for managing patients, appointments, medical records, billing, pharmacy operations, and administrative tasks.

### 🎯 Project Goals

- ✅ Simplify hospital administrative workflows
- ✅ Enhance patient experience and engagement
- ✅ Improve medical record management and accessibility
- ✅ Automate billing and pharmacy operations
- ✅ Provide real-time insights through analytics dashboards

---

## ✨ Key Features

<table>
<tr>
<td width="50%">

### 🧑‍⚕️ **Patient Management**
- 📝 Self-registration and profile management
- 📅 Online appointment booking system
- 📋 Digital medical records access
- 💊 E-prescription viewing
- 💳 Online payment processing
- 🛒 Online pharmacy ordering

</td>
<td width="50%">

### 👨‍⚕️ **Doctor Portal**
- 🗓️ Appointment scheduling & management
- ⏰ Availability calendar management
- 📝 Digital medical record creation
- 💊 E-prescription issuance
- 📊 Patient history viewing
- 👤 Professional profile management

</td>
</tr>
<tr>
<td width="50%">

### 💊 **Pharmacy Module**
- 📦 Inventory management system
- 🔄 Stock level monitoring
- 📋 Prescription fulfillment
- 📧 Email notifications
- 📊 Automated inventory reports
- 🛍️ Online medicine catalog

</td>
<td width="50%">

### 💰 **Billing & Accounting**
- 🧾 Invoice generation & management
- 💳 Multiple payment methods
- 📅 Payment plan setup
- ✅ Bank transfer verification
- 💻 POS terminal integration
- 📈 Financial reporting & analytics

</td>
</tr>
<tr>
<td width="50%">

### 🤝 **Reception Services**
- 👥 Patient registration
- 📞 Appointment scheduling
- 📊 Dashboard analytics
- 📋 Visit management
- 🔔 Appointment reminders

</td>
<td width="50%">

### ⚙️ **Admin Panel**
- 👥 User management (all roles)
- 👨‍⚕️ Doctor profile administration
- 📊 System analytics & statistics
- 📈 Performance monitoring
- 🔐 Access control management
- 📝 Audit logging

</td>
</tr>
</table>

### 🚀 **Advanced Features**

| Feature | Description |
|---------|-------------|
| 🔐 **Security** | Spring Security with role-based access control (RBAC) |
| 📧 **Email Notifications** | Automated emails for appointments, payments, and reports |
| 📄 **PDF Generation** | Dynamic PDF creation for receipts and reports |
| 📊 **Analytics** | Real-time dashboards with Chart.js visualizations |
| 🔄 **Design Patterns** | Observer pattern for auditing, Singleton for configuration |
| 🌐 **RESTful APIs** | Well-documented REST endpoints |
| 📱 **Responsive Design** | Mobile-friendly UI with Tailwind CSS |
| 📁 **File Management** | Secure file upload/download for documents |

---

## 🛠️ Technology Stack

### **Backend Technologies**

```
├── Java 11                   # Core programming language
├── Spring Boot 2.7.14        # Application framework
├── Spring Data JPA           # Database abstraction
├── Spring Security           # Authentication & authorization
├── Spring Web MVC            # Web layer
├── Hibernate                 # ORM framework
└── Spring Mail               # Email service
```

### **Frontend Technologies**

```
├── Thymeleaf                 # Server-side templating
├── HTML5 & CSS3              # Markup and styling
├── Tailwind CSS              # Utility-first CSS framework
├── JavaScript (ES6+)         # Client-side scripting
├── Chart.js                  # Data visualization
├── Flatpickr.js              # Date picker
└── Tom Select                # Enhanced select dropdowns
```

### **Database & Storage**

```
├── MySQL 8.0                 # Relational database
└── File System               # Document storage
```

### **Build & Deployment**

```
├── Apache Maven              # Build automation
├── Lombok                    # Code generation
└── Spring DevTools           # Development utilities
```

### **Libraries & Utilities**

| Library | Purpose |
|---------|---------|
| **iText 7** | PDF generation and manipulation |
| **Flying Saucer** | HTML to PDF rendering |
| **ZXing** | QR code generation |
| **Apache POI** | Excel report generation |
| **Salmos Reports** | Report generation utilities |
| **Springdoc OpenAPI** | API documentation |

---

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                       │
│  (Thymeleaf Templates, HTML, CSS, JavaScript, Tailwind)     │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                     Controller Layer                         │
│        (Spring MVC Controllers, REST Controllers)            │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                      Service Layer                           │
│  (Business Logic, Email Service, PDF Service, etc.)         │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    Repository Layer                          │
│              (Spring Data JPA Repositories)                  │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                      Data Layer                              │
│                    (MySQL Database)                          │
└─────────────────────────────────────────────────────────────┘
```

### **Project Structure**

```
hospital-management-system/
│
├── 📂 src/main/
│   ├── 📂 java/com/hospital/hospitalmanagementsystem/
│   │   ├── 📂 config/              # Configuration classes
│   │   │   ├── WebConfig.java
│   │   │   ├── SecurityConfig.java
│   │   │   └── AppConfig.java
│   │   │
│   │   ├── 📂 controller/          # MVC & REST controllers
│   │   │   ├── AdminController.java
│   │   │   ├── PatientController.java
│   │   │   ├── DoctorController.java
│   │   │   ├── PharmacistController.java
│   │   │   ├── AccountantController.java
│   │   │   └── ReceptionistController.java
│   │   │
│   │   ├── 📂 model/               # JPA entities
│   │   │   ├── User.java
│   │   │   ├── Patient.java
│   │   │   ├── Doctor.java
│   │   │   ├── Appointment.java
│   │   │   ├── MedicalRecord.java
│   │   │   ├── Prescription.java
│   │   │   ├── Medicine.java
│   │   │   └── Invoice.java
│   │   │
│   │   ├── 📂 repository/          # Data access layer
│   │   │   ├── UserRepository.java
│   │   │   ├── PatientRepository.java
│   │   │   ├── DoctorRepository.java
│   │   │   └── ...
│   │   │
│   │   ├── 📂 service/             # Business logic
│   │   │   ├── UserService.java
│   │   │   ├── AppointmentService.java
│   │   │   ├── EmailService.java
│   │   │   ├── PdfService.java
│   │   │   └── ...
│   │   │
│   │   ├── 📂 security/            # Security configurations
│   │   │   ├── CustomUserDetailsService.java
│   │   │   └── SecurityConfig.java
│   │   │
│   │   ├── 📂 exception/           # Custom exceptions
│   │   │   └── ResourceNotFoundException.java
│   │   │
│   │   └── 📂 observer/            # Design patterns
│   │       └── UserManagementObserver.java
│   │
│   └── 📂 resources/
│       ├── 📂 static/              # Static assets
│       │   ├── css/
│       │   ├── js/
│       │   └── images/
│       │
│       ├── 📂 templates/           # Thymeleaf templates
│       │   ├── admin/
│       │   ├── patient/
│       │   ├── doctor/
│       │   ├── pharmacist/
│       │   ├── accountant/
│       │   └── receptionist/
│       │
│       ├── application.properties   # Configuration
│       └── data.sql                # Sample data
│
├── 📂 uploads/                     # File uploads directory
├── 📄 pom.xml                      # Maven configuration
├── 📄 README.md                    # This file
└── 📄 LICENSE                      # MIT License
```

---

## 🚀 Quick Start

### **Prerequisites**

Before you begin, ensure you have the following installed:

| Software | Version | Download Link |
|----------|---------|---------------|
| ☕ **Java JDK** | 11+ | [Oracle JDK](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html) |
| 🗄️ **MySQL** | 8.0+ | [MySQL Downloads](https://dev.mysql.com/downloads/) |
| 📦 **Maven** | 3.6+ | [Maven Downloads](https://maven.apache.org/download.cgi) |
| 💻 **IDE** | Latest | [IntelliJ IDEA](https://www.jetbrains.com/idea/) |
| 🔧 **Git** | Latest | [Git Downloads](https://git-scm.com/downloads) |

### **Installation Steps**

#### **1️⃣ Clone the Repository**

```bash
# Clone the project
git clone https://github.com/IT24102083/hospital-management-system.git

# Navigate to project directory
cd hospital-management-system
```

#### **2️⃣ Database Setup**

```sql
-- Start MySQL service
-- Create database
CREATE DATABASE IF NOT EXISTS hospital_management;

-- Use the database
USE hospital_management;

-- Import sample data (optional)
source src/main/resources/data.sql;
```

Or via command line:

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS hospital_management;"
mysql -u root -p hospital_management < src/main/resources/data.sql
```

#### **3️⃣ Configure Application**

Open `src/main/resources/application.properties` and update:

```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/hospital_management?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=your_mysql_password

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# Email Configuration (Gmail Example)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# File Upload
app.upload.dir=uploads/medicines
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# Server Configuration
server.port=8080
```

> **📌 Note:** For Gmail, you need to generate an [App Password](https://support.google.com/accounts/answer/185833) if 2FA is enabled.

#### **4️⃣ Build the Project**

```bash
# Clean and build with Maven
mvn clean install

# Or skip tests for faster build
mvn clean install -DskipTests
```

#### **5️⃣ Run the Application**

**Option A: Using Maven**
```bash
mvn spring-boot:run
```

**Option B: Using JAR file**
```bash
java -jar target/hospital-management-system-0.0.1-SNAPSHOT.jar
```

**Option C: From IDE**
- Open project in IntelliJ IDEA
- Locate `HospitalManagementSystemApplication.java`
- Right-click → Run

#### **6️⃣ Access the Application**

```
🌐 Application URL: http://localhost:8080
📧 Default Admin: admin / password
```

---

## 👥 User Roles & Permissions

### **Default Login Credentials**

| Role | Username | Password | Access Level |
|------|----------|----------|--------------|
| 🔑 **Admin** | `admin` | `password` | Full system access |
| 👨‍⚕️ **Doctor** | `doctor1`, `doctor2` | `password` | Medical records, appointments |
| 🧑‍⚕️ **Patient** | `patient1`, `patient2` | `password` | Personal health records |
| 💊 **Pharmacist** | `pharmacist1` | `password` | Inventory, prescriptions |
| 🤝 **Receptionist** | `receptionist1` | `password` | Registration, appointments |
| 💰 **Accountant** | *Create via Admin* | - | Billing, financial reports |

> ⚠️ **Security Notice:** Change all default passwords immediately in production!

### **Role-Based Features**

<details>
<summary><b>🔑 Admin (Full Access)</b></summary>

- ✅ User management (CRUD operations)
- ✅ System configuration
- ✅ View all dashboards
- ✅ Generate system reports
- ✅ Audit log access
- ✅ Doctor profile management
- ✅ Global appointment overview

</details>

<details>
<summary><b>👨‍⚕️ Doctor</b></summary>

- ✅ View assigned appointments
- ✅ Manage availability schedule
- ✅ Create/update medical records
- ✅ Issue prescriptions
- ✅ View patient history
- ✅ Update professional profile

</details>

<details>
<summary><b>🧑‍⚕️ Patient</b></summary>

- ✅ Book appointments online
- ✅ View medical records
- ✅ Access prescriptions
- ✅ Make online payments
- ✅ Order medicines
- ✅ Manage payment plans
- ✅ Update personal profile

</details>

<details>
<summary><b>💊 Pharmacist</b></summary>

- ✅ Manage medicine inventory
- ✅ Process prescriptions
- ✅ Update stock levels
- ✅ Generate inventory reports
- ✅ Email reports to management
- ✅ Track medicine orders

</details>

<details>
<summary><b>💰 Accountant</b></summary>

- ✅ Create/manage invoices
- ✅ Process payments
- ✅ Verify bank transfers
- ✅ Manage payment plans
- ✅ Generate financial reports
- ✅ Use POS terminal
- ✅ View analytics dashboard

</details>

<details>
<summary><b>🤝 Receptionist</b></summary>

- ✅ Register new patients
- ✅ Schedule appointments
- ✅ Manage walk-in patients
- ✅ View appointment calendar
- ✅ Send appointment reminders

</details>

---

## 📸 Screenshots

<div align="center">

### 🏠 Landing Page
*Modern, responsive landing page with service information*

### 📊 Admin Dashboard
*Comprehensive system overview with real-time statistics*

### 👨‍⚕️ Doctor Portal
*Appointment management and medical record creation*

### 💊 Pharmacy Management
*Inventory tracking with low-stock alerts*

### 💰 Billing System
*Invoice management with multiple payment options*

</div>

---

## 📚 API Documentation

### **REST Endpoints**

#### **Pharmacy API**

```http
GET    /api/pharmacy/medicines              # Get all medicines
GET    /api/pharmacy/medicines/{id}         # Get medicine by ID
POST   /api/pharmacy/medicines              # Add new medicine
PUT    /api/pharmacy/medicines/{id}         # Update medicine
DELETE /api/pharmacy/medicines/{id}         # Delete medicine
GET    /api/pharmacy/medicines/low-stock    # Get low stock items
```

#### **Billing API**

```http
GET    /api/billing/invoices                # Get all invoices
GET    /api/billing/invoices/{id}           # Get invoice by ID
POST   /api/billing/invoices                # Create invoice
PUT    /api/billing/invoices/{id}/pay       # Process payment
GET    /api/billing/payment-plans           # Get payment plans
```

#### **Medical Records API**

```http
GET    /api/records/patient/{id}            # Get patient records
POST   /api/records                         # Create medical record
PUT    /api/records/{id}                    # Update record
GET    /api/records/{id}/prescriptions      # Get prescriptions
```

### **API Documentation (Swagger/OpenAPI)**

Access interactive API documentation at:
```
http://localhost:8080/swagger-ui.html
```

---

## ⚙️ Configuration

### **Application Properties**

<details>
<summary><b>Database Configuration</b></summary>

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/hospital_management
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
```

</details>

<details>
<summary><b>Email Configuration</b></summary>

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${EMAIL_USERNAME}
spring.mail.password=${EMAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000
```

</details>

<details>
<summary><b>File Upload Configuration</b></summary>

```properties
app.upload.dir=uploads/medicines
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
spring.servlet.multipart.file-size-threshold=2KB
```

</details>

<details>
<summary><b>Security Configuration</b></summary>

```properties
# Session timeout (30 minutes)
server.servlet.session.timeout=30m

# Enable HTTPS (production)
# server.ssl.enabled=true
# server.ssl.key-store=classpath:keystore.p12
# server.ssl.key-store-password=your_password
# server.ssl.key-store-type=PKCS12
```

</details>

### **Environment Variables**

Create a `.env` file for sensitive data:

```bash
DB_USERNAME=root
DB_PASSWORD=your_db_password
EMAIL_USERNAME=your_email@gmail.com
EMAIL_PASSWORD=your_app_password
JWT_SECRET=your_secret_key
```

---

## 🧪 Testing

### **Run Tests**

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=UserServiceTest

# Run with coverage
mvn clean test jacoco:report
```

### **Test Coverage**

View coverage reports at:
```
target/site/jacoco/index.html
```

---

## 🚢 Deployment

### **Deploy to Production**

#### **1. Build Production JAR**

```bash
mvn clean package -Pprod
```

#### **2. Run with Production Profile**

```bash
java -jar -Dspring.profiles.active=prod target/hospital-management-system.jar
```

#### **3. Using Docker (Optional)**

```dockerfile
FROM openjdk:11-jre-slim
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
# Build image
docker build -t hospital-management-system .

# Run container
docker run -p 8080:8080 hospital-management-system
```

### **Cloud Deployment Options**

| Platform | Guide |
|----------|-------|
| ☁️ **AWS** | [Deploy to AWS Elastic Beanstalk](https://docs.aws.amazon.com/elasticbeanstalk/) |
| 🌊 **Heroku** | [Deploy to Heroku](https://devcenter.heroku.com/articles/deploying-spring-boot-apps-to-heroku) |
| 🔷 **Azure** | [Deploy to Azure App Service](https://docs.microsoft.com/en-us/azure/app-service/) |
| ☁️ **Google Cloud** | [Deploy to Google Cloud Run](https://cloud.google.com/run/docs) |

---

## 🤝 Contributing

We welcome contributions from the community! Here's how you can help:

### **How to Contribute**

1. **Fork the repository**
2. **Create a feature branch**
   ```bash
   git checkout -b feature/AmazingFeature
   ```
3. **Commit your changes**
   ```bash
   git commit -m 'Add some AmazingFeature'
   ```
4. **Push to the branch**
   ```bash
   git push origin feature/AmazingFeature
   ```
5. **Open a Pull Request**

### **Contribution Guidelines**

- ✅ Write clear, concise commit messages
- ✅ Follow existing code style and conventions
- ✅ Add unit tests for new features
- ✅ Update documentation as needed
- ✅ Ensure all tests pass before submitting PR

### **Code of Conduct**

Please read our [Code of Conduct](CODE_OF_CONDUCT.md) before contributing.

---

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2025 HealthFirst Development Team

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software...
```

---

## 💬 Support

### **Need Help?**

- 📧 **Email:** it24102083@my.sliit.lk
- 📝 **Issues:** [GitHub Issues](https://github.com/IT24102083/hospital-management-system/issues)
- 📖 **Documentation:** [Wiki](https://github.com/IT24102083/hospital-management-system/wiki)
- 💬 **Discussions:** [GitHub Discussions](https://github.com/IT24102083/hospital-management-system/discussions)

### **Found a Bug?**

Please [open an issue](https://github.com/IT24102083/hospital-management-system/issues/new) with:
- Clear description of the problem
- Steps to reproduce
- Expected vs actual behavior
- Screenshots (if applicable)

### **Feature Requests**

We'd love to hear your ideas! Submit feature requests through [GitHub Issues](https://github.com/IT24102083/hospital-management-system/issues/new?labels=enhancement).

---

## 🌟 Acknowledgments

- Thanks to all contributors who helped build this project
- Spring Boot team for the excellent framework
- Tailwind CSS for the beautiful UI components
- Open source community for various libraries used

---

## 📊 Project Statistics

![GitHub repo size](https://img.shields.io/github/repo-size/IT24102083/hospital-management-system?style=flat-square)
![GitHub issues](https://img.shields.io/github/issues/IT24102083/hospital-management-system?style=flat-square)
![GitHub pull requests](https://img.shields.io/github/issues-pr/IT24102083/hospital-management-system?style=flat-square)
![GitHub](https://img.shields.io/github/license/IT24102083/hospital-management-system?style=flat-square)

---

<div align="center">

### ⭐ If you find this project useful, please consider giving it a star!

**Made with ❤️ by the Kavi-ya**

[⬆ Back to Top](#-healthfirst-hospital-management-system)

</div>
