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

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final OrganizerService organizerService; 

  @Transactional
public User registerUser(UserRegistrationDto registrationDto) {
    String email = registrationDto.getEmail().trim().toLowerCase();
    log.info("Starting registration for email: {}", email);

    if (userRepository.existsByEmail(email)) {
        throw new DuplicateEmailException("Email already exists");
    }

    User user = User.builder()
            .firstName(registrationDto.getFirstName())
            .lastName(registrationDto.getLastName())
            .email(email)
            .password(passwordEncoder.encode(registrationDto.getPassword()))
            .birthday(registrationDto.getBirthday())
            .gender(registrationDto.getGender())
            .accountType(registrationDto.getAccountType())
            .build();

    User savedUser = userRepository.save(user);

    // Create organizer account only if ADMIN
    if (savedUser.getAccountType() == User.AccountType.ADMIN) {
        organizerService.createOrganizerForAdmin(savedUser);
    }

    return savedUser;
}


   public LoginResponseDto login(LoginRequestDto loginRequest) {
    User user = userRepository.findByEmail(loginRequest.getEmail())
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

    if (user.isDeactivated()) {
        return LoginResponseDto.builder()
                .email(user.getEmail())
                .isDeactivated(true)  // Make sure this is set to true
                .message("Account is deactivated. Please reactivate.")
                .build();
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

    public LoginResponseDto verifyToken(String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            throw new RuntimeException("Invalid token");
        }

        String email = jwtTokenProvider.extractUsername(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return LoginResponseDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .birthday(user.getBirthday())
                .gender(user.getGender())
                .accountType(user.getAccountType())
                .build();
    }

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}