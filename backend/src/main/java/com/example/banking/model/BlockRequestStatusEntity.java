package com.example.banking.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "block_request_statuses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BlockRequestStatusEntity {
    @Id @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 100)
    private String label;

    public boolean is(BlockRequestStatus status) {
        return this.code.equals(status.name());
    }
}
