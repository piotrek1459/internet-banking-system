package com.example.banking.dto;

import com.example.banking.model.OperationRecord;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class OperationRecordDto {
    private UUID id;
    private Instant createdAt;
    private String actorName;
    private String actorRole;
    private String target;
    private String type;
    private String severity;
    private String description;

    public static OperationRecordDto from(OperationRecord r) {
        String actorName;
        if (r.getActor() != null) {
            actorName = r.getActor().getFirstName() + " " + r.getActor().getLastName();
        } else {
            actorName = r.getActorEmail();
        }
        return OperationRecordDto.builder()
                .id(r.getId())
                .createdAt(r.getCreatedAt())
                .actorName(actorName)
                .actorRole(r.getActorRole().name())
                .target(r.getTarget())
                .type(r.getType().name())
                .severity(r.getSeverity().name())
                .description(r.getDescription())
                .build();
    }
}
