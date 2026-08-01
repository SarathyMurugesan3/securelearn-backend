package com.example.demo.enrollment.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.example.demo.enrollment.model.Enrollment;

public interface EnrollmentRepository extends MongoRepository<Enrollment, String> {
    List<Enrollment> findByStudentId(String studentId);
    List<Enrollment> findByStudentEmail(String studentEmail);
    List<Enrollment> findByTutorId(String tutorId);
    List<Enrollment> findByTutorEmail(String tutorEmail);
    Optional<Enrollment> findByStudentIdAndTutorId(String studentId, String tutorId);
    Optional<Enrollment> findByStudentEmailAndTutorId(String studentEmail, String tutorId);
}
