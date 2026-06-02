package com.example.banking.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transactions_owner", columnList = "owner_id"),
        @Index(name = "idx_transactions_account", columnList = "account_id"),
        @Index(name = "idx_transactions_created_at", columnList = "createdAt")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    private User owner;

    @ManyToOne(optional = false)
    private BankAccount account;

    @Column(nullable = false)
    private String accountName;

    @Column(nullable = false)
    private Instant createdAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "type_id")
    private TransactionTypeEntity type;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @ManyToOne(optional = false)
    @JoinColumn(name = "direction_id")
    private TransactionDirectionEntity direction;

    @ManyToOne(optional = false)
    @JoinColumn(name = "status_id")
    private TransactionStatusEntity status;

    @Column
    private String counterparty;

    @Column(nullable = false)
    private String reference;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
