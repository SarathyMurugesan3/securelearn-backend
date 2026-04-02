package com.example.demo.superadmin.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.demo.auth.security.JwtService;
import com.example.demo.superadmin.dto.CreateTenantRequest;
import com.example.demo.tenant.model.Tenant;
import com.example.demo.tenant.repository.TenantRepository;
import com.example.demo.user.model.User;
import com.example.demo.user.repository.UserRepository;

@Service
public class SuperAdminService {

    private final String superAdminEmail;
    private final String superAdminPassword;

    private final JwtService jwtService;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    public SuperAdminService(
            @Value("${super.admin.email}") String superAdminEmail,
            @Value("${super.admin.password}") String superAdminPassword,
            JwtService jwtService,
            TenantRepository tenantRepository,
            UserRepository userRepository) {
        this.superAdminEmail = superAdminEmail;
        this.superAdminPassword = superAdminPassword;
        this.jwtService = jwtService;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
    }

    /**
     * Authenticate super admin utilizing explicitly hardcoded/env credentials.
     * Returns a valid JWT with the "SUPER_ADMIN" role on success.
     */
    public String authenticate(String email, String password) {
        if (!superAdminEmail.equals(email) || !superAdminPassword.equals(password)) {
            throw new SecurityException("Invalid super admin credentials");
        }
        
        // Generate Token: (email, role, tenantId, sessionId)
        // Set tenantId to null and sessionId to null. The JwtAuthenticationFilter will bypass sessionId checks for this role.
        return jwtService.generateAccessToken(email, "SUPER_ADMIN", null, null);
    }

    public List<Tenant> getAllTenants() {
        return tenantRepository.findAll();
    }

    public Tenant createTenant(CreateTenantRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Tenant name cannot be empty.");
        }
        if (request.getType() == null || request.getType().isBlank()) {
            throw new IllegalArgumentException("Tenant type cannot be empty.");
        }
        
        Tenant tenant = new Tenant(request.getName(), request.getType());
        return tenantRepository.save(tenant);
    }

    public Tenant setTenantBlockStatus(String tenantId, boolean blocked) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found."));
        tenant.setBlocked(blocked);
        return tenantRepository.save(tenant);
    }

    public void deleteTenant(String tenantId) {
        if (!tenantRepository.existsById(tenantId)) {
            throw new IllegalArgumentException("Tenant not found.");
        }
        tenantRepository.deleteById(tenantId);
        
        // Caution: In production, consider soft-deleting and cascading soft-deletes to Users/Exams.
    }

    public Page<User> getAllUsersAcrossTenants(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
}
