package com.example.attendance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private LocalDate date;
    
    @Column(nullable = false)
    private LocalTime time;
    
    private String place;
    private String description;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(unique = true)
    private String qrCode;
    
    // Price fields
    @Column(nullable = false)
    @Builder.Default
    private Boolean isFree = true;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal price;
    
    // Event type fields
    @Column(name = "is_online")
    @Builder.Default
    private Boolean isOnline = false;
    
    @Column(name = "meeting_url")
    private String meetingUrl;
    
    @Column(name = "meeting_id")
    private String meetingId;
    
    @Column(name = "meeting_passcode")
    private String meetingPasscode;
    
    @Column(name = "max_capacity")
    private Integer maxCapacity;
    
    @Column(name = "registration_deadline")
    private LocalDate registrationDeadline;
    
    @Column(name = "event_status")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EventStatus status = EventStatus.ACTIVE;
    
    @Column(name = "event_category")
    private String category;
    
    // Recurrence fields
    @Column(name = "recurrence_pattern")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RecurrencePattern recurrencePattern = RecurrencePattern.NONE;
    
    @Column(name = "recurrence_interval")
    @Builder.Default
    private Integer recurrenceInterval = 1;
    
    @Column(name = "recurrence_end_date")
    private LocalDate recurrenceEndDate;
    
    @Column(name = "recurrence_count")
    private Integer recurrenceCount;
    
    @Column(name = "original_event_id")
    private Long originalEventId;
    
    @Column(name = "is_recurring_instance")
    @Builder.Default
    private Boolean isRecurringInstance = false;

    @Column(name = "requires_approval")
@Builder.Default
private Boolean requiresApproval = true;

// Add proper getter and setter
public Boolean getRequiresApproval() {
    return requiresApproval != null ? requiresApproval : true;
}

public void setRequiresApproval(Boolean requiresApproval) {
    this.requiresApproval = requiresApproval;
}

    // Helper methods for price handling
    public void setFree(boolean free) {
        this.isFree = free;
        if (free) {
            this.price = BigDecimal.ZERO;
        }
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
        this.isFree = (price == null || price.compareTo(BigDecimal.ZERO) == 0);
    }
    
    public BigDecimal getPrice() {
        return isFree ? BigDecimal.ZERO : (price != null ? price : BigDecimal.ZERO);
    }
    
    // Helper methods for event type
    public void setOnline(boolean online) {
        this.isOnline = online;
        if (!online) {
            this.meetingUrl = null;
            this.meetingId = null;
            this.meetingPasscode = null;
        }
    }
    
    public boolean isOnlineEvent() {
        return Boolean.TRUE.equals(this.isOnline);
    }
    
    public boolean isFaceToFaceEvent() {
        return !isOnlineEvent();
    }
    
    public String getEventType() {
        return isOnlineEvent() ? "Online" : "Face-to-Face";
    }
    
    public String getEventLocation() {
        if (isOnlineEvent()) {
            return meetingUrl != null ? "Online Meeting" : "Online";
        }
        return place != null ? place : "TBD";
    }
    
    // Check if registration is still open
    public boolean isRegistrationOpen() {
        if (registrationDeadline == null) {
            return status == EventStatus.ACTIVE;
        }
        return status == EventStatus.ACTIVE && 
               LocalDate.now().isBefore(registrationDeadline.plusDays(1));
    }
    
    // Check if event has capacity for more attendees
    public boolean hasCapacityForMoreAttendees(int currentAttendeeCount) {
        return maxCapacity == null || currentAttendeeCount < maxCapacity;
    }
    
    // Enum for event status
    public enum EventStatus {
        DRAFT,      // Event is being created/edited
        ACTIVE,     // Event is published and accepting registrations
        FULL,       // Event has reached maximum capacity
        CANCELLED,  // Event has been cancelled
        COMPLETED   // Event has finished
    }
    
    // Enum for recurrence pattern
    public enum RecurrencePattern {
        NONE,       // No recurrence
        DAILY,      // Every day
        WEEKLY,     // Every week on same day
        MONTHLY,    // Every month on same date
        WEEKLY_X,   // Every month on nth weekday (e.g., 2nd Tuesday)
        YEARLY      // Every year on same date
    }
    
    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", date=" + date +
                ", time=" + time +
                ", place='" + place + '\'' +
                ", isOnline=" + isOnline +
                ", isFree=" + isFree +
                ", price=" + price +
                ", status=" + status +
                ", category='" + category + '\'' +
                ", recurrencePattern=" + recurrencePattern +
                ", recurrenceInterval=" + recurrenceInterval +
                ", recurrenceEndDate=" + recurrenceEndDate +
                ", recurrenceCount=" + recurrenceCount +
                ", originalEventId=" + originalEventId +
                ", isRecurringInstance=" + isRecurringInstance +
                ", userId=" + (user != null ? user.getId() : null) +
                '}';
    }
}