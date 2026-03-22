package com.example.demo.content.controller;

import com.example.demo.content.dto.ContentResponse;
import com.example.demo.content.service.ContentService;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.example.demo.user.model.User;
import com.example.demo.user.repository.UserRepository;

@RestController
@RequestMapping("/api/student")
public class StudentContentController {

    private final ContentService contentService;
    private final UserRepository userRepository;

    public StudentContentController(ContentService contentService, UserRepository userRepository) {
        this.contentService = contentService;
        this.userRepository = userRepository;
    }

    @GetMapping("/content")
    public ResponseEntity<Page<ContentResponse>> listContent(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String studentEmail = authentication.getName();
        User student = userRepository.findByEmail(studentEmail).orElseThrow();
        
        // Guard: if student has no adminId (e.g. old account), return empty page
        if (student.getAdminId() == null) {
            return ResponseEntity.ok(Page.empty());
        }
        
        User admin = userRepository.findById(student.getAdminId()).orElse(null);
        if (admin == null) {
            return ResponseEntity.ok(Page.empty());
        }
        
        return ResponseEntity.ok(contentService.listAllContent(admin.getTenantId(), page, size));
    }
}