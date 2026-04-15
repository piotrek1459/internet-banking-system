package com.example.banking.service;

import com.example.banking.dto.*;
import com.example.banking.model.*;
import com.example.banking.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final BlockRequestRepository blockRequestRepository;
    private final OperationRecordRepository operationRecordRepository;
    private final OperationService operationService;

    public AdminDashboardResponse getDashboard() {
        long totalCustomers = userRepository.countByRole(Role.CUSTOMER);
        java.math.BigDecimal totalFunds = bankAccountRepository.sumAllBalances();
        long blockedUsers = userRepository.countByAccountStatus(AccountStatus.LOCKED_LOGIN_FAILURE)
                + userRepository.countByAccountStatus(AccountStatus.BLOCKED_BY_BANK)
                + userRepository.countByAccountStatus(AccountStatus.BLOCKED_BY_CUSTOMER_REQUEST);
        long blockedAccounts = bankAccountRepository.countByStatus(BankAccountStatus.BLOCKED);
        long pendingBlockRequests = blockRequestRepository.countByStatus(BlockRequestStatus.PENDING);

        List<OperationRecordDto> recentCritical = operationRecordRepository
                .findTop6BySeverityInOrderByCreatedAtDesc(
                        List.of(OperationSeverity.CRITICAL, OperationSeverity.WARNING))
                .stream().map(OperationRecordDto::from).toList();

        return AdminDashboardResponse.builder()
                .totalCustomers(totalCustomers)
                .totalFunds(totalFunds)
                .blockedUsers(blockedUsers)
                .blockedAccounts(blockedAccounts)
                .pendingBlockRequests(pendingBlockRequests)
                .recentCriticalOperations(recentCritical)
                .build();
    }

    public List<AdminCustomerSummary> getCustomers() {
        return userRepository.findByRole(Role.CUSTOMER).stream().map(user -> {
            List<BankAccount> accounts = bankAccountRepository.findByOwner(user);
            List<AccountSummaryDto> accountDtos = accounts.stream().map(AccountSummaryDto::from).toList();
            long blockedAccounts = accounts.stream()
                    .filter(a -> a.getStatus() == BankAccountStatus.BLOCKED).count();
            long pendingRequests = blockRequestRepository.countByUserAndStatus(user, BlockRequestStatus.PENDING);
            return AdminCustomerSummary.from(user, accountDtos, blockedAccounts, pendingRequests);
        }).toList();
    }

    public List<OperationRecordDto> getOperations() {
        return operationRecordRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(OperationRecordDto::from).toList();
    }

    public AdminSecurityResponse getSecurity() {
        // Blocked users
        List<AdminCustomerSummary> blockedUsers = userRepository.findByRole(Role.CUSTOMER).stream()
                .filter(u -> u.getAccountStatus() != AccountStatus.ACTIVE)
                .map(user -> {
                    List<BankAccount> accounts = bankAccountRepository.findByOwner(user);
                    List<AccountSummaryDto> accountDtos = accounts.stream().map(AccountSummaryDto::from).toList();
                    long blockedAccounts = accounts.stream()
                            .filter(a -> a.getStatus() == BankAccountStatus.BLOCKED).count();
                    long pendingRequests = blockRequestRepository.countByUserAndStatus(user, BlockRequestStatus.PENDING);
                    return AdminCustomerSummary.from(user, accountDtos, blockedAccounts, pendingRequests);
                }).toList();

        // Pending block requests
        List<BlockRequestDto> pendingRequests = blockRequestRepository
                .findByStatus(BlockRequestStatus.PENDING)
                .stream().map(BlockRequestDto::from).toList();

        // Blocked accounts
        List<AdminSecurityResponse.BlockedAccountEntry> blockedAccounts = bankAccountRepository
                .findByStatus(BankAccountStatus.BLOCKED)
                .stream().map(a -> new AdminSecurityResponse.BlockedAccountEntry(
                        a.getId(),
                        a.getName(),
                        a.getAccountNumber(),
                        a.getOwner().getFirstName() + " " + a.getOwner().getLastName()
                )).toList();

        return AdminSecurityResponse.builder()
                .blockedUsers(blockedUsers)
                .pendingRequests(pendingRequests)
                .blockedAccounts(blockedAccounts)
                .build();
    }

    @Transactional
    public ActionResponse unlockUser(UUID userId, User admin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        operationService.record(admin.getEmail(), Role.ADMIN,
                user.getEmail(), OperationType.ACCESS_UNBLOCKED,
                OperationSeverity.SUCCESS,
                "Administrator restored access for " + user.getFirstName() + " " + user.getLastName());

        return ActionResponse.of("User access restored successfully.");
    }

    @Transactional
    public ActionResponse blockAccount(UUID accountId, User admin) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        account.setStatus(BankAccountStatus.BLOCKED);
        bankAccountRepository.save(account);

        // Auto-approve any pending block request for this account
        blockRequestRepository.findByAccountAndStatus(account, BlockRequestStatus.PENDING)
                .ifPresent(br -> {
                    br.setStatus(BlockRequestStatus.APPROVED);
                    blockRequestRepository.save(br);
                });

        operationService.record(admin.getEmail(), Role.ADMIN,
                account.getAccountNumber(), OperationType.ACCOUNT_BLOCKED,
                OperationSeverity.CRITICAL,
                "Administrator blocked account " + account.getName() +
                        " belonging to " + account.getOwner().getEmail());

        return ActionResponse.of("Account blocked successfully.");
    }

    @Transactional
    public ActionResponse unblockAccount(UUID accountId, User admin) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        account.setStatus(BankAccountStatus.ACTIVE);
        bankAccountRepository.save(account);

        operationService.record(admin.getEmail(), Role.ADMIN,
                account.getAccountNumber(), OperationType.ACCOUNT_UNBLOCKED,
                OperationSeverity.SUCCESS,
                "Administrator unblocked account " + account.getName());

        return ActionResponse.of("Account unblocked successfully.");
    }

    @Transactional
    public ActionResponse approveBlockRequest(UUID requestId, User admin) {
        BlockRequest request = blockRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Block request not found"));

        if (request.getStatus() != BlockRequestStatus.PENDING) {
            throw new IllegalStateException("Block request is not in PENDING state");
        }

        request.setStatus(BlockRequestStatus.APPROVED);
        blockRequestRepository.save(request);

        BankAccount account = request.getAccount();
        account.setStatus(BankAccountStatus.BLOCKED);
        bankAccountRepository.save(account);

        operationService.record(admin.getEmail(), Role.ADMIN,
                account.getAccountNumber(), OperationType.ACCOUNT_BLOCKED,
                OperationSeverity.CRITICAL,
                "Administrator approved block request for account " + account.getName());

        return ActionResponse.of("Block request approved. Account has been blocked.");
    }

    @Transactional
    public ActionResponse rejectBlockRequest(UUID requestId, User admin) {
        BlockRequest request = blockRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Block request not found"));

        if (request.getStatus() != BlockRequestStatus.PENDING) {
            throw new IllegalStateException("Block request is not in PENDING state");
        }

        request.setStatus(BlockRequestStatus.REJECTED);
        blockRequestRepository.save(request);

        BankAccount account = request.getAccount();
        if (account.getStatus() == BankAccountStatus.PENDING_BLOCK) {
            account.setStatus(BankAccountStatus.ACTIVE);
            bankAccountRepository.save(account);
        }

        operationService.record(admin.getEmail(), Role.ADMIN,
                account.getAccountNumber(), OperationType.ACCOUNT_UNBLOCKED,
                OperationSeverity.INFO,
                "Administrator rejected block request for account " + account.getName());

        return ActionResponse.of("Block request rejected. Account remains active.");
    }
}
