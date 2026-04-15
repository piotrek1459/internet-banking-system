package com.example.banking.controller;

import com.example.banking.dto.*;
import com.example.banking.model.User;
import com.example.banking.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        return adminService.getDashboard();
    }

    @GetMapping("/customers")
    public Map<String, List<AdminCustomerSummary>> customers() {
        return Map.of("items", adminService.getCustomers());
    }

    @GetMapping("/operations")
    public Map<String, List<OperationRecordDto>> operations() {
        return Map.of("items", adminService.getOperations());
    }

    @GetMapping("/security")
    public AdminSecurityResponse security() {
        return adminService.getSecurity();
    }

    @PostMapping("/users/{userId}/unlock-access")
    public ActionResponse unlockAccess(@PathVariable UUID userId, Authentication auth) {
        return adminService.unlockUser(userId, principal(auth));
    }

    @PostMapping("/accounts/{accountId}/block")
    public ActionResponse blockAccount(@PathVariable UUID accountId, Authentication auth) {
        return adminService.blockAccount(accountId, principal(auth));
    }

    @PostMapping("/accounts/{accountId}/unblock")
    public ActionResponse unblockAccount(@PathVariable UUID accountId, Authentication auth) {
        return adminService.unblockAccount(accountId, principal(auth));
    }

    @PostMapping("/block-requests/{requestId}/approve")
    public ActionResponse approveRequest(@PathVariable UUID requestId, Authentication auth) {
        return adminService.approveBlockRequest(requestId, principal(auth));
    }

    @PostMapping("/block-requests/{requestId}/reject")
    public ActionResponse rejectRequest(@PathVariable UUID requestId, Authentication auth) {
        return adminService.rejectBlockRequest(requestId, principal(auth));
    }

    private User principal(Authentication auth) {
        return (User) auth.getPrincipal();
    }
}
