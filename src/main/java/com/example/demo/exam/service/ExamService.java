package com.example.demo.exam.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.demo.exam.model.Exam;
import com.example.demo.exam.model.ExamAttempt;
import com.example.demo.exam.model.Question;
import com.example.demo.exam.repository.ExamAttemptRepository;
import com.example.demo.exam.repository.ExamRepository;
import com.example.demo.exam.repository.QuestionRepository;
import com.example.demo.user.model.User;

/**
 * Central service for the exam system.
 *
 * Responsibilities:
 *  - Create exams with associated questions in one transaction boundary
 *  - Tenant-scoped listing by courseId
 *  - MCQ auto-evaluation on submit
 *  - Configurable attempt-limiting (allowMultipleAttempts flag on Exam)
 */
@Service
public class ExamService {

    private final ExamRepository        examRepository;
    private final QuestionRepository    questionRepository;
    private final ExamAttemptRepository attemptRepository;

    public ExamService(ExamRepository examRepository,
                       QuestionRepository questionRepository,
                       ExamAttemptRepository attemptRepository) {
        this.examRepository     = examRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository  = attemptRepository;
    }

    // ── Create ─────────────────────────────────────────────────────────────────

    /**
     * Creates an exam and saves all bundled questions.
     * The caller (controller) has already verified the creator is TUTOR or ADMIN.
     *
     * @param exam      exam metadata; courseId, moduleId, tenantId must be set
     * @param questions list of questions to attach; may be empty
     * @param creator   the authenticated user creating the exam
     * @return          saved exam with questionIds populated
     */
    public Exam createExam(Exam exam, List<Question> questions, User creator) {
        exam.setAdminId(creator.getId());
        if (exam.getTenantId() == null) {
            exam.setTenantId(creator.getTenantId());
        }
        if (exam.getQuestionIds() == null) {
            exam.setQuestionIds(new ArrayList<>());
        }

        Exam savedExam = examRepository.save(exam);

        // Save each question, stamp examId, link back to exam
        for (Question q : questions) {
            q.setExamId(savedExam.getId());
            Question savedQ = questionRepository.save(q);
            savedExam.getQuestionIds().add(savedQ.getId());
        }
        if (!questions.isEmpty()) {
            examRepository.save(savedExam); // persist questionIds list
        }
        return savedExam;
    }

    // ── List ───────────────────────────────────────────────────────────────────

    public List<Exam> getExamsByCourse(String courseId, String tenantId) {
        return examRepository.findByCourseIdAndTenantId(courseId, tenantId);
    }

    // ── Submit ─────────────────────────────────────────────────────────────────

    /**
     * MCQ auto-evaluation and attempt storage.
     *
     * Rules:
     *  1. Exam must exist + same tenant as student.
     *  2. If allowMultipleAttempts == false → block if any SUBMITTED attempt exists.
     *  3. Auto-evaluate: 1 point per correct answer.
     *  4. Mark attempt SUBMITTED, set endTime.
     *
     * @param examId   the exam being submitted
     * @param answers  map of questionId → chosen option
     * @param student  authenticated student
     * @return         saved ExamAttempt with evaluated score
     */
    public ExamAttempt submitExam(String examId, Map<String, String> answers, User student) {

        // 1. Resolve exam + tenant check
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found."));
        if (student.getTenantId() != null && !student.getTenantId().equals(exam.getTenantId())) {
            throw new SecurityException("Access denied: exam belongs to a different tenant.");
        }

        // 2. Attempt-limit enforcement
        if (!exam.isAllowMultipleAttempts()) {
            long priorSubmitted = attemptRepository.findAllByUserIdAndExamId(student.getId(), examId)
                    .stream()
                    .filter(a -> "SUBMITTED".equals(a.getStatus()))
                    .count();
            if (priorSubmitted > 0) {
                throw new IllegalStateException(
                        "You have already submitted this exam and multiple attempts are not allowed.");
            }
        }

        // 3. Auto-evaluate MCQ
        List<String> questionIds = exam.getQuestionIds();
        List<Question> questions = (List<Question>) questionRepository.findAllById(questionIds);

        int score = 0;
        for (Question q : questions) {
            String studentAnswer = answers.get(q.getId());
            if (studentAnswer != null && studentAnswer.equalsIgnoreCase(q.getCorrectAnswer())) {
                score++;
            }
        }

        // 4. Persist attempt
        ExamAttempt attempt = new ExamAttempt();
        attempt.setUserId(student.getId());
        attempt.setExamId(examId);
        attempt.setTenantId(student.getTenantId());
        attempt.setAnswers(answers);
        attempt.setScore(score);
        attempt.setEndTime(LocalDateTime.now());
        attempt.setStatus("SUBMITTED");

        return attemptRepository.save(attempt);
    }
}
