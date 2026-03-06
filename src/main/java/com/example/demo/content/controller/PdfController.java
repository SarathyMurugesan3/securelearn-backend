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

import com.example.demo.auth.security.SignedUrlService;
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
    private final SignedUrlService signedUrlService;

    @Autowired
    public PdfController(ContentRepository contentRepository,
                         PdfWatermarkService watermarkService,
                         UserRepository userRepository,
                         SignedUrlService signedUrlService) {

        this.contentRepository = contentRepository;
        this.watermarkService = watermarkService;
        this.userRepository = userRepository;
        this.signedUrlService = signedUrlService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> viewPdf(@PathVariable String id,@RequestParam String token,
    										@RequestParam long ts,
                                          Authentication authentication)
            throws Exception {

    	String email = authentication.getName();

    	if (!signedUrlService.validateToken(token, id, email, ts)) {
    	    return ResponseEntity.status(403).build();
    	}
        User student = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));


        
        if(student.getAdminId() == null) {
        	return ResponseEntity.status(403).build();
        }
        
        User admin = userRepository.findById(student.getAdminId()).orElseThrow(() -> new RuntimeException("Admin not found"));
        
        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Content not found"));
        
        if (!admin.getEmail().equals(content.getUploadedBy())) {
            return ResponseEntity.status(403).build();
        }

        String watermarkText =
                student.getEmail() + " | IP Tracked | SecureLearn";

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