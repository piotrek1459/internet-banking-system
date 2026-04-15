package com.example.banking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PaymentRequest {
    @NotNull
    private UUID sourceAccountId;

    @NotBlank
    private String payeeName;

    @NotBlank
    private String reference;

    @NotNull
    @Positive
    private BigDecimal amount;
}
