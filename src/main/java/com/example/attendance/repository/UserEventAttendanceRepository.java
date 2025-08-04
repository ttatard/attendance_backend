package com.example.attendance.repository;

import com.example.attendance.entity.UserEventAttendance;
import com.example.attendance.entity.User;
import com.example.attendance.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserEventAttendanceRepository extends JpaRepository<UserEventAttendance, Long> {
    List<UserEventAttendance> findByUserId(Long userId);
    List<UserEventAttendance> findByEventId(Long eventId);
    boolean existsByUserIdAndEventId(Long userId, Long eventId);
    boolean existsByUserAndEvent(User user, Event event); // ✅ Add this
    int countByEventId(Long eventId);
}
