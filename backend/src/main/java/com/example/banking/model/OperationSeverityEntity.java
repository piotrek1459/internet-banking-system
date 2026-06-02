package com.example.banking.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "operation_severities")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OperationSeverityEntity {
    @Id @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 100)
    private String label;

    public boolean is(OperationSeverity severity) {
        return this.code.equals(severity.name());
    }
}
