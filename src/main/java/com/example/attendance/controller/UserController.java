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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final UserEventService userEventService; // Add this missing dependency

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
            
            // Add enrolled organization information
            if (user.getEnrolledOrganization() != null) {
                Map<String, Object> enrolledOrgMap = new HashMap<>();
                enrolledOrgMap.put("id", user.getEnrolledOrganization().getId());
                enrolledOrgMap.put("organizationName", user.getEnrolledOrganization().getOrganizationName());
                response.put("enrolledOrganization", enrolledOrgMap);
            } else {
                response.put("enrolledOrganization", null);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching user data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching user data");
        }
    }

    // REMOVED DUPLICATE METHOD - keeping only one version
    @GetMapping("/me/attended-events")
    public ResponseEntity<?> getMyAttendedEvents(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtTokenProvider.extractUsername(token.substring(7));
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BadCredentialsException("User not found"));
            
            List<UserEventAttendance> attendances = userEventService.getUserAttendedEvents(user.getId());
            
            // Convert to DTO if you have a conversion method
            List<UserAttendanceDto> attendanceDtos = attendances.stream()
                    .map(UserAttendanceDto::fromEntity) // Make sure this method exists in UserAttendanceDto
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
                    UserResponseDto.EnrolledOrganizationDto enrolledOrgDto = null;
                    if (user.getEnrolledOrganization() != null) {
                        enrolledOrgDto = UserResponseDto.EnrolledOrganizationDto.builder()
                            .id(user.getEnrolledOrganization().getId())
                            .organizationName(user.getEnrolledOrganization().getOrganizationName())
                            .build();
                    }
                    
                    return UserResponseDto.builder()
                        .id(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .accountType(user.getAccountType())
                        .ministry(user.getMinistry())
                        .apostolate(user.getApostolate())
                        .isDeactivated(user.isDeactivated())
                        .enrolledOrganization(enrolledOrgDto)
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
    public ResponseEntity<?> enrollUser(
        @PathVariable Long userId,
        @RequestBody EnrollRequest enrollRequest,
        @RequestHeader("Authorization") String token) {
        
        try {
            // Validate token and check if admin
            String email = jwtTokenProvider.extractUsername(token.substring(7));
            User adminUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
            
            if (adminUser.getAccountType() != User.AccountType.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
            }
            
            // Validate request body
            if (enrollRequest.getOrganizationId() == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Organization ID is required"));
            }

            // Get the organizer (organization)
            Optional<Organizer> organizer = organizerRepository.findById(enrollRequest.getOrganizationId());
            if (organizer.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Organization not found"));
            }

            // Get user to enroll
            User userToEnroll = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
            
            if (userToEnroll.isDeleted()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Account not found"));
            }
            
            // Check enrollment status
            if (userToEnroll.getEnrolledOrganization() != null) {
                if (userToEnroll.getEnrolledOrganization().getId().equals(enrollRequest.getOrganizationId())) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "User already enrolled in this organization"));
                }
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "User already enrolled in another organization"));
            }
            
            // Enroll the user
            userToEnroll.setEnrolledOrganization(organizer.get());
            userRepository.save(userToEnroll);
            
            return ResponseEntity.ok(Map.of(
                "message", "User enrolled successfully",
                "userId", userToEnroll.getId(),
                "organizationId", organizer.get().getId()
            ));
        } catch (Exception e) {
            log.error("Error enrolling user", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Error enrolling user: " + e.getMessage()));
        }
    }

    @PostMapping("/{userId}/unenroll")
    public ResponseEntity<?> unenrollUser(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String token) {
        try {
            // Validate token and check if admin
            String email = jwtTokenProvider.extractUsername(token.substring(7));
            User adminUser = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BadCredentialsException("User not found"));
            
            if (adminUser.getAccountType() != User.AccountType.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
            }
            
            // Get the organizer (organization) that this admin belongs to
            Organizer adminOrganizer = organizerService.findByUser(adminUser)
                    .orElseThrow(() -> new BadCredentialsException("Admin is not an organizer"));
            
            // Get user to unenroll
            User userToUnenroll = userRepository.findById(userId)
                    .orElseThrow(() -> new BadCredentialsException("User not found"));
            
            // Check if user is actually enrolled in this organization
            if (userToUnenroll.getEnrolledOrganization() == null || 
                !userToUnenroll.getEnrolledOrganization().getId().equals(adminOrganizer.getId())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("User is not enrolled in your organization");
            }
            
            // Unenroll the user
            userToUnenroll.setEnrolledOrganization(null);
            userRepository.save(userToUnenroll);
            
            return ResponseEntity.ok(Map.of(
                "message", "User unenrolled successfully",
                "userId", userToUnenroll.getId()
            ));
        } catch (Exception e) {
            log.error("Error unenrolling user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error unenrolling user: " + e.getMessage());
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
            User registeredUser = authService.registerUser(registrationDto);
            return ResponseEntity.ok(registeredUser);
        } catch (DuplicateEmailException e) {
            log.error("Registration failed - email already exists: {}", registrationDto.getEmail());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                        "message", "Email already exists",
                        "email", registrationDto.getEmail()
                    ));
        } catch (Exception e) {
            log.error("Error registering user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "message", "Error registering user",
                        "error", e.getMessage()
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
        
        String email = jwtTokenProvider.extractUsername(token.substring(7));
        userRepository.updateDeactivationStatus(email, true);
        
        return ResponseEntity.ok(Map.of(
            "message", "Account deactivated successfully",
            "email", email,
            "isDeactivated", true
        ));
    }

    @GetMapping("/check-active")
    public ResponseEntity<?> checkAccountActive(
        @RequestParam String email,
        @RequestHeader("Authorization") String token) {
        
        if (!jwtTokenProvider.validateToken(token.substring(7))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        return ResponseEntity.ok(Map.of(
            "email", email,
            "isDeactivated", user.isDeactivated()
        ));
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