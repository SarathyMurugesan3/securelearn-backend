package com.example.demo.admin.controller;

import java.io.File;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import com.example.demo.content.model.Content;
import com.example.demo.content.repository.ContentRepository;
import com.example.demo.content.service.FileStorageService;

@RestController
@RequestMapping("/api/admin/manage-content")
public class AdminContentManagementController {
	
	private final ContentRepository contentRepository;
	private final FileStorageService storageService;
	
	@Autowired
	public AdminContentManagementController(ContentRepository contentRepository, FileStorageService storageService) {
		this.contentRepository = contentRepository;
		this.storageService = storageService;
	}
	
	@GetMapping
	public List<Content> getAllContent(Authentication authentication){
		String adminEmail = authentication.getName();
		return contentRepository.findAllByUploadedBy(adminEmail);
	}
	
	@DeleteMapping("/{id}")
	public String deleteContent(@PathVariable String id, Authentication authentication) {
		String adminEmail = authentication.getName();
		Content content = contentRepository.findById(id).orElseThrow();
		if(!content.getUploadedBy().equals(adminEmail)){
			throw new RuntimeException("Unauthorized");
		}
		if (content.getPublicId() != null) {
			storageService.deleteFile(content.getPublicId());
		} else if (content.getFilePath() != null) {
			// Legacy fallback
			new File(content.getFilePath()).delete();
		}
		contentRepository.delete(content);
		return "Content deleted";
	}
	
	
}
