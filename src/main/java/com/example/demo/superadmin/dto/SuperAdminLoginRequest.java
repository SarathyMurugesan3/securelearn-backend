package com.example.demo.superadmin.dto;

public class SuperAdminLoginRequest {
    private String username;
    private String password;

    public SuperAdminLoginRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
