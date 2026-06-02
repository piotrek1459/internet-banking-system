package com.example.banking.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "operation_records", indexes = {
        @Index(name = "idx_operation_records_actor_email", columnList = "actorEmail"),
        @Index(name = "idx_operation_records_target", columnList = "target"),
        @Index(name = "idx_operation_records_created_at", columnList = "createdAt")
})
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

    @ManyToOne(optional = false)
    @JoinColumn(name = "actorRole_id")
    private RoleEntity actorRole;

    @Column(nullable = false)
    private String target;

    @ManyToOne(optional = false)
    @JoinColumn(name = "type_id")
    private OperationTypeEntity type;

    @ManyToOne(optional = false)
    @JoinColumn(name = "severity_id")
    private OperationSeverityEntity severity;

    @Column(nullable = false, length = 512)
    private String description;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
