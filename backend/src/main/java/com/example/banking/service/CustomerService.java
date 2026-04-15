package com.example.banking.service;

import com.example.banking.dto.*;
import com.example.banking.model.*;
import com.example.banking.repository.BankAccountRepository;
import com.example.banking.repository.BlockRequestRepository;
import com.example.banking.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final BlockRequestRepository blockRequestRepository;
    private final OperationService operationService;

    public CustomerOverviewResponse getOverview(User user) {
        List<BankAccount> accounts = bankAccountRepository.findByOwner(user);
        List<AccountSummaryDto> accountDtos = accounts.stream().map(AccountSummaryDto::from).toList();

        BigDecimal totalBalance = accounts.stream()
                .map(BankAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activeAccounts = accounts.stream()
                .filter(a -> a.getStatus() == BankAccountStatus.ACTIVE)
                .count();

        long pendingBlockRequests = accounts.stream()
                .filter(a -> a.getStatus() == BankAccountStatus.PENDING_BLOCK)
                .count();

        List<TransactionDto> recentTransactions = transactionRepository
                .findTop6ByOwnerOrderByCreatedAtDesc(user)
                .stream().map(TransactionDto::from).toList();

        List<String> alerts = new ArrayList<>();
        for (BankAccount account : accounts) {
            if (account.getStatus() == BankAccountStatus.PENDING_BLOCK) {
                alerts.add(account.getName() + " has a pending block request awaiting admin review.");
            } else if (account.getStatus() == BankAccountStatus.BLOCKED) {
                alerts.add(account.getName() + " is blocked and cannot be used for new payments.");
            }
        }

        return CustomerOverviewResponse.builder()
                .user(UserDto.from(user))
                .totalBalance(totalBalance)
                .activeAccounts((int) activeAccounts)
                .pendingBlockRequests((int) pendingBlockRequests)
                .accounts(accountDtos)
                .recentTransactions(recentTransactions)
                .alerts(alerts)
                .build();
    }

    public CustomerAccountsResponse getAccounts(User user) {
        List<AccountSummaryDto> items = bankAccountRepository.findByOwner(user)
                .stream().map(AccountSummaryDto::from).toList();
        return CustomerAccountsResponse.builder().items(items).build();
    }

    public CustomerActivityResponse getActivity(User user) {
        List<AccountSummaryDto> accounts = bankAccountRepository.findByOwner(user)
                .stream().map(AccountSummaryDto::from).toList();
        List<TransactionDto> items = transactionRepository
                .findByOwnerOrderByCreatedAtDesc(user)
                .stream().map(TransactionDto::from).toList();
        return CustomerActivityResponse.builder().accounts(accounts).items(items).build();
    }

    @Transactional
    public ActionResponse submitTransfer(User user, TransferRequest request) {
        BankAccount source = bankAccountRepository.findByIdAndOwner(request.getSourceAccountId(), user)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        if (source.getStatus() != BankAccountStatus.ACTIVE) {
            throw new IllegalStateException("Only active accounts can create transfers");
        }
        if (source.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        source.setBalance(source.getBalance().subtract(request.getAmount()));
        bankAccountRepository.save(source);

        String reference = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        transactionRepository.save(Transaction.builder()
                .owner(user)
                .account(source)
                .accountName(source.getName())
                .type(TransactionType.TRANSFER)
                .title("Transfer to " + request.getRecipientName())
                .description(request.getDescription())
                .amount(request.getAmount())
                .currency(source.getCurrency())
                .direction(TransactionDirection.DEBIT)
                .status(TransactionStatus.COMPLETED)
                .counterparty(request.getRecipientName())
                .reference(reference)
                .build());

        // Credit target if internal account
        bankAccountRepository.findByAccountNumber(request.getRecipientAccountNumber())
                .ifPresent(target -> {
                    if (target.getStatus() == BankAccountStatus.ACTIVE) {
                        target.setBalance(target.getBalance().add(request.getAmount()));
                        bankAccountRepository.save(target);
                        transactionRepository.save(Transaction.builder()
                                .owner(target.getOwner())
                                .account(target)
                                .accountName(target.getName())
                                .type(TransactionType.TRANSFER)
                                .title("Transfer from " + user.getFirstName() + " " + user.getLastName())
                                .description(request.getDescription())
                                .amount(request.getAmount())
                                .currency(target.getCurrency())
                                .direction(TransactionDirection.CREDIT)
                                .status(TransactionStatus.COMPLETED)
                                .counterparty(user.getFirstName() + " " + user.getLastName())
                                .reference(reference)
                                .build());
                    }
                });

        operationService.record(user.getEmail(), Role.CUSTOMER,
                source.getAccountNumber(), OperationType.TRANSFER_CREATED,
                OperationSeverity.INFO,
                "Transfer of " + request.getAmount() + " " + source.getCurrency() +
                        " to " + request.getRecipientName());

        return ActionResponse.of("Transfer completed successfully.");
    }

    @Transactional
    public ActionResponse submitPayment(User user, PaymentRequest request) {
        BankAccount source = bankAccountRepository.findByIdAndOwner(request.getSourceAccountId(), user)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        if (source.getStatus() != BankAccountStatus.ACTIVE) {
            throw new IllegalStateException("Only active accounts can create payments");
        }
        if (source.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        source.setBalance(source.getBalance().subtract(request.getAmount()));
        bankAccountRepository.save(source);

        String reference = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        transactionRepository.save(Transaction.builder()
                .owner(user)
                .account(source)
                .accountName(source.getName())
                .type(TransactionType.PAYMENT)
                .title("Payment to " + request.getPayeeName())
                .description("Ref: " + request.getReference())
                .amount(request.getAmount())
                .currency(source.getCurrency())
                .direction(TransactionDirection.DEBIT)
                .status(TransactionStatus.COMPLETED)
                .counterparty(request.getPayeeName())
                .reference(reference)
                .build());

        operationService.record(user.getEmail(), Role.CUSTOMER,
                source.getAccountNumber(), OperationType.PAYMENT_CREATED,
                OperationSeverity.INFO,
                "Payment of " + request.getAmount() + " " + source.getCurrency() +
                        " to " + request.getPayeeName());

        return ActionResponse.of("Payment completed successfully.");
    }

    @Transactional
    public ActionResponse requestBlock(User user, UUID accountId, BlockRequestSubmitRequest request) {
        BankAccount account = bankAccountRepository.findByIdAndOwner(accountId, user)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        if (account.getStatus() != BankAccountStatus.ACTIVE) {
            throw new IllegalStateException("Only active accounts can be blocked");
        }

        boolean alreadyPending = blockRequestRepository
                .findByAccountAndStatus(account, BlockRequestStatus.PENDING)
                .isPresent();
        if (alreadyPending) {
            throw new IllegalStateException("A block request for this account is already pending");
        }

        account.setStatus(BankAccountStatus.PENDING_BLOCK);
        bankAccountRepository.save(account);

        blockRequestRepository.save(BlockRequest.builder()
                .user(user)
                .account(account)
                .reason(request.getReason())
                .status(BlockRequestStatus.PENDING)
                .build());

        operationService.record(user.getEmail(), Role.CUSTOMER,
                account.getAccountNumber(), OperationType.ACCOUNT_BLOCK_REQUESTED,
                OperationSeverity.WARNING,
                "Customer requested block for account " + account.getName() +
                        ". Reason: " + request.getReason());

        return ActionResponse.of("Block request submitted successfully.");
    }

    public DownloadFileResponse downloadStatement(User user, UUID accountId) {
        BankAccount account = bankAccountRepository.findByIdAndOwner(accountId, user)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        List<Transaction> transactions = transactionRepository
                .findByOwnerAndAccountOrderByCreatedAtDesc(user, account);

        String csv = buildCsv(
                new String[]{"Date", "Title", "Description", "Amount", "Currency", "Direction", "Status", "Counterparty", "Reference"},
                transactions.stream().map(t -> new String[]{
                        t.getCreatedAt().toString(),
                        escapeCsv(t.getTitle()),
                        escapeCsv(t.getDescription() != null ? t.getDescription() : ""),
                        t.getAmount().toPlainString(),
                        t.getCurrency(),
                        t.getDirection().name(),
                        t.getStatus().name(),
                        escapeCsv(t.getCounterparty() != null ? t.getCounterparty() : ""),
                        escapeCsv(t.getReference())
                }).toList()
        );

        operationService.record(user.getEmail(), Role.CUSTOMER,
                account.getAccountNumber(), OperationType.STATEMENT_DOWNLOADED,
                OperationSeverity.INFO, "Account statement downloaded for " + account.getName());

        return DownloadFileResponse.builder()
                .fileName("statement-" + account.getAccountNumber() + ".csv")
                .mimeType("text/csv;charset=utf-8")
                .content(csv)
                .build();
    }

    public DownloadFileResponse downloadHistory(User user, UUID accountId) {
        List<Transaction> transactions;
        String fileName;

        if (accountId != null) {
            BankAccount account = bankAccountRepository.findByIdAndOwner(accountId, user)
                    .orElseThrow(() -> new EntityNotFoundException("Account not found"));
            transactions = transactionRepository.findByOwnerAndAccountOrderByCreatedAtDesc(user, account);
            fileName = "history-" + account.getAccountNumber() + ".csv";
        } else {
            transactions = transactionRepository.findByOwnerOrderByCreatedAtDesc(user);
            fileName = "history-all.csv";
        }

        String csv = buildCsv(
                new String[]{"Date", "Account", "Type", "Title", "Amount", "Currency", "Direction", "Status", "Reference"},
                transactions.stream().map(t -> new String[]{
                        t.getCreatedAt().toString(),
                        escapeCsv(t.getAccountName()),
                        t.getType().name(),
                        escapeCsv(t.getTitle()),
                        t.getAmount().toPlainString(),
                        t.getCurrency(),
                        t.getDirection().name(),
                        t.getStatus().name(),
                        escapeCsv(t.getReference())
                }).toList()
        );

        operationService.record(user.getEmail(), Role.CUSTOMER,
                user.getEmail(), OperationType.HISTORY_DOWNLOADED,
                OperationSeverity.INFO, "Transaction history downloaded");

        return DownloadFileResponse.builder()
                .fileName(fileName)
                .mimeType("text/csv;charset=utf-8")
                .content(csv)
                .build();
    }

    private String buildCsv(String[] headers, List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", headers)).append("\n");
        for (String[] row : rows) {
            sb.append(String.join(",", row)).append("\n");
        }
        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
