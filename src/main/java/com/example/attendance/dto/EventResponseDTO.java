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

    // Default constructor
    public EventResponseDTO() {
    }

    // All-args constructor
    public EventResponseDTO(Long id, String name, LocalDate date, LocalTime time, 
                          String place, String description, String qrCode, Long userId,
                          Boolean isFree, BigDecimal price) {
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
    }

    // Getters and Setters
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

    public static EventResponseDTO fromEntity(Event event) {
        return new EventResponseDTO(
            event.getId(),
            event.getName(),
            event.getDate(),
            event.getTime(),
            event.getPlace(),
            event.getDescription(),
            event.getQrCode(),
            event.getUser().getId(),
            event.getIsFree(),
            event.getPrice()
        );
    }
}