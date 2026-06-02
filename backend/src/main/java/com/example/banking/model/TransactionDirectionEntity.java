package com.example.banking.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "transaction_directions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransactionDirectionEntity {
    @Id @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 100)
    private String label;

    public boolean is(TransactionDirection direction) {
        return this.code.equals(direction.name());
    }
}
