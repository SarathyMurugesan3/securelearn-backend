package com.example.demo.exam.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "exams")
public class Exam {
    @Id
    private String id;
    private String title;
    private String description;
    private int durationMinutes;
    private int passingScore;
    private int totalMarks;               // NEW: max marks for auto-score calculation
    private List<String> questionIds;
    private String adminId;

    // NEW: multi-tenant + course/module scoping
    @Indexed
    private String courseId;
    @Indexed
    private String moduleId;
    @Indexed
    private String tenantId;

    /** If false (default), a student may only attempt this exam once. */
    private boolean allowMultipleAttempts = false;

    public Exam() {}

    public Exam(String title, String description, int durationMinutes, int passingScore, List<String> questionIds, String adminId) {
        this.title = title;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.passingScore = passingScore;
        this.questionIds = questionIds;
        this.adminId = adminId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public int getPassingScore() {
        return passingScore;
    }

    public void setPassingScore(int passingScore) {
        this.passingScore = passingScore;
    }

    public List<String> getQuestionIds() {
        return questionIds;
    }

    public void setQuestionIds(List<String> questionIds) {
        this.questionIds = questionIds;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public String getCourseId()                { return courseId; }
    public void   setCourseId(String v)       { this.courseId = v; }

    public String getModuleId()                { return moduleId; }
    public void   setModuleId(String v)       { this.moduleId = v; }

    public String getTenantId()                { return tenantId; }
    public void   setTenantId(String v)       { this.tenantId = v; }

    public int  getTotalMarks()                { return totalMarks; }
    public void setTotalMarks(int v)          { this.totalMarks = v; }

    public boolean isAllowMultipleAttempts()           { return allowMultipleAttempts; }
    public void    setAllowMultipleAttempts(boolean v) { this.allowMultipleAttempts = v; }
}
