package com.example.attendance.dto;

import com.example.attendance.entity.User;
import lombok.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private LocalDate birthday;
    private User.Gender gender;
    private User.AccountType accountType;
    private String address;
    private String spouseName;
    private User.Ministry ministry;
    private User.Apostolate apostolate;
    private boolean isDeactivated;
    private boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Singular
    private Set<EnrolledOrganizationDto> enrolledOrganizations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnrolledOrganizationDto {
        private Long id;
        private String organizationName;
    }
}