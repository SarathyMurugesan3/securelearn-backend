package com.example.demo.enrollment.model;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "enrollments")
public class Enrollment {

    @Id
    private String id;

    @Indexed
    private String studentId;
    private String studentEmail;
    private String studentName;

    @Indexed
    private String tutorId;
    private String tutorName;
    private String tutorEmail;

    private String status; // APPROVED, PENDING_PAYMENT, REJECTED
    private Double amount;
    private String paymentReference;
    private LocalDateTime createdAt;

    public Enrollment() {
        this.createdAt = LocalDateTime.now();
        this.status = "APPROVED"; // Default to APPROVED for immediate access or pending payment
    }

    public Enrollment(String studentId, String studentEmail, String studentName, String tutorId, String tutorName, String tutorEmail, String status, Double amount) {
        this.studentId = studentId;
        this.studentEmail = studentEmail;
        this.studentName = studentName;
        this.tutorId = tutorId;
        this.tutorName = tutorName;
        this.tutorEmail = tutorEmail;
        this.status = status != null ? status : "APPROVED";
        this.amount = amount != null ? amount : 0.0;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getTutorId() { return tutorId; }
    public void setTutorId(String tutorId) { this.tutorId = tutorId; }

    public String getTutorName() { return tutorName; }
    public void setTutorName(String tutorName) { this.tutorName = tutorName; }

    public String getTutorEmail() { return tutorEmail; }
    public void setTutorEmail(String tutorEmail) { this.tutorEmail = tutorEmail; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
