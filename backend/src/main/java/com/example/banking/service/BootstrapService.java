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

    @Value("${app.admin-email}")
    private String adminEmail;

    @Value("${app.admin-password}")
    private String adminPassword;

    @Value("${app.seed-demo-data:true}")
    private boolean seedDemoData;

    @Override
    @Transactional
    public void run(String... args) {
        seedAdmin();
        if (seedDemoData) {
            seedAlice();
            seedBrian();
            seedLockedCustomer();
        }
    }

    private void seedAdmin() {
        if (!userRepository.existsByRole(Role.ADMIN)) {
            userRepository.save(User.builder()
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .firstName("System")
                    .lastName("Administrator")
                    .role(Role.ADMIN)
                    .accountStatus(AccountStatus.ACTIVE)
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
                .role(Role.CUSTOMER)
                .accountStatus(AccountStatus.ACTIVE)
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
                .status(BankAccountStatus.ACTIVE)
                .build());

        BankAccount savings = bankAccountRepository.save(BankAccount.builder()
                .accountNumber("PL20105000997603123456789456")
                .iban("IE29AIBK93115212341235")
                .name("Savings Vault")
                .type("Savings")
                .owner(alice)
                .currency("EUR")
                .balance(new BigDecimal("16240.00"))
                .status(BankAccountStatus.ACTIVE)
                .build());

        // Salary deposit
        transactionRepository.save(Transaction.builder()
                .owner(alice)
                .account(everyday)
                .accountName(everyday.getName())
                .createdAt(Instant.now().minus(3, ChronoUnit.DAYS))
                .type(TransactionType.DEPOSIT)
                .title("Salary — April 2025")
                .description("Monthly salary")
                .amount(new BigDecimal("3500.00"))
                .currency("EUR")
                .direction(TransactionDirection.CREDIT)
                .status(TransactionStatus.COMPLETED)
                .counterparty("Employer Ltd.")
                .reference("SAL-202504")
                .build());

        // Electricity bill
        transactionRepository.save(Transaction.builder()
                .owner(alice)
                .account(everyday)
                .accountName(everyday.getName())
                .createdAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .type(TransactionType.PAYMENT)
                .title("Payment to Electric Ireland")
                .description("Ref: ELEC-MAR")
                .amount(new BigDecimal("74.82"))
                .currency("EUR")
                .direction(TransactionDirection.DEBIT)
                .status(TransactionStatus.COMPLETED)
                .counterparty("Electric Ireland")
                .reference("PAY-ELEC01")
                .build());

        // Internal transfer from everyday to savings
        transactionRepository.save(Transaction.builder()
                .owner(alice)
                .account(everyday)
                .accountName(everyday.getName())
                .createdAt(Instant.now().minus(7, ChronoUnit.DAYS))
                .type(TransactionType.TRANSFER)
                .title("Transfer to Savings Vault")
                .description("Monthly savings")
                .amount(new BigDecimal("500.00"))
                .currency("EUR")
                .direction(TransactionDirection.DEBIT)
                .status(TransactionStatus.COMPLETED)
                .counterparty("Alice Murphy")
                .reference("TXN-INT001")
                .build());

        transactionRepository.save(Transaction.builder()
                .owner(alice)
                .account(savings)
                .accountName(savings.getName())
                .createdAt(Instant.now().minus(7, ChronoUnit.DAYS))
                .type(TransactionType.TRANSFER)
                .title("Transfer from Everyday Account")
                .description("Monthly savings")
                .amount(new BigDecimal("500.00"))
                .currency("EUR")
                .direction(TransactionDirection.CREDIT)
                .status(TransactionStatus.COMPLETED)
                .counterparty("Alice Murphy")
                .reference("TXN-INT001")
                .build());

        // Insurance payment
        transactionRepository.save(Transaction.builder()
                .owner(alice)
                .account(everyday)
                .accountName(everyday.getName())
                .createdAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .type(TransactionType.PAYMENT)
                .title("Payment to AXA Insurance")
                .description("Ref: INS-HOME")
                .amount(new BigDecimal("120.00"))
                .currency("EUR")
                .direction(TransactionDirection.DEBIT)
                .status(TransactionStatus.COMPLETED)
                .counterparty("AXA Insurance")
                .reference("PAY-INS001")
                .build());

        // Seed operation record
        operationRecordRepository.save(OperationRecord.builder()
                .actor(alice)
                .actorEmail(alice.getEmail())
                .actorRole(Role.CUSTOMER)
                .target(everyday.getAccountNumber())
                .type(OperationType.PAYMENT_CREATED)
                .severity(OperationSeverity.SUCCESS)
                .description("Payment of 74.82 EUR to Electric Ireland")
                .build());
    }

    private void seedBrian() {
        if (userRepository.findByEmail("brian.customer@bank.local").isPresent()) return;

        User brian = userRepository.save(User.builder()
                .email("brian.customer@bank.local")
                .passwordHash(passwordEncoder.encode("Customer123!"))
                .firstName("Brian")
                .lastName("Walsh")
                .role(Role.CUSTOMER)
                .accountStatus(AccountStatus.ACTIVE)
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
                .status(BankAccountStatus.PENDING_BLOCK)
                .build());

        transactionRepository.save(Transaction.builder()
                .owner(brian)
                .account(family)
                .accountName(family.getName())
                .createdAt(Instant.now().minus(6, ChronoUnit.DAYS))
                .type(TransactionType.PAYMENT)
                .title("Payment to Aviva Life Insurance")
                .description("Ref: LIFE-APR")
                .amount(new BigDecimal("89.50"))
                .currency("EUR")
                .direction(TransactionDirection.DEBIT)
                .status(TransactionStatus.COMPLETED)
                .counterparty("Aviva Life Insurance")
                .reference("PAY-AVIVA01")
                .build());

        BlockRequest blockReq = blockRequestRepository.save(BlockRequest.builder()
                .user(brian)
                .account(family)
                .reason("Card and online banking credentials may be compromised.")
                .status(BlockRequestStatus.PENDING)
                .requestedAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build());

        operationRecordRepository.save(OperationRecord.builder()
                .actor(brian)
                .actorEmail(brian.getEmail())
                .actorRole(Role.CUSTOMER)
                .target(family.getAccountNumber())
                .type(OperationType.ACCOUNT_BLOCK_REQUESTED)
                .severity(OperationSeverity.WARNING)
                .description("Customer requested block for account " + family.getName() +
                        ". Reason: " + blockReq.getReason())
                .build());
    }

    private void seedLockedCustomer() {
        if (userRepository.findByEmail("locked.customer@bank.local").isPresent()) return;

        User locked = userRepository.save(User.builder()
                .email("locked.customer@bank.local")
                .passwordHash(passwordEncoder.encode("Customer123!"))
                .firstName("Locked")
                .lastName("Customer")
                .role(Role.CUSTOMER)
                .accountStatus(AccountStatus.LOCKED_LOGIN_FAILURE)
                .failedLoginAttempts(3)
                .enabled(true)
                .build());

        operationRecordRepository.save(OperationRecord.builder()
                .actor(locked)
                .actorEmail(locked.getEmail())
                .actorRole(Role.CUSTOMER)
                .target(locked.getEmail())
                .type(OperationType.LOGIN_FAILURE)
                .severity(OperationSeverity.CRITICAL)
                .description("Access blocked after 3 failed login attempts")
                .build());
    }
}
