package com.example.demo.content.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.activity.service.ActivityLogService;
import com.example.demo.auth.security.SignedUrlService;
import com.example.demo.content.model.Content;
import com.example.demo.content.repository.ContentRepository;
import com.example.demo.user.model.User;
import com.example.demo.user.repository.UserRepository;
import com.example.demo.watermark.PdfWatermarkService;

import jakarta.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.net.URL;

@RestController
@RequestMapping("/api/student/pdf")
public class PdfController {

    private final ContentRepository contentRepository;
    private final PdfWatermarkService watermarkService;
    private final UserRepository userRepository;
    private final SignedUrlService signedUrlService;
    private final ActivityLogService activityLogService;

    @Autowired
    public PdfController(ContentRepository contentRepository,
                         PdfWatermarkService watermarkService,
                         UserRepository userRepository,
                         SignedUrlService signedUrlService,
                         ActivityLogService activityLogService) {

        this.contentRepository = contentRepository;
        this.watermarkService = watermarkService;
        this.userRepository = userRepository;
        this.signedUrlService = signedUrlService;
        this.activityLogService = activityLogService;
    }
    
    @GetMapping("/url/{id}")
    public ResponseEntity<String> getSignedPdfUrl(
            @PathVariable String id,
            Authentication authentication,
            HttpServletRequest request) {

        String email = authentication.getName();
        long ts = System.currentTimeMillis() / 1000;

        String token = signedUrlService.generateToken(id, email, ts);

        // Dynamically resolve base URL from incoming request to support both localhost and Render
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

        String url = baseUrl.toString() + "/api/student/pdf/"
                + id + "?token=" + token + "&ts=" + ts + "&email=" + email;

        return ResponseEntity.ok(url);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> viewPdf(@PathVariable String id,
                                          @RequestParam String token,
                                          @RequestParam long ts,
                                          @RequestParam String email,
                                          HttpServletRequest request) throws Exception {

        if (!signedUrlService.validateToken(token, id, email, ts)) {
            return ResponseEntity.status(403).body("Token validation failed".getBytes());
        }

        User student = userRepository.findByEmail(email).orElse(null);
        if (student == null) {
            return ResponseEntity.status(404).body("User not found".getBytes());
        }

        Content content = contentRepository.findById(id).orElse(null);
        if (content == null) {
            return ResponseEntity.status(404).body("Content not found".getBytes());
        }

        if (!student.getTenantId().equals(content.getTenantId())) {
            return ResponseEntity.status(403).body("Content does not belong to your school".getBytes());
        }

        // Log PDF access event asynchronously
        try {
            activityLogService.logAction(student.getId(), student.getTenantId(), "ACCESS_PDF", null);
        } catch (Exception ignored) {}

        byte[] rawBytes = null;

        // 1. Try local file path if present and exists
        if (content.getFilePath() != null && !content.getFilePath().isBlank()) {
            java.io.File pdfFile = new java.io.File(content.getFilePath());
            if (pdfFile.exists()) {
                rawBytes = java.nio.file.Files.readAllBytes(pdfFile.toPath());
            }
        }

        // 2. Fallback to Cloudinary or Remote HTTP/HTTPS File URL if local path doesn't exist
        if (rawBytes == null && content.getFileUrl() != null && !content.getFileUrl().isBlank()) {
            try (InputStream in = new URL(content.getFileUrl()).openStream()) {
                rawBytes = in.readAllBytes();
            } catch (Exception e) {
                System.err.println("Failed to fetch PDF from Cloudinary URL: " + e.getMessage());
            }
        }

        if (rawBytes == null) {
            return ResponseEntity.status(404).body("PDF document file is not accessible.".getBytes());
        }

        String line1 = student.getEmail();
        String line2 = resolveClientIp(request) + " | " + java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'"));

        byte[] watermarkedPdf = watermarkService.addWatermarkFromBytes(rawBytes, line1, line2);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
        		.header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=secure.pdf")
                .body(watermarkedPdf);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return request.getRemoteAddr();
    }
}