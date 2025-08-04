package com.example.attendance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "organizers")
@Data  // This includes @Getter, @Setter, @ToString, @EqualsAndHashCode
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
    private User user;

    @OneToMany(mappedBy = "enrolledOrganization", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<User> enrolledUsers = new ArrayList<>();

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

    public void addEnrolledUser(User user) {
        enrolledUsers.add(user);
        user.setEnrolledOrganization(this);
    }

    public void removeEnrolledUser(User user) {
        enrolledUsers.remove(user);
        user.setEnrolledOrganization(null);
    }
}