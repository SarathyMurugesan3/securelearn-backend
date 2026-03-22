package com.example.demo.discussion.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.demo.discussion.model.DiscussionMessage;

public interface DiscussionRepository extends MongoRepository<DiscussionMessage, String> {

    /** Paginated, newest-first, excluding soft-deleted messages, scoped to tenant. */
    Page<DiscussionMessage> findByModuleIdAndTenantIdAndDeletedFalseOrderByCreatedAtDesc(
            String moduleId, String tenantId, Pageable pageable);
}
