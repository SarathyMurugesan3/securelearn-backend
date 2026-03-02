package com.example.demo.streaming.controller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.example.demo.content.model.Content;
import com.example.demo.content.repository.ContentRepository;
import com.example.demo.streaming.service.VideoTokenService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/student/video")
public class VideoStreamingController {

    private final ContentRepository contentRepository;
    private final VideoTokenService videoTokenService;

    @Autowired
    public VideoStreamingController(ContentRepository contentRepository,
                                    VideoTokenService videoTokenService) {
        this.contentRepository = contentRepository;
        this.videoTokenService = videoTokenService;
    }

    /**
     * Generate short-lived video token
     */
    @GetMapping("/token/{id}")
    public ResponseEntity<String> generateToken(
            @PathVariable String id,
            @RequestHeader("X-Device-Fingerprint") String fingerprint,
            Authentication authentication,
            HttpServletRequest request) {

        String email = authentication.getName();

        // ✅ SAFE IP EXTRACTION
        String ip = request.getRemoteAddr();

        String token = videoTokenService.generateVideoToken(
                id,
                email,
                fingerprint,
                ip
        );

        return ResponseEntity.ok(token);
    }

    /**
     * Secure HLS Segment (.ts files)
     */
    @GetMapping("/{id}/segment/{file}")
    public ResponseEntity<Resource> streamSegment(
            @PathVariable String id,
            @PathVariable String file,
            @RequestParam String token,
            @RequestHeader("X-Device-Fingerprint") String fingerprint,
            HttpServletRequest request) throws MalformedURLException {

        // ✅ SAFE IP EXTRACTION
        String ip = request.getRemoteAddr();

        if (!videoTokenService.validateToken(token, id, fingerprint, ip)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Content content = contentRepository.findById(id).orElseThrow();

        Path path = Paths.get(content.getFilePath(), file);

        Resource resource = new UrlResource(path.toUri());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp2t"))
                .body(resource);
    }

    /**
     * Secure HLS Playlist
     */
    @GetMapping("/{id}/playlist")
    public ResponseEntity<String> streamPlaylist(
            @PathVariable String id,
            @RequestParam String token,
            @RequestHeader("X-Device-Fingerprint") String fingerprint,
            HttpServletRequest request)
            throws IOException {

        // ✅ SAFE IP EXTRACTION
        String ip = request.getRemoteAddr();

        if (!videoTokenService.validateToken(token, id, fingerprint, ip)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Content content = contentRepository.findById(id).orElseThrow();

        Path playlistPath = Paths.get(content.getFilePath(), "playlist.m3u8");

        List<String> lines = Files.readAllLines(playlistPath);

        List<String> modified = new ArrayList<>();

        for (String line : lines) {
            if (line.endsWith(".ts")) {
                String newLine =
                        "/api/student/video/" + id +
                        "/segment/" + line +
                        "?token=" + token;

                modified.add(newLine);
            } else {
                modified.add(line);
            }
        }

        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType("application/vnd.apple.mpegurl"))
                .body(String.join("\n", modified));
    }
}