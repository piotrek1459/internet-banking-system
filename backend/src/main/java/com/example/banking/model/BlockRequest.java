package com.example.banking.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "block_requests", indexes = {
        @Index(name = "idx_block_requests_user", columnList = "user_id"),
        @Index(name = "idx_block_requests_account", columnList = "account_id")
})
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

    @ManyToOne(optional = false)
    @JoinColumn(name = "status_id")
    private BlockRequestStatusEntity status;

    @PrePersist
    void onCreate() {
        if (requestedAt == null) requestedAt = Instant.now();
    }
}
