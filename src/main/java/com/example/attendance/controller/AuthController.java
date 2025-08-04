package com.example.attendance.controller;

import com.example.attendance.dto.LoginRequestDto;
import com.example.attendance.dto.LoginResponseDto;
import com.example.attendance.dto.UserRegistrationDto;
import com.example.attendance.dto.*;
import com.example.attendance.entity.User;
import com.example.attendance.entity.Organizer;
import com.example.attendance.exception.DuplicateEmailException;
import com.example.attendance.exception.OrganizerCreationException;
import com.example.attendance.service.AuthService;
import com.example.attendance.service.OrganizerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OrganizerService organizerService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationDto registrationDto) {
        try {
            log.info("Registration attempt for email: {}", registrationDto.getEmail());
            
            User user = authService.registerUser(registrationDto);
            
            if (user.getAccountType() == User.AccountType.ADMIN) {
                try {
                    Organizer organizer = organizerService.createOrganizerForAdmin(user);
                    log.info("Created organizer profile for admin user: {}", user.getEmail());
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("user", user);
                    response.put("organizerId", organizer.getId());
                    response.put("message", "Admin account and organizer profile created successfully");
                    
                    return ResponseEntity.status(HttpStatus.CREATED).body(response);
                    
                } catch (OrganizerCreationException e) {
                    log.error("Organizer creation failed for admin user: {}", e.getMessage(), e);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("user", user);
                    response.put("warning", "Admin account created but organizer profile creation failed");
                    response.put("error", e.getMessage());
                    
                    return ResponseEntity.status(HttpStatus.CREATED).body(response);
                }
            }
            
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
            
        } catch (DuplicateEmailException e) {
            log.warn("Registration failed - duplicate email: {}", registrationDto.getEmail());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email already exists", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Registration error for {}: {}", registrationDto.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Registration failed", "message", "Please try again later"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto loginRequest) {
        try {
            log.info("Login attempt for email: {}", loginRequest.getEmail());
            LoginResponseDto response = authService.login(loginRequest);
            
            if (response.getAccountType() == User.AccountType.ADMIN) {
                try {
                    organizerService.findByUserEmail(response.getEmail())
                        .orElseThrow(() -> new IllegalStateException("Organizer profile not found"));
                } catch (Exception e) {
                    log.warn("Admin login but organizer profile missing: {}", response.getEmail());
                    response.setWarning("Organizer profile not found. Some features may be limited.");
                }
            }
            
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for email: {}", loginRequest.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials", "message", "Invalid email or password"));
        } catch (Exception e) {
            log.error("Login error for {}: {}", loginRequest.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Login failed", "message", "Please try again later"));
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyToken(@RequestHeader("Authorization") String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                throw new BadCredentialsException("Invalid token format");
            }
            
            String jwtToken = token.substring(7);
            LoginResponseDto response = authService.verifyToken(jwtToken);
            
            if (response.getAccountType() == User.AccountType.ADMIN) {
                try {
                    organizerService.findByUserEmail(response.getEmail())
                        .orElseThrow(() -> new IllegalStateException("Organizer profile not found"));
                } catch (Exception e) {
                    log.warn("Admin token verification but organizer profile missing: {}", response.getEmail());
                    response.setWarning("Organizer profile not found. Some features may be limited.");
                }
            }
            
            return ResponseEntity.ok(response);
            
        } catch (BadCredentialsException e) {
            log.warn("Token verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid token", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Token verification error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Token verification failed", "message", "Please try again"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        log.info("Logout request received");
        try {
            return ResponseEntity.ok("Logout successful");
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Logout failed");
        }
    }
}