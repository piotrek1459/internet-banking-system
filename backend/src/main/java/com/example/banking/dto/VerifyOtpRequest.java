package com.example.banking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class VerifyOtpRequest {
    @NotNull
    private UUID otpSessionId;
    @NotBlank
    private String otpCode;
}
