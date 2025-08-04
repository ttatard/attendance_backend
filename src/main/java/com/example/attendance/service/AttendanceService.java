package com.example.attendance.service;

import com.example.attendance.entity.Attendance;
import com.example.attendance.repository.AttendanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AttendanceService {
    private static final Logger logger = LoggerFactory.getLogger(AttendanceService.class);
    private final AttendanceRepository attendanceRepository;

    public AttendanceService(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

   public Attendance recordAttendance(Long eventId, String attendeeName, String code) {
    // Validate inputs
    if (eventId == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event ID is required");
    }
    
    if (attendeeName == null || attendeeName.trim().isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attendee name is required");
    }
    
    if (code == null || code.trim().isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QR code is required");
    }

    // Check if this attendee has already checked in with this QR code
    boolean alreadyCheckedIn = attendanceRepository.existsByEventIdAndCodeAndAttendeeName(eventId, code, attendeeName);
    if (alreadyCheckedIn) {
        logger.warn("Duplicate attendance attempt - Event: {}, Code: {}, Attendee: {}", eventId, code, attendeeName);
        throw new ResponseStatusException(HttpStatus.CONFLICT, "This attendee has already checked in with this QR code");
    }
    
    // Create and save new attendance record
    Attendance attendance = new Attendance();
    attendance.setEventId(eventId);
    attendance.setAttendeeName(attendeeName);
    attendance.setCode(code);

    logger.info("Recording attendance for event {} - Attendee: {}", eventId, attendeeName);
    
    return attendanceRepository.save(attendance);
}
}