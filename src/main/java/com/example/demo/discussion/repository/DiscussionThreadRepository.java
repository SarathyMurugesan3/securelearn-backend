package com.example.demo.discussion.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.discussion.model.DiscussionThread;

@Repository
public interface DiscussionThreadRepository extends MongoRepository<DiscussionThread, String> {

    /** All threads in a course, scoped to tenant — newest first. */
    List<DiscussionThread> findByCourseIdAndTenantIdOrderByCreatedAtDesc(String courseId, String tenantId);

    /** All threads in a specific module, scoped to tenant. */
    List<DiscussionThread> findByModuleIdAndTenantIdOrderByCreatedAtDesc(String moduleId, String tenantId);

    /** All threads created by a specific user within tenant. */
    List<DiscussionThread> findByCreatedByAndTenantId(String createdBy, String tenantId);
}
