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

	// Multi-Admin Scoped Methods
	Page<User> findByAdminId(String adminId, Pageable pageable);
	Page<User> findByBlockedAndAdminId(boolean blocked, String adminId, Pageable pageable);
	Page<User> findByRiskScoreGreaterThanAndAdminId(int riskScore, String adminId, Pageable pageable);
	long countByAdminId(String adminId);
	long countByBlockedAndAdminId(boolean blocked, String adminId);
	long countByRiskScoreGreaterThanAndAdminId(int riskScore, String adminId);

	// Tenant Scoped Methods
	Page<User> findByTenantId(String tenantId, Pageable pageable);
	Page<User> findByBlockedAndTenantId(boolean blocked, String tenantId, Pageable pageable);
	Page<User> findByRiskScoreGreaterThanAndTenantId(int riskScore, String tenantId, Pageable pageable);
	long countByTenantId(String tenantId);
	long countByBlockedAndTenantId(boolean blocked, String tenantId);
	long countByRiskScoreGreaterThanAndTenantId(int riskScore, String tenantId);
}
