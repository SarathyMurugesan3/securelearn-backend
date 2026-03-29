package com.example.demo.discussion.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.demo.discussion.model.DiscussionMessage;

public interface DiscussionRepository extends MongoRepository<DiscussionMessage, String> {

    /** All non-deleted messages in a thread, scoped to tenant — oldest first for natural reading order. */
    Page<DiscussionMessage> findByThreadIdAndTenantIdAndDeletedFalseOrderByCreatedAtAsc(
            String threadId, String tenantId, Pageable pageable);

    /** Direct top-level messages (no parent) in a thread. */
    List<DiscussionMessage> findByThreadIdAndParentMessageIdIsNullAndTenantIdAndDeletedFalseOrderByCreatedAtAsc(
            String threadId, String tenantId);

    /** All nested replies under a specific parent message. */
    List<DiscussionMessage> findByParentMessageIdAndTenantIdAndDeletedFalseOrderByCreatedAtAsc(
            String parentMessageId, String tenantId);

    /** Count active messages in thread (used for thread listing summary). */
    long countByThreadIdAndTenantIdAndDeletedFalse(String threadId, String tenantId);

}
