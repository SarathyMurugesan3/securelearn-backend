package com.example.demo.content.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "contents")
public class Content {

	@Id
	private String id;
	private String title;
	private String description;   // Rich description shown to students (Mighty Networks style)
	private String fileName;
	private String filePath;
	private String videoUrl;      // External secure video link (YouTube, Vimeo, Cloudflare Stream, etc.)
	private String uploadedBy;
	private LocalDateTime uploadedAt;
	private String fileUrl;       // Cloudinary secure_url for the file
	private String publicId;      // Cloudinary public_id for deletion
	private String type;          // PDF | VIDEO | VIDEO_URL | UNKNOWN

	@Indexed
	private String tenantId;


	public Content() {}

	/** Constructor for file-based uploads */
	public Content(String title, String description, String fileName, String filePath,
				   String uploadedBy, String type) {
		this.title = title;
		this.description = description;
		this.fileName = fileName;
		this.filePath = filePath;
		this.uploadedBy = uploadedBy;
		this.uploadedAt = LocalDateTime.now();
		this.type = type;
	}

	/** Constructor for video-URL-only entries */
	public Content(String title, String description, String videoUrl,
				   String uploadedBy) {
		this.title = title;
		this.description = description;
		this.videoUrl = videoUrl;
		this.uploadedBy = uploadedBy;
		this.uploadedAt = LocalDateTime.now();
		this.type = "VIDEO_URL";
	}

	/** Full constructor */
	public Content(String id, String title, String description, String fileName,
				   String filePath, String fileUrl, String publicId, String videoUrl, String uploadedBy,
				   LocalDateTime uploadedAt, String type) {
		this.id = id;
		this.title = title;
		this.description = description;
		this.fileName = fileName;
		this.filePath = filePath;
		this.fileUrl = fileUrl;
		this.publicId = publicId;
		this.videoUrl = videoUrl;
		this.uploadedBy = uploadedBy;
		this.uploadedAt = uploadedAt;
		this.type = type;
	}

	// Getters & Setters
	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }

	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }

	public String getFileName() { return fileName; }
	public void setFileName(String fileName) { this.fileName = fileName; }

	public String getFilePath() { return filePath; }
	public void setFilePath(String filePath) { this.filePath = filePath; }

	public String getVideoUrl() { return videoUrl; }
	public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

	public String getUploadedBy() { return uploadedBy; }
	public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

	public LocalDateTime getUploadedAt() { return uploadedAt; }
	public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

	public String getType() { return type; }
	public void setType(String type) { this.type = type; }

	public String getTenantId() { return tenantId; }
	public void setTenantId(String tenantId) { this.tenantId = tenantId; }

	public String getFileUrl() { return fileUrl; }
	public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

	public String getPublicId() { return publicId; }
	public void setPublicId(String publicId) { this.publicId = publicId; }
}
