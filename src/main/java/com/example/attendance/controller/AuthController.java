package com.example.attendance.controller;

import com.example.attendance.dto.*;
import com.example.attendance.exception.DuplicateEmailException;
import com.example.attendance.service.AuthService;
import com.example.attendance.entity.User;
import com.example.attendance.service.SystemOwnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SystemOwnerService systemOwnerService;

    // Regular user registration
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationDto registrationDto) {
        try {
            UserResponseDto registeredUser = authService.registerUser(registrationDto);
            log.info("User registered successfully: {}", registeredUser.getEmail());
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

    // Admin registration (protected endpoint)
    @PostMapping("/admin/register")
    @PreAuthorize("hasRole('SYSTEM_OWNER')")
    public ResponseEntity<?> registerAdmin(@Valid @RequestBody UserRegistrationDto registrationDto) {
        try {
            // Force account type to ADMIN
            registrationDto.setAccountType(User.AccountType.ADMIN);
            UserResponseDto registeredAdmin = authService.registerUser(registrationDto);
            log.info("Admin registered successfully: {}", registeredAdmin.getEmail());
            return ResponseEntity.ok(registeredAdmin);
        } catch (DuplicateEmailException e) {
            log.error("Admin registration failed - email already exists: {}", registrationDto.getEmail());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                        "message", "Email already exists",
                        "email", registrationDto.getEmail()
                    ));
        } catch (Exception e) {
            log.error("Error registering admin", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "message", "Error registering admin",
                        "error", e.getMessage()
                    ));
        }
    }

    // System owner registration (protected endpoint) - Manual conversion
    @PostMapping("/system-owner/register")
    @PreAuthorize("hasRole('SYSTEM_OWNER')")
    public ResponseEntity<?> registerSystemOwner(@Valid @RequestBody UserRegistrationDto registrationDto) {
        try {
            // Assuming systemOwnerService.createSystemOwner returns User entity
            Object result = systemOwnerService.createSystemOwner(registrationDto);
            
            UserResponseDto systemOwner;
            if (result instanceof User) {
                User userEntity = (User) result;
                // Manual conversion from User to UserResponseDto
                systemOwner = UserResponseDto.builder()
                    .id(userEntity.getId())
                    .firstName(userEntity.getFirstName())
                    .lastName(userEntity.getLastName())
                    .email(userEntity.getEmail())
                    .birthday(userEntity.getBirthday())
                    .gender(userEntity.getGender())
                    .accountType(userEntity.getAccountType())
                    .address(userEntity.getAddress())
                    .spouseName(userEntity.getSpouseName())
                    .ministry(userEntity.getMinistry())
                    .apostolate(userEntity.getApostolate())
                    .isDeactivated(userEntity.isDeactivated())
                    .isDeleted(userEntity.isDeleted())
                    .createdAt(userEntity.getCreatedAt())
                    .updatedAt(userEntity.getUpdatedAt())
                    .build();
            } else if (result instanceof UserResponseDto) {
                systemOwner = (UserResponseDto) result;
            } else {
                throw new IllegalStateException("Unexpected return type from createSystemOwner");
            }
            
            log.info("System owner registered successfully: {}", systemOwner.getEmail());
            return ResponseEntity.ok(systemOwner);
        } catch (DuplicateEmailException e) {
            log.error("System owner registration failed - email already exists: {}", registrationDto.getEmail());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                        "message", "Email already exists",
                        "email", registrationDto.getEmail()
                    ));
        } catch (Exception e) {
            log.error("Error registering system owner", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "message", "Error registering system owner",
                        "error", e.getMessage()
                    ));
        }
    }

    // Login endpoint (common for all users)
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
        } catch (Exception e) {
            log.error("Login error for email: {}", loginRequest.getEmail(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                        "message", "Invalid credentials",
                        "email", loginRequest.getEmail(),
                        "isDeactivated", false
                    ));
        }
    }

    // Get current user
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String token) {
        try {
            LoginResponseDto userResponse = authService.verifyToken(token.substring(7));
            return ResponseEntity.ok(userResponse);
        } catch (Exception e) {
            log.error("Error fetching user data", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid or expired token");
        }
    }

    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody ChangePasswordDto changePasswordDto) {
        try {
            LoginResponseDto userResult = authService.verifyToken(token.substring(7));
            String userEmail = userResult.getEmail();
            
            authService.changePassword(userEmail, 
                                     changePasswordDto.getCurrentPassword(), 
                                     changePasswordDto.getNewPassword());
            return ResponseEntity.ok("Password changed successfully");
        } catch (Exception e) {
            log.error("Error changing password", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    @PutMapping("/reactivate")
    public ResponseEntity<?> reactivateAccount(@RequestBody ReactivationRequest request) {
        try {
            LoginResponseDto response = authService.reactivateAccount(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Reactivation failed for email: {}", request.getEmail(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Reactivation failed: " + e.getMessage());
        }
    }
}