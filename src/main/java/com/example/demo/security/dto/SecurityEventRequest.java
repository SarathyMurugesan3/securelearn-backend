package com.example.demo.security.dto;

/**
 * Payload for POST /api/security/event
 * Sent by the frontend useSecurityMonitor hook on each violation.
 */
public class SecurityEventRequest {

    private String userId;       // User email / identifier
    private String type;         // SCREENSHOT_KEY, COPY_ATTEMPT, DEVTOOLS_OPEN, RECORDING_SUSPECT, RIGHT_CLICK
    private String timestamp;    // ISO-8601 timestamp from browser
    private int riskScore;       // Accumulated frontend risk score at time of event

    public SecurityEventRequest() {}

    public String getUserId()      { return userId; }
    public String getType()        { return type; }
    public String getTimestamp()   { return timestamp; }
    public int    getRiskScore()   { return riskScore; }

    public void setUserId(String userId)         { this.userId = userId; }
    public void setType(String type)             { this.type = type; }
    public void setTimestamp(String timestamp)   { this.timestamp = timestamp; }
    public void setRiskScore(int riskScore)      { this.riskScore = riskScore; }
}
