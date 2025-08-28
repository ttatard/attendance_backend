package com.example.attendance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "organizers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organizer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "organization_name")
    private String organizationName;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "website")
    private String website;

    @Column(name = "address")
    private String address;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    private User user;

    @ManyToMany(mappedBy = "enrolledOrganizations", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<User> enrolledUsers = new HashSet<>();

    // Business methods
    public String getFullName() {
        if (user != null) {
            return user.getFirstName() + " " + user.getLastName();
        }
        return "Unknown";
    }

    public String getDisplayName() {
        if (organizationName != null && !organizationName.trim().isEmpty()) {
            return organizationName;
        }
        return getFullName();
    }

    // FIXED: Return mutable sets and allow modification
    public Set<User> getEnrolledUsers() {
        if (enrolledUsers == null) {
            enrolledUsers = new HashSet<>();
        }
        return enrolledUsers;
    }

    // FIXED: Allow setting enrolled users
    public void setEnrolledUsers(Set<User> enrolledUsers) {
        this.enrolledUsers = enrolledUsers != null ? enrolledUsers : new HashSet<>();
    }

    // Relationship management methods
    public void addEnrolledUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (this.enrolledUsers == null) {
            this.enrolledUsers = new HashSet<>();
        }
        this.enrolledUsers.add(user);
        
        // Ensure bidirectional relationship
        if (user.getEnrolledOrganizations() != null) {
            user.getEnrolledOrganizations().add(this);
        }
    }

    public void removeEnrolledUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (this.enrolledUsers != null) {
            this.enrolledUsers.remove(user);
        }
        
        // Ensure bidirectional relationship
        if (user.getEnrolledOrganizations() != null) {
            user.getEnrolledOrganizations().remove(this);
        }
    }

    public boolean hasEnrolledUser(User user) {
        if (user == null || this.enrolledUsers == null) {
            return false;
        }
        return this.enrolledUsers.contains(user);
    }

    // Proper equals and hashCode implementations
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Organizer organizer = (Organizer) o;
        return id != null && id.equals(organizer.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}