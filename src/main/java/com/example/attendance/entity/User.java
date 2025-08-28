package com.example.attendance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"organizer", "enrolledOrganizations"})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false)
    private LocalDate birthday;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Gender gender;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountType accountType;
    
    @Column
    private String address;
    
    @Column(name = "spouse_name")
    private String spouseName;
    
    @Column
    @Enumerated(EnumType.STRING)
    private Ministry ministry;
    
    @Column
    @Enumerated(EnumType.STRING)
    private Apostolate apostolate;
    
    @Column(name = "is_deactivated", nullable = false)
    @Builder.Default
    private boolean isDeactivated = false;
    
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Organizer organizer;
    
    @ManyToMany
    @JoinTable(
        name = "user_organizations",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "organization_id")
    )
    @Builder.Default
    private Set<Organizer> enrolledOrganizations = new HashSet<>();
    
    // Custom method to get full name
    public String getName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return "";
    }
    
    // FIXED: Return mutable sets instead of immutable ones
    public Set<Organizer> getEnrolledOrganizations() {
        if (enrolledOrganizations == null) {
            enrolledOrganizations = new HashSet<>();
        }
        return enrolledOrganizations;
    }
    
    // FIXED: Allow setting enrolled organizations
    public void setEnrolledOrganizations(Set<Organizer> enrolledOrganizations) {
        this.enrolledOrganizations = enrolledOrganizations != null ? enrolledOrganizations : new HashSet<>();
    }
    
    // Methods to manage organization enrollment
    public void enrollToOrganization(Organizer organizer) {
        if (organizer == null) {
            throw new IllegalArgumentException("Organizer cannot be null");
        }
        if (this.enrolledOrganizations == null) {
            this.enrolledOrganizations = new HashSet<>();
        }
        this.enrolledOrganizations.add(organizer);
        
        // Ensure bidirectional relationship
        if (organizer.getEnrolledUsers() != null) {
            organizer.getEnrolledUsers().add(this);
        }
    }
    
    public void unenrollFromOrganization(Organizer organizer) {
        if (organizer == null) {
            throw new IllegalArgumentException("Organizer cannot be null");
        }
        if (this.enrolledOrganizations != null) {
            this.enrolledOrganizations.remove(organizer);
        }
        
        // Ensure bidirectional relationship
        if (organizer.getEnrolledUsers() != null) {
            organizer.getEnrolledUsers().remove(this);
        }
    }
    
    public boolean isEnrolledInOrganization(Organizer organizer) {
        if (organizer == null || this.enrolledOrganizations == null) {
            return false;
        }
        return this.enrolledOrganizations.contains(organizer);
    }
    
    public enum Gender {
        MALE, FEMALE, OTHER, UNSPECIFIED
    }
    
    public enum AccountType {
        USER, ADMIN, SYSTEM_OWNER
    }
    
    public enum Ministry {
        TREASURY,
        SERVICE,
        MANAGEMENT_SERVICES,
        TEACHING,
        YOUTH,
        PLSG,
        INTERCESSORY,
        HOMESTEADS,
        OUTREACHES,
        PRAISE,
        LITURGY,
        LSS
    }
    
    public enum Apostolate {
        MANAGEMENT,
        PASTORAL,
        FORMATION,
        MISSION,
        EVANGELIZATION
    }

    // Proper equals and hashCode implementations
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}