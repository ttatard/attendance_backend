package com.example.attendance.repository;

import com.example.attendance.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    
    // Find events by QR code
    Event findByQrCode(String qrCode);
    
    // Find events by user ID (main method for filtering events by creator)
    List<Event> findByUserId(Long userId);
    
    // Find events by user email (alternative approach)
    @Query("SELECT e FROM Event e WHERE e.user.email = :email")
    List<Event> findByUserEmail(@Param("email") String email);
    
    // Count events by user ID (useful for debugging)
    @Query("SELECT COUNT(e) FROM Event e WHERE e.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);
    
    // Find events by user ID with explicit join (more explicit query)
    @Query("SELECT e FROM Event e JOIN e.user u WHERE u.id = :userId")
    List<Event> findEventsByUserId(@Param("userId") Long userId);
    
    // Find all events with user information (for debugging purposes)
    @Query("SELECT e FROM Event e JOIN FETCH e.user")
    List<Event> findAllWithUser();
    
    // Check if event exists for specific user
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Event e WHERE e.id = :eventId AND e.user.id = :userId")
    boolean existsByIdAndUserId(@Param("eventId") Long eventId, @Param("userId") Long userId);
    
    // Price-related queries
    // Find events by price type (free or paid)
    List<Event> findByIsFree(Boolean isFree);
    
    // Find events within a price range
    @Query("SELECT e FROM Event e WHERE e.isFree = false AND e.price BETWEEN :minPrice AND :maxPrice")
    List<Event> findByPriceBetween(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);
    
    // Find events by user and price type
    @Query("SELECT e FROM Event e WHERE e.user.id = :userId AND e.isFree = :isFree")
    List<Event> findByUserIdAndIsFree(@Param("userId") Long userId, @Param("isFree") Boolean isFree);
    
    // Find free events by user
    @Query("SELECT e FROM Event e WHERE e.user.id = :userId AND e.isFree = true")
    List<Event> findFreeEventsByUserId(@Param("userId") Long userId);
    
    // Find paid events by user
    @Query("SELECT e FROM Event e WHERE e.user.id = :userId AND e.isFree = false")
    List<Event> findPaidEventsByUserId(@Param("userId") Long userId);
    
    // Find events with price less than specified amount
    @Query("SELECT e FROM Event e WHERE e.isFree = false AND e.price < :maxPrice")
    List<Event> findByPriceLessThan(@Param("maxPrice") BigDecimal maxPrice);
    
    // Find events with price greater than specified amount
    @Query("SELECT e FROM Event e WHERE e.isFree = false AND e.price > :minPrice")
    List<Event> findByPriceGreaterThan(@Param("minPrice") BigDecimal minPrice);
    
    // Get average price of paid events
    @Query("SELECT AVG(e.price) FROM Event e WHERE e.isFree = false")
    BigDecimal getAveragePriceOfPaidEvents();
    
    // Count free vs paid events
    @Query("SELECT COUNT(e) FROM Event e WHERE e.isFree = :isFree")
    Long countByIsFree(@Param("isFree") Boolean isFree);
    
    // Find events ordered by price (ascending)
    @Query("SELECT e FROM Event e WHERE e.isFree = false ORDER BY e.price ASC")
    List<Event> findPaidEventsOrderByPriceAsc();
    
    // Find events ordered by price (descending)
    @Query("SELECT e FROM Event e WHERE e.isFree = false ORDER BY e.price DESC")
    List<Event> findPaidEventsOrderByPriceDesc();
}