package com.example.attendance.service;

import com.example.attendance.dto.EventRequestDTO;
import com.example.attendance.entity.Event;
import com.example.attendance.entity.User;
import com.example.attendance.repository.EventRepository;
import com.example.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public Event createEvent(EventRequestDTO eventRequest, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IllegalStateException("User not found with email: " + userEmail));

        log.info("Creating event for user ID: {} with email: {}", user.getId(), userEmail);

        Event event = new Event();
        event.setName(eventRequest.getName());
        event.setDate(eventRequest.getDate());
        event.setTime(eventRequest.getTime());
        event.setPlace(eventRequest.getPlace());
        event.setDescription(eventRequest.getDescription());
        event.setUser(user);
        event.setQrCode(generateQrCode());
        
        // Handle price information
        event.setIsFree(eventRequest.getIsFree());
        if (eventRequest.getIsFree()) {
            event.setPrice(BigDecimal.ZERO);
            log.info("Creating free event: {}", eventRequest.getName());
        } else {
            BigDecimal price = eventRequest.getPrice() != null ? eventRequest.getPrice() : BigDecimal.ZERO;
            event.setPrice(price);
            log.info("Creating paid event: {} with price: {}", eventRequest.getName(), price);
        }
        
        Event savedEvent = eventRepository.save(event);
        log.info("Event created successfully with ID: {} for user ID: {}, Price: {}", 
                savedEvent.getId(), user.getId(), savedEvent.getPrice());
        
        return savedEvent;
    }

    public List<Event> getEventsByUserEmail(String email) {
        log.info("Fetching events for user email: {}", email);
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalStateException("User not found with email: " + email));
        
        log.info("Found user with ID: {} for email: {}", user.getId(), email);
        
        List<Event> events = eventRepository.findByUserId(user.getId());
        log.info("Found {} events for user ID: {}", events.size(), user.getId());
        
        // Log event details for debugging including price info
        events.forEach(event -> 
            log.info("Event: ID={}, name='{}', user_id={}, isFree={}, price={}", 
                event.getId(), event.getName(), event.getUser().getId(), 
                event.getIsFree(), event.getPrice())
        );
        
        return events;
    }

    public Event getEventById(Long id) {
        return eventRepository.findById(id)
            .orElseThrow(() -> new IllegalStateException("Event not found with ID: " + id));
    }

    public Event verifyEventQrCode(Long eventId, String qrCode) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new IllegalStateException("Event not found with ID: " + eventId));

        if (!qrCode.equals(event.getQrCode())) {
            throw new IllegalStateException("Invalid QR code for event ID: " + eventId);
        }
        return event;
    }

    public void deleteEvent(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new IllegalStateException("Event not found with ID: " + id);
        }
        eventRepository.deleteById(id);
        log.info("Event deleted successfully with ID: {}", id);
    }

    // Helper method to get events by user ID directly (useful for debugging)
    public List<Event> getEventsByUserId(Long userId) {
        return eventRepository.findByUserId(userId);
    }
    
    // Price-related service methods
    public List<Event> getFreeEvents() {
        log.info("Fetching all free events");
        return eventRepository.findByIsFree(true);
    }
    
    public List<Event> getPaidEvents() {
        log.info("Fetching all paid events");
        return eventRepository.findByIsFree(false);
    }
    
    public List<Event> getFreeEventsByUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IllegalStateException("User not found with email: " + userEmail));
        log.info("Fetching free events for user: {}", userEmail);
        return eventRepository.findFreeEventsByUserId(user.getId());
    }
    
    public List<Event> getPaidEventsByUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IllegalStateException("User not found with email: " + userEmail));
        log.info("Fetching paid events for user: {}", userEmail);
        return eventRepository.findPaidEventsByUserId(user.getId());
    }
    
    public List<Event> getEventsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        log.info("Fetching events with price between {} and {}", minPrice, maxPrice);
        return eventRepository.findByPriceBetween(minPrice, maxPrice);
    }
    
    public BigDecimal getAveragePriceOfPaidEvents() {
        BigDecimal avgPrice = eventRepository.getAveragePriceOfPaidEvents();
        log.info("Average price of paid events: {}", avgPrice);
        return avgPrice != null ? avgPrice : BigDecimal.ZERO;
    }
    
    public Long countFreeEvents() {
        return eventRepository.countByIsFree(true);
    }
    
    public Long countPaidEvents() {
        return eventRepository.countByIsFree(false);
    }

    private String generateQrCode() {
        return "EVT-" + UUID.randomUUID().toString();
    }
}