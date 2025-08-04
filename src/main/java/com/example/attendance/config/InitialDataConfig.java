package com.example.attendance.config;

import com.example.attendance.entity.User;
import com.example.attendance.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

@Configuration
@RequiredArgsConstructor
public class InitialDataConfig {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        // Create default admin user if not exists
        if (userRepository.findByEmail("attendance.admin@gmail.com").isEmpty()) {
            User admin = User.builder()
                    .firstName("Admin")
                    .lastName("User")
                    .email("attendance.admin@gmail.com")
                    .password(passwordEncoder.encode("attendance123"))
                    .birthday(LocalDate.of(1990, 1, 1))
                    .gender(User.Gender.UNSPECIFIED)
                    .accountType(User.AccountType.ADMIN)
                    .build();
            userRepository.save(admin);
        }

        // Create default regular user if not exists
        if (userRepository.findByEmail("attendance.user@gmail.com").isEmpty()) {
            User user = User.builder()
                    .firstName("Regular")
                    .lastName("User")
                    .email("attendance.user@gmail.com")
                    .password(passwordEncoder.encode("attendance123"))
                    .birthday(LocalDate.of(1995, 1, 1))
                    .gender(User.Gender.UNSPECIFIED)
                    .accountType(User.AccountType.USER)
                    .build();
            userRepository.save(user);
        }
    }
}