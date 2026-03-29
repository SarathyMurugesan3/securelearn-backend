package com.example.demo.risk.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.demo.risk.model.RiskEvent;

public interface RiskEventRepository extends MongoRepository<RiskEvent, String> {
    List<RiskEvent> findByUserIdOrderByOccurredAtDesc(String userId);

    // Tenant Scoped Isolation
    List<RiskEvent> findByTenantIdOrderByOccurredAtDesc(String tenantId);
}
