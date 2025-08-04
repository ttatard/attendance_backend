package com.example.attendance.service;

import com.example.attendance.entity.Organizer;
import com.example.attendance.entity.User;
import com.example.attendance.exception.OrganizerCreationException;
import com.example.attendance.repository.OrganizerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizerService {
    private final OrganizerRepository organizerRepository;

    @Transactional
    public Organizer createOrganizerForAdmin(User user) {
        try {
            log.info("Creating organizer profile for admin user: {}", user.getEmail());
            
            if (organizerRepository.existsByUser(user)) {
                log.warn("Organizer already exists for user: {}", user.getEmail());
                return organizerRepository.findByUser(user)
                    .orElseThrow(() -> new OrganizerCreationException("Organizer exists but could not be retrieved"));
            }

            Organizer organizer = Organizer.builder()
                .user(user)
                .email(user.getEmail())
                .organizationName(generateDefaultOrganizationName(user))
                .description(generateDefaultDescription(user))
                .isActive(true)
                .build();
            
            return organizerRepository.save(organizer);
        } catch (DataIntegrityViolationException e) {
            throw new OrganizerCreationException("Organizer with this email already exists: " + user.getEmail(), e);
        } catch (Exception e) {
            throw new OrganizerCreationException("Failed to create organizer profile: " + e.getMessage(), e);
        }
    }

    public List<Organizer> getAllOrganizers() {
        return organizerRepository.findAll();
    }

    public Optional<Organizer> getOrganizerById(Long id) {
        return organizerRepository.findById(id);
    }

    public Optional<Organizer> findByUser(User user) {
        return organizerRepository.findByUser(user);
    }

    public Optional<Organizer> findByUserEmail(String email) {
        return organizerRepository.findByUserEmail(email);
    }

    // Add this new method
    public Optional<Organizer> findByUserId(Long userId) {
        return organizerRepository.findByUserId(userId);
    }

    @Transactional
    public void deleteOrganizer(Long organizerId) {
        if (!organizerRepository.existsById(organizerId)) {
            throw new RuntimeException("Organizer not found with id: " + organizerId);
        }
        organizerRepository.deleteById(organizerId);
    }

    public boolean existsById(Long organizerId) {
        return organizerRepository.existsById(organizerId);
    }

    public boolean existsByUser(User user) {
        return organizerRepository.existsByUser(user);
    }

    @Transactional
    public Organizer toggleOrganizerStatus(Long organizerId) {
        return organizerRepository.findById(organizerId)
            .map(organizer -> {
                organizer.setIsActive(!organizer.getIsActive());
                return organizerRepository.save(organizer);
            })
            .orElseThrow(() -> new RuntimeException("Organizer not found with id: " + organizerId));
    }

    private String generateDefaultOrganizationName(User user) {
        return user.getFirstName() + " " + user.getLastName() + "'s Organization";
    }

    private String generateDefaultDescription(User user) {
        return "Default organization profile for " + user.getFirstName() + " " + user.getLastName();
    }
}