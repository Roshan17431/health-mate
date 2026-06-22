# 🏥 Health Mate

Health Mate is a healthcare management system built using **Spring Boot**, designed to simplify doctor-patient interactions, appointment scheduling, notifications, and healthcare administration.

---

## 🚀 Features

### 👤 User Management
- User Registration & Authentication
- JWT-Based Security
- Role-Based Access Control
- Profile Management

### 🩺 Doctor Management
- Doctor Registration
- Specialization Management
- Experience Tracking
- Availability Management

### 🧑‍⚕️ Patient Management
- Patient Profiles
- Appointment History
- Healthcare Information Tracking

### 📅 Appointment Management
- Book Appointments
- Cancel Appointments
- Appointment Status Tracking
- Unique Appointment Codes

### ⏰ Time Slot Management
- Doctor Time Slot Creation
- Slot Availability Tracking
- Prevent Double Booking using Optimistic Locking

### 🔔 Notification System
- Appointment Notifications
- Read/Unread Tracking

### 🔐 OTP Verification
- Email OTP Generation
- OTP Verification
- Password Reset Support

### 📋 Audit Logging
- User Activity Tracking
- System Action Logging

---

## 🛠️ Tech Stack

### Backend
- Java 21
- Spring Boot 3
- Spring Security
- Spring Data JPA
- Hibernate

### Database
- MySQL / PostgreSQL

### Authentication
- JWT (JSON Web Token)

### Documentation
- Swagger / OpenAPI

### Build Tool
- Maven

### Utilities
- Lombok
- Jakarta Validation

---

## 📂 Project Structure

```text
src/main/java/com/example/health/mate
│
├── config
├── controller
├── domain
├── dto
├── entity
├── exception
├── repository
├── security
├── service
└── HealthMateApplication
```

---

## 🗄️ Entities

- User
- Doctor
- Patient
- Appointment
- TimeSlot
- DoctorSchedule
- Notification
- OtpToken
- AuditLog

---

## 🔑 User Roles

### Admin
- Manage Users
- Manage Doctors
- View Audit Logs

### Doctor
- Manage Schedule
- View Appointments
- Update Availability

### Patient
- Book Appointments
- View Appointment History
- Manage Profile

---

## ⚙️ Setup & Installation

### Clone Repository

```bash
git clone https://github.com/your-username/health-mate.git
cd health-mate
```

### Configure Database

Update `application.properties`

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/health_mate
spring.datasource.username=root
spring.datasource.password=your_password

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

### Run Application

```bash
mvn clean install
mvn spring-boot:run
```

Application runs on:

```text
http://localhost:8080
```

---

## 📖 API Documentation

Swagger UI

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON

```text
http://localhost:8080/v3/api-docs
```

---

## 🔒 Security Features

- JWT Authentication
- BCrypt Password Encryption
- Role-Based Authorization
- Secure REST APIs
- Global Exception Handling

---

## 📌 Future Enhancements

- Video Consultation
- Prescription Management
- Medical Records Storage
- Payment Gateway Integration
- Email & SMS Notifications
- AI Health Assistant
- Analytics Dashboard

---

## 👨‍💻 Author

**Roshan Mathew**

Built using Spring Boot, Spring Security, JPA, JWT, and modern backend development practices.
