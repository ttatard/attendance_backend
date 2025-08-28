package com.example.attendance.controller;

import com.example.attendance.entity.Event;
import com.example.attendance.entity.User;
import com.example.attendance.repository.EventRepository;
import com.example.attendance.repository.UserEventAttendanceRepository;
import com.example.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminReportController {

    private final EventRepository eventRepository;
    private final UserEventAttendanceRepository attendanceRepository;
    private final UserRepository userRepository;

    @GetMapping("/events/report")
    public ResponseEntity<?> getAdminEventsReport(
            @RequestParam(required = false) String month,
            Authentication authentication) {
        try {
            // Verify admin access
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(403).body(
                    Map.of("error", "Access denied. Admin privileges required.")
                );
            }

            log.info("Generating admin events report for month: {}", month);

            List<Event> events;
            if (month != null && !month.equals("all")) {
                events = getEventsByMonth(month);
            } else {
                events = eventRepository.findAll();
            }

            List<Map<String, Object>> eventsList = events.stream()
                .map(this::buildEventReportData)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("totalEvents", events.size());
            response.put("eventsList", eventsList);
            response.put("monthlyEvents", getMonthlyEventCounts());

            log.info("Admin report generated successfully with {} events", events.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error generating admin events report: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(
                Map.of("error", "Failed to generate admin report: " + e.getMessage())
            );
        }
    }

    @GetMapping("/events/monthly-summary")
    public ResponseEntity<?> getMonthlyEventsSummary(Authentication authentication) {
        try {
            // Verify admin access
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(403).body(
                    Map.of("error", "Access denied. Admin privileges required.")
                );
            }

            Map<String, Long> monthlyData = getMonthlyEventCounts();
            
            return ResponseEntity.ok(Map.of(
                "monthlyEvents", monthlyData,
                "totalEvents", eventRepository.count()
            ));

        } catch (Exception e) {
            log.error("Error generating monthly events summary: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(
                Map.of("error", "Failed to generate monthly summary: " + e.getMessage())
            );
        }
    }

    @GetMapping("/events/{eventId}/attendance-details")
    public ResponseEntity<?> getEventAttendanceDetails(
            @PathVariable Long eventId,
            Authentication authentication) {
        try {
            // Verify admin access
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(403).body(
                    Map.of("error", "Access denied. Admin privileges required.")
                );
            }

            Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));

            long registrantCount = attendanceRepository.countByEventId(eventId);
            long attendeeCount = attendanceRepository.countByEventIdAndAttended(eventId, true);

            Map<String, Object> response = new HashMap<>();
            response.put("eventId", eventId);
            response.put("eventName", event.getName());
            response.put("eventDate", event.getDate());
            response.put("location", event.getPlace());
            response.put("registrantCount", registrantCount);
            response.put("attendeeCount", attendeeCount);
            response.put("attendanceRate", registrantCount > 0 ? 
                String.format("%.2f%%", (double) attendeeCount / registrantCount * 100) : "0.00%");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting event attendance details: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(
                Map.of("error", "Failed to get attendance details: " + e.getMessage())
            );
        }
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        try {
            String email = authentication.getName();
            Optional<User> userOpt = userRepository.findByEmail(email);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                // Check for admin role - try multiple possible method names
                // Adjust this based on your actual User entity structure
                String userRole = null;
                try {
                    // Try common method names for getting user role/type
                    if (hasMethod(user, "getRole")) {
                        userRole = (String) user.getClass().getMethod("getRole").invoke(user);
                    } else if (hasMethod(user, "getUserType")) {
                        userRole = (String) user.getClass().getMethod("getUserType").invoke(user);
                    } else if (hasMethod(user, "getType")) {
                        userRole = (String) user.getClass().getMethod("getType").invoke(user);
                    } else if (hasMethod(user, "getAuthority")) {
                        userRole = (String) user.getClass().getMethod("getAuthority").invoke(user);
                    }
                } catch (Exception e) {
                    log.debug("Error getting user role via reflection: {}", e.getMessage());
                }
                
                if (userRole != null) {
                    return "ADMIN".equalsIgnoreCase(userRole) || "admin".equalsIgnoreCase(userRole);
                }
            }
            
            // Fallback: check if authentication contains admin authorities
            return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN") || 
                                auth.getAuthority().equals("ADMIN"));
                
        } catch (Exception e) {
            log.error("Error checking admin privileges: {}", e.getMessage());
            return false;
        }
    }

    private List<Event> getEventsByMonth(String monthStr) {
        try {
            YearMonth yearMonth = YearMonth.parse(monthStr);
            LocalDate startDate = yearMonth.atDay(1);
            LocalDate endDate = yearMonth.atEndOfMonth();

            return eventRepository.findAll().stream()
                .filter(event -> {
                    LocalDate eventDate = event.getDate();
                    return eventDate != null && 
                           !eventDate.isBefore(startDate) && 
                           !eventDate.isAfter(endDate);
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Error parsing month string: {}", monthStr, e);
            return new ArrayList<>();
        }
    }

    private Map<String, Object> buildEventReportData(Event event) {
        Map<String, Object> eventData = new HashMap<>();
        
        eventData.put("id", event.getId());
        eventData.put("name", event.getName());
        eventData.put("date", event.getDate());
        eventData.put("location", event.getPlace());
        eventData.put("place", event.getPlace()); // Alternative field name
        
        // Get attendance statistics
        long registrantCount = attendanceRepository.countByEventId(event.getId());
        long attendeeCount = attendanceRepository.countByEventIdAndAttended(event.getId(), true);
        
        eventData.put("registrantCount", registrantCount);
        eventData.put("attendeeCount", attendeeCount);
        
        // Determine if event is online or face-to-face
        // This is a simple heuristic based on location/place field
        // You might want to add an explicit field to Event entity
        String location = event.getPlace();
        boolean isOnline = location != null && 
            (location.toLowerCase().contains("online") ||
             location.toLowerCase().contains("zoom") ||
             location.toLowerCase().contains("teams") ||
             location.toLowerCase().contains("virtual") ||
             location.toLowerCase().contains("webinar") ||
             location.toLowerCase().startsWith("http"));
        
        eventData.put("isOnline", isOnline);
        eventData.put("eventType", isOnline ? "Online" : "Face-to-Face");
        
        // Additional data
        eventData.put("description", event.getDescription());
        eventData.put("time", event.getTime());
        eventData.put("createdBy", event.getUser().getEmail());
        eventData.put("isFree", event.getIsFree());
        eventData.put("price", event.getPrice());
        
        return eventData;
    }

    private Map<String, Long> getMonthlyEventCounts() {
        Map<String, Long> monthlyData = new LinkedHashMap<>();
        
        // Get all events
        List<Event> allEvents = eventRepository.findAll();
        
        // Group by month
        Map<String, Long> eventsByMonth = allEvents.stream()
            .filter(event -> event.getDate() != null)
            .collect(Collectors.groupingBy(
                event -> event.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                Collectors.counting()
            ));
        
        // Ensure we have entries for the current year's months (even if 0)
        int currentYear = LocalDate.now().getYear();
        for (int month = 1; month <= 12; month++) {
            String monthKey = String.format("%d-%02d", currentYear, month);
            monthlyData.put(monthKey, eventsByMonth.getOrDefault(monthKey, 0L));
        }
        
        // Add any additional months from the data that aren't in current year
        eventsByMonth.forEach((month, count) -> {
            if (!monthlyData.containsKey(month)) {
                monthlyData.put(month, count);
            }
        });
        
        return monthlyData;
    }
    
    // Helper method to check if a method exists on an object
    private boolean hasMethod(Object obj, String methodName) {
        try {
            obj.getClass().getMethod(methodName);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}