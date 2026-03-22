package com.example.demo.content.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.content.dto.FileUploadResponse;

import java.io.IOException;
import java.util.Map;

@Service
public class FileStorageService {

    private final Cloudinary cloudinary;

    public FileStorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public FileUploadResponse uploadFile(MultipartFile file) throws IOException {
        @SuppressWarnings("rawtypes")
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "resource_type", "auto"
        ));
        
        FileUploadResponse response = new FileUploadResponse();
        response.setUrl(uploadResult.get("secure_url").toString());
        response.setPublicId(uploadResult.get("public_id").toString());
        return response;
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
