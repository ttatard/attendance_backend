package com.example.attendance.controller;

import com.example.attendance.dto.AttendanceRequestDTO;
import com.example.attendance.entity.Attendance;
import com.example.attendance.service.AttendanceService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.extern.slf4j.Slf4j;

@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {
    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping
    public ResponseEntity<?> recordAttendance(@RequestBody AttendanceRequestDTO request) {
        try {
            Attendance attendance = attendanceService.recordAttendance(
                request.getEventId(),
                request.getAttendeeName(),
                request.getCode()
            );
            return ResponseEntity.ok(attendance);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error recording attendance: " + e.getMessage());
        }
    }
}