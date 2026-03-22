package com.example.demo.watermark;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.watermark.dto.WatermarkResponse;

import jakarta.servlet.http.HttpServletRequest;

/**
 * GET /api/watermark
 *
 * Returns per-request watermark metadata for the authenticated user.
 * Updates on every call — timestamp is live, IP is the actual request IP.
 *
 * Used by:
 *  - Frontend: overlay text on the video player canvas
 *  - Backend: PdfController already calls PdfWatermarkService with the same data
 */
@RestController
@RequestMapping("/api/watermark")
public class WatermarkController {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'");

    @GetMapping
    public ResponseEntity<WatermarkResponse> getWatermark(
            Authentication authentication,
            HttpServletRequest request) {

        String email     = authentication.getName();
        String ip        = resolveClientIp(request);
        String timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(FORMATTER);

        return ResponseEntity.ok(new WatermarkResponse(email, ip, timestamp));
    }

    /**
     * Resolve real client IP respecting reverse-proxy forwarding headers.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // May contain comma-separated chain: "client, proxy1, proxy2"
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
