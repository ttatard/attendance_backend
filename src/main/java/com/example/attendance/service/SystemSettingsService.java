package com.example.attendance.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.example.attendance.entity.SystemSettings;
import com.example.attendance.repository.SystemSettingsRepository;

@Service
public class SystemSettingsService {

    @Autowired
    private SystemSettingsRepository systemSettingsRepository;

    public SystemSettings getCurrentSettings() {
        // Use the Spring Data method naming convention instead of custom query
        return systemSettingsRepository.findFirstByOrderByIdDesc()
                .orElseGet(() -> {
                    // Create and save default settings if none exist
                    SystemSettings defaultSettings = new SystemSettings();
                    defaultSettings.setSidebarColor("#2c3e50");
                    defaultSettings.setOrganizationName("Attendance System");
                    defaultSettings.setSidebarLogoUrl(""); // Empty by default
                    
                    System.out.println("Creating default system settings");
                    return systemSettingsRepository.save(defaultSettings);
                });
    }

    public SystemSettings updateSettings(SystemSettings newSettings) {
        SystemSettings currentSettings = getCurrentSettings();
        
        System.out.println("=== Updating System Settings ===");
        System.out.println("Current settings ID: " + currentSettings.getId());
        System.out.println("New sidebar color: " + newSettings.getSidebarColor());
        System.out.println("New organization name: " + newSettings.getOrganizationName());
        System.out.println("New sidebar logo URL: " + newSettings.getSidebarLogoUrl());
        
        if (newSettings.getSidebarColor() != null && !newSettings.getSidebarColor().trim().isEmpty()) {
            currentSettings.setSidebarColor(newSettings.getSidebarColor().trim());
        }
        
        if (newSettings.getSidebarLogoUrl() != null) {
            // Allow empty string to clear the logo
            currentSettings.setSidebarLogoUrl(newSettings.getSidebarLogoUrl().trim());
        }
        
        if (newSettings.getOrganizationName() != null && !newSettings.getOrganizationName().trim().isEmpty()) {
            currentSettings.setOrganizationName(newSettings.getOrganizationName().trim());
        }
        
        SystemSettings savedSettings = systemSettingsRepository.save(currentSettings);
        System.out.println("Settings saved successfully with ID: " + savedSettings.getId());
        
        return savedSettings;
    }

    public SystemSettings updateSidebarColor(String color) {
        if (color == null || color.trim().isEmpty()) {
            throw new IllegalArgumentException("Color cannot be null or empty");
        }
        
        SystemSettings settings = getCurrentSettings();
        settings.setSidebarColor(color.trim());
        return systemSettingsRepository.save(settings);
    }

    public SystemSettings updateSidebarLogo(String logoUrl) {
        SystemSettings settings = getCurrentSettings();
        // Allow null or empty string to clear the logo
        settings.setSidebarLogoUrl(logoUrl != null ? logoUrl.trim() : "");
        return systemSettingsRepository.save(settings);
    }
}