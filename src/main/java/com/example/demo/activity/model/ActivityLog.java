package com.example.demo.activity.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "activity_logs")
public class ActivityLog {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String tenantId;

    private String action;   // LOGIN, PLAY_VIDEO, ACCESS_PDF, LOGOUT, etc.
    private String ipAddress;
    private String fingerprint;
    @Indexed
    private String adminId;
    private LocalDateTime timestamp;

    public ActivityLog() {}

    public ActivityLog(String userId, String tenantId, String action, String ipAddress) {
        this.userId    = userId;
        this.tenantId  = tenantId;
        this.action    = action;
        this.ipAddress = ipAddress;
        this.timestamp = LocalDateTime.now();
    }

    public ActivityLog(String userId, String action, String ipAddress, String fingerprint, String adminId) {
        this.userId = userId;
        this.action = action;
        this.ipAddress = ipAddress;
        this.fingerprint = fingerprint;
        this.adminId = adminId;
        this.timestamp = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }

    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }
}
