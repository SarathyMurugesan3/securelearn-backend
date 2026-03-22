package com.example.demo.risk.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "user_risks")
public class UserRisk {

    @Id
    private String id;
    private String userId;
    private String tenantId;
    private int riskScore;
    private LocalDateTime lastUpdated;

    public UserRisk() {}

    public UserRisk(String userId, String tenantId) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.riskScore = 0;
        this.lastUpdated = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
        this.lastUpdated = LocalDateTime.now();
    }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}
