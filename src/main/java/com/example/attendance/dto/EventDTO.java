package com.example.attendance.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class EventDTO {
    private String name;
    private String organizerName;  // organizer by name
    private LocalDate date;
    private LocalTime time;
    private String place;
}
