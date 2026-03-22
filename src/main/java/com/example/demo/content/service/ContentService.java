package com.example.demo.content.service;

import com.example.demo.content.dto.ContentResponse;
import com.example.demo.content.model.Content;
import com.example.demo.content.repository.ContentRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class ContentService {

    private final ContentRepository contentRepository;

    public ContentService(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    @Cacheable(value = "contents", key = "#tenantId + '-' + #page + '-' + #size")
    public Page<ContentResponse> listAllContent(String tenantId, int page, int size) {
        // Enforced tenant isolation
        Pageable pageable = PageRequest.of(page, size);
        Page<Content> contents = contentRepository.findAllByTenantId(tenantId, pageable);

        return contents.map(content -> new ContentResponse(
                content.getId(),
                content.getTitle(),
                content.getDescription(),
                content.getType(),
                content.getFileName(),
                content.getVideoUrl(),
                content.getUploadedAt()
        ));
    }
}