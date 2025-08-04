package com.example.attendance.controller;

import com.example.attendance.entity.Event;
import com.example.attendance.entity.EventRegistration;
import com.example.attendance.entity.User;
import com.example.attendance.repository.EventRepository;
import com.example.attendance.repository.EventRegistrationRepository;
import com.example.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class EventRegistrationController {
    
    private final EventRegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @PostMapping("/pre-register/{eventId}")
    public ResponseEntity<?> preRegisterForEvent(
            @PathVariable Long eventId,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            log.info("Pre-registering user {} for event {}", email, eventId);
            
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found with email: " + email));
            
            Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Event not found with ID: " + eventId));
            
            // Check if user is already registered
            if (registrationRepository.existsByEventIdAndUserEmail(eventId, email)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "You are already registered for this event"));
            }
            
            // Create new registration with PENDING status
            EventRegistration registration = new EventRegistration(eventId, email, user.getName());
            registration.setStatus(EventRegistration.RegistrationStatus.PENDING);
            EventRegistration savedRegistration = registrationRepository.save(registration);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", savedRegistration.getId());
            response.put("eventId", eventId);
            response.put("eventName", event.getName());
            response.put("registrationDate", savedRegistration.getRegistrationDate());
            response.put("status", savedRegistration.getStatus());
            response.put("message", "Successfully pre-registered for event. Awaiting approval.");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("Error pre-registering for event: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "Pre-registration failed",
                    "message", e.getMessage()
                ));
        }
    }

    @GetMapping("/my-registrations")
    public ResponseEntity<?> getMyRegistrations(Authentication authentication) {
        try {
            String email = authentication.getName();
            List<EventRegistration> registrations = registrationRepository.findByUserEmail(email);
            
            List<Map<String, Object>> response = registrations.stream()
                .map(reg -> {
                    Map<String, Object> regMap = new HashMap<>();
                    regMap.put("id", reg.getId());
                    regMap.put("eventId", reg.getEventId());
                    regMap.put("registrationDate", reg.getRegistrationDate());
                    regMap.put("status", reg.getStatus());
                    return regMap;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching registrations", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<?> getEventRegistrations(
            @PathVariable Long eventId,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
            
            // Verify that the user is the owner of the event
            Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Event not found"));
            
            if (!event.getUser().getEmail().equals(email)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only view registrations for your own events"));
            }
            
            List<EventRegistration> registrations = registrationRepository.findByEventId(eventId);
            
            List<Map<String, Object>> response = registrations.stream()
                .map(reg -> {
                    Map<String, Object> regMap = new HashMap<>();
                    regMap.put("id", reg.getId());
                    regMap.put("userEmail", reg.getUserEmail());
                    regMap.put("userName", reg.getUserName());
                    regMap.put("registrationDate", reg.getRegistrationDate());
                    regMap.put("status", reg.getStatus());
                    return regMap;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching event registrations", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/approve/{registrationId}")
    public ResponseEntity<?> approveRegistration(
            @PathVariable Long registrationId,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            log.info("Approving registration {} by user {}", registrationId, email);
            
            EventRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalStateException("Registration not found"));
            
            // Verify that the user is the owner of the event
            Event event = eventRepository.findById(registration.getEventId())
                .orElseThrow(() -> new IllegalStateException("Event not found"));
            
            if (!event.getUser().getEmail().equals(email)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only approve registrations for your own events"));
            }
            
            // Update registration status to APPROVED
            registration.setStatus(EventRegistration.RegistrationStatus.APPROVED);
            registrationRepository.save(registration);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", registration.getId());
            response.put("status", registration.getStatus());
            response.put("message", "Registration approved successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error approving registration: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "Failed to approve registration",
                    "message", e.getMessage()
                ));
        }
    }

    @PostMapping("/disapprove/{registrationId}")
    public ResponseEntity<?> disapproveRegistration(
            @PathVariable Long registrationId,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            log.info("Disapproving registration {} by user {}", registrationId, email);
            
            EventRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalStateException("Registration not found"));
            
            // Verify that the user is the owner of the event
            Event event = eventRepository.findById(registration.getEventId())
                .orElseThrow(() -> new IllegalStateException("Event not found"));
            
            if (!event.getUser().getEmail().equals(email)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only disapprove registrations for your own events"));
            }
            
            // Update registration status to DISAPPROVED
            registration.setStatus(EventRegistration.RegistrationStatus.DISAPPROVED);
            registrationRepository.save(registration);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", registration.getId());
            response.put("status", registration.getStatus());
            response.put("message", "Registration disapproved successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error disapproving registration: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "Failed to disapprove registration",
                    "message", e.getMessage()
                ));
        }
    }

    @DeleteMapping("/cancel/{eventId}")
    public ResponseEntity<?> cancelRegistration(
            @PathVariable Long eventId,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            
            if (!registrationRepository.existsByEventIdAndUserEmail(eventId, email)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "You are not registered for this event"));
            }
            
            registrationRepository.deleteByEventIdAndUserEmail(eventId, email);
            
            return ResponseEntity.ok(Map.of("message", "Registration cancelled successfully"));
            
        } catch (Exception e) {
            log.error("Error cancelling registration", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to cancel registration"));
        }
    }
    
    @GetMapping("/check/{eventId}")
    public ResponseEntity<?> checkRegistration(
            @PathVariable Long eventId,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            EventRegistration registration = registrationRepository.findByEventIdAndUserEmail(eventId, email);
            
            if (registration == null) {
                return ResponseEntity.ok(Map.of(
                    "isRegistered", false,
                    "status", "NOT_REGISTERED"
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                "isRegistered", true,
                "status", registration.getStatus(),
                "registrationDate", registration.getRegistrationDate()
            ));
            
        } catch (Exception e) {
            log.error("Error checking registration", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/approved/event/{eventId}")
    public ResponseEntity<?> getApprovedRegistrations(@PathVariable Long eventId) {
        try {
            List<EventRegistration> approvedRegistrations = 
                registrationRepository.findByEventIdAndStatus(eventId, EventRegistration.RegistrationStatus.APPROVED);
            
            List<Map<String, Object>> response = approvedRegistrations.stream()
                .map(reg -> {
                    Map<String, Object> regMap = new HashMap<>();
                    regMap.put("id", reg.getId());
                    regMap.put("userEmail", reg.getUserEmail());
                    regMap.put("userName", reg.getUserName());
                    regMap.put("registrationDate", reg.getRegistrationDate());
                    regMap.put("status", reg.getStatus());
                    return regMap;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching approved registrations", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/pending/event/{eventId}")
    public ResponseEntity<?> getPendingRegistrations(
            @PathVariable Long eventId,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            
            // Verify that the user is the owner of the event
            Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Event not found"));
            
            if (!event.getUser().getEmail().equals(email)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only view pending registrations for your own events"));
            }
            
            List<EventRegistration> pendingRegistrations = 
                registrationRepository.findByEventIdAndStatus(eventId, EventRegistration.RegistrationStatus.PENDING);
            
            List<Map<String, Object>> response = pendingRegistrations.stream()
                .map(reg -> {
                    Map<String, Object> regMap = new HashMap<>();
                    regMap.put("id", reg.getId());
                    regMap.put("userEmail", reg.getUserEmail());
                    regMap.put("userName", reg.getUserName());
                    regMap.put("registrationDate", reg.getRegistrationDate());
                    regMap.put("status", reg.getStatus());
                    return regMap;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching pending registrations", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}