package com.example.demo.discussion.dto;

/** Inbound payload for POST /discussion/send */
public class SendMessageRequest {
    private String moduleId;
    private String courseId;
    private String message;

    public String getModuleId() { return moduleId; }
    public void setModuleId(String moduleId) { this.moduleId = moduleId; }

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
