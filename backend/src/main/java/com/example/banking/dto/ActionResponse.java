package com.example.banking.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ActionResponse {
    private String message;

    public static ActionResponse of(String message) {
        return ActionResponse.builder().message(message).build();
    }
}
