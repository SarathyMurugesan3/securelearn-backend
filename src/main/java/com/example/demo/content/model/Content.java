package com.example.demo.content.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "contents")
public class Content {
	
	@Id
	private String id;
	private String title;
	private String fileName;
	private String filePath;
	private String uploadedBy;
	private LocalDateTime uploadedAt;
	private String type;
	
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Content(String id, String title, String fileName, String filePath, String uploadedBy,
			LocalDateTime uploadedAt, String type) {
		super();
		this.id = id;
		this.title = title;
		this.fileName = fileName;
		this.filePath = filePath;
		this.uploadedBy = uploadedBy;
		this.uploadedAt = uploadedAt;
		this.type = type;
	}

	public Content(String title,String fileName,String filePath,String uploadedBy) {
		this.title = title;
		this.fileName = fileName;
		this.filePath = filePath;
		this.uploadedBy = uploadedBy;
		this.uploadedAt = LocalDateTime.now();
	}
	
	public String getId() {
		return id;
	}
	public String getTitle() {
		return title;
	}
	public String getFilePath() {
		return filePath;
	}

	public LocalDateTime getUploadedAt() {
		// TODO Auto-generated method stub
		return uploadedAt;
	}
	
}
