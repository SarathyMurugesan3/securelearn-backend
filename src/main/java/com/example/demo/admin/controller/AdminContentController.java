package com.example.demo.admin.controller;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.content.model.Content;
import com.example.demo.content.repository.ContentRepository;
import com.example.demo.content.service.PdfStorageService;

@RestController
@RequestMapping("/api/admin/content")
public class AdminContentController {
	
	private final PdfStorageService storageService;
	private final ContentRepository contentRepository;
	
	public AdminContentController(PdfStorageService storageService,ContentRepository contentRepository) {
		this.storageService = storageService;
		this.contentRepository = contentRepository;
	}
	
	@PostMapping("/upload")
	public String upload(
	        @RequestParam String title,
	        @RequestParam MultipartFile file,
	        Authentication authentication
	) throws IOException {

	    String adminEmail = authentication.getName();

	    String filePath = storageService.store(file);

	    Content content = new Content(
	            title,
	            file.getOriginalFilename(),
	            filePath,
	            adminEmail
	    );

	    contentRepository.save(content);

	    return "Uploaded Successfully";
	}
	
	
	
	
	
}
