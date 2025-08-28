package com.example.attendance.service;

import com.example.attendance.dto.OrganizerDto;
import com.example.attendance.entity.Organizer;
import com.example.attendance.entity.User;
import com.example.attendance.exception.OrganizerCreationException;
import com.example.attendance.exception.OrganizerNotFoundException;
import com.example.attendance.mapper.OrganizerMapper;
import com.example.attendance.repository.OrganizerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizerService {

    private final OrganizerRepository organizerRepository;
    private final OrganizerMapper organizerMapper;

    @Transactional
    public OrganizerDto createOrganizerForAdmin(User adminUser) {
        try {
            log.info("Creating organizer profile for admin user: {}", adminUser.getEmail());
            
            if (organizerRepository.existsByUser(adminUser)) {
                log.warn("Organizer already exists for admin: {}", adminUser.getEmail());
                return organizerRepository.findByUser(adminUser)
                    .map(organizerMapper::toDto)
                    .orElseThrow(() -> new OrganizerCreationException(
                        "Organizer exists but could not be retrieved for admin: " + adminUser.getEmail()
                    ));
            }

            Organizer organizer = Organizer.builder()
                .user(adminUser)
                .email(adminUser.getEmail())
                .organizationName(generateDefaultOrganizationName(adminUser))
                .description(generateDefaultDescription(adminUser))
                .isActive(true)
                .build();
            
            Organizer savedOrganizer = organizerRepository.save(organizer);
            log.info("Successfully created organizer with ID: {}", savedOrganizer.getId());
            
            return organizerMapper.toDto(savedOrganizer);
        } catch (DataIntegrityViolationException e) {
            String errorMsg = "Organizer with this email already exists: " + adminUser.getEmail();
            log.error(errorMsg, e);
            throw new OrganizerCreationException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Failed to create organizer profile for admin: " + adminUser.getEmail();
            log.error(errorMsg, e);
            throw new OrganizerCreationException(errorMsg, e);
        }
    }

    @Transactional(readOnly = true)
    public List<OrganizerDto> getAllOrganizers() {
        log.info("Fetching all organizers");
        return organizerRepository.findAll()
            .stream()
            .map(organizerMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrganizerDto getOrganizerById(Long id) {
        log.info("Fetching organizer by ID: {}", id);
        return organizerRepository.findById(id)
            .map(organizerMapper::toDto)
            .orElseThrow(() -> {
                log.warn("Organizer not found with ID: {}", id);
                return new OrganizerNotFoundException("Organizer not found with ID: " + id);
            });
    }

    @Transactional(readOnly = true)
    public Optional<OrganizerDto> findByUser(User user) {
        log.debug("Finding organizer by user: {}", user.getEmail());
        return organizerRepository.findByUser(user)
            .map(organizerMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<OrganizerDto> findByUserEmail(String email) {
        log.debug("Finding organizer by user email: {}", email);
        return organizerRepository.findByUserEmail(email)
            .map(organizerMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<OrganizerDto> findByUserId(Long userId) {
        log.debug("Finding organizer by user ID: {}", userId);
        return organizerRepository.findByUserId(userId)
            .map(organizerMapper::toDto);
    }

    // This method returns Optional<Organizer> for entity operations
    @Transactional(readOnly = true)
    public Optional<Organizer> getOrganizerEntity(Long id) {
        return organizerRepository.findById(id);
    }

    // This method returns Optional<Organizer> by user ID for entity operations
    @Transactional(readOnly = true)
    public Optional<Organizer> findEntityByUserId(Long userId) {
        return organizerRepository.findByUserId(userId);
    }

    @Transactional
    public void deleteOrganizer(Long organizerId) {
        log.info("Deleting organizer with ID: {}", organizerId);
        if (!organizerRepository.existsById(organizerId)) {
            log.warn("Delete failed - organizer not found with ID: {}", organizerId);
            throw new OrganizerNotFoundException("Organizer not found with ID: " + organizerId);
        }
        organizerRepository.deleteById(organizerId);
        log.info("Successfully deleted organizer with ID: {}", organizerId);
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long organizerId) {
        log.debug("Checking existence of organizer with ID: {}", organizerId);
        return organizerRepository.existsById(organizerId);
    }

    @Transactional(readOnly = true)
    public boolean existsByUser(User user) {
        log.debug("Checking existence of organizer for user: {}", user.getEmail());
        return organizerRepository.existsByUser(user);
    }

    @Transactional
    public OrganizerDto toggleOrganizerStatus(Long organizerId) {
        log.info("Toggling status for organizer with ID: {}", organizerId);
        return organizerRepository.findById(organizerId)
            .map(organizer -> {
                boolean newStatus = !organizer.getIsActive();
                organizer.setIsActive(newStatus);
                Organizer updated = organizerRepository.save(organizer);
                log.info("Organizer ID: {} status changed to: {}", organizerId, newStatus);
                return organizerMapper.toDto(updated);
            })
            .orElseThrow(() -> {
                log.warn("Toggle failed - organizer not found with ID: {}", organizerId);
                return new OrganizerNotFoundException("Organizer not found with ID: " + organizerId);
            });
    }

    @Transactional
    public OrganizerDto updateOrganizer(Long organizerId, OrganizerDto organizerDto) {
        log.info("Updating organizer with ID: {}", organizerId);
        return organizerRepository.findById(organizerId)
            .map(existingOrganizer -> {
                organizerMapper.updateOrganizerFromDto(organizerDto, existingOrganizer);
                Organizer updated = organizerRepository.save(existingOrganizer);
                log.info("Successfully updated organizer with ID: {}", organizerId);
                return organizerMapper.toDto(updated);
            })
            .orElseThrow(() -> {
                log.warn("Update failed - organizer not found with ID: {}", organizerId);
                return new OrganizerNotFoundException("Organizer not found with ID: " + organizerId);
            });
    }

    // Helper methods
    private String generateDefaultOrganizationName(User user) {
        return user.getFirstName() + " " + user.getLastName() + "'s Organization";
    }

    private String generateDefaultDescription(User user) {
        return "Default organization profile for " + user.getFirstName() + " " + user.getLastName();
    }
}