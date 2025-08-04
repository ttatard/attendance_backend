package com.example.attendance.repository;

import com.example.attendance.entity.EventRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {
    
    // Check if user is registered for event
    boolean existsByEventIdAndUserEmail(Long eventId, String userEmail);
    
    // Find registration by event and user email
    EventRegistration findByEventIdAndUserEmail(Long eventId, String userEmail);
    
    // Find all registrations by user email
    List<EventRegistration> findByUserEmail(String userEmail);
    
    // Find all registrations by event ID
    List<EventRegistration> findByEventId(Long eventId);
    
    // Find registrations by event ID and status
    List<EventRegistration> findByEventIdAndStatus(Long eventId, EventRegistration.RegistrationStatus status);
    
    // Find registrations by status
    List<EventRegistration> findByStatus(EventRegistration.RegistrationStatus status);
    
    // Count registrations for an event
    Long countByEventId(Long eventId);
    
    // Count registrations for an event by status
    Long countByEventIdAndStatus(Long eventId, EventRegistration.RegistrationStatus status);
    
    // Count approved registrations for an event
    @Query("SELECT COUNT(r) FROM EventRegistration r WHERE r.eventId = :eventId AND r.status = 'APPROVED'")
    Long countApprovedByEventId(@Param("eventId") Long eventId);
    
    // Count pending registrations for an event
    @Query("SELECT COUNT(r) FROM EventRegistration r WHERE r.eventId = :eventId AND r.status = 'PENDING'")
    Long countPendingByEventId(@Param("eventId") Long eventId);
    
    // Find all approved registrations for an event
    @Query("SELECT r FROM EventRegistration r WHERE r.eventId = :eventId AND r.status = 'APPROVED' ORDER BY r.registrationDate ASC")
    List<EventRegistration> findApprovedByEventId(@Param("eventId") Long eventId);
    
    // Find all pending registrations for an event
    @Query("SELECT r FROM EventRegistration r WHERE r.eventId = :eventId AND r.status = 'PENDING' ORDER BY r.registrationDate ASC")
    List<EventRegistration> findPendingByEventId(@Param("eventId") Long eventId);
    
    // Find all disapproved registrations for an event
    @Query("SELECT r FROM EventRegistration r WHERE r.eventId = :eventId AND r.status = 'DISAPPROVED' ORDER BY r.registrationDate ASC")
    List<EventRegistration> findDisapprovedByEventId(@Param("eventId") Long eventId);
    
    // Delete registration
    void deleteByEventIdAndUserEmail(Long eventId, String userEmail);
    
    // Find registrations by user email and status
    List<EventRegistration> findByUserEmailAndStatus(String userEmail, EventRegistration.RegistrationStatus status);
    
    // Check if user has approved registration for event
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM EventRegistration r WHERE r.eventId = :eventId AND r.userEmail = :userEmail AND r.status = 'APPROVED'")
    boolean hasApprovedRegistration(@Param("eventId") Long eventId, @Param("userEmail") String userEmail);
    
    // Get registration status for user and event
    @Query("SELECT r.status FROM EventRegistration r WHERE r.eventId = :eventId AND r.userEmail = :userEmail")
    Optional<EventRegistration.RegistrationStatus> getRegistrationStatus(@Param("eventId") Long eventId, @Param("userEmail") String userEmail);
}