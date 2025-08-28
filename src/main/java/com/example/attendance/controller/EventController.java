package com.example.attendance.controller;

import com.example.attendance.dto.EventRequestDTO;
import com.example.attendance.dto.EventResponseDTO;
import com.example.attendance.entity.Organizer;
import com.example.attendance.entity.Event;
import com.example.attendance.entity.User;
import com.example.attendance.entity.UserEventAttendance;
import com.example.attendance.repository.EventRepository;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.security.JwtTokenProvider;
import com.example.attendance.service.EventService;
import com.example.attendance.service.UserEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserEventService userEventService;
    private final EventService eventService;

   @PostMapping
public ResponseEntity<?> createEvent(
    @Valid @RequestBody EventRequestDTO request,
    Authentication authentication) {
try {
    String email = authentication.getName();
    log.info("Creating event for user: {}", email);
    log.info("Request details: {}", request.toString());

    // Custom validation for date and time
    if (!request.isValidDateTime()) {
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "error", "Invalid date or time",
                        "message", "Event date cannot be in the past. For today's events, time must be at least 5 minutes in the future."
                ));
    }

    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalStateException("User not found with email: " + email));

    Event event = new Event();
    event.setName(request.getName());
    event.setDate(request.getDate());
    event.setTime(request.getTime());
    event.setPlace(request.getPlace());
    event.setDescription(request.getDescription());
    event.setUser(user);
    event.setQrCode("EVT-" + UUID.randomUUID().toString());

    // Handle price fields
    event.setIsFree(request.getIsFree());
    if (request.getIsFree()) {
        event.setPrice(BigDecimal.ZERO);
    } else {
        event.setPrice(request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO);
    }

    // Handle requiresApproval field
    if (request.getRequiresApproval() != null) {
        event.setRequiresApproval(request.getRequiresApproval());
    } else {
        // Default to true if not specified
        event.setRequiresApproval(true);
    }

    // Handle recurrence fields
    if (request.getRecurrencePattern() != null && !request.getRecurrencePattern().equals("none")) {
        event.setRecurrencePattern(request.getRecurrencePatternEnum());
        event.setRecurrenceInterval(request.getRecurrenceInterval() != null ? request.getRecurrenceInterval() : 1);
        event.setRecurrenceEndDate(request.getRecurrenceEndDate());
        event.setRecurrenceCount(request.getRecurrenceCount());
        event.setIsRecurringInstance(false);
        
        log.info("Setting recurrence: pattern={}, interval={}, endDate={}, count={}", 
                request.getRecurrencePattern(), 
                request.getRecurrenceInterval(),
                request.getRecurrenceEndDate(),
                request.getRecurrenceCount());
    } else {
        event.setRecurrencePattern(Event.RecurrencePattern.NONE);
        event.setRecurrenceInterval(1);
        event.setIsRecurringInstance(false);
        log.info("Creating non-recurring event");
    }

    // Handle online/offline fields
    event.setIsOnline(request.getIsOnline());
    if (request.getIsOnline()) {
        event.setMeetingUrl(request.getMeetingUrl());
        event.setMeetingId(request.getMeetingId());
        event.setMeetingPasscode(request.getMeetingPasscode());
    }
    
    event.setMaxCapacity(request.getMaxCapacity());
    event.setRegistrationDeadline(request.getRegistrationDeadline());
    event.setCategory(request.getCategory());
    event.setStatus(request.getStatus());

    Event createdEvent = eventRepository.save(event);
    log.info("Event created successfully with ID: {} for date: {} at time: {}", 
             createdEvent.getId(), createdEvent.getDate(), createdEvent.getTime());

    Map<String, Object> response = new HashMap<>();
    response.put("id", createdEvent.getId());
    response.put("name", createdEvent.getName());
    response.put("date", createdEvent.getDate());
    response.put("time", createdEvent.getTime());
    response.put("place", createdEvent.getPlace());
    response.put("description", createdEvent.getDescription());
    response.put("qrCode", createdEvent.getQrCode());
    response.put("userId", createdEvent.getUser().getId());
    response.put("isFree", createdEvent.getIsFree());
    response.put("price", createdEvent.getPrice());
    response.put("requiresApproval", createdEvent.getRequiresApproval());
    
    // Include recurrence info in response
    response.put("recurrencePattern", createdEvent.getRecurrencePattern());
    response.put("recurrenceInterval", createdEvent.getRecurrenceInterval());
    response.put("recurrenceEndDate", createdEvent.getRecurrenceEndDate());
    response.put("recurrenceCount", createdEvent.getRecurrenceCount());
    response.put("isRecurringInstance", createdEvent.getIsRecurringInstance());
    
    // Include online/offline fields
    response.put("isOnline", createdEvent.getIsOnline());
    response.put("meetingUrl", createdEvent.getMeetingUrl());
    response.put("meetingId", createdEvent.getMeetingId());
    response.put("meetingPasscode", createdEvent.getMeetingPasscode());
    response.put("maxCapacity", createdEvent.getMaxCapacity());
    response.put("registrationDeadline", createdEvent.getRegistrationDeadline());
    response.put("category", createdEvent.getCategory());
    response.put("status", createdEvent.getStatus());

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
} catch (Exception e) {
    log.error("Error creating event: {}", e.getMessage(), e);
    return ResponseEntity.internalServerError()
            .body(Map.of(
                    "error", "Event creation failed",
                    "message", e.getMessage(),
                    "details", e.getClass().getSimpleName()
            ));
}
}

    @GetMapping("/my-events")
public ResponseEntity<List<EventResponseDTO>> getEventsByUser(Authentication authentication) {
    try {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        // Get all events for admin (including past ones)
        List<Event> events = eventRepository.findByUserId(user.getId());

        List<EventResponseDTO> response = events.stream()
                .map(EventResponseDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    } catch (Exception e) {
        log.error("Error fetching events", e);
        return ResponseEntity.internalServerError().build();
    }
}

@GetMapping
public ResponseEntity<List<EventResponseDTO>> getAllEvents(Authentication authentication) {
    try {
        String email = authentication.getName();
        log.info("Fetching events for user email: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        log.info("User found: ID={}, Email={}", user.getId(), user.getEmail());
        
        // Get all organizations the user is enrolled in
        Set<Organizer> enrolledOrganizations = user.getEnrolledOrganizations();
        log.info("User enrolled in {} organizations", enrolledOrganizations.size());
        
        if (enrolledOrganizations.isEmpty()) {
            log.info("User {} is not enrolled in any organization", email);
            return ResponseEntity.ok(Collections.emptyList());
        }
        
        // Get the organization IDs user is enrolled in
        Set<Long> enrolledOrgIds = enrolledOrganizations.stream()
                .map(Organizer::getId)
                .collect(Collectors.toSet());
        
        log.info("User enrolled in organization IDs: {}", enrolledOrgIds);
        
        // Check if user is admin - admins see all events, users see only upcoming events
boolean isAdmin = user.getAccountType() != null && user.getAccountType().equals(User.AccountType.ADMIN);        
        List<Event> filteredEvents;
        
        if (isAdmin) {
            // For admins, show all events (including past ones)
            filteredEvents = eventRepository.findEventsByOrganizationIds(enrolledOrgIds);
            log.info("Admin view: Found {} total events for user's organizations", filteredEvents.size());
        } else {
            // For regular users, show only upcoming events
            filteredEvents = eventRepository.findUpcomingEventsByOrganizationIds(enrolledOrgIds);
            log.info("User view: Found {} upcoming events for user's organizations", filteredEvents.size());
        }
        
        // Convert to DTOs
        List<EventResponseDTO> response = filteredEvents.stream()
                .map(event -> {
                    EventResponseDTO dto = EventResponseDTO.fromEntity(event);
                    
                    // Set organizer information
                    if (event.getUser() != null && event.getUser().getOrganizer() != null) {
                        dto.setOrganizerName(event.getUser().getOrganizer().getOrganizationName());
                        dto.setOrganizerId(event.getUser().getOrganizer().getId());
                    } else if (event.getUser() != null) {
                        // Fallback: use first enrolled organization info
                        Optional<Organizer> creatorOrg = event.getUser().getEnrolledOrganizations().stream().findFirst();
                        if (creatorOrg.isPresent()) {
                            dto.setOrganizerName(creatorOrg.get().getOrganizationName());
                            dto.setOrganizerId(creatorOrg.get().getId());
                        } else {
                            dto.setOrganizerName(event.getUser().getName());
                        }
                    }
                    
                    return dto;
                })
                .collect(Collectors.toList());

        log.info("Returning {} events for user {}", response.size(), email);
        return ResponseEntity.ok(response);
        
    } catch (Exception e) {
        log.error("Error fetching filtered events for user: {}", authentication.getName(), e);
        return ResponseEntity.internalServerError().build();
    }
}

    @GetMapping("/{id}")
    public ResponseEntity<?> getEventById(@PathVariable Long id) {
        try {
            Event event = eventRepository.findById(id)
                    .orElseThrow(() -> new IllegalStateException("Event not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("id", event.getId());
            response.put("name", event.getName());
            response.put("date", event.getDate());
            response.put("time", event.getTime());
            response.put("place", event.getPlace());
            response.put("description", event.getDescription());
            response.put("qrCode", event.getQrCode());
            response.put("userId", event.getUser().getId());
            response.put("isFree", event.getIsFree());
            response.put("price", event.getPrice());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "error", "Event not found",
                            "message", e.getMessage()
                    ));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            Event event = eventRepository.findById(id)
                    .orElseThrow(() -> new IllegalStateException("Event not found"));

            if (!event.getUser().getEmail().equals(email)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You can only delete your own events");
            }

            eventRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Deletion failed",
                            "message", e.getMessage()
                    ));
        }
    }

    @GetMapping("/filter/free")
    public ResponseEntity<List<EventResponseDTO>> getFreeEvents(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalStateException("User not found"));

            // Get the first organization the user is enrolled in
            Optional<Organizer> firstOrganization = user.getEnrolledOrganizations().stream().findFirst();
            
            List<Event> events;
            
            if (firstOrganization.isPresent()) {
                Long organizationId = firstOrganization.get().getId();
                events = eventRepository.findFreeEventsByOrganizationId(organizationId);
            } else {
                events = new ArrayList<>();
            }
            
            List<EventResponseDTO> response = events.stream()
                    .map(EventResponseDTO::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching free events", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/filter/paid")
    public ResponseEntity<List<EventResponseDTO>> getPaidEvents(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalStateException("User not found"));

            // Get the first organization the user is enrolled in
            Optional<Organizer> firstOrganization = user.getEnrolledOrganizations().stream().findFirst();
            
            List<Event> events;
            
            if (firstOrganization.isPresent()) {
                Long organizationId = firstOrganization.get().getId();
                events = eventRepository.findPaidEventsByOrganizationId(organizationId);
            } else {
                events = new ArrayList<>();
            }
            
            List<EventResponseDTO> response = events.stream()
                    .map(EventResponseDTO::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching paid events", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{eventId}/scan")
    public ResponseEntity<?> scanEventQrCode(
            @PathVariable Long eventId,
            @RequestBody String qrCode,
            Authentication authentication) {
        try {
            Event event = eventService.verifyEventQrCode(eventId, qrCode);
            Long userId = getUserIdFromAuthentication(authentication);

            UserEventAttendance attendance = userEventService.recordAttendance(userId, eventId);

            return ResponseEntity.ok(Map.of(
                    "message", "Attendance recorded successfully",
                    "event", event.getName(),
                    "attendanceDate", attendance.getAttendanceDate()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        return user.getId();
    }
}