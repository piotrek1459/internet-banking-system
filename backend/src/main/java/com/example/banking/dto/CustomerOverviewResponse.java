package com.example.banking.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CustomerOverviewResponse {
    private UserDto user;
    private BigDecimal totalBalance;
    private int activeAccounts;
    private int pendingBlockRequests;
    private List<AccountSummaryDto> accounts;
    private List<TransactionDto> recentTransactions;
    private List<String> alerts;
}
