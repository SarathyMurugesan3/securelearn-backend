package com.example.demo.exam.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.exam.model.Exam;
import com.example.demo.exam.model.ExamAttempt;
import com.example.demo.exam.model.Question;
import com.example.demo.exam.service.ExamService;
import com.example.demo.user.model.User;
import com.example.demo.user.repository.UserRepository;

/**
 * Unified exam API matching the requested endpoint shapes.
 *
 * POST /api/exam/create           → TUTOR / ADMIN only
 * GET  /api/exam/{courseId}       → STUDENT / TUTOR / ADMIN (tenant-scoped)
 * POST /api/exam/submit/{examId}  → STUDENT only
 *
 * The existing /api/admin/exams/* and /api/student/exams/* controllers are
 * preserved for backward compatibility.
 */
@RestController
@RequestMapping("/api/exam")
public class ExamController {

    private final ExamService    examService;
    private final UserRepository userRepository;

    public ExamController(ExamService examService, UserRepository userRepository) {
        this.examService    = examService;
        this.userRepository = userRepository;
    }

    // ── POST /api/exam/create ──────────────────────────────────────────────────

    /**
     * Create a new exam with optional embedded questions.
     *
     * Request body:
     * {
     *   "exam": { "title": "...", "courseId": "...", "moduleId": "...",
     *             "durationMinutes": 30, "totalMarks": 10,
     *             "allowMultipleAttempts": false },
     *   "questions": [
     *     { "text": "What is 2+2?", "options": ["1","2","3","4"], "correctAnswer": "4" }
     *   ]
     * }
     */
    @PostMapping("/create")
    public ResponseEntity<?> createExam(
            @RequestBody CreateExamRequest body,
            Authentication authentication) {

        User caller = resolveUser(authentication);

        // Only TUTOR and ADMIN may create exams
        String role = caller.getRole();
        if (!"TUTOR".equals(role) && !"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only tutors and admins can create exams.");
        }

        Exam exam      = body.getExam();
        List<Question> questions = body.getQuestions() != null ? body.getQuestions() : List.of();

        if (exam == null || exam.getTitle() == null || exam.getTitle().isBlank()) {
            return ResponseEntity.badRequest().body("Exam title is required.");
        }

        Exam saved = examService.createExam(exam, questions, caller);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ── GET /api/exam/{courseId} ───────────────────────────────────────────────

    /**
     * List all exams for a course, scoped to the caller's tenant.
     * Correct answers are never exposed — this returns exam metadata only.
     */
    @GetMapping("/{courseId}")
    public ResponseEntity<?> getExamsByCourse(
            @PathVariable String courseId,
            Authentication authentication) {

        User caller = resolveUser(authentication);
        List<Exam> exams = examService.getExamsByCourse(courseId, caller.getTenantId());
        return ResponseEntity.ok(exams);
    }

    // ── POST /api/exam/submit/{examId} ─────────────────────────────────────────

    /**
     * Submit answers and receive an auto-evaluated score.
     *
     * Request body: Map of { questionId → chosenOption }
     * e.g. { "q1id": "4", "q2id": "Paris" }
     *
     * Enforces:
     *  - STUDENT role only
     *  - Tenant isolation (student tenant must match exam tenant)
     *  - Attempt limit (if exam.allowMultipleAttempts == false)
     */
    @PostMapping("/submit/{examId}")
    public ResponseEntity<?> submitExam(
            @PathVariable String examId,
            @RequestBody Map<String, String> answers,
            Authentication authentication) {

        User caller = resolveUser(authentication);

        if (!"STUDENT".equals(caller.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only students can submit exam answers.");
        }

        try {
            ExamAttempt result = examService.submitExam(examId, answers, caller);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    // ── Private ────────────────────────────────────────────────────────────────

    private User resolveUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found."));
    }

    // ── Inner request DTO ──────────────────────────────────────────────────────

    public static class CreateExamRequest {
        private Exam           exam;
        private List<Question> questions;

        public Exam           getExam()                        { return exam; }
        public void           setExam(Exam exam)              { this.exam = exam; }
        public List<Question> getQuestions()                  { return questions; }
        public void           setQuestions(List<Question> q)  { this.questions = q; }
    }
}
