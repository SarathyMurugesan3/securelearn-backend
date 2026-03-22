package com.example.demo.watermark.dto;

/**
 * Returned by GET /api/watermark.
 *
 * The frontend uses these values to overlay a visible watermark on the video player.
 * The backend injects the same values into PDF pages at serve time.
 */
public class WatermarkResponse {

    private String email;
    private String ip;
    private String timestamp;
    private String text;      // pre-formatted: "email | ip | timestamp"

    public WatermarkResponse() {}

    public WatermarkResponse(String email, String ip, String timestamp) {
        this.email     = email;
        this.ip        = ip;
        this.timestamp = timestamp;
        this.text      = email + " | " + ip + " | " + timestamp;
    }

    public String getEmail()     { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getIp()        { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getText()      { return text; }
    public void setText(String text) { this.text = text; }
}
