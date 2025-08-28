package com.example.attendance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            File directory = uploadPath.toFile();
            
            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                System.out.println("Upload directory created: " + created + " at " + directory.getAbsolutePath());
            }

            // Configure resource handler for serving uploaded files
            registry.addResourceHandler("/uploads/**")
                    .addResourceLocations("file:" + directory.getAbsolutePath() + File.separator)
                    .setCachePeriod(3600) // Cache for 1 hour in production
                    .setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                    .resourceChain(true)
                    .addResolver(new PathResourceResolver() {
                        @Override
                        protected Resource getResource(String resourcePath, Resource location) throws IOException {
                            System.out.println("WebConfig - Attempting to serve resource: " + resourcePath);
                            
                            Resource requestedResource = location.createRelative(resourcePath);
                            
                            if (requestedResource.exists() && requestedResource.isReadable()) {
                                try {
                                    // Security check - ensure file is within upload directory
                                    String requestedPath = requestedResource.getFile().getCanonicalPath();
                                    String uploadDirCanonical = directory.getCanonicalPath();
                                    
                                    if (requestedPath.startsWith(uploadDirCanonical)) {
                                        System.out.println("WebConfig - ✅ Successfully serving file: " + resourcePath);
                                        System.out.println("WebConfig - File path: " + requestedPath);
                                        System.out.println("WebConfig - File size: " + requestedResource.contentLength() + " bytes");
                                        return requestedResource;
                                    } else {
                                        System.err.println("WebConfig - ❌ Security violation: file outside upload directory");
                                        System.err.println("WebConfig - Requested: " + requestedPath);
                                        System.err.println("WebConfig - Allowed: " + uploadDirCanonical);
                                    }
                                } catch (IOException e) {
                                    System.err.println("WebConfig - ❌ Error checking file security: " + e.getMessage());
                                }
                            } else {
                                System.err.println("WebConfig - ❌ File not found or not readable: " + resourcePath);
                                System.err.println("WebConfig - Expected location: " + location.getURI());
                                System.err.println("WebConfig - Exists: " + requestedResource.exists());
                                System.err.println("WebConfig - Readable: " + requestedResource.isReadable());
                                
                                // List available files for debugging
                                File[] availableFiles = directory.listFiles();
                                if (availableFiles != null) {
                                    System.err.println("WebConfig - Available files in directory:");
                                    for (File file : availableFiles) {
                                        System.err.println("  - " + file.getName() + " (" + file.length() + " bytes, readable: " + file.canRead() + ")");
                                    }
                                } else {
                                    System.err.println("WebConfig - No files found in upload directory");
                                }
                            }
                            
                            return null;
                        }
                    });
                    
            System.out.println("=== Static File Handler Configuration ===");
            System.out.println("Upload directory: " + directory.getAbsolutePath());
            System.out.println("Directory exists: " + directory.exists());
            System.out.println("Directory readable: " + directory.canRead());
            System.out.println("Directory writable: " + directory.canWrite());
            
            if (directory.listFiles() != null) {
                System.out.println("Files count: " + directory.listFiles().length);
                File[] files = directory.listFiles();
                for (File file : files) {
                    System.out.println("  - " + file.getName() + " (size: " + file.length() + ", readable: " + file.canRead() + ")");
                }
            }
            
        } catch (Exception e) {
            System.err.println("WebConfig - Error configuring resource handler: " + e.getMessage());
            e.printStackTrace();
        }
    }
}