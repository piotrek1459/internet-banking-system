package com.example.banking.dto;

import com.example.banking.model.Transaction;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class TransactionDto {
    private UUID id;
    private UUID accountId;
    private String accountName;
    private Instant createdAt;
    private String type;
    private String title;
    private String description;
    private BigDecimal amount;
    private String currency;
    private String direction;
    private String status;
    private String counterparty;
    private String reference;

    public static TransactionDto from(Transaction t) {
        return TransactionDto.builder()
                .id(t.getId())
                .accountId(t.getAccount().getId())
                .accountName(t.getAccountName())
                .createdAt(t.getCreatedAt())
                .type(t.getType().name())
                .title(t.getTitle())
                .description(t.getDescription())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .direction(t.getDirection().name())
                .status(t.getStatus().name())
                .counterparty(t.getCounterparty())
                .reference(t.getReference())
                .build();
    }
}
