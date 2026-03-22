package com.example.demo.activity.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.demo.activity.model.ActivityLog;

public interface ActivityLogRepository extends MongoRepository<ActivityLog, String> {
    List<ActivityLog> findByUserIdOrderByTimestampDesc(String userId);
    List<ActivityLog> findByTenantIdOrderByTimestampDesc(String tenantId);

	long countByAction(String action);
	List<ActivityLog> findByTimestampAfter(LocalDateTime time);
	
	// Multi-Admin Scoped Methods
	long countByActionAndAdminId(String action, String adminId);
	List<ActivityLog> findByTimestampAfterAndAdminId(LocalDateTime time, String adminId);
}
