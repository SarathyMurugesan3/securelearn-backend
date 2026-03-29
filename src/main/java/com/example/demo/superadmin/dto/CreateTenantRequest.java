package com.example.demo.superadmin.dto;

public class CreateTenantRequest {
    private String name;
    private String type; // EDTECH_COMPANY or SOLO_TUTOR

    public CreateTenantRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
