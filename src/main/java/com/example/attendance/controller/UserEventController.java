package com.example.attendance.controller;

import com.example.attendance.entity.UserEventAttendance;
import com.example.attendance.service.UserEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/user-events")
@RequiredArgsConstructor
public class UserEventController {
    
    private final UserEventService userEventService;

    @PostMapping("/record/{eventId}")
    public ResponseEntity<?> recordAttendance(
            @PathVariable Long eventId,
            Authentication authentication) {
        try {
            // In a real implementation, you'd get the user ID from the authentication
            // For now, we'll use a placeholder
            Long userId = getUserIdFromAuthentication(authentication);
            UserEventAttendance attendance = userEventService.recordAttendance(userId, eventId);
            return ResponseEntity.ok(attendance);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/my-attendances")
    public ResponseEntity<List<UserEventAttendance>> getMyAttendedEvents(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        List<UserEventAttendance> attendances = userEventService.getUserAttendedEvents(userId);
        return ResponseEntity.ok(attendances);
    }

    @GetMapping("/event/{eventId}/attendees")
    public ResponseEntity<List<UserEventAttendance>> getEventAttendees(@PathVariable Long eventId) {
        List<UserEventAttendance> attendances = userEventService.getEventAttendees(eventId);
        return ResponseEntity.ok(attendances);
    }

    // In UserEventController.java
@GetMapping("/event/{eventId}/attendance-count")
public ResponseEntity<Long> getEventAttendanceCount(@PathVariable Long eventId) {
    long count = userEventService.getEventAttendanceCount(eventId);
    return ResponseEntity.ok(count);
}

    private Long getUserIdFromAuthentication(Authentication authentication) {
        // Implement logic to get user ID from authentication
        // This is just a placeholder
        return 1L; // Replace with actual implementation
    }
}