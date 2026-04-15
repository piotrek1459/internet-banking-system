package com.example.banking.dto;

import com.example.banking.model.AccountStatus;
import com.example.banking.model.User;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UserDto {
    private UUID id;
    private String email;
    private String role;
    private String firstName;
    private String lastName;
    private int failedLoginAttempts;
    private boolean isAccessBlocked;
    private Instant lastLoginAt;

    public static UserDto from(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .isAccessBlocked(user.getAccountStatus() != AccountStatus.ACTIVE)
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
