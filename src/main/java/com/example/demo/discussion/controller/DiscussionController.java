package com.example.demo.discussion.controller;

import java.util.List;

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

import com.example.demo.discussion.dto.CreateThreadRequest;
import com.example.demo.discussion.dto.PostMessageRequest;
import com.example.demo.discussion.model.DiscussionMessage;
import com.example.demo.discussion.model.DiscussionThread;
import com.example.demo.discussion.service.DiscussionService;
import com.example.demo.user.model.User;
import com.example.demo.user.repository.UserRepository;

/**
 * REST endpoints for the threaded Discussion System.
 *
 * All routes require a valid JWT (enforced by JwtAuthenticationFilter).
 * Tenant isolation and access rights are enforced in DiscussionService.
 *
 * Endpoints:
 *   POST   /api/discussions/thread                   → create a new thread
 *   GET    /api/discussions/course/{courseId}        → list threads for a course
 *   POST   /api/discussions/message                  → post a message or nested reply
 *   GET    /api/discussions/thread/{threadId}        → get paginated messages in a thread
 *   GET    /api/discussions/thread/{threadId}/replies/{messageId} → get replies to a message
 *   DELETE /api/discussions/message/{messageId}      → soft-delete (TUTOR/ADMIN only)
 */
@RestController
@RequestMapping("/api/discussions")
public class DiscussionController {

    private final DiscussionService discussionService;
    private final UserRepository    userRepository;

    public DiscussionController(DiscussionService discussionService,
                                UserRepository userRepository) {
        this.discussionService = discussionService;
        this.userRepository    = userRepository;
    }

    // ── POST /api/discussions/thread ───────────────────────────────────────────

    /**
     * Create a new discussion thread under a course.
     *
     * Body: { "courseId": "...", "moduleId": "...(optional)", "title": "..." }
     * Any authenticated role (STUDENT, TUTOR, ADMIN) within the tenant may start a thread.
     */
    @PostMapping("/thread")
    public ResponseEntity<?> createThread(
            @RequestBody CreateThreadRequest request,
            Authentication authentication) {

        User caller = resolveUser(authentication);

        try {
            DiscussionThread thread = discussionService.createThread(request, caller);
            return ResponseEntity.status(HttpStatus.CREATED).body(thread);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── GET /api/discussions/course/{courseId} ─────────────────────────────────

    /**
     * List all threads in a course — scoped to the caller's tenant.
     * Returns threads ordered newest-first.
     */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<DiscussionThread>> getThreadsByCourse(
            @PathVariable String courseId,
            Authentication authentication) {

        User caller = resolveUser(authentication);
        List<DiscussionThread> threads =
                discussionService.getThreadsByCourse(courseId, caller.getTenantId());
        return ResponseEntity.ok(threads);
    }

    // ── POST /api/discussions/message ──────────────────────────────────────────

    /**
     * Post a message into a thread, or a nested reply to an existing message.
     *
     * Body:
     *   { "threadId": "...", "message": "..." }                           ← top-level
     *   { "threadId": "...", "parentMessageId": "...", "message": "..." } ← reply
     */
    @PostMapping("/message")
    public ResponseEntity<?> postMessage(
            @RequestBody PostMessageRequest request,
            Authentication authentication) {

        User caller = resolveUser(authentication);

        try {
            DiscussionMessage saved = discussionService.postMessage(request, caller);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    // ── GET /api/discussions/thread/{threadId} ─────────────────────────────────

    /**
     * Fetch paginated messages in a thread.
     * Only messages within the caller's tenant are returned.
     *
     * Query params:
     *   page (default 0), size (default 20, max 50)
     */
    @GetMapping("/thread/{threadId}")
    public ResponseEntity<?> getThreadMessages(
            @PathVariable String threadId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        User caller = resolveUser(authentication);

        try {
            Page<DiscussionMessage> messages =
                    discussionService.getMessages(threadId, caller.getTenantId(), page, size);
            return ResponseEntity.ok(messages);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    // ── GET /api/discussions/thread/{threadId}/replies/{messageId} ─────────────

    /**
     * Fetch all direct replies to a specific message (nested replies tree node).
     * Useful for lazy-loading reply subtrees in the frontend.
     */
    @GetMapping("/thread/{threadId}/replies/{messageId}")
    public ResponseEntity<?> getReplies(
            @PathVariable String threadId,
            @PathVariable String messageId,
            Authentication authentication) {

        User caller = resolveUser(authentication);

        try {
            // Validate thread access first (tenant check)
            discussionService.getThread(threadId, caller.getTenantId());
            return ResponseEntity.ok(
                    discussionService.getReplies(messageId, caller.getTenantId())
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    // ── DELETE /api/discussions/message/{messageId} ────────────────────────────

    /**
     * Soft-delete a message. Restricted to TUTOR and ADMIN roles.
     * The message is hidden from all subsequent GET calls immediately.
     */
    @DeleteMapping("/message/{messageId}")
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
