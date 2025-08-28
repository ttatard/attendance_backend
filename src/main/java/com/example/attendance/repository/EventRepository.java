package com.example.attendance.repository;

import com.example.attendance.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    
    // Find events by QR code
    Event findByQrCode(String qrCode);
    
    // Find events by user ID (main method for filtering events by creator)
    List<Event> findByUserId(Long userId);
    
    // Find events by user email (alternative approach)
    @Query("SELECT e FROM Event e WHERE e.user.email = :email")
    List<Event> findByUserEmail(@Param("email") String email);
    
    // FIXED: Find events by organization ID - events created by users whose organizer belongs to the organization
    // OR events where the event creator is enrolled in the organization
    @Query("SELECT DISTINCT e FROM Event e " +
           "JOIN e.user u " +
           "LEFT JOIN u.organizer o " +
           "LEFT JOIN u.enrolledOrganizations eo " +
           "WHERE o.id = :organizationId OR eo.id = :organizationId")
    List<Event> findEventsByOrganizationId(@Param("organizationId") Long organizationId);
    
    // ALTERNATIVE: If you want events only from organizers in the specific organization
    @Query("SELECT e FROM Event e " +
           "JOIN e.user u " +
           "JOIN u.organizer o " +
           "WHERE o.id = :organizationId")
    List<Event> findEventsByOrganizerOrganizationId(@Param("organizationId") Long organizationId);
    
    // ALTERNATIVE: If you want events visible to users enrolled in an organization
    // (events created by ANY user whose organizer belongs to that organization)
    @Query("SELECT e FROM Event e " +
           "WHERE EXISTS (" +
           "  SELECT 1 FROM User u " +
           "  JOIN u.organizer o " +
           "  WHERE u.id = e.user.id AND o.id = :organizationId" +
           ")")
    List<Event> findEventsVisibleToOrganization(@Param("organizationId") Long organizationId);
    
    // FIXED: Find free events by organization ID
    @Query("SELECT DISTINCT e FROM Event e " +
           "JOIN e.user u " +
           "LEFT JOIN u.organizer o " +
           "LEFT JOIN u.enrolledOrganizations eo " +
           "WHERE (o.id = :organizationId OR eo.id = :organizationId) AND e.isFree = true")
    List<Event> findFreeEventsByOrganizationId(@Param("organizationId") Long organizationId);
    
    // FIXED: Find paid events by organization ID
    @Query("SELECT DISTINCT e FROM Event e " +
           "JOIN e.user u " +
           "LEFT JOIN u.organizer o " +
           "LEFT JOIN u.enrolledOrganizations eo " +
           "WHERE (o.id = :organizationId OR eo.id = :organizationId) AND e.isFree = false")
    List<Event> findPaidEventsByOrganizationId(@Param("organizationId") Long organizationId);
    
    // FIXED: Count events by organization ID
    @Query("SELECT COUNT(DISTINCT e) FROM Event e " +
           "JOIN e.user u " +
           "LEFT JOIN u.organizer o " +
           "LEFT JOIN u.enrolledOrganizations eo " +
           "WHERE o.id = :organizationId OR eo.id = :organizationId")
    Long countEventsByOrganizationId(@Param("organizationId") Long organizationId);
    
    // Count events by user ID (useful for debugging)
    @Query("SELECT COUNT(e) FROM Event e WHERE e.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);
    
    // Find events by user ID with explicit join (more explicit query)
    @Query("SELECT e FROM Event e JOIN e.user u WHERE u.id = :userId")
    List<Event> findEventsByUserId(@Param("userId") Long userId);
    
    // Find all events with user information (for debugging purposes)
    @Query("SELECT e FROM Event e JOIN FETCH e.user")
    List<Event> findAllWithUser();
    
    // FIXED: Find all events with user and organizer information (for debugging organization filtering)
    @Query("SELECT DISTINCT e FROM Event e " +
           "JOIN FETCH e.user u " +
           "LEFT JOIN FETCH u.organizer " +
           "LEFT JOIN FETCH u.enrolledOrganizations")
    List<Event> findAllWithUserAndOrganizer();
    
    // Check if event exists for specific user
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Event e WHERE e.id = :eventId AND e.user.id = :userId")
    boolean existsByIdAndUserId(@Param("eventId") Long eventId, @Param("userId") Long userId);
    
    // FIXED: Check if event exists for specific organization
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Event e " +
           "JOIN e.user u " +
           "LEFT JOIN u.organizer o " +
           "LEFT JOIN u.enrolledOrganizations eo " +
           "WHERE e.id = :eventId AND (o.id = :organizationId OR eo.id = :organizationId)")
    boolean existsByIdAndOrganizationId(@Param("eventId") Long eventId, @Param("organizationId") Long organizationId);
    
    // Price-related queries
    // Find events by price type (free or paid)
    List<Event> findByIsFree(Boolean isFree);
    
    // Find events within a price range
    @Query("SELECT e FROM Event e WHERE e.isFree = false AND e.price BETWEEN :minPrice AND :maxPrice")
    List<Event> findByPriceBetween(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);
    
    // FIXED: Find events within a price range by organization
    @Query("SELECT DISTINCT e FROM Event e " +
           "JOIN e.user u " +
           "LEFT JOIN u.organizer o " +
           "LEFT JOIN u.enrolledOrganizations eo " +
           "WHERE (o.id = :organizationId OR eo.id = :organizationId) " +
           "AND e.isFree = false AND e.price BETWEEN :minPrice AND :maxPrice")
    List<Event> findByPriceBetweenAndOrganizationId(@Param("minPrice") BigDecimal minPrice, 
                                                   @Param("maxPrice") BigDecimal maxPrice, 
                                                   @Param("organizationId") Long organizationId);
    
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
    
    // FIXED: Find events with price less than specified amount by organization
    @Query("SELECT DISTINCT e FROM Event e " +
           "JOIN e.user u " +
           "LEFT JOIN u.organizer o " +
           "LEFT JOIN u.enrolledOrganizations eo " +
           "WHERE (o.id = :organizationId OR eo.id = :organizationId) " +
           "AND e.isFree = false AND e.price < :maxPrice")
    List<Event> findByPriceLessThanAndOrganizationId(@Param("maxPrice") BigDecimal maxPrice, 
                                                    @Param("organizationId") Long organizationId);
    
    // Find events with price greater than specified amount
    @Query("SELECT e FROM Event e WHERE e.isFree = false AND e.price > :minPrice")
    List<Event> findByPriceGreaterThan(@Param("minPrice") BigDecimal minPrice);
    
    // FIXED: Find events with price greater than specified amount by organization
    @Query("SELECT DISTINCT e FROM Event e " +
           "JOIN e.user u " +
           "LEFT JOIN u.organizer o " +
           "LEFT JOIN u.enrolledOrganizations eo " +
           "WHERE (o.id = :organizationId OR eo.id = :organizationId) " +
           "AND e.isFree = false AND e.price > :minPrice")
    List<Event> findByPriceGreaterThanAndOrganizationId(@Param("minPrice") BigDecimal minPrice, 
                                                       @Param("organizationId") Long organizationId);
    
    // Get average price of paid events
    @Query("SELECT AVG(e.price) FROM Event e WHERE e.isFree = false")
    BigDecimal getAveragePriceOfPaidEvents();
    
    // FIXED: Get average price of paid events by organization
    @Query("SELECT AVG(e.price) FROM Event e " +
           "JOIN e.user u " +
           "LEFT JOIN u.organizer o " +
           "LEFT JOIN u.enrolledOrganizations eo " +
           "WHERE (o.id = :organizationId OR eo.id = :organizationId) AND e.isFree = false")
    BigDecimal getAveragePriceOfPaidEventsByOrganization(@Param("organizationId") Long organizationId);
    
    // Count free vs paid events
    @Query("SELECT COUNT(e) FROM Event e WHERE e.isFree = :isFree")
    Long countByIsFree(@Param("isFree") Boolean isFree);
    
    // FIXED: Count free vs paid events by organization
    @Query("SELECT COUNT(DISTINCT e) FROM Event e " +
           "JOIN e.user u " +
           "LEFT JOIN u.organizer o " +
           "LEFT JOIN u.enrolledOrganizations eo " +
           "WHERE (o.id = :organizationId OR eo.id = :organizationId) AND e.isFree = :isFree")
    Long countByIsFreeAndOrganizationId(@Param("isFree") Boolean isFree, @Param("organizationId") Long organizationId);
    
    // Find events ordered by price (ascending)
    @Query("SELECT e FROM Event e WHERE e.isFree = false ORDER BY e.price ASC")
    List<Event> findPaidEventsOrderByPriceAsc();
    
    // FIXED: Find events ordered by price (ascending) by organization
    @Query("SELECT DISTINCT e FROM Event e " +
           "JOIN e.user u " +
           "LEFT JOIN u.organizer o " +
           "LEFT JOIN u.enrolledOrganizations eo " +
           "WHERE (o.id = :organizationId OR eo.id = :organizationId) AND e.isFree = false " +
           "ORDER BY e.price ASC")
    List<Event> findPaidEventsOrderByPriceAscByOrganization(@Param("organizationId") Long organizationId);
    
    // Find events ordered by price (descending)
    @Query("SELECT e FROM Event e WHERE e.isFree = false ORDER BY e.price DESC")
    List<Event> findPaidEventsOrderByPriceDesc();
    
    // FIXED: Find events ordered by price (descending) by organization
    @Query("SELECT DISTINCT e FROM Event e " +
           "JOIN e.user u " +
           "LEFT JOIN u.organizer o " +
           "LEFT JOIN u.enrolledOrganizations eo " +
           "WHERE (o.id = :organizationId OR eo.id = :organizationId) AND e.isFree = false " +
           "ORDER BY e.price DESC")
    List<Event> findPaidEventsOrderByPriceDescByOrganization(@Param("organizationId") Long organizationId);
    
    // FIXED: Find events by organization ordered by date
    @Query("SELECT DISTINCT e FROM Event e " +
           "JOIN e.user u " +
           "LEFT JOIN u.organizer o " +
           "LEFT JOIN u.enrolledOrganizations eo " +
           "WHERE o.id = :organizationId OR eo.id = :organizationId " +
           "ORDER BY e.date ASC, e.time ASC")
    List<Event> findEventsByOrganizationIdOrderByDate(@Param("organizationId") Long organizationId);
    
    // FIXED: Find upcoming events by organization (including today's events with future time)
    @Query("SELECT DISTINCT e FROM Event e " +
           "JOIN e.user u " +
           "LEFT JOIN u.organizer o " +
           "LEFT JOIN u.enrolledOrganizations eo " +
           "WHERE (o.id = :organizationId OR eo.id = :organizationId) " +
           "AND (e.date > CURRENT_DATE OR (e.date = CURRENT_DATE AND e.time >= CURRENT_TIME)) " +
           "ORDER BY e.date ASC, e.time ASC")
    List<Event> findUpcomingEventsByOrganizationId(@Param("organizationId") Long organizationId);
    
    // FIXED: Find all events by organization (including past events for admin)
    @Query("SELECT DISTINCT e FROM Event e " +
           "JOIN e.user u " +
           "LEFT JOIN u.organizer o " +
           "LEFT JOIN u.enrolledOrganizations eo " +
           "WHERE o.id = :organizationId OR eo.id = :organizationId " +
           "ORDER BY e.date ASC, e.time ASC")
    List<Event> findAllEventsByOrganizationId(@Param("organizationId") Long organizationId);
    
    // FIXED: Find events by category and organization
    @Query("SELECT DISTINCT e FROM Event e " +
           "JOIN e.user u " +
           "LEFT JOIN u.organizer o " +
           "LEFT JOIN u.enrolledOrganizations eo " +
           "WHERE (o.id = :organizationId OR eo.id = :organizationId) AND e.category = :category")
    List<Event> findEventsByCategoryAndOrganizationId(@Param("category") String category, 
                                                     @Param("organizationId") Long organizationId);
    
    // FIXED: Find events by status and organization
    @Query("SELECT DISTINCT e FROM Event e " +
           "JOIN e.user u " +
           "LEFT JOIN u.organizer o " +
           "LEFT JOIN u.enrolledOrganizations eo " +
           "WHERE (o.id = :organizationId OR eo.id = :organizationId) AND e.status = :status")
    List<Event> findEventsByStatusAndOrganizationId(@Param("status") Event.EventStatus status, 
                                                   @Param("organizationId") Long organizationId);
    
    // FIXED: Find active events by organization
    @Query("SELECT DISTINCT e FROM Event e " +
           "JOIN e.user u " +
           "LEFT JOIN u.organizer o " +
           "LEFT JOIN u.enrolledOrganizations eo " +
           "WHERE (o.id = :organizationId OR eo.id = :organizationId) " +
           "AND e.status = 'ACTIVE' " +
           "ORDER BY e.date ASC, e.time ASC")
    List<Event> findActiveEventsByOrganizationId(@Param("organizationId") Long organizationId);

    // FIXED: Find events by multiple organization IDs
    @Query("SELECT DISTINCT e FROM Event e " +
           "JOIN e.user u " +
           "LEFT JOIN u.organizer o " +
           "LEFT JOIN u.enrolledOrganizations eo " +
           "WHERE o.id IN :organizationIds OR eo.id IN :organizationIds " +
           "ORDER BY e.date ASC, e.time ASC")
    List<Event> findEventsByOrganizationIds(@Param("organizationIds") Set<Long> organizationIds);

    // FIXED: Find upcoming events by multiple organization IDs (for users)
    @Query("SELECT DISTINCT e FROM Event e " +
           "JOIN e.user u " +
           "LEFT JOIN u.organizer o " +
           "LEFT JOIN u.enrolledOrganizations eo " +
           "WHERE (o.id IN :organizationIds OR eo.id IN :organizationIds) " +
           "AND (e.date > CURRENT_DATE OR (e.date = CURRENT_DATE AND e.time >= CURRENT_TIME)) " +
           "ORDER BY e.date ASC, e.time ASC")
    List<Event> findUpcomingEventsByOrganizationIds(@Param("organizationIds") Set<Long> organizationIds);
}