package com.example.demo.content.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.content.model.Content;
import com.example.demo.content.repository.ContentRepository;
import com.example.demo.user.model.User;
import com.example.demo.user.repository.UserRepository;
import com.example.demo.watermark.PdfWatermarkService;

@RestController
@RequestMapping("/api/student/pdf")
public class PdfController {

    private final ContentRepository contentRepository;
    private final PdfWatermarkService watermarkService;
    private final UserRepository userRepository;

    @Autowired
    public PdfController(ContentRepository contentRepository,
                         PdfWatermarkService watermarkService,
                         UserRepository userRepository) {

        this.contentRepository = contentRepository;
        this.watermarkService = watermarkService;
        this.userRepository = userRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> viewPdf(@PathVariable String id,
                                          Authentication authentication)
            throws Exception {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow();

        Content content = contentRepository.findById(id)
                .orElseThrow();

        String watermarkText =
                user.getEmail() + " | IP Tracked | SecureLearn";

        byte[] watermarkedPdf =
                watermarkService.addWatermark(
                        content.getFilePath(),
                        watermarkText
                );

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=secure.pdf")
                .body(watermarkedPdf);
    }
}