package com.example.attendance.dto;

import com.example.attendance.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private User.AccountType accountType;
    private User.Ministry ministry;
    private User.Apostolate apostolate;
    private boolean isDeactivated;
    private EnrolledOrganizationDto enrolledOrganization;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnrolledOrganizationDto {
        private Long id;
        private String organizationName;
    }
}