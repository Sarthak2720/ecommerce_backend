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
    private String uploadDir; // uploads/images

    @Value("${file.video-dir}")
    private String videoDir;   // uploads/videos

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
            Files.createDirectories(Paths.get(videoDir));
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload folders!");
        }
    }

    public String saveFile(MultipartFile file, String type) {
        try {
            // 1. Determine which root folder to use
            String targetDir = type.equalsIgnoreCase("video") ? videoDir : uploadDir;
            Path root = Paths.get(targetDir);

            // 2. Generate a unique filename
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

            // 3. IMPORTANT: Resolve the path (This is what you mentioned)
            Path destinationFile = root.resolve(fileName)
                    .normalize()
                    .toAbsolutePath();

            // 4. Security Check: Ensure the file is actually inside the intended folder
            if (!destinationFile.getParent().equals(root.toAbsolutePath())) {
                throw new RuntimeException("Cannot store file outside current directory.");
            }

            // 5. Store the file
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            // 6. Return the URL path for the database
            // Note: We return "/uploads/images/..." or "/uploads/videos/..."
            // to match your WebConfig.java ResourceHandlers
            String subFolder = type.equalsIgnoreCase("video") ? "videos/" : "images/";
            return "/uploads/" + subFolder + fileName;

        } catch (Exception e) {
            throw new RuntimeException("Could not store " + type + ". Error: " + e.getMessage());
        }
    }
}