package com.example.attendance.controller;

import com.example.attendance.dto.EventRequestDTO;
import com.example.attendance.dto.EventResponseDTO;
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

            event.setIsFree(request.getIsFree());
            if (request.getIsFree()) {
                event.setPrice(BigDecimal.ZERO);
            } else {
                event.setPrice(request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO);
            }

            Event createdEvent = eventRepository.save(event);

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

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating event: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Event creation failed",
                            "message", e.getMessage()
                    ));
        }
    }

    @GetMapping("/my-events")
    public ResponseEntity<List<EventResponseDTO>> getEventsByUser(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalStateException("User not found"));

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
    public ResponseEntity<List<EventResponseDTO>> getAllEvents() {
        try {
            List<Event> events = eventRepository.findAll();

            List<EventResponseDTO> response = events.stream()
                    .map(EventResponseDTO::fromEntity)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching all events", e);
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
    public ResponseEntity<List<EventResponseDTO>> getFreeEvents() {
        try {
            List<Event> events = eventRepository.findByIsFree(true);
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
    public ResponseEntity<List<EventResponseDTO>> getPaidEvents() {
        try {
            List<Event> events = eventRepository.findByIsFree(false);
            List<EventResponseDTO> response = events.stream()
                    .map(EventResponseDTO::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching paid events", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/events/{eventId}/scan")
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

    // 🔧 Helper method
    private Long getUserIdFromAuthentication(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        return user.getId();
    }
}
