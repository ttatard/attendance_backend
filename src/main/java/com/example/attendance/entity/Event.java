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
}