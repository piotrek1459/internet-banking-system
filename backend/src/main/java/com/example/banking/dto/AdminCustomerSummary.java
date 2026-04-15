package com.example.banking.dto;

import com.example.banking.model.AccountStatus;
import com.example.banking.model.User;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AdminCustomerSummary {
    private UUID userId;
    private String name;
    private String email;
    private String accessStatus;
    private int failedLoginAttempts;
    private long blockedAccounts;
    private long pendingBlockRequests;
    private Instant lastLoginAt;
    private List<AccountSummaryDto> accounts;

    public static AdminCustomerSummary from(User user, List<AccountSummaryDto> accounts,
                                            long blockedAccounts, long pendingBlockRequests) {
        return AdminCustomerSummary.builder()
                .userId(user.getId())
                .name(user.getFirstName() + " " + user.getLastName())
                .email(user.getEmail())
                .accessStatus(user.getAccountStatus() == AccountStatus.ACTIVE ? "ACTIVE" : "BLOCKED")
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .blockedAccounts(blockedAccounts)
                .pendingBlockRequests(pendingBlockRequests)
                .lastLoginAt(user.getLastLoginAt())
                .accounts(accounts)
                .build();
    }
}
