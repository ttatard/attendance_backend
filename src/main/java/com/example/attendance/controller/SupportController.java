package com.example.attendance.controller;

import com.example.attendance.entity.SupportTicket;
import com.example.attendance.service.SupportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/support")
public class SupportController {
    private final SupportService supportService;

    public SupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    @PostMapping
    public ResponseEntity<?> createSupportTicket(
            @RequestParam("concernType") String concernType,
            @RequestParam("message") String message,
            @RequestParam(value = "attachment", required = false) MultipartFile attachment,
            @RequestParam("userId") String userIdString) {
        
        try {
            Long userId = Long.parseLong(userIdString);
            
            SupportTicket ticket = supportService.createSupportTicket(
                userId, concernType, message, attachment);
            
            // Create a simplified response DTO to avoid serialization issues
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("message", "Support ticket created successfully");
            response.put("ticketId", ticket.getId());
            response.put("createdAt", ticket.getCreatedAt().toString());
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
                    
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Invalid user ID format"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Failed to create support ticket"));
        }
    }
}