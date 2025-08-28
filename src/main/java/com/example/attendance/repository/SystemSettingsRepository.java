package com.example.attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import com.example.attendance.entity.SystemSettings;

@Repository
public interface SystemSettingsRepository extends JpaRepository<SystemSettings, Long> {
    
    // Fix: Use proper JPA query syntax instead of LIMIT
    @Query("SELECT s FROM SystemSettings s ORDER BY s.id DESC")
    Optional<SystemSettings> findLatestSettings();
    
    // Alternative: Use Spring Data method naming convention
    Optional<SystemSettings> findFirstByOrderByIdDesc();
}