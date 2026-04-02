package com.example.demo.streaming.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.example.demo.content.model.Content;
import com.example.demo.content.repository.ContentRepository;
import com.example.demo.streaming.service.VideoTokenService;
import com.example.demo.user.model.User;
import com.example.demo.user.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/student/video")
@CrossOrigin(origins = "*")
public class VideoStreamingController {

    private final ContentRepository contentRepository;
    private final VideoTokenService videoTokenService;
    private final UserRepository userRepository;

    @Autowired
    public VideoStreamingController(ContentRepository contentRepository,
                                    VideoTokenService videoTokenService,
                                    UserRepository userRepository) {
        this.contentRepository = contentRepository;
        this.videoTokenService = videoTokenService;
        this.userRepository = userRepository;
    }

    /**
     * Generate short-lived video token (for HLS file-based streaming)
     */
    @GetMapping("/token/{id}")
    public ResponseEntity<String> generateToken(
            @PathVariable String id,
            @RequestHeader("X-Device-Fingerprint") String fingerprint,
            Authentication authentication,
            HttpServletRequest request) {

        String email = authentication.getName();
        String ip = request.getRemoteAddr();

        String token = videoTokenService.generateVideoToken(id, email, fingerprint, ip);
        return ResponseEntity.ok(token);
    }

    /**
     * Secure Video URL (Mighty Networks style)
     *
     * Returns the videoUrl for VIDEO_URL type content.
     * Access is gated behind JWT — the URL is never exposed publicly.
     * The student must belong to the admin who uploaded the content.
     *
     * GET /api/student/video/{id}/secure-url
     */
    @GetMapping("/{id}/secure-url")
    public ResponseEntity<?> getSecureVideoUrl(
            @PathVariable String id,
            Authentication authentication) {

        String studentEmail = authentication.getName();

        // Load student to get their adminId
        User student = userRepository.findByEmail(studentEmail)
                .orElse(null);
        if (student == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Student not found");
        }

        // Load content
        Content content = contentRepository.findById(id).orElse(null);
        if (content == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Content not found");
        }

        // Verify content belongs to the student's admin (access control)
        User admin = userRepository.findById(student.getAdminId() != null ? student.getAdminId() : "").orElse(null);
        if (admin == null || !content.getUploadedBy().equals(admin.getEmail())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }

        // Must be a VIDEO_URL type
        if (!"VIDEO_URL".equals(content.getType())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("This content is not a video link. Use the HLS stream endpoint.");
        }

        return ResponseEntity.ok(content.getVideoUrl());
    }

    /**
     * Secure HLS Playlist
     * Redirects to the Cloudinary hosted file. Cloudinary can auto-convert to HLS 
     * by replacing the extension with .m3u8 or the client can do it.
     */
    @GetMapping("/{id}/playlist")
    public ResponseEntity<String> streamPlaylist(
            @PathVariable String id,
            @RequestParam String token,
            @RequestHeader("X-Device-Fingerprint") String fingerprint,
            HttpServletRequest request) {

        String ip = request.getRemoteAddr();

        if (!videoTokenService.validateToken(token, id, fingerprint, ip)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Content content = contentRepository.findById(id).orElseThrow();
        
        String url = content.getFileUrl();
        if (url == null || url.isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        // Return 302 Redirect to Cloudinary URL
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, url)
                .build();
    }
}