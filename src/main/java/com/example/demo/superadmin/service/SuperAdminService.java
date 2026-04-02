package com.example.demo.superadmin.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

    public SuperAdminService(
            @Value("${super.admin.email}") String superAdminEmail,
            @Value("${super.admin.password}") String superAdminPassword,
            JwtService jwtService,
            TenantRepository tenantRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.superAdminEmail = superAdminEmail;
        this.superAdminPassword = superAdminPassword;
        this.jwtService = jwtService;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Authenticate super admin using env credentials.
     * Returns a JWT with "SUPER_ADMIN" role.
     */
    public String authenticate(String email, String password) {
        if (!superAdminEmail.equals(email) || !superAdminPassword.equals(password)) {
            throw new SecurityException("Invalid super admin credentials");
        }
        return jwtService.generateAccessToken(email, "SUPER_ADMIN", null, null);
    }

    public List<Tenant> getAllTenants() {
        return tenantRepository.findAll();
    }

    /**
     * Create tenant AND auto-create an ADMIN user for that company.
     */
    public Tenant createTenant(CreateTenantRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Tenant name cannot be empty.");
        }
        if (request.getType() == null || request.getType().isBlank()) {
            throw new IllegalArgumentException("Tenant type cannot be empty.");
        }
        if (request.getAdminEmail() == null || request.getAdminEmail().isBlank()) {
            throw new IllegalArgumentException("Admin email is required.");
        }
        if (request.getAdminPassword() == null || request.getAdminPassword().isBlank()) {
            throw new IllegalArgumentException("Admin password is required.");
        }
        if (userRepository.findByEmail(request.getAdminEmail()).isPresent()) {
            throw new IllegalArgumentException("A user with that admin email already exists.");
        }

        // 1. Save the tenant
        Tenant tenant = new Tenant(request.getName(), request.getType());
        tenant = tenantRepository.save(tenant);

        // 2. Auto-create ADMIN user linked to this tenant
        String encodedPassword = passwordEncoder.encode(request.getAdminPassword());
        String adminName = (request.getAdminName() != null && !request.getAdminName().isBlank())
                ? request.getAdminName()
                : request.getName() + " Admin";

        User adminUser = new User(
                adminName,
                request.getAdminEmail(),
                encodedPassword,
                "ADMIN",
                tenant.getId(),
                null // adminId is null for top-level ADMIN users
        );
        userRepository.save(adminUser);

        return tenant;
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
        // Delete all users belonging to this tenant
        userRepository.deleteAll(userRepository.findAll().stream()
                .filter(u -> tenantId.equals(u.getTenantId()))
                .toList());
        tenantRepository.deleteById(tenantId);
    }

    public Page<User> getAllUsersAcrossTenants(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * Returns all ADMIN users for a specific tenant (company admins).
     */
    public List<User> getAdminsForTenant(String tenantId) {
        return userRepository.findAll().stream()
                .filter(u -> tenantId.equals(u.getTenantId()) && "ADMIN".equals(u.getRole()))
                .toList();
    }
}
