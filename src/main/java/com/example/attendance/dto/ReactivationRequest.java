// Rename file to ReactivationRequest.java (must match class name)
package com.example.attendance.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReactivationRequest {
    private String email;
    private String password;
}