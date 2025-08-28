package com.example.attendance.dto;

import jakarta.validation.constraints.NotNull;

public class CodeVerificationRequest {
    
    @NotNull(message = "Event ID is required")
    private Long eventId;
    
    @NotNull(message = "Code is required")
    private String code;

    // Default constructor
    public CodeVerificationRequest() {}

    // Constructor with parameters
    public CodeVerificationRequest(Long eventId, String code) {
        this.eventId = eventId;
        this.code = code;
    }

    // Getters and setters
    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "CodeVerificationRequest{" +
                "eventId=" + eventId +
                ", code='" + code + '\'' +
                '}';
    }
}