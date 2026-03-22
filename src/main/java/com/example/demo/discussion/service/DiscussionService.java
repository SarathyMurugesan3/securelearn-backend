package com.example.demo.discussion.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.demo.discussion.dto.SendMessageRequest;
import com.example.demo.discussion.model.DiscussionMessage;
import com.example.demo.discussion.repository.DiscussionRepository;
import com.example.demo.user.model.User;

@Service
public class DiscussionService {

    private final DiscussionRepository discussionRepository;

    public DiscussionService(DiscussionRepository discussionRepository) {
        this.discussionRepository = discussionRepository;
    }

    /**
     * Post a new message. Caller (controller) has already verified enrolment / tenant.
     */
    public DiscussionMessage sendMessage(User sender, SendMessageRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty.");
        }
        if (request.getMessage().length() > 2000) {
            throw new IllegalArgumentException("Message exceeds 2000 character limit.");
        }

        DiscussionMessage msg = new DiscussionMessage(
                request.getModuleId(),
                request.getCourseId(),
                sender.getId(),
                sender.getTenantId(),
                sender.getName(),
                sender.getRole(),
                request.getMessage()
        );
        return discussionRepository.save(msg);
    }

    /**
     * Paginated read — only active (non-deleted) messages, scoped to tenant.
     *
     * @param moduleId  the module to fetch messages for
     * @param tenantId  caller's tenantId for isolation
     * @param page      0-indexed page number
     * @param size      messages per page (capped at 50)
     */
    public Page<DiscussionMessage> getMessages(String moduleId, String tenantId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(50, Math.max(1, size));
        Pageable pageable = PageRequest.of(safePage, safeSize);
        return discussionRepository
                .findByModuleIdAndTenantIdAndDeletedFalseOrderByCreatedAtDesc(moduleId, tenantId, pageable);
    }

    /**
     * Soft-delete a message. Only allowed if the caller is a tutor/admin within the same tenant.
     *
     * @throws IllegalArgumentException if the message does not exist or belongs to a different tenant
     * @throws SecurityException        if the caller is not permitted to moderate
     */
    public void deleteMessage(String messageId, User caller) {
        DiscussionMessage msg = discussionRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found."));

        // Tenant isolation: moderator must be in the same tenant as the message
        if (!msg.getTenantId().equals(caller.getTenantId())) {
            throw new SecurityException("Cannot moderate a message from another tenant.");
        }

        // Only tutors and admins may delete messages
        String role = caller.getRole();
        if (!"TUTOR".equals(role) && !"ADMIN".equals(role)) {
            throw new SecurityException("Only tutors or admins may delete messages.");
        }

        msg.setDeleted(true);
        discussionRepository.save(msg);
    }
}
