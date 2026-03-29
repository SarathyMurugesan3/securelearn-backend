package com.example.demo.security.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.monitoring.service.MonitoringService;
import com.example.demo.security.dto.SecurityEventRequest;
import com.example.demo.user.model.User;
import com.example.demo.user.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

/**
 * SecurityController — POST /api/security/event
 *
 * Receives security violation events from the frontend useSecurityMonitor hook.
 * Stores the event as an ActivityLog, updates the user's risk score, and
 * blocks the user if the backend threshold is breached.
 *
 * NOTE: OS-level screenshots cannot be fully blocked in browsers.
 * This system provides deterrence, detection, and content protection only.
 */
@RestController
@RequestMapping("/api/security")
public class SecurityController {

    private final MonitoringService monitoringService;
    private final UserRepository userRepository;

    @Autowired
    public SecurityController(MonitoringService monitoringService, UserRepository userRepository) {
        this.monitoringService = monitoringService;
        this.userRepository = userRepository;
    }

    /**
     * POST /api/security/event
     *
     * Expected body:
     * {
     *   "userId":    "user@example.com",
     *   "type":      "SCREENSHOT_KEY",   // one of: SCREENSHOT_KEY, COPY_ATTEMPT, DEVTOOLS_OPEN, RECORDING_SUSPECT, RIGHT_CLICK
     *   "timestamp": "2026-03-25T12:00:00.000Z",
     *   "riskScore": 40
     * }
     *
     * Returns 200 on success, 404 if user not found, 400 on bad input.
     */
    @PostMapping("/event")
    public ResponseEntity<String> receiveSecurityEvent(
            @RequestBody SecurityEventRequest eventRequest,
            Authentication authentication,
            HttpServletRequest request) {

        // Use authenticated principal as the authoritative user identifier
        // (do not trust the userId field in the body blindly)
        String authenticatedEmail = authentication.getName();

        User user = userRepository.findByEmail(authenticatedEmail)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        String violationType = eventRequest.getType();
        if (violationType == null || violationType.isBlank()) {
            return ResponseEntity.badRequest().body("Missing violation type");
        }

        monitoringService.logSecurityEvent(user, violationType, eventRequest.getRiskScore(), request);

        return ResponseEntity.ok("Security event recorded");
    }
}
