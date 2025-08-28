package com.example.attendance.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
public class FileUploadController {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    // List of allowed image types
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @PostMapping("/upload/logo")
    @PreAuthorize("hasRole('SYSTEM_OWNER')")
    public ResponseEntity<Map<String, String>> uploadLogo(@RequestParam("file") MultipartFile file) {
        try {
            // Validate file
            if (file.isEmpty()) {
                throw new RuntimeException("File is empty");
            }

            // Check file size
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new RuntimeException("File size exceeds maximum limit of 5MB");
            }

            // Validate file type
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                throw new RuntimeException("Invalid filename");
            }

            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
                throw new RuntimeException("Invalid file type. Only JPEG, PNG, GIF, and WebP files are allowed.");
            }

            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            File directory = uploadPath.toFile();
            
            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                if (!created) {
                    throw new RuntimeException("Failed to create upload directory");
                }
                System.out.println("Created upload directory: " + directory.getAbsolutePath());
            }

            // Generate unique filename
            String uniqueFilename = "logo_" + System.currentTimeMillis() + "." + fileExtension;

            // Save file
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Verify file was saved
            File savedFile = filePath.toFile();
            if (!savedFile.exists() || !savedFile.canRead()) {
                throw new RuntimeException("File was not saved properly or is not readable");
            }

            // Return the relative URL path that will be served by the static resource handler
            String fileUrl = "/uploads/" + uniqueFilename;
            
            Map<String, String> response = new HashMap<>();
            response.put("url", fileUrl);
            response.put("message", "File uploaded successfully");
            response.put("filename", uniqueFilename);
            response.put("absolutePath", filePath.toString()); // For debugging

            System.out.println("=== File Upload Success ===");
            System.out.println("Original filename: " + originalFilename);
            System.out.println("Unique filename: " + uniqueFilename);
            System.out.println("File path: " + filePath.toString());
            System.out.println("File exists: " + savedFile.exists());
            System.out.println("File readable: " + savedFile.canRead());
            System.out.println("File size: " + savedFile.length() + " bytes");
            System.out.println("File URL: " + fileUrl);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            System.err.println("IO Error during file upload: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error during file upload: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            File directory = uploadPath.toFile();
            
            response.put("status", "ok");
            response.put("uploadDir", uploadDir);
            response.put("absolutePath", uploadPath.toString());
            response.put("directoryExists", directory.exists());
            response.put("directoryWritable", directory.canWrite());
            response.put("directoryReadable", directory.canRead());
            
            // List files
            if (directory.exists()) {
                File[] files = directory.listFiles();
                if (files != null) {
                    response.put("fileCount", files.length);
                    List<String> fileList = Arrays.stream(files)
                            .map(f -> f.getName() + " (" + f.length() + " bytes, readable: " + f.canRead() + ")")
                            .toList();
                    response.put("files", fileList);
                } else {
                    response.put("fileCount", 0);
                    response.put("files", Arrays.asList());
                }
            }
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    // Add a direct file serving endpoint for debugging
    @GetMapping("/uploads/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(filename).normalize();
            
            // Security check - ensure file is within upload directory
            if (!filePath.startsWith(uploadPath)) {
                throw new RuntimeException("Invalid file path");
            }
            
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                System.out.println("Serving file directly: " + filePath);
                
                // Determine content type
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .body(resource);
            } else {
                System.err.println("File not found or not readable: " + filePath);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            System.err.println("Error serving file: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}