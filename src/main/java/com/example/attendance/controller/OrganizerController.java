package com.example.attendance.controller;

import com.example.attendance.entity.Organizer;
import com.example.attendance.entity.User;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.service.OrganizerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/organizers")
@RequiredArgsConstructor
public class OrganizerController {
    private final OrganizerService organizerService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<Organizer>> getAllOrganizers() {
        try {
            return ResponseEntity.ok(organizerService.getAllOrganizers());
        } catch (Exception e) {
            log.error("Error fetching organizers: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrganizerById(@PathVariable Long id) {
        try {
            Optional<Organizer> organizer = organizerService.getOrganizerById(id);
            if (organizer.isPresent()) {
                return ResponseEntity.ok(organizer.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Organizer not found"));
            }
        } catch (Exception e) {
            log.error("Error fetching organizer: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to fetch organizer"));
        }
    }

    @GetMapping("/by-email/{email}")
    public ResponseEntity<?> getOrganizerByEmail(@PathVariable String email) {
        try {
            Optional<Organizer> organizer = organizerService.findByUserEmail(email);
            if (organizer.isPresent()) {
                return ResponseEntity.ok(organizer.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Organizer not found"));
            }
        } catch (Exception e) {
            log.error("Error fetching organizer: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to fetch organizer"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrganizer(@PathVariable Long id) {
        try {
            organizerService.deleteOrganizer(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting organizer: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to delete organizer"));
        }
    }

    @GetMapping("/exists/{id}")
    public ResponseEntity<?> organizerExists(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(Map.of("exists", organizerService.existsById(id)));
        } catch (Exception e) {
            log.error("Error checking organizer existence: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to check organizer existence"));
        }
    }

    @GetMapping("/by-user/{userId}")
public ResponseEntity<?> getOrganizerByUserId(
    @PathVariable Long userId, 
    Authentication authentication) {
    
    try {
        // Verify authentication
        String authenticatedEmail = authentication.getName();
        User requestingUser = userRepository.findByEmail(authenticatedEmail)
            .orElseThrow(() -> new BadCredentialsException("User not found"));

        // Find the organizer
        Optional<Organizer> organizer = organizerService.findByUserId(userId);
        
        if (organizer.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Organizer not found for this user"));
        }

        // Simplified response with only needed fields
        Map<String, Object> response = new HashMap<>();
        response.put("id", organizer.get().getId());
        response.put("organizationName", organizer.get().getOrganizationName());
        
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(response);
            
    } catch (Exception e) {
        log.error("Error fetching organizer by user ID: {}", userId, e);
        return ResponseEntity.internalServerError()
            .body(Map.of("error", "Failed to fetch organizer data"));
    }
}
}