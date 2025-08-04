package com.example.attendance.service;

import com.example.attendance.entity.Event;
import com.example.attendance.entity.User;
import com.example.attendance.entity.UserEventAttendance;
import com.example.attendance.repository.EventRepository;
import com.example.attendance.repository.UserEventAttendanceRepository;
import com.example.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventService {
    private final UserEventAttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Transactional
    public UserEventAttendance recordAttendance(Long userId, Long eventId) {
        log.info("Recording attendance for user {} at event {}", userId, eventId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));

        if (attendanceRepository.existsByUserAndEvent(user, event)) {
            throw new IllegalStateException("Attendance already recorded for user " + userId + " at event " + eventId);
        }

        UserEventAttendance attendance = UserEventAttendance.builder()
                .user(user)
                .event(event)
                .attended(true)
                .build();
        
        UserEventAttendance savedAttendance = attendanceRepository.save(attendance);
        log.info("Successfully recorded attendance with ID: {}", savedAttendance.getId());
        return savedAttendance;
    }

    public List<UserEventAttendance> getUserAttendedEvents(Long userId) {
        log.debug("Fetching attended events for user: {}", userId);
        
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        
        List<UserEventAttendance> attendedEvents = attendanceRepository.findByUserId(userId);
        log.debug("Found {} attended events for user {}", attendedEvents.size(), userId);
        return attendedEvents;
    }

    public List<UserEventAttendance> getEventAttendees(Long eventId) {
        log.debug("Fetching attendees for event: {}", eventId);
        
        if (!eventRepository.existsById(eventId)) {
            throw new IllegalArgumentException("Event not found with ID: " + eventId);
        }
        
        return attendanceRepository.findByEventId(eventId);
    }

    public int getEventAttendanceCount(Long eventId) {
        log.debug("Counting attendance for event: {}", eventId);
        
        if (!eventRepository.existsById(eventId)) {
            throw new IllegalArgumentException("Event not found with ID: " + eventId);
        }
        
        return attendanceRepository.countByEventId(eventId);
    }

    public List<Event> getRegisteredEvents(Long userId) {
        log.debug("Fetching registered events for user: {}", userId);
        
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        
        // Get all events where the user has attendance records (registered/attended)
        List<Event> registeredEvents = eventRepository.findAll().stream()
            .filter(event -> attendanceRepository.existsByUserIdAndEventId(userId, event.getId()))
            .collect(Collectors.toList());
            
        log.debug("Found {} registered events for user {}", registeredEvents.size(), userId);
        return registeredEvents;
    }

    public List<Event> getAllEvents() {
        log.debug("Fetching all events");
        return eventRepository.findAll();
    }

    // Additional utility method to check if user is registered for an event
    public boolean isUserRegisteredForEvent(Long userId, Long eventId) {
        return attendanceRepository.existsByUserIdAndEventId(userId, eventId);
    }

    // Method to get user's attendance statistics
    public UserAttendanceStats getUserAttendanceStats(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }

        List<Event> registeredEvents = getRegisteredEvents(userId);
        List<UserEventAttendance> attendedEvents = getUserAttendedEvents(userId);

        double attendancePercentage = registeredEvents.isEmpty() ? 0.0 : 
            (double) attendedEvents.size() / registeredEvents.size() * 100;

        return UserAttendanceStats.builder()
            .userId(userId)
            .registeredEventsCount(registeredEvents.size())
            .attendedEventsCount(attendedEvents.size())
            .attendancePercentage(attendancePercentage)
            .build();
    }

    // Inner class for attendance statistics
    @lombok.Data
    @lombok.Builder
    public static class UserAttendanceStats {
        private Long userId;
        private int registeredEventsCount;
        private int attendedEventsCount;
        private double attendancePercentage;
    }
}