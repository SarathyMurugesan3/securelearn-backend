package com.example.demo.discussion.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.discussion.dto.SendMessageRequest;
import com.example.demo.discussion.model.DiscussionMessage;
import com.example.demo.discussion.service.DiscussionService;
import com.example.demo.user.model.User;
import com.example.demo.user.repository.UserRepository;

/**
 * REST endpoints for per-module discussion chat.
 *
 * All routes require a valid JWT (handled by JwtAuthenticationFilter).
 * Enrolment and tenant checks are enforced here before delegating to DiscussionService.
 *
 * Endpoints:
 *   POST   /api/discussion/send                    → post a message
 *   GET    /api/discussion/module/{moduleId}        → paginated message list
 *   DELETE /api/discussion/{messageId}              → tutor/admin soft-delete
 */
@RestController
@RequestMapping("/api/discussion")
public class DiscussionController {

    private final DiscussionService discussionService;
    private final UserRepository    userRepository;

    public DiscussionController(DiscussionService discussionService,
                                UserRepository userRepository) {
        this.discussionService = discussionService;
        this.userRepository    = userRepository;
    }

    // ── POST /api/discussion/send ──────────────────────────────────────────────

    /**
     * Any authenticated user (STUDENT, TUTOR, ADMIN) within the tenant can post.
     *
     * Body: { "moduleId": "...", "courseId": "...", "message": "..." }
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            @RequestBody SendMessageRequest request,
            Authentication authentication) {

        User caller = resolveUser(authentication);

        // Guard: message and moduleId must be present
        if (request.getModuleId() == null || request.getModuleId().isBlank()) {
            return ResponseEntity.badRequest().body("moduleId is required.");
        }

        try {
            DiscussionMessage saved = discussionService.sendMessage(caller, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── GET /api/discussion/module/{moduleId} ──────────────────────────────────

    /**
     * Returns paginated messages for the given module.
     * Only messages within the caller's own tenantId are returned.
     *
     * Query params:
     *   page  (default 0)
     *   size  (default 20, max 50)
     */
    @GetMapping("/module/{moduleId}")
    public ResponseEntity<?> getMessages(
            @PathVariable String moduleId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        User caller = resolveUser(authentication);

        Page<DiscussionMessage> messages =
                discussionService.getMessages(moduleId, caller.getTenantId(), page, size);

        return ResponseEntity.ok(messages);
    }

    // ── DELETE /api/discussion/{messageId} ────────────────────────────────────

    /**
     * Soft-deletes a message. Restricted to TUTOR and ADMIN roles.
     * The deleted message is hidden immediately from all subsequent GET calls.
     */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<?> deleteMessage(
            @PathVariable String messageId,
            Authentication authentication) {

        User caller = resolveUser(authentication);

        try {
            discussionService.deleteMessage(messageId, caller);
            return ResponseEntity.ok("Message removed.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    // ── Private helper ─────────────────────────────────────────────────────────

    private User resolveUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database."));
    }
}
