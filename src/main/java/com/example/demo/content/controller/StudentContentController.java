package com.example.demo.content.controller;

import com.example.demo.content.dto.ContentResponse;
import com.example.demo.content.service.ContentService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student")
public class StudentContentController {

    private final ContentService contentService;

    public StudentContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping("/content")
    public List<ContentResponse> listContent(Authentication authentication) {

        // Optional: can use authentication.getName() if later
        // you want role-based filtering or subscription-based content

        return contentService.listAllContent();
    }
}