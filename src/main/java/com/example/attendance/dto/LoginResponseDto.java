package com.example.attendance.dto;

import com.example.attendance.entity.User;
import lombok.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private LocalDate birthday;
    private User.Gender gender;
    private User.AccountType accountType;
    private String token;
    private String warning;
    private boolean isDeactivated;
    private String message;
}