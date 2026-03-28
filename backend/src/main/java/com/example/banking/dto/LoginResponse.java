package com.example.banking.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class LoginResponse {
    private String status;
    private String message;
    private UUID otpSessionId;
}
