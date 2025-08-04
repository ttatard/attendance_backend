package com.example.attendance.repository;

import com.example.attendance.entity.Event;
import com.example.attendance.entity.Organizer;
import com.example.attendance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizerRepository extends JpaRepository<Organizer, Long> {

    Optional<Organizer> findByUserId(Long userId);

    // Basic queries
    Optional<Organizer> findByUser(User user);
    
    boolean existsByUser(User user);

    Optional<Organizer> findByEmail(String email);

    // Find organizer by user email
    @Query("SELECT o FROM Organizer o JOIN o.user u WHERE u.email = :email")
    Optional<Organizer> findByUserEmail(@Param("email") String email);
    
    // Find events by organizer's user email
    @Query("SELECT e FROM Event e JOIN e.user u WHERE u.email = :email")
    List<Event> findEventsByOrganizerEmail(@Param("email") String email);

    // Find events by organizer's user ID
    @Query("SELECT e FROM Event e WHERE e.user.id = :userId")
    List<Event> findEventsByUserId(@Param("userId") Long userId);

    // Find organizers by status
    @Query("SELECT o FROM Organizer o WHERE o.isActive = :isActive")
    List<Organizer> findByIsActive(@Param("isActive") Boolean isActive);

    // Native query to find organizer's events
    @Query(value = "SELECT e.* FROM events e " +
                   "JOIN users u ON e.user_id = u.id " +
                   "WHERE u.email = :email", nativeQuery = true)
    List<Event> findOrganizerEventsNative(@Param("email") String email);

    // Count events by organizer's user ID
    @Query("SELECT COUNT(e) FROM Event e WHERE e.user.id = :userId")
    Long countEventsByUserId(@Param("userId") Long userId);

    // Find organizers by organization name
    @Query("SELECT o FROM Organizer o WHERE o.organizationName LIKE %:name%")
    List<Organizer> findByOrganizationNameContaining(@Param("name") String name);
}