package com.example.demo.risk.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.demo.risk.model.UserRisk;

public interface UserRiskRepository extends MongoRepository<UserRisk, String> {
    Optional<UserRisk> findByUserId(String userId);

    // Tenant Scoped Isolation
    java.util.List<UserRisk> findByTenantId(String tenantId);
}
