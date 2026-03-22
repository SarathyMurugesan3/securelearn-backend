package com.example.demo.content.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;

import com.example.demo.activity.service.ActivityLogService;
import com.example.demo.auth.security.SignedUrlService;
import com.example.demo.content.model.Content;
import com.example.demo.content.repository.ContentRepository;
import com.example.demo.risk.model.UserRisk;
import com.example.demo.risk.service.RiskEngineService;
import com.example.demo.user.model.User;
import com.example.demo.user.repository.UserRepository;

import java.io.File;

@RestController
@RequestMapping("/api/student/video")
public class VideoController {

    private final ContentRepository contentRepository;
    private final UserRepository userRepository;
    private final SignedUrlService signedUrlService;
    private final RiskEngineService riskEngineService;
    private final ActivityLogService activityLogService;

    @Autowired
    public VideoController(ContentRepository contentRepository,
                           UserRepository userRepository,
                           SignedUrlService signedUrlService,
                           RiskEngineService riskEngineService,
                           ActivityLogService activityLogService) {

        this.contentRepository = contentRepository;
        this.userRepository = userRepository;
        this.signedUrlService = signedUrlService;
        this.riskEngineService = riskEngineService;
        this.activityLogService = activityLogService;
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
    public ResponseEntity<?> viewVideo(@PathVariable String id,
                                       @RequestParam String token,
                                       @RequestParam long ts,
                                       @RequestParam String email,
                                       @RequestHeader HttpHeaders headers) throws Exception {

        System.out.println("STEP 1: request received for video " + id);
        System.out.println("STEP 2: user email = " + email);

        if (!signedUrlService.validateToken(token, id, email, ts)) {
            System.out.println("STEP 3: token validation FAILED");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token validation failed");
        }

        System.out.println("STEP 4: token valid");

        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Risk guard: block access for blocked users or those above risk threshold
        if (student.isBlocked()) {
            System.out.println("RISK BLOCK: user " + email + " is blocked");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied: account is blocked due to high risk score");
        }
        UserRisk risk = riskEngineService.getRisk(student.getId());
        if (risk != null && risk.getRiskScore() > 100) {
            System.out.println("RISK RESTRICT: user " + email + " risk score = " + risk.getRiskScore());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied: risk score too high");
        }

        if (student.getAdminId() == null) {
            System.out.println("STEP 6: student has no adminId");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Student has no assigned admin");
        }

        // Log video play event asynchronously
        activityLogService.logAction(student.getId(), student.getTenantId(), "PLAY_VIDEO", null);

        User admin = userRepository.findById(student.getAdminId())
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Content not found"));

        if (!admin.getEmail().equals(content.getUploadedBy())) {
            System.out.println("STEP 9: admin mismatch -> forbidden");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Content does not belong to your admin");
        }

        File videoFile = new File(content.getFilePath());
        if (!videoFile.exists()) {
            System.out.println("STEP 11: File missing on disk! (Probably wiped by Render)");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Error: The physical video file no longer exists on this server. " +
                          "This happens on free Render tiers because the temporary folder resets. " +
                          "Please re-upload the file.");
        }

        Resource videoResource = new FileSystemResource(videoFile);
        MediaType mediaType = MediaType.parseMediaType("video/mp4");

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(mediaType);
        responseHeaders.set("Accept-Ranges", "bytes");

        // If no Range header is present (e.g. Postman, direct link), return full file with 200 OK
        if (headers.getRange().isEmpty()) {
            System.out.println("STEP 12: No Range header — returning full video with 200 OK");
            responseHeaders.setContentLength(videoResource.contentLength());
            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .body(videoResource);
        }

        // Range request (browser video player) — return 206 Partial Content
        System.out.println("STEP 12: Range header present — returning partial content 206");
        HttpRange range = headers.getRange().get(0);
        long contentLength = videoResource.contentLength();
        long start = range.getRangeStart(contentLength);
        long end = range.getRangeEnd(contentLength);
        long rangeLength = Math.min(1024 * 1024, end - start + 1); // max 1 MB chunk

        ResourceRegion region = new ResourceRegion(videoResource, start, rangeLength);

        responseHeaders.set(HttpHeaders.CONTENT_RANGE,
                "bytes " + start + "-" + (start + rangeLength - 1) + "/" + contentLength);
        responseHeaders.setContentLength(rangeLength);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .headers(responseHeaders)
                .body(region);
    }
}
