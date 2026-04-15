package com.example.banking.controller;

import com.example.banking.dto.*;
import com.example.banking.model.User;
import com.example.banking.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/overview")
    public CustomerOverviewResponse overview(Authentication auth) {
        return customerService.getOverview(principal(auth));
    }

    @GetMapping("/accounts")
    public CustomerAccountsResponse accounts(Authentication auth) {
        return customerService.getAccounts(principal(auth));
    }

    @GetMapping("/activity")
    public CustomerActivityResponse activity(Authentication auth) {
        return customerService.getActivity(principal(auth));
    }

    @PostMapping("/transfers")
    public ActionResponse transfer(Authentication auth,
                                   @Valid @RequestBody TransferRequest request) {
        return customerService.submitTransfer(principal(auth), request);
    }

    @PostMapping("/payments")
    public ActionResponse payment(Authentication auth,
                                  @Valid @RequestBody PaymentRequest request) {
        return customerService.submitPayment(principal(auth), request);
    }

    @PostMapping("/accounts/{accountId}/request-block")
    public ActionResponse requestBlock(Authentication auth,
                                       @PathVariable UUID accountId,
                                       @Valid @RequestBody BlockRequestSubmitRequest request) {
        return customerService.requestBlock(principal(auth), accountId, request);
    }

    @GetMapping("/downloads/statement")
    public DownloadFileResponse downloadStatement(Authentication auth,
                                                   @RequestParam UUID accountId) {
        return customerService.downloadStatement(principal(auth), accountId);
    }

    @GetMapping("/downloads/history")
    public DownloadFileResponse downloadHistory(Authentication auth,
                                                 @RequestParam(required = false) UUID accountId) {
        return customerService.downloadHistory(principal(auth), accountId);
    }

    private User principal(Authentication auth) {
        return (User) auth.getPrincipal();
    }
}
