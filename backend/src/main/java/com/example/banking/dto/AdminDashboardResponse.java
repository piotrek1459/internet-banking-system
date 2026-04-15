package com.example.banking.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class AdminDashboardResponse {
    private long totalCustomers;
    private BigDecimal totalFunds;
    private long blockedUsers;
    private long blockedAccounts;
    private long pendingBlockRequests;
    private List<OperationRecordDto> recentCriticalOperations;
}
