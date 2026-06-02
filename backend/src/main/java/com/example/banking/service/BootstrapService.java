package com.example.banking.service;

import com.example.banking.model.*;
import com.example.banking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class BootstrapService implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final BlockRequestRepository blockRequestRepository;
    private final OperationRecordRepository operationRecordRepository;
    private final PasswordEncoder passwordEncoder;

    private final RoleEntityRepository roleEntityRepository;
    private final AccountStatusEntityRepository accountStatusEntityRepository;
    private final BankAccountStatusEntityRepository bankAccountStatusEntityRepository;
    private final TransactionTypeEntityRepository transactionTypeEntityRepository;
    private final TransactionDirectionEntityRepository transactionDirectionEntityRepository;
    private final TransactionStatusEntityRepository transactionStatusEntityRepository;
    private final BlockRequestStatusEntityRepository blockRequestStatusEntityRepository;
    private final OperationTypeEntityRepository operationTypeEntityRepository;
    private final OperationSeverityEntityRepository operationSeverityEntityRepository;

    @Value("${app.admin-email}")
    private String adminEmail;

    @Value("${app.admin-password}")
    private String adminPassword;

    @Value("${app.seed-demo-data:true}")
    private boolean seedDemoData;

    @Override
    @Transactional
    public void run(String... args) {
        seedLookupTables();
        seedAdmin();
        if (seedDemoData) {
            seedAlice();
            seedBrian();
            seedLockedCustomer();
        }
    }

    private <T> T upsert(org.springframework.data.jpa.repository.JpaRepository<T, ?> repo,
                         java.util.function.Supplier<java.util.Optional<T>> finder,
                         java.util.function.Supplier<T> creator) {
        return finder.get().orElseGet(() -> repo.save(creator.get()));
    }

    private RoleEntity role(Role r) {
        return upsert(roleEntityRepository,
                () -> roleEntityRepository.findByCode(r.name()),
                () -> RoleEntity.builder().code(r.name()).label(r.name().charAt(0) + r.name().substring(1).toLowerCase()).build());
    }

    private AccountStatusEntity accountStatus(AccountStatus s) {
        return upsert(accountStatusEntityRepository,
                () -> accountStatusEntityRepository.findByCode(s.name()),
                () -> AccountStatusEntity.builder().code(s.name()).label(s.name().replace("_", " ")).build());
    }

    private BankAccountStatusEntity bankAccountStatus(BankAccountStatus s) {
        return upsert(bankAccountStatusEntityRepository,
                () -> bankAccountStatusEntityRepository.findByCode(s.name()),
                () -> BankAccountStatusEntity.builder().code(s.name()).label(s.name().replace("_", " ")).build());
    }

    private TransactionTypeEntity transactionType(TransactionType t) {
        return upsert(transactionTypeEntityRepository,
                () -> transactionTypeEntityRepository.findByCode(t.name()),
                () -> TransactionTypeEntity.builder().code(t.name()).label(t.name().charAt(0) + t.name().substring(1).toLowerCase()).build());
    }

    private TransactionDirectionEntity transactionDirection(TransactionDirection d) {
        return upsert(transactionDirectionEntityRepository,
                () -> transactionDirectionEntityRepository.findByCode(d.name()),
                () -> TransactionDirectionEntity.builder().code(d.name()).label(d.name().charAt(0) + d.name().substring(1).toLowerCase()).build());
    }

    private TransactionStatusEntity transactionStatus(TransactionStatus s) {
        return upsert(transactionStatusEntityRepository,
                () -> transactionStatusEntityRepository.findByCode(s.name()),
                () -> TransactionStatusEntity.builder().code(s.name()).label(s.name().charAt(0) + s.name().substring(1).toLowerCase()).build());
    }

    private BlockRequestStatusEntity blockRequestStatus(BlockRequestStatus s) {
        return upsert(blockRequestStatusEntityRepository,
                () -> blockRequestStatusEntityRepository.findByCode(s.name()),
                () -> BlockRequestStatusEntity.builder().code(s.name()).label(s.name().charAt(0) + s.name().substring(1).toLowerCase()).build());
    }

    private OperationTypeEntity operationType(OperationType t) {
        return upsert(operationTypeEntityRepository,
                () -> operationTypeEntityRepository.findByCode(t.name()),
                () -> OperationTypeEntity.builder().code(t.name()).label(t.name().replace("_", " ")).build());
    }

    private OperationSeverityEntity operationSeverity(OperationSeverity s) {
        return upsert(operationSeverityEntityRepository,
                () -> operationSeverityEntityRepository.findByCode(s.name()),
                () -> OperationSeverityEntity.builder().code(s.name()).label(s.name().charAt(0) + s.name().substring(1).toLowerCase()).build());
    }

    private void seedLookupTables() {
        for (Role r : Role.values()) role(r);
        for (AccountStatus s : AccountStatus.values()) accountStatus(s);
        for (BankAccountStatus s : BankAccountStatus.values()) bankAccountStatus(s);
        for (TransactionType t : TransactionType.values()) transactionType(t);
        for (TransactionDirection d : TransactionDirection.values()) transactionDirection(d);
        for (TransactionStatus s : TransactionStatus.values()) transactionStatus(s);
        for (BlockRequestStatus s : BlockRequestStatus.values()) blockRequestStatus(s);
        for (OperationType t : OperationType.values()) operationType(t);
        for (OperationSeverity s : OperationSeverity.values()) operationSeverity(s);
    }

    private void seedAdmin() {
        if (!userRepository.existsByRole(role(Role.ADMIN))) {
            userRepository.save(User.builder()
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .firstName("System")
                    .lastName("Administrator")
                    .role(role(Role.ADMIN))
                    .accountStatus(accountStatus(AccountStatus.ACTIVE))
                    .failedLoginAttempts(0)
                    .enabled(true)
                    .build());
        }
    }

    private void seedAlice() {
        if (userRepository.findByEmail("alice.customer@bank.local").isPresent()) return;

        User alice = userRepository.save(User.builder()
                .email("alice.customer@bank.local")
                .passwordHash(passwordEncoder.encode("Customer123!"))
                .firstName("Alice")
                .lastName("Murphy")
                .role(role(Role.CUSTOMER))
                .accountStatus(accountStatus(AccountStatus.ACTIVE))
                .failedLoginAttempts(0)
                .enabled(true)
                .lastLoginAt(Instant.now().minus(2, ChronoUnit.DAYS))
                .build());

        BankAccount everyday = bankAccountRepository.save(BankAccount.builder()
                .accountNumber("PL10105000997603123456789123")
                .iban("IE29AIBK93115212341234")
                .name("Everyday Account")
                .type("Current")
                .owner(alice)
                .currency("EUR")
                .balance(new BigDecimal("8425.18"))
                .status(bankAccountStatus(BankAccountStatus.ACTIVE))
                .build());

        BankAccount savings = bankAccountRepository.save(BankAccount.builder()
                .accountNumber("PL20105000997603123456789456")
                .iban("IE29AIBK93115212341235")
                .name("Savings Vault")
                .type("Savings")
                .owner(alice)
                .currency("EUR")
                .balance(new BigDecimal("16240.00"))
                .status(bankAccountStatus(BankAccountStatus.ACTIVE))
                .build());

        transactionRepository.save(Transaction.builder()
                .owner(alice).account(everyday).accountName(everyday.getName())
                .createdAt(Instant.now().minus(3, ChronoUnit.DAYS))
                .type(transactionType(TransactionType.DEPOSIT))
                .title("Salary — April 2025").description("Monthly salary")
                .amount(new BigDecimal("3500.00")).currency("EUR")
                .direction(transactionDirection(TransactionDirection.CREDIT))
                .status(transactionStatus(TransactionStatus.COMPLETED))
                .counterparty("Employer Ltd.").reference("SAL-202504").build());

        transactionRepository.save(Transaction.builder()
                .owner(alice).account(everyday).accountName(everyday.getName())
                .createdAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .type(transactionType(TransactionType.PAYMENT))
                .title("Payment to Electric Ireland").description("Ref: ELEC-MAR")
                .amount(new BigDecimal("74.82")).currency("EUR")
                .direction(transactionDirection(TransactionDirection.DEBIT))
                .status(transactionStatus(TransactionStatus.COMPLETED))
                .counterparty("Electric Ireland").reference("PAY-ELEC01").build());

        transactionRepository.save(Transaction.builder()
                .owner(alice).account(everyday).accountName(everyday.getName())
                .createdAt(Instant.now().minus(7, ChronoUnit.DAYS))
                .type(transactionType(TransactionType.TRANSFER))
                .title("Transfer to Savings Vault").description("Monthly savings")
                .amount(new BigDecimal("500.00")).currency("EUR")
                .direction(transactionDirection(TransactionDirection.DEBIT))
                .status(transactionStatus(TransactionStatus.COMPLETED))
                .counterparty("Alice Murphy").reference("TXN-INT001").build());

        transactionRepository.save(Transaction.builder()
                .owner(alice).account(savings).accountName(savings.getName())
                .createdAt(Instant.now().minus(7, ChronoUnit.DAYS))
                .type(transactionType(TransactionType.TRANSFER))
                .title("Transfer from Everyday Account").description("Monthly savings")
                .amount(new BigDecimal("500.00")).currency("EUR")
                .direction(transactionDirection(TransactionDirection.CREDIT))
                .status(transactionStatus(TransactionStatus.COMPLETED))
                .counterparty("Alice Murphy").reference("TXN-INT001").build());

        transactionRepository.save(Transaction.builder()
                .owner(alice).account(everyday).accountName(everyday.getName())
                .createdAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .type(transactionType(TransactionType.PAYMENT))
                .title("Payment to AXA Insurance").description("Ref: INS-HOME")
                .amount(new BigDecimal("120.00")).currency("EUR")
                .direction(transactionDirection(TransactionDirection.DEBIT))
                .status(transactionStatus(TransactionStatus.COMPLETED))
                .counterparty("AXA Insurance").reference("PAY-INS001").build());

        operationRecordRepository.save(OperationRecord.builder()
                .actor(alice).actorEmail(alice.getEmail())
                .actorRole(role(Role.CUSTOMER))
                .target(everyday.getAccountNumber())
                .type(operationType(OperationType.PAYMENT_CREATED))
                .severity(operationSeverity(OperationSeverity.SUCCESS))
                .description("Payment of 74.82 EUR to Electric Ireland").build());
    }

    private void seedBrian() {
        if (userRepository.findByEmail("brian.customer@bank.local").isPresent()) return;

        User brian = userRepository.save(User.builder()
                .email("brian.customer@bank.local")
                .passwordHash(passwordEncoder.encode("Customer123!"))
                .firstName("Brian")
                .lastName("Walsh")
                .role(role(Role.CUSTOMER))
                .accountStatus(accountStatus(AccountStatus.ACTIVE))
                .failedLoginAttempts(0)
                .enabled(true)
                .lastLoginAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build());

        BankAccount family = bankAccountRepository.save(BankAccount.builder()
                .accountNumber("PL30105000997603123456789789")
                .iban("IE29AIBK93115212341236")
                .name("Family Account")
                .type("Current")
                .owner(brian)
                .currency("EUR")
                .balance(new BigDecimal("2190.40"))
                .status(bankAccountStatus(BankAccountStatus.PENDING_BLOCK))
                .build());

        transactionRepository.save(Transaction.builder()
                .owner(brian).account(family).accountName(family.getName())
                .createdAt(Instant.now().minus(6, ChronoUnit.DAYS))
                .type(transactionType(TransactionType.PAYMENT))
                .title("Payment to Aviva Life Insurance").description("Ref: LIFE-APR")
                .amount(new BigDecimal("89.50")).currency("EUR")
                .direction(transactionDirection(TransactionDirection.DEBIT))
                .status(transactionStatus(TransactionStatus.COMPLETED))
                .counterparty("Aviva Life Insurance").reference("PAY-AVIVA01").build());

        BlockRequest blockReq = blockRequestRepository.save(BlockRequest.builder()
                .user(brian).account(family)
                .reason("Card and online banking credentials may be compromised.")
                .status(blockRequestStatus(BlockRequestStatus.PENDING))
                .requestedAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build());

        operationRecordRepository.save(OperationRecord.builder()
                .actor(brian).actorEmail(brian.getEmail())
                .actorRole(role(Role.CUSTOMER))
                .target(family.getAccountNumber())
                .type(operationType(OperationType.ACCOUNT_BLOCK_REQUESTED))
                .severity(operationSeverity(OperationSeverity.WARNING))
                .description("Customer requested block for account " + family.getName() +
                        ". Reason: " + blockReq.getReason()).build());
    }

    private void seedLockedCustomer() {
        if (userRepository.findByEmail("locked.customer@bank.local").isPresent()) return;

        User locked = userRepository.save(User.builder()
                .email("locked.customer@bank.local")
                .passwordHash(passwordEncoder.encode("Customer123!"))
                .firstName("Locked")
                .lastName("Customer")
                .role(role(Role.CUSTOMER))
                .accountStatus(accountStatus(AccountStatus.LOCKED_LOGIN_FAILURE))
                .failedLoginAttempts(3)
                .enabled(true)
                .build());

        operationRecordRepository.save(OperationRecord.builder()
                .actor(locked).actorEmail(locked.getEmail())
                .actorRole(role(Role.CUSTOMER))
                .target(locked.getEmail())
                .type(operationType(OperationType.LOGIN_FAILURE))
                .severity(operationSeverity(OperationSeverity.CRITICAL))
                .description("Access blocked after 3 failed login attempts").build());
    }
}
