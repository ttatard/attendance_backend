package com.example.attendance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import com.example.attendance.entity.Event;

public class EventResponseDTO {
    private Long id;
    private String name;
    private LocalDate date;
    private LocalTime time;
    private String place;
    private String description;
    private String qrCode;
    private Long userId;
    private Boolean isFree;
    private BigDecimal price;
    
    // Online/offline fields
    private Boolean isOnline;
    private String meetingUrl;
    private String meetingId;
    private String meetingPasscode;
    private Integer maxCapacity;
    private LocalDate registrationDeadline;
    private String category;
    private Event.EventStatus status;
    
    // Recurrence fields
    private Event.RecurrencePattern recurrencePattern;
    private Integer recurrenceInterval;
    private LocalDate recurrenceEndDate;
    private Integer recurrenceCount;
    private Long originalEventId;
    private Boolean isRecurringInstance;
    
    // NEW: Organizer information
    private String organizerName;
    private Long organizerId;
    
    // Computed fields
    private String eventType;
    private String eventLocation;
    private Boolean registrationOpen;

    // Constructors
    public EventResponseDTO() {}

    public EventResponseDTO(Long id, String name, LocalDate date, LocalTime time, 
                          String place, String description, String qrCode, Long userId,
                          Boolean isFree, BigDecimal price, Boolean isOnline, String meetingUrl,
                          String meetingId, String meetingPasscode, Integer maxCapacity,
                          LocalDate registrationDeadline, String category, Event.EventStatus status,
                          Event.RecurrencePattern recurrencePattern, Integer recurrenceInterval,
                          LocalDate recurrenceEndDate, Integer recurrenceCount, Long originalEventId,
                          Boolean isRecurringInstance) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.time = time;
        this.place = place;
        this.description = description;
        this.qrCode = qrCode;
        this.userId = userId;
        this.isFree = isFree;
        this.price = price;
        this.isOnline = isOnline;
        this.meetingUrl = meetingUrl;
        this.meetingId = meetingId;
        this.meetingPasscode = meetingPasscode;
        this.maxCapacity = maxCapacity;
        this.registrationDeadline = registrationDeadline;
        this.category = category;
        this.status = status;
        this.recurrencePattern = recurrencePattern;
        this.recurrenceInterval = recurrenceInterval;
        this.recurrenceEndDate = recurrenceEndDate;
        this.recurrenceCount = recurrenceCount;
        this.originalEventId = originalEventId;
        this.isRecurringInstance = isRecurringInstance;
        
        // Set computed fields
        this.eventType = getComputedEventType();
        this.eventLocation = getComputedEventLocation();
        this.registrationOpen = computeRegistrationOpen();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    
    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }
    
    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public Boolean getIsFree() { return isFree; }
    public void setIsFree(Boolean isFree) { this.isFree = isFree; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Boolean getIsOnline() { return isOnline; }
    public void setIsOnline(Boolean isOnline) { 
        this.isOnline = isOnline; 
        this.eventType = getComputedEventType();
        this.eventLocation = getComputedEventLocation();
    }
    
    public String getMeetingUrl() { return meetingUrl; }
    public void setMeetingUrl(String meetingUrl) { this.meetingUrl = meetingUrl; }
    
    public String getMeetingId() { return meetingId; }
    public void setMeetingId(String meetingId) { this.meetingId = meetingId; }
    
    public String getMeetingPasscode() { return meetingPasscode; }
    public void setMeetingPasscode(String meetingPasscode) { this.meetingPasscode = meetingPasscode; }
    
    public Integer getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(Integer maxCapacity) { this.maxCapacity = maxCapacity; }
    
    public LocalDate getRegistrationDeadline() { return registrationDeadline; }
    public void setRegistrationDeadline(LocalDate registrationDeadline) { 
        this.registrationDeadline = registrationDeadline;
        this.registrationOpen = computeRegistrationOpen();
    }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public Event.EventStatus getStatus() { return status; }
    public void setStatus(Event.EventStatus status) { 
        this.status = status;
        this.registrationOpen = computeRegistrationOpen();
    }

    public Event.RecurrencePattern getRecurrencePattern() { return recurrencePattern; }
    public void setRecurrencePattern(Event.RecurrencePattern recurrencePattern) { 
        this.recurrencePattern = recurrencePattern; 
    }
    
    public Integer getRecurrenceInterval() { return recurrenceInterval; }
    public void setRecurrenceInterval(Integer recurrenceInterval) { 
        this.recurrenceInterval = recurrenceInterval; 
    }
    
    public LocalDate getRecurrenceEndDate() { return recurrenceEndDate; }
    public void setRecurrenceEndDate(LocalDate recurrenceEndDate) { 
        this.recurrenceEndDate = recurrenceEndDate; 
    }
    
    public Integer getRecurrenceCount() { return recurrenceCount; }
    public void setRecurrenceCount(Integer recurrenceCount) { 
        this.recurrenceCount = recurrenceCount; 
    }
    
    public Long getOriginalEventId() { return originalEventId; }
    public void setOriginalEventId(Long originalEventId) { 
        this.originalEventId = originalEventId; 
    }
    
    public Boolean getIsRecurringInstance() { return isRecurringInstance; }
    public void setIsRecurringInstance(Boolean isRecurringInstance) { 
        this.isRecurringInstance = isRecurringInstance; 
    }

    // NEW: Organizer getters and setters
    public String getOrganizerName() { return organizerName; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }
    
    public Long getOrganizerId() { return organizerId; }
    public void setOrganizerId(Long organizerId) { this.organizerId = organizerId; }

    // Computed field getters
    public String getEventType() { return eventType; }
    public String getEventLocation() { return eventLocation; }
    public Boolean getRegistrationOpen() { return registrationOpen; }

    // Helper methods for computed fields
    private String getComputedEventType() {
        return (isOnline != null && isOnline) ? "Online" : "Face-to-Face";
    }

    private String getComputedEventLocation() {
        if (isOnline != null && isOnline) {
            return meetingUrl != null ? "Online Meeting" : "Online";
        }
        return place != null ? place : "TBD";
    }

    private Boolean computeRegistrationOpen() {
        if (status != Event.EventStatus.ACTIVE) {
            return false;
        }
        if (registrationDeadline == null) {
            return true;
        }
        return LocalDate.now().isBefore(registrationDeadline.plusDays(1));
    }

    // Factory method to create from Entity
    public static EventResponseDTO fromEntity(Event event) {
        if (event == null) {
            return null;
        }
        
        EventResponseDTO dto = new EventResponseDTO(
            event.getId(),
            event.getName(),
            event.getDate(),
            event.getTime(),
            event.getPlace(),
            event.getDescription(),
            event.getQrCode(),
            event.getUser() != null ? event.getUser().getId() : null,
            event.getIsFree(),
            event.getPrice(),
            event.getIsOnline(),
            event.getMeetingUrl(),
            event.getMeetingId(),
            event.getMeetingPasscode(),
            event.getMaxCapacity(),
            event.getRegistrationDeadline(),
            event.getCategory(),
            event.getStatus(),
            event.getRecurrencePattern(),
            event.getRecurrenceInterval(),
            event.getRecurrenceEndDate(),
            event.getRecurrenceCount(),
            event.getOriginalEventId(),
            event.getIsRecurringInstance()
        );
        
        // Set organizer information if available
        if (event.getUser() != null && event.getUser().getOrganizer() != null) {
            dto.setOrganizerId(event.getUser().getOrganizer().getId());
            dto.setOrganizerName(event.getUser().getOrganizer().getOrganizationName());
        }
        
        return dto;
    }

    // Factory method for basic event info (without sensitive data)
    public static EventResponseDTO fromEntityPublic(Event event) {
        EventResponseDTO dto = fromEntity(event);
        if (dto != null) {
            dto.setMeetingPasscode(null);
            dto.setQrCode(null);
        }
        return dto;
    }

    @Override
    public String toString() {
        return "EventResponseDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", date=" + date +
                ", time=" + time +
                ", eventType='" + eventType + '\'' +
                ", eventLocation='" + eventLocation + '\'' +
                ", organizerName='" + organizerName + '\'' +
                ", organizerId=" + organizerId +
                ", recurrencePattern=" + recurrencePattern +
                ", recurrenceInterval=" + recurrenceInterval +
                ", isFree=" + isFree +
                ", price=" + price +
                ", category='" + category + '\'' +
                ", status=" + status +
                ", registrationOpen=" + registrationOpen +
                '}';
    }
}