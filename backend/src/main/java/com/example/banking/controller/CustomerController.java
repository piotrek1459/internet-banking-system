package com.example.banking.controller;

import com.example.banking.dto.AccountSummaryDto;
import com.example.banking.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {
    private final AuthService authService;

    public CustomerController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/accounts")
    public List<AccountSummaryDto> accounts(@RequestHeader(value = "X-Demo-User", required = false) String email) {
        String resolvedEmail = email == null ? "admin@bank.local" : email;
        return authService.findAccountsByUserEmail(resolvedEmail);
    }
}
