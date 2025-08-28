package com.example.attendance.dto;

import com.example.attendance.entity.Event;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class EventRequestDTO {
    @NotBlank(message = "Event name is required")
    private String name;
    
    @NotNull(message = "Event date is required")
    // Removed @Future constraint to allow same-day events
    private LocalDate date;
    
    @NotNull(message = "Event time is required")
    private LocalTime time;
    
    private String place;
    private String description;
    
    // Price fields
    @NotNull
    private Boolean isFree = true;
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be greater than or equal to 0")
    private BigDecimal price;
    
    // Online/offline fields
    @NotNull
    private Boolean isOnline = false;
    
    private String meetingUrl;
    private String meetingId;
    private String meetingPasscode;
    private Integer maxCapacity;
    private LocalDate registrationDeadline;
    private String category;
    
    @NotNull
    private Event.EventStatus status = Event.EventStatus.ACTIVE;
    
    // Recurrence fields - FIXED: Accept string from frontend, convert to enum
    private String recurrencePattern = "none"; // Change from enum to String
    private Integer recurrenceInterval = 1;
    private LocalDate recurrenceEndDate;
    private Integer recurrenceCount;
    private Long originalEventId;
    private Boolean isRecurringInstance = false;
    private Boolean requiresApproval;

    public Boolean getRequiresApproval() {
        return requiresApproval;
    }

    public void setRequiresApproval(Boolean requiresApproval) {
        this.requiresApproval = requiresApproval;
    }

    // Constructors
    public EventRequestDTO() {}

    // Custom validation method for date and time
    public boolean isValidDateTime() {
        if (date == null || time == null) {
            return false;
        }
        
        LocalDate today = LocalDate.now();
        LocalTime currentTime = LocalTime.now();
        
        // Allow past dates only if they're not today
        if (date.isBefore(today)) {
            return false;
        }
        
        // For today's events, ensure time is not in the past (with 5-minute buffer)
        if (date.equals(today)) {
            LocalTime timeWithBuffer = currentTime.plusMinutes(5);
            return time.isAfter(timeWithBuffer) || time.equals(timeWithBuffer);
        }
        
        // Future dates are always valid
        return true;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsFree() {
        return isFree;
    }

    public void setIsFree(Boolean isFree) {
        this.isFree = isFree;
        if (isFree != null && isFree) {
            this.price = BigDecimal.ZERO;
        }
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
        if (price == null || price.compareTo(BigDecimal.ZERO) == 0) {
            this.isFree = true;
        } else {
            this.isFree = false;
        }
    }

    public Boolean getIsOnline() {
        return isOnline;
    }

    public void setIsOnline(Boolean isOnline) {
        this.isOnline = isOnline;
        if (isOnline != null && !isOnline) {
            this.meetingUrl = null;
            this.meetingId = null;
            this.meetingPasscode = null;
        }
    }

    public String getMeetingUrl() {
        return meetingUrl;
    }

    public void setMeetingUrl(String meetingUrl) {
        this.meetingUrl = meetingUrl;
    }

    public String getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(String meetingId) {
        this.meetingId = meetingId;
    }

    public String getMeetingPasscode() {
        return meetingPasscode;
    }

    public void setMeetingPasscode(String meetingPasscode) {
        this.meetingPasscode = meetingPasscode;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public LocalDate getRegistrationDeadline() {
        return registrationDeadline;
    }

    public void setRegistrationDeadline(LocalDate registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Event.EventStatus getStatus() {
        return status;
    }

    public void setStatus(Event.EventStatus status) {
        this.status = status;
    }

    // FIXED: Changed to accept String and provide conversion method
    public String getRecurrencePattern() {
        return recurrencePattern;
    }

    public void setRecurrencePattern(String recurrencePattern) {
        this.recurrencePattern = recurrencePattern;
    }

    // Helper method to convert string to enum for backend processing
    public Event.RecurrencePattern getRecurrencePatternEnum() {
        if (recurrencePattern == null || recurrencePattern.equals("none")) {
            return Event.RecurrencePattern.NONE;
        }
        
        try {
            // First try direct conversion (in case frontend sends uppercase)
            return Event.RecurrencePattern.valueOf(recurrencePattern.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Handle frontend string values
            switch (recurrencePattern.toLowerCase()) {
                case "daily":
                    return Event.RecurrencePattern.DAILY;
                case "weekly":
                    return Event.RecurrencePattern.WEEKLY;
                case "monthly":
                    return Event.RecurrencePattern.MONTHLY;
                case "weekly_x":
                    return Event.RecurrencePattern.WEEKLY_X;
                case "yearly":
                    return Event.RecurrencePattern.YEARLY;
                case "none":
                default:
                    return Event.RecurrencePattern.NONE;
            }
        }
    }

    public Integer getRecurrenceInterval() {
        return recurrenceInterval;
    }

    public void setRecurrenceInterval(Integer recurrenceInterval) {
        this.recurrenceInterval = recurrenceInterval != null ? recurrenceInterval : 1;
    }

    public LocalDate getRecurrenceEndDate() {
        return recurrenceEndDate;
    }

    public void setRecurrenceEndDate(LocalDate recurrenceEndDate) {
        this.recurrenceEndDate = recurrenceEndDate;
    }

    public Integer getRecurrenceCount() {
        return recurrenceCount;
    }

    public void setRecurrenceCount(Integer recurrenceCount) {
        this.recurrenceCount = recurrenceCount;
    }

    public Long getOriginalEventId() {
        return originalEventId;
    }

    public void setOriginalEventId(Long originalEventId) {
        this.originalEventId = originalEventId;
    }

    public Boolean getIsRecurringInstance() {
        return isRecurringInstance;
    }

    public void setIsRecurringInstance(Boolean isRecurringInstance) {
        this.isRecurringInstance = isRecurringInstance != null ? isRecurringInstance : false;
    }

    // Validation methods
    public boolean isValid() {
        if (isOnline != null && isOnline) {
            return name != null && !name.trim().isEmpty() &&
                   date != null && time != null && isValidDateTime();
        } else {
            return name != null && !name.trim().isEmpty() &&
                   date != null && time != null && isValidDateTime() &&
                   place != null && !place.trim().isEmpty();
        }
    }

    public String getEventType() {
        return (isOnline != null && isOnline) ? "Online" : "Face-to-Face";
    }

    public String getEventLocation() {
        if (isOnline != null && isOnline) {
            return meetingUrl != null ? "Online Meeting" : "Online";
        }
        return place != null ? place : "TBD";
    }

    @Override
    public String toString() {
        return "EventRequestDTO{" +
                "name='" + name + '\'' +
                ", date=" + date +
                ", time=" + time +
                ", place='" + place + '\'' +
                ", isOnline=" + isOnline +
                ", isFree=" + isFree +
                ", price=" + price +
                ", recurrencePattern='" + recurrencePattern + '\'' +
                ", recurrenceInterval=" + recurrenceInterval +
                ", recurrenceEndDate=" + recurrenceEndDate +
                ", recurrenceCount=" + recurrenceCount +
                ", category='" + category + '\'' +
                ", status=" + status +
                '}';
    }
}