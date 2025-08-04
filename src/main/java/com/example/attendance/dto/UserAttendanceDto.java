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
        dto.setAttendanceDate(attendance.getAttendanceDate());
        return dto;
    }
}