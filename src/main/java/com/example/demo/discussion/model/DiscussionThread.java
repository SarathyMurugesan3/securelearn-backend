package com.example.demo.discussion.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "discussion_threads")
public class DiscussionThread {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    @Indexed
    private String courseId;

    private String moduleId; // nullable — thread may belong to a course, not a specific module

    @Indexed
    private String createdBy; // userId of the author

    private String createdByName; // denormalised for display
    private String createdByRole;

    private String title;

    private LocalDateTime createdAt;

    public DiscussionThread() {
    }

    public DiscussionThread(String tenantId, String courseId, String moduleId,
            String createdBy, String createdByName, String createdByRole,
            String title) {
        this.tenantId = tenantId;
        this.courseId = courseId;
        this.moduleId = moduleId;
        this.createdBy = createdBy;
        this.createdByName = createdByName;
        this.createdByRole = createdByRole;
        this.title = title;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getModuleId() {
        return moduleId;
    }

    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public String getCreatedByRole() {
        return createdByRole;
    }

    public void setCreatedByRole(String createdByRole) {
        this.createdByRole = createdByRole;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
