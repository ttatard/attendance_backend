package com.example.attendance.dto;

import com.example.attendance.entity.Event;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalDate;
import com.example.attendance.entity.UserEventAttendance;

@Data
public class UserAttendanceDto {
    private Long attendanceId;
    private Long eventId;
    private String eventName;
    private LocalDate eventDate;
    private String eventPlace;
    private LocalDateTime attendanceDate;
    
    public static UserAttendanceDto fromEntity(UserEventAttendance attendance) {
        UserAttendanceDto dto = new UserAttendanceDto();
        dto.setAttendanceId(attendance.getId());
        Event event = attendance.getEvent();
        dto.setEventId(event.getId());
        dto.setEventName(event.getName());
        dto.setEventDate(event.getDate());
        dto.setEventPlace(event.getPlace());
        
        // Fixed: Convert LocalDate to LocalDateTime if needed
        // If the attendance.getAttendanceDate() returns LocalDateTime, use it directly
        // If it returns LocalDate, convert it to LocalDateTime
        if (attendance.getAttendanceDate() != null) {
            // Check if getAttendanceDate() returns LocalDateTime or LocalDate
            Object attendanceDate = attendance.getAttendanceDate();
            if (attendanceDate instanceof LocalDateTime) {
                dto.setAttendanceDate((LocalDateTime) attendanceDate);
            } else if (attendanceDate instanceof LocalDate) {
                dto.setAttendanceDate(((LocalDate) attendanceDate).atStartOfDay());
            }
        } else if (event.getDate() != null) {
            // If attendance date is null but event date exists, use event date at start of day
            dto.setAttendanceDate(event.getDate().atStartOfDay());
        }
        
        return dto;
    }
}