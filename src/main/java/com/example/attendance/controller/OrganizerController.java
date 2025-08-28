package com.example.attendance.controller;

import com.example.attendance.entity.Organizer;
import com.example.attendance.entity.User;
import com.example.attendance.dto.OrganizerDto;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.service.OrganizerService;
import com.example.attendance.mapper.OrganizerMapper;
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
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/organizers")
@RequiredArgsConstructor
public class OrganizerController {
    private final OrganizerService organizerService;
    private final UserRepository userRepository;
    private final OrganizerMapper organizerMapper;

    @GetMapping
    public ResponseEntity<List<OrganizerDto>> getAllOrganizers() {
        List<OrganizerDto> organizers = organizerService.getAllOrganizers();
        return ResponseEntity.ok(organizers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrganizerById(@PathVariable Long id) {
        try {
            OrganizerDto organizer = organizerService.getOrganizerById(id);
            return ResponseEntity.ok(organizer);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/by-email/{email}")
    public ResponseEntity<?> getOrganizerByEmail(@PathVariable String email) {
        try {
            Optional<OrganizerDto> organizerOptional = organizerService.findByUserEmail(email);
            if (organizerOptional.isPresent()) {
                return ResponseEntity.ok(organizerOptional.get());
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
            Optional<OrganizerDto> organizerOptional = organizerService.findByUserId(userId);
            
            if (organizerOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Organizer not found for this user"));
            }

            OrganizerDto organizer = organizerOptional.get();

            // Simplified response with only needed fields
            Map<String, Object> response = new HashMap<>();
            response.put("id", organizer.getId());
            response.put("organizationName", organizer.getOrganizationName());
            
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