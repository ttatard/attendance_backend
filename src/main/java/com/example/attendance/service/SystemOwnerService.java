package com.example.attendance.service;

import com.example.attendance.dto.UserRegistrationDto;
import com.example.attendance.entity.User;
import com.example.attendance.exception.DuplicateEmailException;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.security.JwtTokenProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SystemOwnerService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @PostConstruct
    public void initSystemOwner() {
        if (!userRepository.existsByEmail("system@owner.com")) {
            User systemOwner = User.builder()
                .firstName("System")
                .lastName("Owner")
                .email("system@owner.com")
                .password(passwordEncoder.encode("ChangeThisPassword123!"))
                .birthday(LocalDate.now())
                .gender(User.Gender.UNSPECIFIED)
                .accountType(User.AccountType.SYSTEM_OWNER)
                .build();
            
            userRepository.save(systemOwner);
        }
    }

    public User createSystemOwner(UserRegistrationDto registrationDto) {
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new DuplicateEmailException("Email already exists");
        }

        User systemOwner = User.builder()
            .firstName(registrationDto.getFirstName())
            .lastName(registrationDto.getLastName())
            .email(registrationDto.getEmail())
            .password(passwordEncoder.encode(registrationDto.getPassword()))
            .birthday(registrationDto.getBirthday())
            .gender(registrationDto.getGender())
            .accountType(User.AccountType.SYSTEM_OWNER)
            .build();

        return userRepository.save(systemOwner);
    }
}