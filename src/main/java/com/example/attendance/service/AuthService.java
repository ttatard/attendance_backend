package com.example.attendance.service;

import com.example.attendance.dto.*;
import com.example.attendance.entity.User;
import com.example.attendance.exception.DuplicateEmailException;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final OrganizerService organizerService;

    @Transactional
    public UserResponseDto registerUser(UserRegistrationDto registrationDto) {
        String email = registrationDto.getEmail().trim().toLowerCase();
        log.info("Starting registration for email: {}", email);

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("Email already exists");
        }

        User user = User.builder()
                .firstName(registrationDto.getFirstName().trim())
                .lastName(registrationDto.getLastName().trim())
                .email(email)
                .password(passwordEncoder.encode(registrationDto.getPassword()))
                .birthday(registrationDto.getBirthday())
                .gender(registrationDto.getGender())
                .accountType(registrationDto.getAccountType())
                .isDeactivated(false)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);

        if (savedUser.getAccountType() == User.AccountType.ADMIN) {
            organizerService.createOrganizerForAdmin(savedUser);
        }

        return convertToUserResponseDto(savedUser);
    }

    public LoginResponseDto login(LoginRequestDto loginRequest) {
        String email = loginRequest.getEmail().trim().toLowerCase();
        log.info("Login attempt for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (user.isDeactivated()) {
            log.warn("Login attempt for deactivated account: {}", email);
            return LoginResponseDto.builder()
                    .email(user.getEmail())
                    .isDeactivated(true)
                    .message("Account is deactivated. Please reactivate.")
                    .build();
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String token = jwtTokenProvider.generateToken(
                user.getEmail(),
                user.getAccountType().name(),
                user.getId()
        );

        return LoginResponseDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .birthday(user.getBirthday())
                .gender(user.getGender())
                .accountType(user.getAccountType())
                .token(token)
                .isDeactivated(false)
                .build();
    }

    @Transactional
    public LoginResponseDto reactivateAccount(ReactivationRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Attempting to reactivate account: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!user.isDeactivated()) {
            return buildLoginResponse(user);
        }

        user.setDeactivated(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return buildLoginResponse(user);
    }

    public LoginResponseDto verifyToken(String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            throw new RuntimeException("Invalid token");
        }

        String email = jwtTokenProvider.extractUsername(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return buildLoginResponse(user);
    }

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        log.info("Password change requested for: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private LoginResponseDto buildLoginResponse(User user) {
        String token = jwtTokenProvider.generateToken(
                user.getEmail(),
                user.getAccountType().name(),
                user.getId()
        );

        return LoginResponseDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .birthday(user.getBirthday())
                .gender(user.getGender())
                .accountType(user.getAccountType())
                .token(token)
                .isDeactivated(false)
                .build();
    }

    // Manual conversion method to replace UserMapper
    private UserResponseDto convertToUserResponseDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .birthday(user.getBirthday())
                .gender(user.getGender())
                .accountType(user.getAccountType())
                .address(user.getAddress())
                .spouseName(user.getSpouseName())
                .ministry(user.getMinistry())
                .apostolate(user.getApostolate())
                .isDeactivated(user.isDeactivated())
                .isDeleted(user.isDeleted())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}