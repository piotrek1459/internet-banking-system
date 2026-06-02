package com.example.banking.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "operation_types")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OperationTypeEntity {
    @Id @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 100)
    private String label;

    public boolean is(OperationType type) {
        return this.code.equals(type.name());
    }
}
