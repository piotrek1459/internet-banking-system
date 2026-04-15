package com.example.banking.service;

import com.example.banking.dto.BlockRequestSubmitRequest;
import com.example.banking.dto.PaymentRequest;
import com.example.banking.dto.TransferRequest;
import com.example.banking.model.*;
import com.example.banking.repository.BankAccountRepository;
import com.example.banking.repository.BlockRequestRepository;
import com.example.banking.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock BankAccountRepository bankAccountRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock BlockRequestRepository blockRequestRepository;
    @Mock OperationService operationService;

    private CustomerService customerService;
    private User customer;
    private BankAccount activeAccount;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(bankAccountRepository, transactionRepository,
                blockRequestRepository, operationService);

        customer = User.builder()
                .id(UUID.randomUUID())
                .email("alice@bank.local")
                .firstName("Alice")
                .lastName("Murphy")
                .role(Role.CUSTOMER)
                .accountStatus(AccountStatus.ACTIVE)
                .failedLoginAttempts(0)
                .enabled(true)
                .build();

        activeAccount = BankAccount.builder()
                .id(UUID.randomUUID())
                .accountNumber("PL10105000997603123456789123")
                .iban("IE29AIBK93115212341234")
                .name("Everyday Account")
                .type("Current")
                .owner(customer)
                .currency("EUR")
                .balance(new BigDecimal("1000.00"))
                .status(BankAccountStatus.ACTIVE)
                .build();
    }

    @Test
    void submitTransfer_throwsWhenAccountNotFound() {
        when(bankAccountRepository.findByIdAndOwner(any(), any())).thenReturn(Optional.empty());

        TransferRequest req = new TransferRequest();
        req.setSourceAccountId(UUID.randomUUID());
        req.setRecipientName("Bob");
        req.setRecipientAccountNumber("PL99");
        req.setAmount(new BigDecimal("100.00"));
        req.setDescription("Test");

        assertThatThrownBy(() -> customerService.submitTransfer(customer, req))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void submitTransfer_throwsWhenInsufficientBalance() {
        when(bankAccountRepository.findByIdAndOwner(any(), any())).thenReturn(Optional.of(activeAccount));

        TransferRequest req = new TransferRequest();
        req.setSourceAccountId(activeAccount.getId());
        req.setRecipientName("Bob");
        req.setRecipientAccountNumber("PL99");
        req.setAmount(new BigDecimal("9999.00"));
        req.setDescription("Too much");

        assertThatThrownBy(() -> customerService.submitTransfer(customer, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient");
    }

    @Test
    void submitTransfer_throwsWhenAccountNotActive() {
        activeAccount.setStatus(BankAccountStatus.BLOCKED);
        when(bankAccountRepository.findByIdAndOwner(any(), any())).thenReturn(Optional.of(activeAccount));

        TransferRequest req = new TransferRequest();
        req.setSourceAccountId(activeAccount.getId());
        req.setRecipientName("Bob");
        req.setRecipientAccountNumber("PL99");
        req.setAmount(new BigDecimal("100.00"));
        req.setDescription("Test");

        assertThatThrownBy(() -> customerService.submitTransfer(customer, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active");
    }

    @Test
    void submitTransfer_deductsBalanceAndCreatesTransaction() {
        when(bankAccountRepository.findByIdAndOwner(any(), any())).thenReturn(Optional.of(activeAccount));
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bankAccountRepository.findByAccountNumber(anyString())).thenReturn(Optional.empty());

        TransferRequest req = new TransferRequest();
        req.setSourceAccountId(activeAccount.getId());
        req.setRecipientName("Bob");
        req.setRecipientAccountNumber("EXTERNAL-ACC");
        req.setAmount(new BigDecimal("250.00"));
        req.setDescription("Test transfer");

        customerService.submitTransfer(customer, req);

        assertThat(activeAccount.getBalance()).isEqualByComparingTo("750.00");
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void submitTransfer_creditsBothSidesForInternalTransfer() {
        BankAccount target = BankAccount.builder()
                .id(UUID.randomUUID())
                .accountNumber("INTERNAL-ACC")
                .name("Target Account")
                .type("Current")
                .owner(customer)
                .currency("EUR")
                .balance(new BigDecimal("500.00"))
                .status(BankAccountStatus.ACTIVE)
                .build();

        when(bankAccountRepository.findByIdAndOwner(any(), any())).thenReturn(Optional.of(activeAccount));
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bankAccountRepository.findByAccountNumber("INTERNAL-ACC")).thenReturn(Optional.of(target));

        TransferRequest req = new TransferRequest();
        req.setSourceAccountId(activeAccount.getId());
        req.setRecipientName("Alice");
        req.setRecipientAccountNumber("INTERNAL-ACC");
        req.setAmount(new BigDecimal("200.00"));
        req.setDescription("Internal");

        customerService.submitTransfer(customer, req);

        assertThat(activeAccount.getBalance()).isEqualByComparingTo("800.00");
        assertThat(target.getBalance()).isEqualByComparingTo("700.00");
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void submitPayment_throwsWhenInsufficientBalance() {
        when(bankAccountRepository.findByIdAndOwner(any(), any())).thenReturn(Optional.of(activeAccount));

        PaymentRequest req = new PaymentRequest();
        req.setSourceAccountId(activeAccount.getId());
        req.setPayeeName("Electric Co");
        req.setReference("ELEC001");
        req.setAmount(new BigDecimal("5000.00"));

        assertThatThrownBy(() -> customerService.submitPayment(customer, req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requestBlock_setsAccountToPendingBlock() {
        when(bankAccountRepository.findByIdAndOwner(any(), any())).thenReturn(Optional.of(activeAccount));
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(blockRequestRepository.findByAccountAndStatus(any(), any())).thenReturn(Optional.empty());
        when(blockRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BlockRequestSubmitRequest req = new BlockRequestSubmitRequest();
        req.setReason("Suspicious activity");

        customerService.requestBlock(customer, activeAccount.getId(), req);

        assertThat(activeAccount.getStatus()).isEqualTo(BankAccountStatus.PENDING_BLOCK);
        verify(blockRequestRepository).save(any(BlockRequest.class));
    }

    @Test
    void requestBlock_throwsWhenAlreadyPending() {
        when(bankAccountRepository.findByIdAndOwner(any(), any())).thenReturn(Optional.of(activeAccount));
        when(blockRequestRepository.findByAccountAndStatus(any(), any()))
                .thenReturn(Optional.of(new BlockRequest()));

        BlockRequestSubmitRequest req = new BlockRequestSubmitRequest();
        req.setReason("Again");

        assertThatThrownBy(() -> customerService.requestBlock(customer, activeAccount.getId(), req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pending");
    }
}
