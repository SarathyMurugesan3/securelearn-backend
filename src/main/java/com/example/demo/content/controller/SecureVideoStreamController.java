package com.example.demo.content.controller;

import java.io.File;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.activity.service.ActivityLogService;
import com.example.demo.auth.security.JwtService;
import com.example.demo.auth.service.SessionService;
import com.example.demo.content.model.Content;
import com.example.demo.content.repository.ContentRepository;
import com.example.demo.content.service.VideoStreamService;
import com.example.demo.risk.model.UserRisk;
import com.example.demo.risk.service.RiskEngineService;
import com.example.demo.user.model.User;
import com.example.demo.user.repository.UserRepository;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;


/**
 * Secure video streaming endpoint.
 *
 * Flow:
 *   1. Client calls GET /video/stream/{videoId}/token  → receives a short-lived stream token
 *   2. Client calls GET /video/stream/{videoId}?streamToken=<token> → streamed video bytes
 *
 * All guards (JWT, tenant, session, risk) are enforced at step 1 (token issuance).
 * Step 2 only validates the lightweight stream token, keeping hot-path latency minimal.
 */
@RestController
@RequestMapping("/api/video/stream")
public class SecureVideoStreamController {

    private static final int RISK_BLOCK_THRESHOLD = 100;
    private static final long MAX_CHUNK_BYTES     = 2 * 1024 * 1024L; // 2 MB per range chunk

    private final UserRepository       userRepository;
    private final ContentRepository    contentRepository;
    private final JwtService           jwtService;
    private final SessionService       sessionService;
    private final RiskEngineService    riskEngineService;
    private final VideoStreamService   videoStreamService;
    private final ActivityLogService   activityLogService;

    public SecureVideoStreamController(
            UserRepository userRepository,
            ContentRepository contentRepository,
            JwtService jwtService,
            SessionService sessionService,
            RiskEngineService riskEngineService,
            VideoStreamService videoStreamService,
            ActivityLogService activityLogService) {

        this.userRepository     = userRepository;
        this.contentRepository  = contentRepository;
        this.jwtService         = jwtService;
        this.sessionService     = sessionService;
        this.riskEngineService  = riskEngineService;
        this.videoStreamService = videoStreamService;
        this.activityLogService = activityLogService;
    }

    // ── Step 1: Issue a short-lived stream token ───────────────────────────────

    /**
     * Called by the frontend before starting playback.
     * Requires valid Authorization: Bearer <accessToken> header (enforced by JwtAuthenticationFilter).
     *
     * Validates: JWT → session active → tenant match → risk score → content ownership
     * Returns: a 7-minute stream token scoped to this exact video + tenant.
     */
    @GetMapping("/{videoId}/token")
    public ResponseEntity<?> issueStreamToken(
            @PathVariable String videoId,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String email = authentication.getName();

        // Extract raw Bearer token from Authorization header
        String authHeader = httpRequest.getHeader("Authorization");
        String accessToken = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7) : null;
        if (accessToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing Authorization header.");
        }

        // 1. Load user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Session validation
        String sessionId = jwtService.extractSessionId(accessToken);
        if (sessionId == null || !sessionService.validateSession(sessionId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Session expired or invalid. Please log in again.");
        }

        // 3. Block check
        if (user.isBlocked()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Account is blocked due to suspicious activity.");
        }

        // 4. Risk score check
        UserRisk risk = riskEngineService.getRisk(user.getId());
        if (risk != null && risk.getRiskScore() > RISK_BLOCK_THRESHOLD) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied: risk score exceeds threshold (" + risk.getRiskScore() + ").");
        }

        // 5. Tenant isolation: content must belong to same tenant
        Content content = contentRepository.findById(videoId).orElse(null);
        if (content == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Video not found.");
        }
        if (user.getTenantId() != null && !user.getTenantId().equals(content.getTenantId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Content does not belong to your tenant.");
        }

        // 6. Generate stream token (7 min, scoped to videoId + tenantId)
        String streamToken = videoStreamService.generateStreamToken(email, videoId, user.getTenantId());

        // 7. Log asynchronously
        activityLogService.logAction(user.getId(), user.getTenantId(), "REQUEST_STREAM_TOKEN", null);

        return ResponseEntity.ok(streamToken);
    }

    // ── Step 2: Stream video bytes ─────────────────────────────────────────────

    /**
     * Does NOT require the Authorization header — the stream token is self-contained.
     * Validates: stream token signature + expiry + videoId + tenantId.
     * Supports HTTP Range requests for seeking/buffering.
     */
    @GetMapping("/{videoId}")
    public ResponseEntity<?> streamVideo(
            @PathVariable String videoId,
            @RequestParam String streamToken,
            @RequestHeader HttpHeaders headers) throws Exception {

        // 1. Parse + validate the stream token
        //    We need the tenantId embedded in the token itself, so we validate after decoding.
        Claims claims;
        try {
            // For tenant validation we use the claim inside the token (no DB hit needed)
            String embeddedTenantId = extractTenantIdFromToken(streamToken);
            claims = videoStreamService.validateStreamToken(streamToken, videoId, embeddedTenantId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }

        String email = claims.getSubject();

        // 2. Load content record
        Content content = contentRepository.findById(videoId).orElse(null);
        if (content == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Video not found.");
        }

        // 3. Ensure the physical file exists
        File videoFile = new File(content.getFilePath());
        if (!videoFile.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Video file is temporarily unavailable. Please contact support.");
        }

        // 4. Log stream start (async — zero latency impact)
        userRepository.findByEmail(email).ifPresent(u ->
            activityLogService.logAction(u.getId(), u.getTenantId(), "PLAY_VIDEO", null)
        );

        // 5. Build response — support both full and range requests
        Resource videoResource = new FileSystemResource(videoFile);
        MediaType mediaType    = MediaType.parseMediaType("video/mp4");

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(mediaType);
        responseHeaders.set("Accept-Ranges", "bytes");
        // Security headers: prevent embedding or caching of secure content
        responseHeaders.set("Cache-Control", "no-store, no-cache, must-revalidate");
        responseHeaders.set("X-Content-Type-Options", "nosniff");

        // Full file (e.g. Postman or direct link)
        if (headers.getRange().isEmpty()) {
            responseHeaders.setContentLength(videoResource.contentLength());
            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .body(videoResource);
        }

        // Range request (browser seeking / adaptive buffering)
        HttpRange range      = headers.getRange().get(0);
        long contentLength   = videoResource.contentLength();
        long start           = range.getRangeStart(contentLength);
        long end             = range.getRangeEnd(contentLength);
        long rangeLength     = Math.min(MAX_CHUNK_BYTES, end - start + 1);

        ResourceRegion region = new ResourceRegion(videoResource, start, rangeLength);

        responseHeaders.set(HttpHeaders.CONTENT_RANGE,
                "bytes " + start + "-" + (start + rangeLength - 1) + "/" + contentLength);
        responseHeaders.setContentLength(rangeLength);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .headers(responseHeaders)
                .body(region);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Quick extract of tenantId from the stream token WITHOUT full validation —
     * used only so we can call validateStreamToken with the expected tenant.
     * The validateStreamToken call itself re-parses and fully verifies the signature.
     */
    private String extractTenantIdFromToken(String token) {
        try {
            // Split JWT and decode claims without verification just to read the tenant.
            // Full verification happens inside validateStreamToken.
            String payload  = token.split("\\.")[1];
            byte[] decoded  = java.util.Base64.getUrlDecoder().decode(payload);
            String json     = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
            // Simple string extraction (avoids pulling in Jackson here)
            int start = json.indexOf("\"tid\":\"") + 7;
            if (start < 7) return null;
            int end = json.indexOf("\"", start);
            return end > start ? json.substring(start, end) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
