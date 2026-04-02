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

import java.util.List;

/**
 * ADMIN panel: Create/manage users under the logged-in admin's tenant.
 * Supports creating TUTOR and STUDENT roles.
 * The adminId is always resolved from JWT — never trusted from the request body.
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @Autowired
    public AdminUserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    /**
     * Create a TUTOR or STUDENT under the admin's tenant.
     * Accepted roles: TUTOR, STUDENT
     */
    @PostMapping
    public ResponseEntity<?> createUser(
            @RequestBody CreateUserRequest request,
            Authentication authentication) {

        String adminEmail = authentication.getName();
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        // Validate target role — ADMIN can only create TUTOR or STUDENT
        String requestedRole = request.getRole();
        if (requestedRole == null ||
                (!requestedRole.equalsIgnoreCase("TUTOR") && !requestedRole.equalsIgnoreCase("STUDENT"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid role. Admin can only create TUTOR or STUDENT users.");
        }

        request.setRole(requestedRole.toUpperCase());
        request.setAdminId(admin.getId());
        request.setTenantId(admin.getTenantId());

        User created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * List all TUTOR users under the admin.
     */
    @GetMapping("/tutors")
    public ResponseEntity<List<User>> getTutors(Authentication authentication) {
        String adminEmail = authentication.getName();
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        List<User> tutors = userRepository.findAll().stream()
                .filter(u -> admin.getId().equals(u.getAdminId()) && "TUTOR".equals(u.getRole()))
                .toList();

        return ResponseEntity.ok(tutors);
    }

    /**
     * List all STUDENT users under the admin.
     */
    @GetMapping("/students")
    public ResponseEntity<List<User>> getStudents(Authentication authentication) {
        String adminEmail = authentication.getName();
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        List<User> students = userRepository.findAll().stream()
                .filter(u -> admin.getId().equals(u.getAdminId()) && "STUDENT".equals(u.getRole()))
                .toList();

        return ResponseEntity.ok(students);
    }

    /**
     * Block a user (TUTOR or STUDENT) under this admin.
     */
    @PostMapping("/{id}/block")
    public ResponseEntity<?> blockUser(@PathVariable String id, Authentication authentication) {
        String adminEmail = authentication.getName();
        User admin = userRepository.findByEmail(adminEmail).orElseThrow();
        User user = userRepository.findById(id).orElseThrow();
        if (!admin.getId().equals(user.getAdminId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized");

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
        User admin = userRepository.findByEmail(adminEmail).orElseThrow();
        User user = userRepository.findById(id).orElseThrow();
        if (!admin.getId().equals(user.getAdminId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized");

        user.setBlocked(false);
        user.setRiskScore(0);
        userRepository.save(user);
        return ResponseEntity.ok("User unblocked");
    }

    /**
     * Delete a user under this admin.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id, Authentication authentication) {
        String adminEmail = authentication.getName();
        User admin = userRepository.findByEmail(adminEmail).orElseThrow();
        User user = userRepository.findById(id).orElseThrow();
        if (!admin.getId().equals(user.getAdminId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized");

        userRepository.deleteById(id);
        return ResponseEntity.ok("User deleted");
    }
}
