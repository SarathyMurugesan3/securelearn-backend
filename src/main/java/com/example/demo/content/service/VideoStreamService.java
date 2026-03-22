package com.example.demo.content.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/**
 * Issues and validates short-lived signed stream tokens.
 *
 * Token lifetime: 7 minutes (420 000 ms) — long enough for buffering, short
 * enough to be useless if leaked.
 *
 * Claims embedded in the JWT:
 *   - sub  : user email
 *   - vid  : videoId the token is scoped to
 *   - tid  : tenantId of the requesting user
 *   - type : "STREAM" — prevents reuse as a general access token
 */
@Service
public class VideoStreamService {

    private static final long STREAM_TOKEN_TTL_MS = 7 * 60 * 1000L; // 7 minutes

    private final SecretKey secretKey;

    public VideoStreamService(@Value("${securelearn.jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // ── Token issuance ─────────────────────────────────────────────────────────

    public String generateStreamToken(String email, String videoId, String tenantId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("vid",  videoId);
        claims.put("tid",  tenantId);
        claims.put("type", "STREAM");

        Date now    = new Date();
        Date expiry = new Date(now.getTime() + STREAM_TOKEN_TTL_MS);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // ── Token validation ───────────────────────────────────────────────────────

    /**
     * @return parsed claims if valid; throws IllegalArgumentException with a human
     *         readable reason on any validation failure.
     */
    public Claims validateStreamToken(String token, String expectedVideoId, String expectedTenantId) {
        Claims claims;
        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new IllegalArgumentException("Stream token has expired");
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid stream token: " + e.getMessage());
        }

        // Ensure claim type matches
        if (!"STREAM".equals(claims.get("type", String.class))) {
            throw new IllegalArgumentException("Token is not a stream token");
        }

        // Ensure the token is scoped to the requested video
        if (!expectedVideoId.equals(claims.get("vid", String.class))) {
            throw new IllegalArgumentException("Stream token is not valid for this video");
        }

        // Ensure tenant scope matches
        if (!expectedTenantId.equals(claims.get("tid", String.class))) {
            throw new IllegalArgumentException("Tenant mismatch in stream token");
        }

        return claims;
    }
}
