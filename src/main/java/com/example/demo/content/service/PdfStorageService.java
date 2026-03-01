package com.example.demo.content.service;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PdfStorageService {

	
	@Value("${securelearn.storage.pdf-path}")
	private String storagePath;
	
	public String store(MultipartFile file) throws IOException{
		String uniqueName = UUID.randomUUID()+"_"+file.getOriginalFilename();
		File destination = new File(storagePath + uniqueName);
		file.transferTo(destination);
		return destination.getAbsolutePath();
	}
	
	
	
}
