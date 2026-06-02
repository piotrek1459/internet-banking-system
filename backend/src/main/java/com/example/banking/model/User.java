package com.example.banking.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @ManyToOne(optional = false)
    @JoinColumn(name = "role_id")
    private RoleEntity role;

    @ManyToOne(optional = false)
    @JoinColumn(name = "accountStatus_id")
    private AccountStatusEntity accountStatus;

    @Column(nullable = false)
    private int failedLoginAttempts;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant lastLoginAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        enabled = true;
    }
}
