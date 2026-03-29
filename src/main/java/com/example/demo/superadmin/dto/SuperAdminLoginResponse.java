package com.example.demo.superadmin.dto;

public class SuperAdminLoginResponse {
    private String accessToken;

    public SuperAdminLoginResponse(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
