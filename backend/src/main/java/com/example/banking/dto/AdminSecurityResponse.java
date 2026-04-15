package com.example.banking.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AdminSecurityResponse {
    private List<AdminCustomerSummary> blockedUsers;
    private List<BlockRequestDto> pendingRequests;
    private List<BlockedAccountEntry> blockedAccounts;

    public record BlockedAccountEntry(
            UUID accountId,
            String accountName,
            String accountNumber,
            String customerName
    ) {}
}
