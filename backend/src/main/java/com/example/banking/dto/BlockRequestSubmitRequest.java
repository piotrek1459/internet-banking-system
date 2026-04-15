package com.example.banking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BlockRequestSubmitRequest {
    @NotBlank
    private String reason;
}
