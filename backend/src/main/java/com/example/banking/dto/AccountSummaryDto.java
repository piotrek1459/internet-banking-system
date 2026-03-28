package com.example.banking.dto;

import com.example.banking.model.BankAccount;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class AccountSummaryDto {
    private UUID id;
    private String accountNumber;
    private String currency;
    private BigDecimal balance;
    private String status;

    public static AccountSummaryDto from(BankAccount account) {
        return AccountSummaryDto.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .currency(account.getCurrency())
                .balance(account.getBalance())
                .status(account.getStatus())
                .build();
    }
}
