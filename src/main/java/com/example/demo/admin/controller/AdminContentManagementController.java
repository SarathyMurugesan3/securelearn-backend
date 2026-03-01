package com.example.demo.admin.controller;

import java.io.File;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.content.model.Content;
import com.example.demo.content.repository.ContentRepository;

@RestController
@RequestMapping("/api/admin/manage-content")
public class AdminContentManagementController {
	
	private final ContentRepository contentRepository;
	
	@Autowired
	public AdminContentManagementController(ContentRepository contentRepository) {
		this.contentRepository = contentRepository;
	}
	
	@GetMapping
	public List<Content> getAllContent(){
		return contentRepository.findAll();
	}
	
	@DeleteMapping("/{id}")
	public String deleteContent(@PathVariable String id) {
		Content content = contentRepository.findById(id).orElseThrow();
		new File(content.getFilePath()).delete();
		contentRepository.delete(content);
		return "Content deleted";
	}
	
	
}
