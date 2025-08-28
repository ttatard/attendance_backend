package com.example.attendance.controller;

import com.example.attendance.entity.Event;
import com.example.attendance.entity.EventRegistration;
import com.example.attendance.entity.User;
import com.example.attendance.entity.UserEventAttendance;
import com.example.attendance.repository.EventRepository;
import com.example.attendance.repository.EventRegistrationRepository;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.repository.UserEventAttendanceRepository;
import com.example.attendance.dto.CodeVerificationRequest;
import com.example.attendance.entity.EventRegistration.RegistrationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/registrations")
@CrossOrigin(origins = "http://localhost:3000")
public class EventRegistrationController {
    
    private static final Logger log = LoggerFactory.getLogger(EventRegistrationController.class);
    
    private final EventRegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final UserEventAttendanceRepository userEventAttendanceRepository;

    public EventRegistrationController(EventRegistrationRepository registrationRepository,
                                     UserRepository userRepository,
                                     EventRepository eventRepository,
                                     UserEventAttendanceRepository userEventAttendanceRepository) {
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.userEventAttendanceRepository = userEventAttendanceRepository;
    }

    // Generate a unique code for registration
    private String generateUniqueCode() {
        String code;
        do {
            // Generate a 6-character alphanumeric code
            code = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 6).toUpperCase();
        } while (registrationRepository.existsByUniqueCode(code));
        return code;
    }

   @PostMapping("/pre-register/{eventId}")
public ResponseEntity<?> preRegisterForEvent(
        @PathVariable Long eventId,
        Authentication authentication) {
    try {
        String email = authentication.getName();
        log.info("=== PRE-REGISTER START ===");
        log.info("Pre-registering user {} for event {}", email, eventId);
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> {
                log.error("User not found with email: {}", email);
                return new IllegalStateException("User not found with email: " + email);
            });
        
        log.info("User found: ID={}, Name={}", user.getId(), user.getName());
        
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> {
                log.error("Event not found with ID: {}", eventId);
                return new IllegalStateException("Event not found with ID: " + eventId);
            });
        
        log.info("Event found: ID={}, Name={}, RequiresApproval={}", 
                 event.getId(), event.getName(), event.getRequiresApproval());
        
        // Check if user is already registered
        EventRegistration existingRegistration = registrationRepository.findByEventIdAndUserEmail(eventId, email);
        
        if (existingRegistration != null) {
            log.info("Existing registration found: Status={}", existingRegistration.getStatus());
            if (existingRegistration.getStatus() == RegistrationStatus.APPROVED) {
                log.info("User already approved for event");
                return ResponseEntity.ok()
                    .body(Map.of(
                        "message", "You are already registered for this event",
                        "isApproved", true,
                        "uniqueCode", existingRegistration.getUniqueCode()
                    ));
            } else {
                log.info("User has pending registration");
                return ResponseEntity.ok()
                    .body(Map.of(
                        "message", "Your registration is pending approval",
                        "isApproved", false,
                        "uniqueCode", existingRegistration.getUniqueCode()
                    ));
            }
        }
        
        log.info("No existing registration found, creating new one");
        
        // Generate unique code for this registration
        String uniqueCode = generateUniqueCode();
        
        // Create new registration - auto-approve if no approval needed
        EventRegistration registration = new EventRegistration(eventId, email, user.getName());
        registration.setUniqueCode(uniqueCode);
        
        // Auto-approve if event doesn't require approval (default to true if null)
        boolean requiresApproval = event.getRequiresApproval() != null ? event.getRequiresApproval() : true;
        
        log.info("Registration requires approval: {}", requiresApproval);
        
        if (!requiresApproval) {
            registration.setStatus(RegistrationStatus.APPROVED);
            log.info("Auto-approving registration");
        } else {
            registration.setStatus(RegistrationStatus.PENDING);
            log.info("Setting registration to pending");
        }
        
        EventRegistration savedRegistration = registrationRepository.save(registration);
        log.info("Registration saved: ID={}, Status={}, Code={}", 
                savedRegistration.getId(), savedRegistration.getStatus(), savedRegistration.getUniqueCode());
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", savedRegistration.getId());
        response.put("eventId", eventId);
        response.put("eventName", event.getName());
        response.put("registrationDate", savedRegistration.getRegistrationDate());
        response.put("status", savedRegistration.getStatus());
        response.put("uniqueCode", savedRegistration.getUniqueCode());
        response.put("isApproved", savedRegistration.getStatus() == RegistrationStatus.APPROVED);
        response.put("message", savedRegistration.getStatus() == RegistrationStatus.APPROVED ? 
            "Successfully registered for event. Your unique code is: " + uniqueCode : 
            "Successfully pre-registered for event. Your unique code is: " + uniqueCode + ". Awaiting approval.");
        
        log.info("=== PRE-REGISTER COMPLETE ===");
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

@GetMapping("/debug/{eventId}")
public ResponseEntity<?> debugRegistration(
        @PathVariable Long eventId,
        Authentication authentication) {
    try {
        String email = authentication.getName();
        log.info("Debug registration for user {} and event {}", email, eventId);
        
        // Check if event exists
        boolean eventExists = eventRepository.existsById(eventId);
        if (!eventExists) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Event not found"));
        }
        
        // Check if user exists
        boolean userExists = userRepository.findByEmail(email).isPresent();
        if (!userExists) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "User not found"));
        }
        
        // Check registration status
        EventRegistration registration = registrationRepository.findByEventIdAndUserEmail(eventId, email);
        
        Map<String, Object> response = new HashMap<>();
        response.put("eventExists", eventExists);
        response.put("userExists", userExists);
        response.put("hasRegistration", registration != null);
        
        if (registration != null) {
            response.put("registrationStatus", registration.getStatus());
            response.put("registrationId", registration.getId());
            response.put("uniqueCode", registration.getUniqueCode());
        }
        
        return ResponseEntity.ok(response);
        
    } catch (Exception e) {
        log.error("Debug error: {}", e.getMessage(), e);
        return ResponseEntity.internalServerError()
            .body(Map.of("error", "Debug failed", "message", e.getMessage()));
    }
}

@GetMapping("/debug-endpoints")
public ResponseEntity<?> debugEndpoints() {
    Map<String, String> endpoints = new HashMap<>();
    endpoints.put("pre-register", "POST /api/registrations/pre-register/{eventId}");
    endpoints.put("cancel", "DELETE /api/registrations/cancel/{eventId}");
    endpoints.put("check", "GET /api/registrations/check/{eventId}");
    endpoints.put("my-registrations", "GET /api/registrations/my-registrations");
    
    return ResponseEntity.ok(endpoints);
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
                    regMap.put("uniqueCode", reg.getUniqueCode());
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
                    regMap.put("uniqueCode", reg.getUniqueCode());
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
            response.put("uniqueCode", registration.getUniqueCode());
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
            response.put("uniqueCode", registration.getUniqueCode());
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

    @PostMapping("/cancel/{eventId}")
public ResponseEntity<?> cancelRegistration(
        @PathVariable Long eventId,
        Authentication authentication) {
    try {
        String email = authentication.getName();
        log.info("Cancelling registration for event {} by user {}", eventId, email);
        
        // Check if registration exists
        EventRegistration registration = registrationRepository.findByEventIdAndUserEmail(eventId, email);
        
        if (registration == null) {
            log.warn("User {} is not registered for event {}", email, eventId);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "You are not registered for this event"));
        }
        
        log.info("Found registration: ID={}, Status={}, Code={}", 
                registration.getId(), registration.getStatus(), registration.getUniqueCode());
        
        // Delete the registration
        registrationRepository.delete(registration);
        log.info("Registration deleted successfully");
        
        return ResponseEntity.ok(Map.of(
            "message", "Registration cancelled successfully",
            "eventId", eventId,
            "eventName", "Event" // You might want to get the actual event name
        ));
        
    } catch (Exception e) {
        log.error("Error cancelling registration for event {}: {}", eventId, e.getMessage(), e);
        return ResponseEntity.internalServerError()
            .body(Map.of(
                "error", "Failed to cancel registration",
                "message", e.getMessage()
            ));
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
                "registrationDate", registration.getRegistrationDate(),
                "uniqueCode", registration.getUniqueCode()
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
                    regMap.put("uniqueCode", reg.getUniqueCode());
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
                    regMap.put("uniqueCode", reg.getUniqueCode());
                    return regMap;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching pending registrations", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Verify unique code for attendance
  @PostMapping("/verify-code")
public ResponseEntity<?> verifyCodeForAttendance(
        @RequestBody CodeVerificationRequest request,
        Authentication authentication) {
    try {
        log.info("Verifying code {} for event {}", request.getCode(), request.getEventId());
        
        // Find registration by event ID and unique code
        EventRegistration registration = registrationRepository.findByEventIdAndUniqueCode(
            request.getEventId(), request.getCode());
        
        if (registration == null) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "status", "error",
                    "message", "Invalid code for this event"
                ));
        }
        
        if (registration.getStatus() != RegistrationStatus.APPROVED) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "status", "error",
                    "message", "Registration is not approved"
                ));
        }
        
        // Find user by email
        User user = userRepository.findByEmail(registration.getUserEmail())
            .orElseThrow(() -> new IllegalStateException("User not found"));
        
        // Find event by ID
        Event event = eventRepository.findById(request.getEventId())
            .orElseThrow(() -> new IllegalStateException("Event not found"));
        
        // Check if user has already attended this event
        boolean alreadyAttended = userEventAttendanceRepository.existsByUserAndEvent(user, event);
        if (alreadyAttended) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "status", "error",
                    "message", "User already recorded for the event",
                    "userName", registration.getUserName(),
                    "userEmail", registration.getUserEmail()
                ));
        }
        
        // Record attendance
        UserEventAttendance attendance = UserEventAttendance.builder()
            .user(user)
            .event(event)
            .attended(true)
            .checkInTime(LocalDateTime.now())
            .build();
        
        userEventAttendanceRepository.save(attendance);
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Attendance recorded successfully",
            "userName", registration.getUserName(),
            "userEmail", registration.getUserEmail()
        ));
        
    } catch (Exception e) {
        log.error("Error verifying code: {}", e.getMessage(), e);
        return ResponseEntity.internalServerError()
            .body(Map.of(
                "status", "error",
                "message", "Code verification failed: " + e.getMessage()
            ));
    }
}
}