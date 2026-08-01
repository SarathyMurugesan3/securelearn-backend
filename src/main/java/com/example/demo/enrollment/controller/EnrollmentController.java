package com.example.demo.enrollment.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.example.demo.enrollment.model.Enrollment;
import com.example.demo.enrollment.repository.EnrollmentRepository;
import com.example.demo.user.model.User;
import com.example.demo.user.repository.UserRepository;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    public EnrollmentController(EnrollmentRepository enrollmentRepository, UserRepository userRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
    }

    /**
     * Public / Student list of all tutors available for enrollment
     */
    @GetMapping("/tutors")
    public ResponseEntity<List<User>> getAllAvailableTutors() {
        List<User> tutors = userRepository.findAll().stream()
                .filter(u -> "TUTOR".equalsIgnoreCase(u.getRole()))
                .filter(u -> !u.isBlocked())
                .toList();
        return ResponseEntity.ok(tutors);
    }

    /**
     * Get enrollments for currently logged in student
     */
    @GetMapping("/my")
    public ResponseEntity<List<Enrollment>> getMyEnrollments(Authentication authentication) {
        String email = authentication.getName();
        List<Enrollment> list = enrollmentRepository.findByStudentEmail(email);
        return ResponseEntity.ok(list);
    }

    /**
     * Student enroll / pay to access a tutor
     */
    @PostMapping("/enroll")
    public ResponseEntity<?> enrollStudent(@RequestBody Map<String, Object> body, Authentication authentication) {
        String studentEmail = authentication.getName();
        User student = userRepository.findByEmail(studentEmail).orElse(null);
        if (student == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Student account not found.");
        }

        String tutorId = (String) body.get("tutorId");
        if (tutorId == null || tutorId.isBlank()) {
            return ResponseEntity.badRequest().body("Tutor ID is required.");
        }

        User tutor = userRepository.findById(tutorId).orElse(null);
        if (tutor == null) {
            return ResponseEntity.badRequest().body("Selected tutor not found.");
        }

        // Check if already enrolled
        Optional<Enrollment> existing = enrollmentRepository.findByStudentEmailAndTutorId(studentEmail, tutorId);
        if (existing.isPresent()) {
            return ResponseEntity.ok(existing.get());
        }

        Double amount = body.get("amount") != null ? Double.parseDouble(body.get("amount").toString()) : 0.0;
        String reference = (String) body.getOrDefault("paymentReference", "SIMULATED-PAYMENT-" + System.currentTimeMillis());

        // Default status: APPROVED (or PENDING_PAYMENT if tutor approval required)
        String status = body.containsKey("requireApproval") && Boolean.TRUE.equals(body.get("requireApproval"))
                ? "PENDING_PAYMENT"
                : "APPROVED";

        Enrollment enrollment = new Enrollment(
                student.getId(),
                student.getEmail(),
                student.getName(),
                tutor.getId(),
                tutor.getName(),
                tutor.getEmail(),
                status,
                amount
        );
        enrollment.setPaymentReference(reference);

        Enrollment saved = enrollmentRepository.save(enrollment);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Get enrollment & payment requests for logged-in Tutor
     */
    @GetMapping("/tutor-requests")
    public ResponseEntity<List<Enrollment>> getTutorEnrollmentRequests(Authentication authentication) {
        String tutorEmail = authentication.getName();
        User tutor = userRepository.findByEmail(tutorEmail).orElse(null);
        
        List<Enrollment> requests;
        if (tutor != null) {
            requests = enrollmentRepository.findByTutorId(tutor.getId());
            if (requests.isEmpty()) {
                requests = enrollmentRepository.findByTutorEmail(tutorEmail);
            }
        } else {
            requests = enrollmentRepository.findByTutorEmail(tutorEmail);
        }

        return ResponseEntity.ok(requests);
    }

    /**
     * Tutor approves a student's payment / enrollment request
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveEnrollment(@PathVariable String id, Authentication authentication) {
        Enrollment enrollment = enrollmentRepository.findById(id).orElse(null);
        if (enrollment == null) {
            return ResponseEntity.notFound().build();
        }

        enrollment.setStatus("APPROVED");
        enrollmentRepository.save(enrollment);
        return ResponseEntity.ok("Enrollment approved");
    }

    /**
     * Tutor rejects / revokes a student's enrollment
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectEnrollment(@PathVariable String id, Authentication authentication) {
        Enrollment enrollment = enrollmentRepository.findById(id).orElse(null);
        if (enrollment == null) {
            return ResponseEntity.notFound().build();
        }

        enrollment.setStatus("REJECTED");
        enrollmentRepository.save(enrollment);
        return ResponseEntity.ok("Enrollment rejected");
    }
}
