package com.example.attendance.controller;

import com.example.attendance.entity.Event;
import com.example.attendance.entity.UserEventAttendance;
import com.example.attendance.service.UserEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/events/report")
@RequiredArgsConstructor
public class UserEventReportController {

    private final UserEventService userEventService;

    @GetMapping("/summary")
    public ResponseEntity<?> getUserEventSummary(Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);

            List<Event> registered = userEventService.getRegisteredEvents(userId);
            List<UserEventAttendance> attended = userEventService.getUserAttendedEvents(userId);
            
            // Get only registered events count for attendance percentage calculation
            int totalRegisteredEvents = registered.size();

            List<Map<String, Object>> registeredEventsList = registered.stream().map(event -> {
                Map<String, Object> eventMap = new HashMap<>();
                eventMap.put("name", event.getName());
                eventMap.put("date", event.getDate().toString()); // Ensure proper date formatting
                eventMap.put("location", event.getPlace());
                return eventMap;
            }).collect(Collectors.toList());

            List<Map<String, Object>> attendedEventsList = attended.stream().map(att -> {
                Map<String, Object> eventMap = new HashMap<>();
                eventMap.put("name", att.getEvent().getName());
                eventMap.put("date", att.getEvent().getDate().toString()); // Ensure proper date formatting
                eventMap.put("location", att.getEvent().getPlace());
                return eventMap;
            }).collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("registeredEvents", registered.size());
            response.put("attendedEvents", attended.size());
            response.put("totalEvents", totalRegisteredEvents); // Use registered events for percentage calculation
            response.put("registeredEventsList", registeredEventsList);
            response.put("attendedEventsList", attendedEventsList);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch report data: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();
        
        // If using custom UserDetails implementation
        if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;
            
            // Try to cast to your custom UserDetails if you have one
            // Assuming you have a custom UserDetails implementation like CustomUserPrincipal
            try {
                // If you have a custom UserDetails class that contains user ID
                // return ((CustomUserPrincipal) userDetails).getUserId();
                
                // For now, try to extract from username if it's the user ID
                String username = userDetails.getUsername();
                try {
                    return Long.parseLong(username);
                } catch (NumberFormatException e) {
                    // If username is email or string, you might need to look up user by username
                    // This would require injecting UserService or UserRepository
                    // For testing purposes, return a default user ID
                    return 1L; // TEMPORARY - replace with actual logic
                }
            } catch (ClassCastException e) {
                // If it's not your custom UserDetails, try to extract username
                return 1L; // TEMPORARY - replace with actual logic
            }
        }
        
        // If using JWT with custom claims (when principal is a string)
        if (principal instanceof String) {
            try {
                // If the principal is the user ID directly
                return Long.parseLong((String) principal);
            } catch (NumberFormatException e) {
                return 1L; // TEMPORARY - replace with actual logic
            }
        }
        
        // Fallback for testing
        return 1L; // TEMPORARY - replace with actual user lookup logic
    }
}