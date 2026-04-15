package com.example.banking.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "block_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BlockRequest {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    private User user;

    @ManyToOne(optional = false)
    private BankAccount account;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private Instant requestedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BlockRequestStatus status;

    @PrePersist
    void onCreate() {
        if (requestedAt == null) requestedAt = Instant.now();
        if (status == null) status = BlockRequestStatus.PENDING;
    }
}
