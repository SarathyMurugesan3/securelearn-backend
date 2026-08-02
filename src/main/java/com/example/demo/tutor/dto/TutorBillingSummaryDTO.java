package com.example.demo.tutor.dto;

import java.util.List;
import com.example.demo.tutor.model.TutorPayment;

public class TutorBillingSummaryDTO {

    private String tutorId;
    private String tutorName;
    private String tutorEmail;
    
    private int studentCount;
    private int contentCount;
    
    private double baseFee;
    private double perStudentRate;
    private double perContentRate;
    
    private double studentSubtotal;
    private double contentSubtotal;
    private double totalAmountDue;
    
    private String paymentStatus; // PAID, DUE, UNPAID
    private String paidUntil;
    
    private List<TutorPayment> paymentHistory;

    public TutorBillingSummaryDTO() {}

    public String getTutorId() { return tutorId; }
    public void setTutorId(String tutorId) { this.tutorId = tutorId; }

    public String getTutorName() { return tutorName; }
    public void setTutorName(String tutorName) { this.tutorName = tutorName; }

    public String getTutorEmail() { return tutorEmail; }
    public void setTutorEmail(String tutorEmail) { this.tutorEmail = tutorEmail; }

    public int getStudentCount() { return studentCount; }
    public void setStudentCount(int studentCount) { this.studentCount = studentCount; }

    public int getContentCount() { return contentCount; }
    public void setContentCount(int contentCount) { this.contentCount = contentCount; }

    public double getBaseFee() { return baseFee; }
    public void setBaseFee(double baseFee) { this.baseFee = baseFee; }

    public double getPerStudentRate() { return perStudentRate; }
    public void setPerStudentRate(double perStudentRate) { this.perStudentRate = perStudentRate; }

    public double getPerContentRate() { return perContentRate; }
    public void setPerContentRate(double perContentRate) { this.perContentRate = perContentRate; }

    public double getStudentSubtotal() { return studentSubtotal; }
    public void setStudentSubtotal(double studentSubtotal) { this.studentSubtotal = studentSubtotal; }

    public double getContentSubtotal() { return contentSubtotal; }
    public void setContentSubtotal(double contentSubtotal) { this.contentSubtotal = contentSubtotal; }

    public double getTotalAmountDue() { return totalAmountDue; }
    public void setTotalAmountDue(double totalAmountDue) { this.totalAmountDue = totalAmountDue; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaidUntil() { return paidUntil; }
    public void setPaidUntil(String paidUntil) { this.paidUntil = paidUntil; }

    public List<TutorPayment> getPaymentHistory() { return paymentHistory; }
    public void setPaymentHistory(List<TutorPayment> paymentHistory) { this.paymentHistory = paymentHistory; }
}
