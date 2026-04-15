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
    private String name;
    private String type;
    private String accountNumber;
    private String iban;
    private String currency;
    private BigDecimal balance;
    private String status;

    public static AccountSummaryDto from(BankAccount account) {
        return AccountSummaryDto.builder()
                .id(account.getId())
                .name(account.getName())
                .type(account.getType())
                .accountNumber(account.getAccountNumber())
                .iban(account.getIban())
                .currency(account.getCurrency())
                .balance(account.getBalance())
                .status(account.getStatus().name())
                .build();
    }
}
