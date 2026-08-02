package com.example.demo.content.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.content.dto.FileUploadResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Cloudinary cloudinary;

    public FileStorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public FileUploadResponse uploadFile(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        String resourceType = "auto";
        if (contentType != null) {
            if (contentType.startsWith("video/")) {
                resourceType = "video";
            } else if (contentType.equals("application/pdf")) {
                resourceType = "raw";
            }
        }

        try {
            @SuppressWarnings("rawtypes")
            Map uploadResult;
            if (file.getSize() > 5 * 1024 * 1024) {
                uploadResult = cloudinary.uploader().uploadLarge(file.getInputStream(), ObjectUtils.asMap(
                        "resource_type", resourceType,
                        "chunk_size", 6 * 1024 * 1024
                ));
            } else {
                uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                        "resource_type", resourceType
                ));
            }

            FileUploadResponse response = new FileUploadResponse();
            response.setUrl(uploadResult.get("secure_url").toString());
            response.setPublicId(uploadResult.get("public_id").toString());
            return response;
        } catch (Exception e) {
            System.out.println("Cloudinary upload failed or unconfigured (" + e.getMessage() + "). Falling back to local disk storage.");
            Path uploadDir = Paths.get("uploads").toAbsolutePath();
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path destPath = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), destPath, StandardCopyOption.REPLACE_EXISTING);

            FileUploadResponse response = new FileUploadResponse();
            response.setUrl("file://" + destPath.toString().replace("\\", "/"));
            response.setPublicId("local_" + filename);
            response.setFilePath(destPath.toString());
            return response;
        }
    }

    public void deleteFile(String publicId) {
        if (publicId == null || publicId.isEmpty()) return;
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new RuntimeException("Could not delete file from Cloudinary", e);
        }
    }
}
