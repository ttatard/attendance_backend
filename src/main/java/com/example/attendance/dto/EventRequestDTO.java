package com.example.attendance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class EventRequestDTO {
    @NotBlank
    private String name;
    
    @NotNull
    private LocalDate date;
    
    @NotNull
    private LocalTime time;
    
    @NotBlank
    private String place;
    
    private String description;
    
    // Price fields
    @NotNull
    private Boolean isFree = true;
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be greater than or equal to 0")
    private BigDecimal price;

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
        // If event is free, set price to zero
        if (isFree != null && isFree) {
            this.price = BigDecimal.ZERO;
        }
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
        // If price is set to zero or null, mark as free
        if (price == null || price.compareTo(BigDecimal.ZERO) == 0) {
            this.isFree = true;
        } else {
            this.isFree = false;
        }
    }
}