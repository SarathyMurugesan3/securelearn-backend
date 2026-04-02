package com.example.demo.superadmin.dto;

public class CreateTenantRequest {
    private String name;
    private String type; // EDTECH_COMPANY or SOLO_TUTOR

    // Admin user auto-created along with the tenant
    private String adminName;
    private String adminEmail;
    private String adminPassword;

    public CreateTenantRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getAdminName() { return adminName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }

    public String getAdminEmail() { return adminEmail; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }

    public String getAdminPassword() { return adminPassword; }
    public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }
}
