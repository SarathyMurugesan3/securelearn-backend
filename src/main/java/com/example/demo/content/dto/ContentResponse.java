package com.example.demo.content.dto;

import java.time.LocalDateTime;

public class ContentResponse {

    private String id;
    private String title;
    private String type;
    private LocalDateTime uploadedAt;

    public ContentResponse(String id,
                           String title,
                           String type,
                           LocalDateTime uploadedAt) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.uploadedAt = uploadedAt;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getType() { return type; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}