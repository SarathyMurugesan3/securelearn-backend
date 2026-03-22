package com.example.demo.content.repository;


import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.demo.content.model.Content;

public interface ContentRepository extends MongoRepository<Content,String>{
	Optional<Content> findById(String id);
	
	// Multi-Admin Scoped Methods
	java.util.List<Content> findAllByUploadedBy(String uploadedBy);
	Page<Content> findAllByUploadedBy(String uploadedBy, Pageable pageable);
	long countByUploadedBy(String uploadedBy);

	// Tenant Scoped Isolation
	java.util.List<Content> findAllByTenantId(String tenantId);
	Page<Content> findAllByTenantId(String tenantId, Pageable pageable);
	long countByTenantId(String tenantId);
}
