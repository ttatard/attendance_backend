package com.example.attendance.controller;

import com.example.attendance.dto.*;
import com.example.attendance.entity.Organizer;
import com.example.attendance.entity.User;
import com.example.attendance.entity.UserEventAttendance;
import com.example.attendance.exception.DuplicateEmailException;
import com.example.attendance.repository.OrganizerRepository;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.security.JwtTokenProvider;
import com.example.attendance.service.AuthService;
import com.example.attendance.service.OrganizerService;
import com.example.attendance.service.UserEventService;
import com.example.attendance.service.SystemOwnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final OrganizerService organizerService;
    private final OrganizerRepository organizerRepository;
    private final UserEventService userEventService;
    private final SystemOwnerService systemOwnerService;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }
            
            String email = jwtTokenProvider.extractUsername(token.substring(7));
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BadCredentialsException("User not found"));
            
            if (user.isDeleted()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found");
            }
            
            if (user.isDeactivated()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                            "message", "Account is deactivated",
                            "isDeactivated", true,
                            "email", user.getEmail()
                        ));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("email", user.getEmail());
            response.put("birthday", user.getBirthday());
            response.put("gender", user.getGender() != null ? user.getGender().name() : null);
            response.put("accountType", user.getAccountType() != null ? user.getAccountType().name() : null);
            response.put("address", user.getAddress());
            response.put("spouseName", user.getSpouseName());
            response.put("ministry", user.getMinistry() != null ? user.getMinistry().name() : null);
            response.put("apostolate", user.getApostolate() != null ? user.getApostolate().name() : null);
            response.put("createdAt", user.getCreatedAt());
            response.put("updatedAt", user.getUpdatedAt());
            response.put("isDeleted", user.isDeleted());
            response.put("isDeactivated", user.isDeactivated());
            
            // Add enrolled organizations information
            if (!user.getEnrolledOrganizations().isEmpty()) {
                List<Map<String, Object>> enrolledOrgs = user.getEnrolledOrganizations().stream()
                    .map(org -> {
                        Map<String, Object> orgMap = new HashMap<>();
                        orgMap.put("id", org.getId());
                        orgMap.put("organizationName", org.getOrganizationName());
                        return orgMap;
                    })
                    .collect(Collectors.toList());
                response.put("enrolledOrganizations", enrolledOrgs);
            } else {
                response.put("enrolledOrganizations", Collections.emptyList());
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching user data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching user data");
        }
    }

    @GetMapping("/me/attended-events")
    public ResponseEntity<?> getMyAttendedEvents(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtTokenProvider.extractUsername(token.substring(7));
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BadCredentialsException("User not found"));
            
            List<UserEventAttendance> attendances = userEventService.getUserAttendedEvents(user.getId());
            
            List<UserAttendanceDto> attendanceDtos = attendances.stream()
                    .map(UserAttendanceDto::fromEntity)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(attendanceDtos);
        } catch (Exception e) {
            log.error("Error fetching attended events", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching attended events");
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers(@RequestHeader("Authorization") String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }

            String email = jwtTokenProvider.extractUsername(token.substring(7));
            User requestingUser = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BadCredentialsException("User not found"));

            if (requestingUser.getAccountType() != User.AccountType.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
            }

            List<User> users = userRepository.findAllNonAdminUsers();

            List<UserResponseDto> userDtos = users.stream()
                .map(user -> {
                    Set<UserResponseDto.EnrolledOrganizationDto> enrolledOrgDtos = user.getEnrolledOrganizations().stream()
                        .map(org -> UserResponseDto.EnrolledOrganizationDto.builder()
                            .id(org.getId())
                            .organizationName(org.getOrganizationName())
                            .build())
                        .collect(Collectors.toSet());
                    
                    return UserResponseDto.builder()
                        .id(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .accountType(user.getAccountType())
                        .ministry(user.getMinistry())
                        .apostolate(user.getApostolate())
                        .isDeactivated(user.isDeactivated())
                        .enrolledOrganizations(enrolledOrgDtos)
                        .build();
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(userDtos);
        } catch (Exception e) {
            log.error("Error fetching users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching users");
        }
    }

 @PostMapping("/{userId}/enroll")
    @Transactional
    public ResponseEntity<?> enrollUser(
        @PathVariable Long userId,
        @RequestBody EnrollRequest enrollRequest,
        @RequestHeader("Authorization") String token) {
        
        try {
            String email = jwtTokenProvider.extractUsername(token.substring(7));
            User adminUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
            
            if (adminUser.getAccountType() != User.AccountType.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
            }
            
            if (enrollRequest.getOrganizationId() == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Organization ID is required"));
            }

            Organizer organizer = organizerRepository.findById(enrollRequest.getOrganizationId())
                .orElseThrow(() -> new BadCredentialsException("Organization not found"));

            User userToEnroll = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
            
            if (userToEnroll.isDeleted()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Account not found"));
            }
            
            // Direct access to mutable collections
            Set<Organizer> userOrgs = userToEnroll.getEnrolledOrganizations();
            Set<User> orgUsers = organizer.getEnrolledUsers();
            
            // Check if already enrolled
            boolean alreadyEnrolled = userOrgs.stream()
                .anyMatch(org -> org.getId().equals(organizer.getId()));
            
            if (alreadyEnrolled) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "User already enrolled in this organization"));
            }
            
            // Add to collections
            userOrgs.add(organizer);
            orgUsers.add(userToEnroll);
            
            // Save both entities
            userRepository.save(userToEnroll);
            organizerRepository.save(organizer);
            
            log.info("Successfully enrolled user {} in organization {}", userId, enrollRequest.getOrganizationId());
            
            return ResponseEntity.ok(Map.of(
                "message", "User enrolled successfully",
                "userId", userToEnroll.getId(),
                "organizationId", organizer.getId()
            ));
            
        } catch (Exception e) {
            log.error("Error enrolling user {} in organization {}: {}", 
                     userId, enrollRequest.getOrganizationId(), e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Error enrolling user: " + e.getMessage()));
        }
    }

@PostMapping("/{userId}/unenroll")
    @Transactional
    public ResponseEntity<?> unenrollUser(
        @PathVariable Long userId,
        @RequestBody EnrollRequest enrollRequest,
        @RequestHeader("Authorization") String token) {
        
        try {
            String email = jwtTokenProvider.extractUsername(token.substring(7));
            User adminUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
            
            if (adminUser.getAccountType() != User.AccountType.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
            }
            
            if (enrollRequest.getOrganizationId() == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Organization ID is required"));
            }

            Organizer organizer = organizerRepository.findById(enrollRequest.getOrganizationId())
                .orElseThrow(() -> new BadCredentialsException("Organization not found"));

            User userToUnenroll = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
            
            // Direct access to mutable collections
            Set<Organizer> userOrgs = userToUnenroll.getEnrolledOrganizations();
            Set<User> orgUsers = organizer.getEnrolledUsers();
            
            // Check if user is actually enrolled
            boolean isEnrolled = userOrgs.stream()
                .anyMatch(org -> org.getId().equals(organizer.getId()));
                
            if (!isEnrolled) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "User is not enrolled in this organization"));
            }
            
            // Remove from collections
            userOrgs.removeIf(org -> org.getId().equals(organizer.getId()));
            orgUsers.removeIf(user -> user.getId().equals(userId));
            
            // Save both entities
            userRepository.save(userToUnenroll);
            organizerRepository.save(organizer);
            
            log.info("Successfully unenrolled user {} from organization {}", userId, enrollRequest.getOrganizationId());
            
            return ResponseEntity.ok(Map.of(
                "message", "User unenrolled successfully",
                "userId", userToUnenroll.getId(),
                "organizationId", organizer.getId()
            ));
            
        } catch (Exception e) {
            log.error("Error unenrolling user {} from organization {}: {}", 
                     userId, enrollRequest.getOrganizationId(), e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Error unenrolling user: " + e.getMessage()));
        }
    }

    @PutMapping("/me")
    @Transactional
    public ResponseEntity<?> updateCurrentUser(
            @RequestHeader("Authorization") String token,
            @RequestBody User updatedUser) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }
            
            String email = jwtTokenProvider.extractUsername(token.substring(7));
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BadCredentialsException("User not found"));
            
            if (user.isDeleted()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found");
            }
            
            if (user.isDeactivated()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Account is deactivated");
            }
            
            user.setFirstName(updatedUser.getFirstName());
            user.setLastName(updatedUser.getLastName());
            user.setBirthday(updatedUser.getBirthday());
            user.setGender(updatedUser.getGender());
            user.setAddress(updatedUser.getAddress());
            user.setSpouseName(updatedUser.getSpouseName());
            user.setMinistry(updatedUser.getMinistry());
            user.setApostolate(updatedUser.getApostolate());
            user.setUpdatedAt(LocalDateTime.now());
            
            return ResponseEntity.ok(userRepository.save(user));
        } catch (Exception e) {
            log.error("Error updating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating user");
        }
    }

    @PutMapping("/me/password")
    @Transactional
    public ResponseEntity<?> changePassword(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody ChangePasswordDto changePasswordDto) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }
            
            String email = jwtTokenProvider.extractUsername(token.substring(7));
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BadCredentialsException("User not found"));
            
            if (user.isDeleted()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found");
            }
            
            if (user.isDeactivated()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Account is deactivated");
            }
            
            if (!passwordEncoder.matches(changePasswordDto.getCurrentPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Current password is incorrect");
            }
            
            user.setPassword(passwordEncoder.encode(changePasswordDto.getNewPassword()));
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            
            return ResponseEntity.ok("Password changed successfully");
        } catch (Exception e) {
            log.error("Error changing password", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error changing password");
        }
    }

@PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationDto registrationDto) {
        try {
            log.info("Registration attempt for email: {}", registrationDto.getEmail());
            
            // Validate that system owner registration is not allowed through this endpoint
            if (registrationDto.getAccountType() == User.AccountType.SYSTEM_OWNER) {
                log.warn("Attempted system owner registration through public endpoint");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                            "message", "System Owner accounts can only be created by existing System Owners",
                            "error", "FORBIDDEN_ACCOUNT_TYPE"
                        ));
            }
            
            UserResponseDto registeredUser = authService.registerUser(registrationDto);
            log.info("User registered successfully: {}", registeredUser.getEmail());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "User registered successfully",
                "user", registeredUser
            ));
            
        } catch (DuplicateEmailException e) {
            log.error("Registration failed - email already exists: {}", registrationDto.getEmail());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                        "message", "An account with this email already exists",
                        "email", registrationDto.getEmail(),
                        "error", "DUPLICATE_EMAIL"
                    ));
        } catch (Exception e) {
            log.error("Error registering user: {}", registrationDto.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "message", "Registration failed. Please try again.",
                        "error", "INTERNAL_SERVER_ERROR"
                    ));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto loginRequest) {
        try {
            log.info("Login attempt for email: {}", loginRequest.getEmail());
            LoginResponseDto response = authService.login(loginRequest);
            
            if (response.isDeactivated()) {
                log.info("Deactivated account login attempt: {}", loginRequest.getEmail());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                            "message", "Account is deactivated",
                            "email", loginRequest.getEmail(),
                            "isDeactivated", true
                        ));
            }

            log.info("Successful login for email: {}", loginRequest.getEmail());
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            log.warn("Invalid credentials for email: {}", loginRequest.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                        "message", "Invalid credentials",
                        "email", loginRequest.getEmail(),
                        "isDeactivated", false
                    ));
        } catch (Exception e) {
            log.error("Login error for email: {}", loginRequest.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "message", "Login failed",
                        "email", loginRequest.getEmail(),
                        "isDeactivated", false
                    ));
        }
    }

    @DeleteMapping("/me")
    @Transactional
    public ResponseEntity<?> deleteAccount(
            @RequestHeader("Authorization") String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }
            
            String email = jwtTokenProvider.extractUsername(token.substring(7));
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BadCredentialsException("User not found"));
            
            user.setDeleted(true);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            
            return ResponseEntity.ok("Account deleted successfully");
        } catch (Exception e) {
            log.error("Error deleting account", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting account");
        }
    }

    @PutMapping("/me/deactivate")
    @Transactional
    public ResponseEntity<?> deactivateAccount(
            @RequestHeader("Authorization") String token,
            @RequestBody(required = false) Map<String, String> request) {
        
        try {
            String email = jwtTokenProvider.extractUsername(token.substring(7));
            userRepository.updateDeactivationStatus(email, true);
            
            return ResponseEntity.ok(Map.of(
                "message", "Account deactivated successfully",
                "email", email,
                "isDeactivated", true
            ));
        } catch (Exception e) {
            log.error("Error deactivating account", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error deactivating account");
        }
    }

    @GetMapping("/check-active")
    public ResponseEntity<?> checkAccountActive(
        @RequestParam String email,
        @RequestHeader("Authorization") String token) {
        
        try {
            if (!jwtTokenProvider.validateToken(token.substring(7))) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BadCredentialsException("User not found"));

            return ResponseEntity.ok(Map.of(
                "email", email,
                "isDeactivated", user.isDeactivated()
            ));
        } catch (Exception e) {
            log.error("Error checking account status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error checking account status");
        }
    }

    @PostMapping("/system-owner/register")
    @Transactional
    public ResponseEntity<?> registerSystemOwner(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UserRegistrationDto registrationDto) {
        
        try {
            // Only existing SYSTEM_OWNER can create new SYSTEM_OWNER
            String email = jwtTokenProvider.extractUsername(token.substring(7));
            User requestingUser = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BadCredentialsException("User not found"));
            
            if (requestingUser.getAccountType() != User.AccountType.SYSTEM_OWNER) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Only system owners can create new system owners");
            }
            
            User systemOwner = systemOwnerService.createSystemOwner(registrationDto);
            return ResponseEntity.ok(systemOwner);
        } catch (DuplicateEmailException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Email already exists"));
        } catch (Exception e) {
            log.error("Error creating system owner", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating system owner");
        }
    }

    @GetMapping("/system-owners")
    public ResponseEntity<?> getAllSystemOwners(@RequestHeader("Authorization") String token) {
        try {
            // Verify requesting user is SYSTEM_OWNER
            String email = jwtTokenProvider.extractUsername(token.substring(7));
            User requestingUser = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BadCredentialsException("User not found"));
            
            if (requestingUser.getAccountType() != User.AccountType.SYSTEM_OWNER) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Access denied");
            }
            
            List<User> systemOwners = userRepository.findByAccountType(User.AccountType.SYSTEM_OWNER);
            return ResponseEntity.ok(systemOwners);
        } catch (Exception e) {
            log.error("Error fetching system owners", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching system owners");
        }
    }

    @PutMapping("/reactivate")
    @Transactional
    public ResponseEntity<?> reactivateAccount(@RequestBody ReactivationRequest request) {
        try {
            String email = request.getEmail();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BadCredentialsException("User not found"));

            // Verify password
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Invalid credentials");
            }

            if (!user.isDeactivated()) {
                return ResponseEntity.ok().body("Account is already active");
            }

            // Update database
            user.setDeactivated(false);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            // Verify update
            User updatedUser = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found after update"));
            
            if (updatedUser.isDeactivated()) {
                throw new RuntimeException("Deactivation status not updated in database");
            }

            // Generate new token
            String token = jwtTokenProvider.generateToken(
                user.getEmail(),
                user.getAccountType().name(),
                user.getId()
            );

            return ResponseEntity.ok(Map.of(
                "message", "Account reactivated successfully",
                "token", token,
                "user", user
            ));
        } catch (Exception e) {
            log.error("Reactivation failed for email: {}", request.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Reactivation failed: " + e.getMessage());
        }
    }
}