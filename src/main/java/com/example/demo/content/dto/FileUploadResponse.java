package com.example.demo.content.dto;

public class FileUploadResponse {
    private String url;
    private String publicId;

    public FileUploadResponse() {}

    public FileUploadResponse(String url, String publicId) {
        this.url = url;
        this.publicId = publicId;
    }

    private String filePath;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}
