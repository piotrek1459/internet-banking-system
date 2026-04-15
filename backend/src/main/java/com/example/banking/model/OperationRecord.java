package com.example.banking.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "operation_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OperationRecord {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private Instant createdAt;

    @ManyToOne
    private User actor;

    @Column(nullable = false)
    private String actorEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role actorRole;

    @Column(nullable = false)
    private String target;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationSeverity severity;

    @Column(nullable = false, length = 512)
    private String description;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
