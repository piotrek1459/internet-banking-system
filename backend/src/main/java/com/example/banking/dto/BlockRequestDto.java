package com.example.banking.dto;

import com.example.banking.model.BlockRequest;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class BlockRequestDto {
    private UUID id;
    private UUID userId;
    private UUID accountId;
    private String customerName;
    private String customerEmail;
    private String accountNumber;
    private String reason;
    private Instant requestedAt;
    private String status;

    public static BlockRequestDto from(BlockRequest r) {
        return BlockRequestDto.builder()
                .id(r.getId())
                .userId(r.getUser().getId())
                .accountId(r.getAccount().getId())
                .customerName(r.getUser().getFirstName() + " " + r.getUser().getLastName())
                .customerEmail(r.getUser().getEmail())
                .accountNumber(r.getAccount().getAccountNumber())
                .reason(r.getReason())
                .requestedAt(r.getRequestedAt())
                .status(r.getStatus().name())
                .build();
    }
}
