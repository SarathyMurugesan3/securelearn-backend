package com.example.demo.superadmin.dto;

public class SuperAdminLoginRequest {
    private String email;
    private String password;

    public SuperAdminLoginRequest() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
