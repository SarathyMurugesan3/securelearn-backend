package com.example.demo.discussion.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.demo.discussion.dto.CreateThreadRequest;
import com.example.demo.discussion.dto.PostMessageRequest;
import com.example.demo.discussion.model.DiscussionMessage;
import com.example.demo.discussion.model.DiscussionThread;
import com.example.demo.discussion.repository.DiscussionRepository;
import com.example.demo.discussion.repository.DiscussionThreadRepository;
import com.example.demo.user.model.User;

/**
 * Business logic for the Discussion System.
 *
 * All methods enforce strict tenant isolation — no cross-tenant data access is possible.
 *
 * Supported operations:
 *   - createThread()    — STUDENT, TUTOR, ADMIN can start a thread
 *   - postMessage()     — post to a thread (top-level or nested reply)
 *   - getThreadsByCourse() — list threads for a course
 *   - getMessages()     — paginated messages in a thread
 *   - softDeleteMessage() — TUTOR / ADMIN moderation
 */
@Service
public class DiscussionService {

    private final DiscussionThreadRepository  threadRepository;
    private final DiscussionRepository        messageRepository;

    public DiscussionService(DiscussionThreadRepository threadRepository,
                             DiscussionRepository messageRepository) {
        this.threadRepository  = threadRepository;
        this.messageRepository = messageRepository;
    }

    // ── Threads ────────────────────────────────────────────────────────────────

    /**
     * Create a new discussion thread under a course (and optionally a module).
     */
    public DiscussionThread createThread(CreateThreadRequest request, User creator) {
        if (request.getCourseId() == null || request.getCourseId().isBlank()) {
            throw new IllegalArgumentException("courseId is required.");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Thread title cannot be empty.");
        }
        if (request.getTitle().length() > 200) {
            throw new IllegalArgumentException("Thread title exceeds 200 character limit.");
        }

        DiscussionThread thread = new DiscussionThread(
                creator.getTenantId(),
                request.getCourseId(),
                request.getModuleId(),
                creator.getId(),
                creator.getName(),
                creator.getRole(),
                request.getTitle().trim()
        );
        return threadRepository.save(thread);
    }

    /**
     * List all threads for a course, strictly scoped to the caller's tenant.
     */
    public List<DiscussionThread> getThreadsByCourse(String courseId, String tenantId) {
        return threadRepository.findByCourseIdAndTenantIdOrderByCreatedAtDesc(courseId, tenantId);
    }

    /**
     * Fetch a single thread — returns null if not found or belongs to a different tenant.
     */
    public DiscussionThread getThread(String threadId, String tenantId) {
        return threadRepository.findById(threadId).map(thread -> {
            if (!thread.getTenantId().equals(tenantId)) {
                throw new SecurityException("Access denied: thread belongs to a different tenant.");
            }
            return thread;
        }).orElseThrow(() -> new IllegalArgumentException("Thread not found."));
    }

    // ── Messages ───────────────────────────────────────────────────────────────

    /**
     * Post a message (or nested reply) into a thread.
     *
     * Validates:
     *   - Thread exists and belongs to caller's tenant.
     *   - If parentMessageId is set, that message exists and is in the same thread/tenant.
     *   - Message non-empty, max 2000 chars.
     */
    public DiscussionMessage postMessage(PostMessageRequest request, User sender) {
        if (request.getThreadId() == null || request.getThreadId().isBlank()) {
            throw new IllegalArgumentException("threadId is required.");
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty.");
        }
        if (request.getMessage().length() > 2000) {
            throw new IllegalArgumentException("Message exceeds 2000 character limit.");
        }

        // Tenant-safe thread resolution
        DiscussionThread thread = getThread(request.getThreadId(), sender.getTenantId());

        // If it's a reply, validate the parent message is in the same thread and tenant
        if (request.getParentMessageId() != null && !request.getParentMessageId().isBlank()) {
            DiscussionMessage parent = messageRepository.findById(request.getParentMessageId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent message not found."));
            if (!parent.getThreadId().equals(thread.getId())) {
                throw new IllegalArgumentException("Parent message does not belong to this thread.");
            }
            if (!parent.getTenantId().equals(sender.getTenantId())) {
                throw new SecurityException("Access denied: parent message belongs to a different tenant.");
            }
        }

        DiscussionMessage msg = new DiscussionMessage(
                thread.getId(),
                request.getParentMessageId(),
                sender.getId(),
                sender.getTenantId(),
                sender.getName(),
                sender.getRole(),
                request.getMessage().trim()
        );
        return messageRepository.save(msg);
    }

    /**
     * Paginated messages for a thread — only active (non-deleted), scoped to tenant.
     */
    public Page<DiscussionMessage> getMessages(String threadId, String tenantId, int page, int size) {
        // Validate thread exists and belongs to tenant first
        getThread(threadId, tenantId);

        int safePage = Math.max(0, page);
        int safeSize = Math.min(50, Math.max(1, size));
        Pageable pageable = PageRequest.of(safePage, safeSize);
        return messageRepository
                .findByThreadIdAndTenantIdAndDeletedFalseOrderByCreatedAtAsc(threadId, tenantId, pageable);
    }

    /**
     * Fetch direct replies to a specific parent message.
     */
    public List<DiscussionMessage> getReplies(String parentMessageId, String tenantId) {
        return messageRepository
                .findByParentMessageIdAndTenantIdAndDeletedFalseOrderByCreatedAtAsc(parentMessageId, tenantId);
    }

    /**
     * Soft-delete a message. Only TUTOR or ADMIN within the same tenant may moderate.
     */
    public void deleteMessage(String messageId, User caller) {
        DiscussionMessage msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found."));

        if (!msg.getTenantId().equals(caller.getTenantId())) {
            throw new SecurityException("Cannot moderate a message from another tenant.");
        }

        String role = caller.getRole();
        if (!"TUTOR".equals(role) && !"ADMIN".equals(role)) {
            throw new SecurityException("Only tutors or admins may delete messages.");
        }

        msg.setDeleted(true);
        messageRepository.save(msg);
    }
}
