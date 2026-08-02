package com.example.demo.tutor.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.content.repository.ContentRepository;
import com.example.demo.enrollment.repository.EnrollmentRepository;
import com.example.demo.tutor.dto.TutorBillingSummaryDTO;
import com.example.demo.tutor.model.TutorPayment;
import com.example.demo.tutor.repository.TutorPaymentRepository;
import com.example.demo.user.model.User;
import com.example.demo.user.repository.UserRepository;

@Service
public class TutorBillingService {

    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TutorPaymentRepository tutorPaymentRepository;

    private static final double BASE_FEE = 500.0;
    private static final double PER_STUDENT_RATE = 100.0;
    private static final double PER_CONTENT_RATE = 150.0;

    @Autowired
    public TutorBillingService(UserRepository userRepository,
                               ContentRepository contentRepository,
                               EnrollmentRepository enrollmentRepository,
                               TutorPaymentRepository tutorPaymentRepository) {
        this.userRepository = userRepository;
        this.contentRepository = contentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.tutorPaymentRepository = tutorPaymentRepository;
    }

    public TutorBillingSummaryDTO getBillingSummary(String tutorEmail) {
        User tutor = userRepository.findByEmail(tutorEmail)
                .orElseThrow(() -> new RuntimeException("Tutor user not found for email: " + tutorEmail));

        return buildSummaryForUser(tutor);
    }

    public TutorBillingSummaryDTO getBillingSummaryByTutorId(String tutorId) {
        User tutor = userRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor user not found for id: " + tutorId));

        return buildSummaryForUser(tutor);
    }

    private TutorBillingSummaryDTO buildSummaryForUser(User tutor) {
        // Calculate student count assigned to or enrolled under this tutor
        long directStudents = userRepository.findAll().stream()
                .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole()))
                .filter(u -> tutor.getId().equals(u.getAdminId()))
                .count();

        long enrolledStudents = enrollmentRepository.findByTutorId(tutor.getId()).stream()
                .filter(e -> "APPROVED".equalsIgnoreCase(e.getStatus()))
                .map(e -> e.getStudentEmail())
                .distinct()
                .count();

        int studentCount = (int) Math.max(directStudents, enrolledStudents);

        // Calculate uploaded content count
        int contentCount = (int) contentRepository.countByUploadedBy(tutor.getEmail());

        double studentSubtotal = studentCount * PER_STUDENT_RATE;
        double contentSubtotal = contentCount * PER_CONTENT_RATE;
        double totalAmountDue = BASE_FEE + studentSubtotal + contentSubtotal;

        List<TutorPayment> history = tutorPaymentRepository.findByTutorId(tutor.getId());

        TutorBillingSummaryDTO summary = new TutorBillingSummaryDTO();
        summary.setTutorId(tutor.getId());
        summary.setTutorName(tutor.getName());
        summary.setTutorEmail(tutor.getEmail());
        summary.setStudentCount(studentCount);
        summary.setContentCount(contentCount);
        summary.setBaseFee(BASE_FEE);
        summary.setPerStudentRate(PER_STUDENT_RATE);
        summary.setPerContentRate(PER_CONTENT_RATE);
        summary.setStudentSubtotal(studentSubtotal);
        summary.setContentSubtotal(contentSubtotal);
        summary.setTotalAmountDue(totalAmountDue);

        String paymentStatus = tutor.getPaymentStatus();
        if (paymentStatus == null || paymentStatus.isBlank()) {
            paymentStatus = "PAID"; // default active for testing
        }
        summary.setPaymentStatus(paymentStatus);

        String paidUntil = tutor.getPaidUntil();
        if (paidUntil == null || paidUntil.isBlank()) {
            paidUntil = LocalDateTime.now().plusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        summary.setPaidUntil(paidUntil);
        summary.setPaymentHistory(history);

        return summary;
    }

    public TutorBillingSummaryDTO processPayment(String tutorEmail, String paymentMethod, String paymentReference, double customAmount) {
        User tutor = userRepository.findByEmail(tutorEmail)
                .orElseThrow(() -> new RuntimeException("Tutor not found"));

        TutorBillingSummaryDTO currentSummary = buildSummaryForUser(tutor);
        double finalAmount = customAmount > 0 ? customAmount : currentSummary.getTotalAmountDue();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime paidUntil = now.plusDays(30);

        String ref = (paymentReference != null && !paymentReference.isBlank())
                ? paymentReference
                : "PAY-TUTOR-" + System.currentTimeMillis();

        TutorPayment payment = new TutorPayment(
                tutor.getId(),
                tutor.getEmail(),
                tutor.getName(),
                finalAmount,
                paymentMethod != null ? paymentMethod : "CARD",
                ref,
                "SUCCESS",
                now,
                paidUntil,
                currentSummary.getStudentCount(),
                currentSummary.getContentCount()
        );

        tutorPaymentRepository.save(payment);

        tutor.setPaymentStatus("PAID");
        tutor.setPaidUntil(paidUntil.format(DateTimeFormatter.ISO_LOCAL_DATE));
        userRepository.save(tutor);

        return buildSummaryForUser(tutor);
    }

    public List<TutorBillingSummaryDTO> getAllTutorsBillingSummary() {
        return userRepository.findAll().stream()
                .filter(u -> "TUTOR".equalsIgnoreCase(u.getRole()))
                .map(this::buildSummaryForUser)
                .toList();
    }
}
