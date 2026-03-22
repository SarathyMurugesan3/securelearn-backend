package com.example.demo.auth.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.example.demo.auth.model.UserSession;

public interface UserSessionRepository extends MongoRepository<UserSession, String> {
    List<UserSession> findByUserIdAndIsActiveTrueOrderByLoginTimeAsc(String userId);
}
