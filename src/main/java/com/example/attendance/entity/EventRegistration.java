package com.example.attendance.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "event_registration", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"event_id", "user_email"}),
           @UniqueConstraint(columnNames = {"unique_code"})
       })
public class EventRegistration {
    
    public enum RegistrationStatus {
        PENDING,
        APPROVED,
        DISAPPROVED
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "event_id", nullable = false)
    private Long eventId;
    
    @Column(name = "user_email", nullable = false)
    private String userEmail;
    
    @Column(name = "user_name", nullable = false)
    private String userName;
    
    @Column(name = "unique_code", nullable = false, unique = true, length = 6)
    private String uniqueCode;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RegistrationStatus status = RegistrationStatus.PENDING;
    
    @CreationTimestamp
    @Column(name = "registration_date")
    private LocalDateTime registrationDate;
    
    @Column(name = "approved_date")
    private LocalDateTime approvedDate;
    
    @Column(name = "approved_by")
    private String approvedBy;
    
    // Default constructor
    public EventRegistration() {}
    
    // Constructor with parameters
    public EventRegistration(Long eventId, String userEmail, String userName) {
        this.eventId = eventId;
        this.userEmail = userEmail;
        this.userName = userName;
        this.status = RegistrationStatus.PENDING;
    }
    
    // Helper method to approve registration
    public void approve(String approvedByEmail) {
        this.status = RegistrationStatus.APPROVED;
        this.approvedDate = LocalDateTime.now();
        this.approvedBy = approvedByEmail;
    }
    
    // Helper method to disapprove registration
    public void disapprove() {
        this.status = RegistrationStatus.DISAPPROVED;
        this.approvedDate = null;
        this.approvedBy = null;
    }
    
    // Helper method to check if registration is approved
    public boolean isApproved() {
        return this.status == RegistrationStatus.APPROVED;
    }
    
    // Helper method to check if registration is pending
    public boolean isPending() {
        return this.status == RegistrationStatus.PENDING;
    }
    
    // Helper method to check if registration is disapproved
    public boolean isDisapproved() {
        return this.status == RegistrationStatus.DISAPPROVED;
    }
}