package com.example.demo.admin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.example.demo.user.dto.CreateUserRequest;
import com.example.demo.user.model.User;
import com.example.demo.user.repository.UserRepository;
import com.example.demo.user.service.UserService;
import com.example.demo.risk.repository.UserRiskRepository;
// import com.example.demo.risk.model.UserRisk;

import java.util.List;

/**
 * ADMIN panel: Create/manage users under the logged-in admin's tenant.
 * Supports creating TUTOR and STUDENT roles.
 * The adminId is always resolved from JWT — never trusted from the request
 * body.
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final UserRiskRepository userRiskRepository;

    @Autowired
    public AdminUserController(UserService userService, UserRepository userRepository,
            UserRiskRepository userRiskRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.userRiskRepository = userRiskRepository;
    }

    /**
     * Create a TUTOR or STUDENT under the admin's tenant.
     * Accepted roles: TUTOR, STUDENT
     * Note: Super Admin CANNOT create STUDENT accounts.
     */
    @PostMapping
    public ResponseEntity<?> createUser(
            @RequestBody CreateUserRequest request,
            Authentication authentication) {

        String adminEmail = authentication.getName();
        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        boolean isSuperAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("SUPER_ADMIN"));

        // Validate target role — ADMIN can only create TUTOR or STUDENT
        String requestedRole = request.getRole();
        if (requestedRole == null ||
                (!requestedRole.equalsIgnoreCase("TUTOR") && !requestedRole.equalsIgnoreCase("STUDENT"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid role. Admin can only create TUTOR or STUDENT users.");
        }

        // Requirement: Super Admin CANNOT add STUDENT accounts
        if (isSuperAdmin && requestedRole.equalsIgnoreCase("STUDENT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Super Admin is not allowed to add student accounts.");
        }

        // Prevent horizontal privilege escalation: TUTORs cannot create other TUTORs
        if (admin != null && "TUTOR".equalsIgnoreCase(admin.getRole()) && requestedRole.equalsIgnoreCase("TUTOR")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Tutors are only permitted to create STUDENT accounts.");
        }

        request.setRole(requestedRole.toUpperCase());
        if (admin != null) {
            request.setAdminId(admin.getId());
            request.setTenantId(admin.getTenantId());
        } else if (isSuperAdmin) {
            request.setAdminId("superadmin");
            if (request.getTenantId() == null) {
                request.setTenantId("default");
            }
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Admin user not found.");
        }

        User created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * List all TUTOR users under the admin.
     */
    @GetMapping("/tutors")
    public ResponseEntity<List<User>> getTutors(Authentication authentication) {
        String adminEmail = authentication.getName();
        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        boolean isSuperAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("SUPER_ADMIN"));

        List<User> tutors = userRepository.findAll().stream()
                .filter(u -> "TUTOR".equalsIgnoreCase(u.getRole()))
                .filter(u -> isSuperAdmin || (admin != null && (admin.getId().equals(u.getAdminId()) || 
                        (admin.getTenantId() != null && admin.getTenantId().equals(u.getTenantId())))))
                .toList();

        return ResponseEntity.ok(tutors);
    }

    /**
     * List all STUDENT users under the admin.
     */
    @GetMapping("/students")
    public ResponseEntity<List<User>> getStudents(Authentication authentication) {
        String adminEmail = authentication.getName();
        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        boolean isSuperAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("SUPER_ADMIN"));

        List<User> students = userRepository.findAll().stream()
                .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole()))
                .filter(u -> isSuperAdmin || (admin != null && (admin.getId().equals(u.getAdminId()) || 
                        (admin.getTenantId() != null && admin.getTenantId().equals(u.getTenantId())))))
                .toList();

        return ResponseEntity.ok(students);
    }

    /**
     * Block a user (TUTOR or STUDENT) under this admin.
     */
    @PostMapping("/{id}/block")
    public ResponseEntity<?> blockUser(@PathVariable String id, Authentication authentication) {
        String adminEmail = authentication.getName();
        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        boolean isSuperAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("SUPER_ADMIN"));

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }

        if (!isSuperAdmin && (admin == null || (!admin.getId().equals(user.getAdminId()) && !"ADMIN".equalsIgnoreCase(admin.getRole())))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized");
        }

        user.setBlocked(true);
        userRepository.save(user);
        return ResponseEntity.ok("User blocked");
    }

    /**
     * Unblock a user (TUTOR or STUDENT) under this admin.
     */
    @PostMapping("/{id}/unblock")
    public ResponseEntity<?> unblockUser(@PathVariable String id, Authentication authentication) {
        String adminEmail = authentication.getName();
        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        boolean isSuperAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("SUPER_ADMIN"));

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }

        if (!isSuperAdmin && (admin == null || (!admin.getId().equals(user.getAdminId()) && !"ADMIN".equalsIgnoreCase(admin.getRole())))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized");
        }

        user.setBlocked(false);
        user.setRiskScore(0);
        userRepository.save(user);

        // Also clear the risk score in the UserRisk collection
        userRiskRepository.findByUserId(id).ifPresent(risk -> {
            risk.setRiskScore(0);
            userRiskRepository.save(risk);
        });

        return ResponseEntity.ok("User unblocked");
    }

    /**
     * Delete a user under this admin.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id, Authentication authentication) {
        String adminEmail = authentication.getName();
        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        boolean isSuperAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("SUPER_ADMIN"));

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }

        if (!isSuperAdmin && (admin == null || (!admin.getId().equals(user.getAdminId()) && !"ADMIN".equalsIgnoreCase(admin.getRole())))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized");
        }

        userRepository.deleteById(id);
        return ResponseEntity.ok("User deleted");
    }
}
