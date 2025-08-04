package com.example.attendance.dto;

public class AttendanceRequestDTO {
    private Long eventId;
    private String attendeeName;
    private String code;

    // Default constructor
    public AttendanceRequestDTO() {
    }

    // Getters and Setters
    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getAttendeeName() {
        return attendeeName;
    }

    public void setAttendeeName(String attendeeName) {
        this.attendeeName = attendeeName;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}