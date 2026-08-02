package com.example.demo.tutor.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.tutor.model.TutorPayment;

@Repository
public interface TutorPaymentRepository extends MongoRepository<TutorPayment, String> {
    List<TutorPayment> findByTutorId(String tutorId);
    List<TutorPayment> findByTutorEmail(String tutorEmail);
}
