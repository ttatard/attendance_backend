package com.example.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizerDto {
    private Long id;
    private String email;
    private String organizationName;
    private String contactNumber;
    private String description;
    private String website;
    private String address;
    private Boolean isActive;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}