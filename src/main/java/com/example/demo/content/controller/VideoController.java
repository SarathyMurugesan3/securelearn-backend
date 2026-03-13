package com.example.demo.content.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.example.demo.auth.security.SignedUrlService;
import com.example.demo.content.model.Content;
import com.example.demo.content.repository.ContentRepository;
import com.example.demo.user.model.User;
import com.example.demo.user.repository.UserRepository;

import java.io.File;

@RestController
@RequestMapping("/api/student/video")
public class VideoController {

    private final ContentRepository contentRepository;
    private final UserRepository userRepository;
    private final SignedUrlService signedUrlService;

    @Autowired
    public VideoController(ContentRepository contentRepository,
                           UserRepository userRepository,
                           SignedUrlService signedUrlService) {

        this.contentRepository = contentRepository;
        this.userRepository = userRepository;
        this.signedUrlService = signedUrlService;
    }

    @GetMapping("/url/{id}")
    public ResponseEntity<String> getSignedVideoUrl(
            @PathVariable String id,
            Authentication authentication) {

        String email = authentication.getName();
        long ts = System.currentTimeMillis() / 1000;
        String token = signedUrlService.generateToken(id, email, ts);

        String url = "https://securelearn-backend.onrender.com/api/student/video/"
                + id + "?token=" + token + "&ts=" + ts + "&email=" + email;

        return ResponseEntity.ok(url);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> viewVideo(@PathVariable String id,
                                              @RequestParam String token,
                                              @RequestParam long ts,
                                              @RequestParam String email) {

        System.out.println("STEP 1: request received for video " + id);
        System.out.println("STEP 2: user email = " + email);

        if (!signedUrlService.validateToken(token, id, email, ts)) {
            System.out.println("STEP 3: token validation FAILED");
            return ResponseEntity.status(403).build();
        }

        System.out.println("STEP 4: token valid");

        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (student.getAdminId() == null) {
            System.out.println("STEP 6: student has no adminId");
            return ResponseEntity.status(403).build();
        }

        User admin = userRepository.findById(student.getAdminId())
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Content not found"));

        if (!admin.getEmail().equals(content.getUploadedBy())) {
            System.out.println("STEP 9: admin mismatch -> forbidden");
            return ResponseEntity.status(403).build();
        }

        File videoFile = new File(content.getFilePath());
        if (!videoFile.exists()) {
            System.out.println("STEP 11: File missing on disk! (Probably wiped by Render)");
            return ResponseEntity.status(404).build();
        }

        Resource videoResource = new FileSystemResource(videoFile);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + content.getFileName() + "\"")
                .body(videoResource);
    }
}
