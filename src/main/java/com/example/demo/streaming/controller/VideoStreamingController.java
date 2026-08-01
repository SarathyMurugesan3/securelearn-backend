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
            @RequestHeader(value = "X-Device-Fingerprint", required = false) String fingerprint,
            Authentication authentication,
            HttpServletRequest request) {

        String email = authentication.getName();
        String ip = request.getRemoteAddr();
        String fp = (fingerprint != null && !fingerprint.isBlank()) ? fingerprint : "default-fp";

        String token = videoTokenService.generateVideoToken(id, email, fp, ip);
        return ResponseEntity.ok(token);
    }

    /**
     * Secure Video URL (Mighty Networks style)
     *
     * Returns the videoUrl or Cloudinary fileUrl for VIDEO/VIDEO_URL type content.
     * Access is gated behind JWT — the URL is never exposed publicly.
     *
     * GET /api/student/video/{id}/secure-url
     */
    @GetMapping("/{id}/secure-url")
    public ResponseEntity<?> getSecureVideoUrl(
            @PathVariable String id,
            Authentication authentication) {

        String studentEmail = authentication.getName();

        // Load student
        User student = userRepository.findByEmail(studentEmail).orElse(null);
        if (student == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Student not found");
        }

        // Load content
        Content content = contentRepository.findById(id).orElse(null);
        if (content == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Content not found");
        }

        String mediaUrl = content.getVideoUrl();
        if (mediaUrl == null || mediaUrl.isBlank()) {
            mediaUrl = content.getFileUrl();
        }

        if (mediaUrl == null || mediaUrl.isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Media URL not found");
        }

        return ResponseEntity.ok(mediaUrl);
    }

    /**
     * Secure HLS Playlist / Media Redirect
     */
    @GetMapping("/{id}/playlist")
    public ResponseEntity<String> streamPlaylist(
            @PathVariable String id,
            @RequestParam String token,
            @RequestHeader(value = "X-Device-Fingerprint", required = false) String fingerprint,
            HttpServletRequest request) {

        String ip = request.getRemoteAddr();
        String fp = (fingerprint != null && !fingerprint.isBlank()) ? fingerprint : "default-fp";

        if (!videoTokenService.validateToken(token, id, fp, ip)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Content content = contentRepository.findById(id).orElse(null);
        if (content == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        String url = content.getFileUrl();
        if (url == null || url.isBlank()) {
            url = content.getVideoUrl();
        }
        
        if (url == null || url.isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        // Return 302 Redirect to Cloudinary / Media URL
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, url)
                .build();
    }
}