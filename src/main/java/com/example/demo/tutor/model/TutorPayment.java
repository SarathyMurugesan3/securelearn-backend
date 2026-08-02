package com.example.demo.tutor.model;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "tutor_payments")
public class TutorPayment {

    @Id
    private String id;
    
    @Indexed
    private String tutorId;
    @Indexed
    private String tutorEmail;
    
    private String tutorName;
    private double amount;
    private String paymentMethod; // CARD, UPI, NETBANKING
    private String paymentReference;
    private String status; // SUCCESS, PENDING, FAILED
    private LocalDateTime paidAt;
    private LocalDateTime paidUntil;
    
    private int studentCount;
    private int contentCount;

    public TutorPayment() {}

    public TutorPayment(String tutorId, String tutorEmail, String tutorName, double amount, 
                        String paymentMethod, String paymentReference, String status, 
                        LocalDateTime paidAt, LocalDateTime paidUntil, int studentCount, int contentCount) {
        this.tutorId = tutorId;
        this.tutorEmail = tutorEmail;
        this.tutorName = tutorName;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentReference = paymentReference;
        this.status = status;
        this.paidAt = paidAt;
        this.paidUntil = paidUntil;
        this.studentCount = studentCount;
        this.contentCount = contentCount;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTutorId() { return tutorId; }
    public void setTutorId(String tutorId) { this.tutorId = tutorId; }

    public String getTutorEmail() { return tutorEmail; }
    public void setTutorEmail(String tutorEmail) { this.tutorEmail = tutorEmail; }

    public String getTutorName() { return tutorName; }
    public void setTutorName(String tutorName) { this.tutorName = tutorName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public LocalDateTime getPaidUntil() { return paidUntil; }
    public void setPaidUntil(LocalDateTime paidUntil) { this.paidUntil = paidUntil; }

    public int getStudentCount() { return studentCount; }
    public void setStudentCount(int studentCount) { this.studentCount = studentCount; }

    public int getContentCount() { return contentCount; }
    public void setContentCount(int contentCount) { this.contentCount = contentCount; }
}
