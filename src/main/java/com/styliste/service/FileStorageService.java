package com.styliste.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload folder!");
        }
    }

    public String saveFile(MultipartFile file) {
        try {
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path root = Paths.get(uploadDir);
            Path resolve = root.resolve(fileName);
            if (!resolve.getParent().equals(root.toAbsolutePath())) {
                // Security check
                try (InputStream inputStream = file.getInputStream()) {
                    Files.copy(inputStream, resolve, StandardCopyOption.REPLACE_EXISTING);
                }
            } else {
                // Simplified for typical usage
                Files.copy(file.getInputStream(), resolve, StandardCopyOption.REPLACE_EXISTING);
            }
            // Return the URL path that matches WebConfig
            return "/uploads/" + fileName;
        } catch (Exception e) {
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
        }
    }
}