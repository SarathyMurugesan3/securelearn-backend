package com.example.demo.discussion.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "discussion_messages")
public class DiscussionMessage {

    @Id
    private String id;

    @Indexed
    private String threadId;           // parent thread (required)

    private String parentMessageId;    // null = top-level; non-null = nested reply

    @Indexed
    private String userId;

    @Indexed
    private String tenantId;

    private String senderName;  // denormalised for display
    private String senderRole;  // STUDENT | TUTOR | ADMIN
    private String message;
    private boolean deleted;    // soft-delete flag (moderation)
    private LocalDateTime createdAt;

    public DiscussionMessage() {}

    public DiscussionMessage(String threadId, String parentMessageId, String userId,
                             String tenantId, String senderName, String senderRole,
                             String message) {
        this.threadId         = threadId;
        this.parentMessageId  = parentMessageId;
        this.userId           = userId;
        this.tenantId         = tenantId;
        this.senderName       = senderName;
        this.senderRole       = senderRole;
        this.message          = message;
        this.deleted          = false;
        this.createdAt        = LocalDateTime.now();
    }

    // Getters & Setters
    public String getId()                     { return id; }
    public void setId(String id)             { this.id = id; }

    public String getThreadId()               { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }

    public String getParentMessageId()                          { return parentMessageId; }
    public void setParentMessageId(String parentMessageId)     { this.parentMessageId = parentMessageId; }

    public String getUserId()                 { return userId; }
    public void setUserId(String userId)     { this.userId = userId; }

    public String getTenantId()               { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getSenderName()             { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderRole()             { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }

    public String getMessage()                { return message; }
    public void setMessage(String message)   { this.message = message; }

    public boolean isDeleted()                { return deleted; }
    public void setDeleted(boolean deleted)  { this.deleted = deleted; }

    public LocalDateTime getCreatedAt()       { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
