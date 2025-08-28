package com.example.attendance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_event_attendance")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEventAttendance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean attended = false;
    
    @Column(name = "attendance_date")
    @Builder.Default
    private LocalDate attendanceDate = LocalDate.now();
    
    @Column(name = "registration_date")
    @Builder.Default
    private LocalDateTime registrationDate = LocalDateTime.now();
    
    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;
    
    // Additional fields for enhanced tracking
    @Column(name = "qr_code_scanned")
    @Builder.Default
    private Boolean qrCodeScanned = false;
    
    @Column(name = "scan_timestamp")
    private LocalDateTime scanTimestamp;
    
    @Column(name = "notes")
    private String notes;
    
    // Convenience methods
    public void markAsAttended() {
        this.attended = true;
        this.checkInTime = LocalDateTime.now();
        this.attendanceDate = LocalDate.now();
    }
    
    public void markQrCodeScanned() {
        this.qrCodeScanned = true;
        this.scanTimestamp = LocalDateTime.now();
        markAsAttended(); // Scanning QR code implies attendance
    }
    
    // Getters for computed fields
    public boolean isPresent() {
        return Boolean.TRUE.equals(this.attended);
    }
    
    public boolean hasScannedQrCode() {
        return Boolean.TRUE.equals(this.qrCodeScanned);
    }
    
    // Override toString to avoid circular references with lazy loading
    @Override
    public String toString() {
        return "UserEventAttendance{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", eventId=" + (event != null ? event.getId() : null) +
                ", attended=" + attended +
                ", attendanceDate=" + attendanceDate +
                ", registrationDate=" + registrationDate +
                ", checkInTime=" + checkInTime +
                ", qrCodeScanned=" + qrCodeScanned +
                ", scanTimestamp=" + scanTimestamp +
                '}';
    }
}