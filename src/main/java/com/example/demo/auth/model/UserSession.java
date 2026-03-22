package com.example.demo.auth.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "user_sessions")
public class UserSession {

    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    @Indexed
    private String tenantId;
    private String ipAddress;
    private String deviceInfo;
    private LocalDateTime loginTime;
    private LocalDateTime lastActive;
    private boolean isActive;

    public UserSession() {}

    public UserSession(String userId, String tenantId, String ipAddress, String deviceInfo) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.ipAddress = ipAddress;
        this.deviceInfo = deviceInfo;
        this.loginTime = LocalDateTime.now();
        this.lastActive = LocalDateTime.now();
        this.isActive = true;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }

    public LocalDateTime getLoginTime() { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }

    public LocalDateTime getLastActive() { return lastActive; }
    public void setLastActive(LocalDateTime lastActive) { this.lastActive = lastActive; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
