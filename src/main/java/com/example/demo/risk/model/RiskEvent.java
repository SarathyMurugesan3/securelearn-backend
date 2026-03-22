package com.example.demo.risk.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "risk_events")
public class RiskEvent {

    @Id
    private String id;
    private String userId;
    private String tenantId;
    private String eventType;   // NEW_IP, VPN_SUSPECTED, MULTIPLE_DEVICES, CONCURRENT_SESSIONS
    private int scoreAdded;
    private String ipAddress;
    private String deviceInfo;
    private LocalDateTime occurredAt;

    public RiskEvent() {}

    public RiskEvent(String userId, String tenantId, String eventType, int scoreAdded,
                     String ipAddress, String deviceInfo) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.eventType = eventType;
        this.scoreAdded = scoreAdded;
        this.ipAddress = ipAddress;
        this.deviceInfo = deviceInfo;
        this.occurredAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public int getScoreAdded() { return scoreAdded; }
    public void setScoreAdded(int scoreAdded) { this.scoreAdded = scoreAdded; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }

    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
}
