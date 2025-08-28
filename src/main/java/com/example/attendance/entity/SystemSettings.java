package com.example.attendance.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// SystemSettings.java
@Entity
@Table(name = "system_settings")
public class SystemSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sidebar_color")
    private String sidebarColor = "#2c3e50"; // Default color

    @Column(name = "sidebar_logo_url")
    private String sidebarLogoUrl;

    @Column(name = "organization_name")
    private String organizationName = "Attendance System";

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    @PrePersist
    public void updateTimestamps() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSidebarColor() { return sidebarColor; }
    public void setSidebarColor(String sidebarColor) { this.sidebarColor = sidebarColor; }
    
    public String getSidebarLogoUrl() { return sidebarLogoUrl; }
    public void setSidebarLogoUrl(String sidebarLogoUrl) { this.sidebarLogoUrl = sidebarLogoUrl; }
    
    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}