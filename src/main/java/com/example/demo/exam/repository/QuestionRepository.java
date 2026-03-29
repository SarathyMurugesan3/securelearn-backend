package com.example.demo.exam.repository;

import com.example.demo.exam.model.Question;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepository extends MongoRepository<Question, String> {
	java.util.List<Question> findByTenantId(String tenantId);
	java.util.List<Question> findByExamIdAndTenantId(String examId, String tenantId);
}
