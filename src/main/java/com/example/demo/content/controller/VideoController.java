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

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.net.URI;

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
            Authentication authentication,
            HttpServletRequest request) {

        String email = authentication.getName();
        long ts = System.currentTimeMillis() / 1000;
        String token = signedUrlService.generateToken(id, email, ts);

        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();

        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto != null && !forwardedProto.isBlank()) {
            scheme = forwardedProto;
        }

        StringBuilder baseUrl = new StringBuilder();
        baseUrl.append(scheme).append("://").append(serverName);
        if ((scheme.equals("http") && serverPort != 80) || (scheme.equals("https") && serverPort != 443)) {
            baseUrl.append(":").append(serverPort);
        }

        String url = baseUrl.toString() + "/api/student/video/"
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

        if (!student.getTenantId().equals(content.getTenantId())) {
            System.out.println("STEP 9: tenant mismatch -> forbidden");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Content does not belong to your school");
        }

        File videoFile = (content.getFilePath() != null && !content.getFilePath().isBlank())
                ? new File(content.getFilePath())
                : null;

        if (videoFile == null || !videoFile.exists()) {
            String remoteUrl = content.getFileUrl() != null && !content.getFileUrl().isBlank()
                    ? content.getFileUrl()
                    : content.getVideoUrl();
            if (remoteUrl != null && !remoteUrl.isBlank()) {
                System.out.println("STEP 11: Local video file not found. Redirecting to remote video URL: " + remoteUrl);
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(remoteUrl))
                        .build();
            }

            System.out.println("STEP 11: File missing on disk and no remote URL!");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Error: The physical video file no longer exists on this server.");
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
