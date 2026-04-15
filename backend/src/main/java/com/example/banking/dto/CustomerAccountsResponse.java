package com.example.banking.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CustomerAccountsResponse {
    private List<AccountSummaryDto> items;
}
