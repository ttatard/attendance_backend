package com.example.attendance.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data // This includes @Getter, @Setter, @ToString, @EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"organizer", "enrolledOrganization"})
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
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrolled_organization_id")
    private Organizer enrolledOrganization;
    
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
    
    public enum Gender {
        MALE, FEMALE, OTHER, UNSPECIFIED
    }
    
    public enum AccountType {
        USER, ADMIN
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
}