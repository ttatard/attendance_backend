package com.example.attendance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import com.example.attendance.entity.SystemSettings; // ADD THIS IMPORT
import com.example.attendance.service.SystemSettingsService;

// SystemSettingsController.java
@RestController
@RequestMapping("/api/system/settings")
@PreAuthorize("hasRole('SYSTEM_OWNER')")
public class SystemSettingsController {

    @Autowired
    private SystemSettingsService systemSettingsService;

    @GetMapping
    public ResponseEntity<SystemSettings> getSystemSettings() {
        SystemSettings settings = systemSettingsService.getCurrentSettings();
        return ResponseEntity.ok(settings);
    }

    @PutMapping
    public ResponseEntity<SystemSettings> updateSystemSettings(@RequestBody SystemSettings newSettings) {
        SystemSettings updatedSettings = systemSettingsService.updateSettings(newSettings);
        return ResponseEntity.ok(updatedSettings);
    }

    @PutMapping("/sidebar-color")
    public ResponseEntity<SystemSettings> updateSidebarColor(@RequestBody Map<String, String> request) {
        String color = request.get("color");
        if (color == null || color.isEmpty()) {
            throw new IllegalArgumentException("Color is required");
        }
        
        SystemSettings updatedSettings = systemSettingsService.updateSidebarColor(color);
        return ResponseEntity.ok(updatedSettings);
    }

    @PutMapping("/sidebar-logo")
    public ResponseEntity<SystemSettings> updateSidebarLogo(@RequestBody Map<String, String> request) {
        String logoUrl = request.get("logoUrl");
        if (logoUrl == null || logoUrl.isEmpty()) {
            throw new IllegalArgumentException("Logo URL is required");
        }
        
        SystemSettings updatedSettings = systemSettingsService.updateSidebarLogo(logoUrl);
        return ResponseEntity.ok(updatedSettings);
    }
}