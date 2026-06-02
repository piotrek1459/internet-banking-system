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
    private final RoleEntityRepository roleEntityRepository;
    private final AccountStatusEntityRepository accountStatusEntityRepository;
    private final BankAccountStatusEntityRepository bankAccountStatusEntityRepository;
    private final BlockRequestStatusEntityRepository blockRequestStatusEntityRepository;
    private final OperationSeverityEntityRepository operationSeverityEntityRepository;

    public AdminDashboardResponse getDashboard() {
        RoleEntity customerRole = roleEntityRepository.findByCode(Role.CUSTOMER.name()).orElseThrow();
        BankAccountStatusEntity blockedAccountStatus = bankAccountStatusEntityRepository.findByCode(BankAccountStatus.BLOCKED.name()).orElseThrow();
        BlockRequestStatusEntity pendingRequestStatus = blockRequestStatusEntityRepository.findByCode(BlockRequestStatus.PENDING.name()).orElseThrow();

        long totalCustomers = userRepository.countByRole(customerRole);
        java.math.BigDecimal totalFunds = bankAccountRepository.sumAllBalances();
        long blockedUsers =
                userRepository.countByAccountStatus(accountStatusEntityRepository.findByCode(AccountStatus.LOCKED_LOGIN_FAILURE.name()).orElseThrow())
                + userRepository.countByAccountStatus(accountStatusEntityRepository.findByCode(AccountStatus.BLOCKED_BY_BANK.name()).orElseThrow())
                + userRepository.countByAccountStatus(accountStatusEntityRepository.findByCode(AccountStatus.BLOCKED_BY_CUSTOMER_REQUEST.name()).orElseThrow());
        long blockedAccounts = bankAccountRepository.countByStatus(blockedAccountStatus);
        long pendingBlockRequests = blockRequestRepository.countByStatus(pendingRequestStatus);

        List<OperationRecordDto> recentCritical = operationRecordRepository
                .findTop6BySeverityInOrderByCreatedAtDesc(
                        List.of(
                                operationSeverityEntityRepository.findByCode(OperationSeverity.CRITICAL.name()).orElseThrow(),
                                operationSeverityEntityRepository.findByCode(OperationSeverity.WARNING.name()).orElseThrow()
                        ))
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
        RoleEntity customerRole = roleEntityRepository.findByCode(Role.CUSTOMER.name()).orElseThrow();
        BlockRequestStatusEntity pendingStatus = blockRequestStatusEntityRepository.findByCode(BlockRequestStatus.PENDING.name()).orElseThrow();
        BankAccountStatusEntity blockedStatus = bankAccountStatusEntityRepository.findByCode(BankAccountStatus.BLOCKED.name()).orElseThrow();

        return userRepository.findByRole(customerRole).stream().map(user -> {
            List<BankAccount> accounts = bankAccountRepository.findByOwner(user);
            List<AccountSummaryDto> accountDtos = accounts.stream().map(AccountSummaryDto::from).toList();
            long blockedAccounts = accounts.stream()
                    .filter(a -> a.getStatus().is(BankAccountStatus.BLOCKED)).count();
            long pendingRequests = blockRequestRepository.countByUserAndStatus(user, pendingStatus);
            return AdminCustomerSummary.from(user, accountDtos, blockedAccounts, pendingRequests);
        }).toList();
    }

    public List<OperationRecordDto> getOperations() {
        return operationRecordRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(OperationRecordDto::from).toList();
    }

    public AdminSecurityResponse getSecurity() {
        RoleEntity customerRole = roleEntityRepository.findByCode(Role.CUSTOMER.name()).orElseThrow();
        AccountStatusEntity activeStatus = accountStatusEntityRepository.findByCode(AccountStatus.ACTIVE.name()).orElseThrow();
        BlockRequestStatusEntity pendingRequestStatus = blockRequestStatusEntityRepository.findByCode(BlockRequestStatus.PENDING.name()).orElseThrow();
        BankAccountStatusEntity blockedAccountStatus = bankAccountStatusEntityRepository.findByCode(BankAccountStatus.BLOCKED.name()).orElseThrow();

        List<AdminCustomerSummary> blockedUsers = userRepository.findByRole(customerRole).stream()
                .filter(u -> !u.getAccountStatus().is(AccountStatus.ACTIVE))
                .map(user -> {
                    List<BankAccount> accounts = bankAccountRepository.findByOwner(user);
                    List<AccountSummaryDto> accountDtos = accounts.stream().map(AccountSummaryDto::from).toList();
                    long blockedAccounts = accounts.stream()
                            .filter(a -> a.getStatus().is(BankAccountStatus.BLOCKED)).count();
                    long pendingRequests = blockRequestRepository.countByUserAndStatus(user, pendingRequestStatus);
                    return AdminCustomerSummary.from(user, accountDtos, blockedAccounts, pendingRequests);
                }).toList();

        List<BlockRequestDto> pendingRequests = blockRequestRepository
                .findByStatus(pendingRequestStatus)
                .stream().map(BlockRequestDto::from).toList();

        List<AdminSecurityResponse.BlockedAccountEntry> blockedAccounts = bankAccountRepository
                .findByStatus(blockedAccountStatus)
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

        AccountStatusEntity activeStatus = accountStatusEntityRepository.findByCode(AccountStatus.ACTIVE.name()).orElseThrow();
        user.setAccountStatus(activeStatus);
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

        BankAccountStatusEntity blockedStatus = bankAccountStatusEntityRepository.findByCode(BankAccountStatus.BLOCKED.name()).orElseThrow();
        BlockRequestStatusEntity pendingStatus = blockRequestStatusEntityRepository.findByCode(BlockRequestStatus.PENDING.name()).orElseThrow();
        BlockRequestStatusEntity approvedStatus = blockRequestStatusEntityRepository.findByCode(BlockRequestStatus.APPROVED.name()).orElseThrow();

        account.setStatus(blockedStatus);
        bankAccountRepository.save(account);

        blockRequestRepository.findByAccountAndStatus(account, pendingStatus)
                .ifPresent(br -> {
                    br.setStatus(approvedStatus);
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

        BankAccountStatusEntity activeStatus = bankAccountStatusEntityRepository.findByCode(BankAccountStatus.ACTIVE.name()).orElseThrow();
        account.setStatus(activeStatus);
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

        BlockRequestStatusEntity pendingStatus = blockRequestStatusEntityRepository.findByCode(BlockRequestStatus.PENDING.name()).orElseThrow();
        if (!request.getStatus().is(BlockRequestStatus.PENDING)) {
            throw new IllegalStateException("Block request is not in PENDING state");
        }

        BlockRequestStatusEntity approvedStatus = blockRequestStatusEntityRepository.findByCode(BlockRequestStatus.APPROVED.name()).orElseThrow();
        request.setStatus(approvedStatus);
        blockRequestRepository.save(request);

        BankAccount account = request.getAccount();
        BankAccountStatusEntity blockedStatus = bankAccountStatusEntityRepository.findByCode(BankAccountStatus.BLOCKED.name()).orElseThrow();
        account.setStatus(blockedStatus);
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

        if (!request.getStatus().is(BlockRequestStatus.PENDING)) {
            throw new IllegalStateException("Block request is not in PENDING state");
        }

        BlockRequestStatusEntity rejectedStatus = blockRequestStatusEntityRepository.findByCode(BlockRequestStatus.REJECTED.name()).orElseThrow();
        request.setStatus(rejectedStatus);
        blockRequestRepository.save(request);

        BankAccount account = request.getAccount();
        if (account.getStatus().is(BankAccountStatus.PENDING_BLOCK)) {
            BankAccountStatusEntity activeStatus = bankAccountStatusEntityRepository.findByCode(BankAccountStatus.ACTIVE.name()).orElseThrow();
            account.setStatus(activeStatus);
            bankAccountRepository.save(account);
        }

        operationService.record(admin.getEmail(), Role.ADMIN,
                account.getAccountNumber(), OperationType.ACCOUNT_UNBLOCKED,
                OperationSeverity.INFO,
                "Administrator rejected block request for account " + account.getName());

        return ActionResponse.of("Block request rejected. Account remains active.");
    }
}
