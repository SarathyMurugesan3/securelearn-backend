package com.example.demo.admin.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.content.model.Content;
import com.example.demo.content.repository.ContentRepository;
import com.example.demo.content.service.FileStorageService;
import com.example.demo.content.dto.FileUploadResponse;
import java.time.LocalDateTime;
import com.example.demo.user.model.User;
import com.example.demo.user.repository.UserRepository;

/**
 * Admin Content Upload Controller
 *
 * Supports two modes:
 *   1. File upload (PDF or VIDEO): provide `file` multipart param.
 *   2. Video URL (Mighty Networks style): provide `videoUrl` param with no file.
 *
 * In both cases, `title` and optional `description` are accepted.
 */
@RestController
@RequestMapping("/api/admin/content")
public class AdminContentController {

	private final FileStorageService storageService;
	private final ContentRepository contentRepository;
	private final UserRepository userRepository;

	public AdminContentController(FileStorageService storageService, ContentRepository contentRepository, UserRepository userRepository) {
		this.storageService = storageService;
		this.contentRepository = contentRepository;
		this.userRepository = userRepository;
	}

	@PostMapping("/upload")
	public ResponseEntity<String> upload(
	        @RequestParam String title,
	        @RequestParam(required = false) String description,
	        @RequestParam(required = false) String videoUrl,
	        @RequestParam(required = false) MultipartFile file,
	        Authentication authentication
	) throws IOException {

	    String adminEmail = authentication.getName();
	    User admin = userRepository.findByEmail(adminEmail)
	            .orElseThrow(() -> new RuntimeException("Admin not found"));

	    // Mode 1: Video URL (no file) — Mighty Networks style
	    if ((file == null || file.isEmpty()) && videoUrl != null && !videoUrl.isBlank()) {
	        Content content = new Content(title, description, videoUrl, adminEmail);
	        content.setTenantId(admin.getTenantId());
	        contentRepository.save(content);
	        return ResponseEntity.status(HttpStatus.CREATED).body("Video link saved successfully");
	    }

	    // Mode 2: File upload (PDF or VIDEO file)
	    if (file == null || file.isEmpty()) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	                .body("Either a file or a videoUrl must be provided.");
	    }

	    String contentType = file.getContentType();
	    String type = "UNKNOWN";
	    if (contentType != null) {
	        if (contentType.startsWith("video/")) {
	            type = "VIDEO";
	        } else if (contentType.equals("application/pdf")) {
	            type = "PDF";
	        }
	    }

	    FileUploadResponse uploadResponse = storageService.uploadFile(file);

	    Content content = new Content();
	    content.setTitle(title);
	    content.setDescription(description);
	    content.setFileName(file.getOriginalFilename());
	    content.setFileUrl(uploadResponse.getUrl());
	    content.setPublicId(uploadResponse.getPublicId());
	    content.setUploadedBy(adminEmail);
	    content.setType(type);
	    content.setUploadedAt(LocalDateTime.now());
	    content.setTenantId(admin.getTenantId());

	    contentRepository.save(content);
	    return ResponseEntity.status(HttpStatus.CREATED).body("Uploaded successfully");
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteContent(@PathVariable String id, Authentication authentication) {
	    String email = authentication.getName();
	    userRepository.findByEmail(email)
	            .orElseThrow(() -> new RuntimeException("Admin not found"));
	            
	    contentRepository.deleteById(id);
	    return ResponseEntity.ok("Deleted");
	}
}
