package com.example.demo.user.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.demo.user.model.User;

public interface UserRepository extends MongoRepository<User,String> {

	Optional<User> findByEmail(String email);
	Page<User> findByBlocked(boolean blocked,Pageable pageable);
	Page<User> findByRiskScoreGreaterThan(int riskScore,Pageable pageable);
	long countByBlocked(boolean blocked);
	long countByRiskScoreGreaterThan(int riskScore);
}
