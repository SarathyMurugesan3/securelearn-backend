package com.example.demo.superadmin.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.superadmin.dto.CreateTenantRequest;
import com.example.demo.superadmin.dto.SuperAdminLoginRequest;
import com.example.demo.superadmin.dto.SuperAdminLoginResponse;
import com.example.demo.superadmin.service.SuperAdminService;
import com.example.demo.tenant.model.Tenant;
import com.example.demo.user.model.User;

@RestController
@RequestMapping("/api/super-admin")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    public SuperAdminController(SuperAdminService superAdminService) {
        this.superAdminService = superAdminService;
    }

    // ── Publicly accessible for initialization (guarded by credential check) ──

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody SuperAdminLoginRequest request) {
        try {
            String token = superAdminService.authenticate(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(new SuperAdminLoginResponse(token));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    // ── Routes below require the "SUPER_ADMIN" role (handled in SecurityConfig) ──

    @GetMapping("/tenants")
    public ResponseEntity<List<Tenant>> getAllTenants() {
        return ResponseEntity.ok(superAdminService.getAllTenants());
    }

    @PostMapping("/tenant")
    public ResponseEntity<?> createTenant(@RequestBody CreateTenantRequest request) {
        try {
            Tenant created = superAdminService.createTenant(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/block/{tenantId}")
    public ResponseEntity<?> blockTenant(@PathVariable String tenantId) {
        try {
            Tenant updated = superAdminService.setTenantBlockStatus(tenantId, true);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PutMapping("/unblock/{tenantId}")
    public ResponseEntity<?> unblockTenant(@PathVariable String tenantId) {
        try {
            // Helper route explicitly for unblocking
            Tenant updated = superAdminService.setTenantBlockStatus(tenantId, false);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/tenant/{tenantId}")
    public ResponseEntity<?> deleteTenant(@PathVariable String tenantId) {
        try {
            superAdminService.deleteTenant(tenantId);
            return ResponseEntity.ok("Tenant successfully deleted.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/users")
    public ResponseEntity<Page<User>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(superAdminService.getAllUsersAcrossTenants(pageable));
    }
}
