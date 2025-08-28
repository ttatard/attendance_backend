package com.example.attendance.repository;

import com.example.attendance.entity.Event;
import com.example.attendance.entity.User;
import com.example.attendance.entity.UserEventAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface UserEventAttendanceRepository extends JpaRepository<UserEventAttendance, Long> {
    
    // Existing methods
    List<UserEventAttendance> findByUserId(Long userId);
    List<UserEventAttendance> findByEventId(Long eventId);
    boolean existsByUserAndEvent(User user, Event event);
    boolean existsByUserIdAndEventId(Long userId, Long eventId);
    
    // Count methods for admin reports
    @Query("SELECT COUNT(ua) FROM UserEventAttendance ua WHERE ua.event.id = :eventId")
    long countByEventId(@Param("eventId") Long eventId);
    
    @Query("SELECT COUNT(ua) FROM UserEventAttendance ua WHERE ua.event.id = :eventId AND ua.attended = :attended")
    long countByEventIdAndAttended(@Param("eventId") Long eventId, @Param("attended") Boolean attended);
    
    @Query("SELECT COUNT(ua) FROM UserEventAttendance ua WHERE ua.user.id = :userId AND ua.attended = :attended")
    long countByUserIdAndAttended(@Param("userId") Long userId, @Param("attended") Boolean attended);
    
    // Monthly attendance reports
    @Query("SELECT COUNT(ua) FROM UserEventAttendance ua WHERE ua.attendanceDate BETWEEN :startDate AND :endDate")
    long countAttendanceInDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT ua FROM UserEventAttendance ua WHERE ua.attendanceDate BETWEEN :startDate AND :endDate")
    List<UserEventAttendance> findAttendanceInDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    // Event-specific queries
    @Query("SELECT ua FROM UserEventAttendance ua WHERE ua.event.id = :eventId AND ua.attended = true")
    List<UserEventAttendance> findAttendedByEventId(@Param("eventId") Long eventId);
    
    @Query("SELECT ua FROM UserEventAttendance ua WHERE ua.event.id = :eventId AND ua.attended = false")
    List<UserEventAttendance> findRegisteredButNotAttendedByEventId(@Param("eventId") Long eventId);
    
    // User-specific queries for reports
    @Query("SELECT ua FROM UserEventAttendance ua WHERE ua.user.id = :userId AND ua.attended = true")
    List<UserEventAttendance> findAttendedEventsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT ua FROM UserEventAttendance ua WHERE ua.user.id = :userId AND ua.attended = false")
    List<UserEventAttendance> findRegisteredButNotAttendedByUserId(@Param("userId") Long userId);
    
    // Monthly statistics for admin dashboard
    @Query("SELECT MONTH(ua.attendanceDate) as month, YEAR(ua.attendanceDate) as year, COUNT(ua) as count " +
           "FROM UserEventAttendance ua WHERE ua.attended = true " +
           "GROUP BY YEAR(ua.attendanceDate), MONTH(ua.attendanceDate) " +
           "ORDER BY year DESC, month DESC")
    List<Object[]> getMonthlyAttendanceStats();
    
    // Event attendance rate calculations
    @Query("SELECT ua.event.id, COUNT(ua) as totalRegistered, " +
           "SUM(CASE WHEN ua.attended = true THEN 1 ELSE 0 END) as totalAttended " +
           "FROM UserEventAttendance ua " +
           "GROUP BY ua.event.id")
    List<Object[]> getEventAttendanceStats();
    
    // Find all attendance records for events created by a specific user (event creator)
    @Query("SELECT ua FROM UserEventAttendance ua WHERE ua.event.user.id = :creatorUserId")
    List<UserEventAttendance> findAttendanceForEventsByCreator(@Param("creatorUserId") Long creatorUserId);
    
    // Get attendance records for events within a date range
    @Query("SELECT ua FROM UserEventAttendance ua WHERE ua.event.date BETWEEN :startDate AND :endDate")
    List<UserEventAttendance> findAttendanceForEventsInDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    // Count unique attendees across all events
    @Query("SELECT COUNT(DISTINCT ua.user.id) FROM UserEventAttendance ua WHERE ua.attended = true")
    long countUniqueAttendees();
    
    // Count unique registrants across all events
    @Query("SELECT COUNT(DISTINCT ua.user.id) FROM UserEventAttendance ua")
    long countUniqueRegistrants();
    
    // Get top attended events
    @Query("SELECT ua.event, COUNT(ua) as attendeeCount FROM UserEventAttendance ua " +
           "WHERE ua.attended = true " +
           "GROUP BY ua.event " +
           "ORDER BY attendeeCount DESC")
    List<Object[]> getTopAttendedEvents();
    
    // Get events with low attendance (less than specified percentage)
    @Query("SELECT ua.event.id, ua.event.name, " +
           "COUNT(ua) as totalRegistered, " +
           "SUM(CASE WHEN ua.attended = true THEN 1 ELSE 0 END) as totalAttended, " +
           "(SUM(CASE WHEN ua.attended = true THEN 1 ELSE 0 END) * 100.0 / COUNT(ua)) as attendanceRate " +
           "FROM UserEventAttendance ua " +
           "GROUP BY ua.event.id, ua.event.name " +
           "HAVING (SUM(CASE WHEN ua.attended = true THEN 1 ELSE 0 END) * 100.0 / COUNT(ua)) < :threshold")
    List<Object[]> getEventsWithLowAttendance(@Param("threshold") double threshold);
}