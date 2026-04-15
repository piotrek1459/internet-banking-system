package com.example.banking.service;

import com.example.banking.model.*;
import com.example.banking.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock UserRepository userRepository;
    @Mock BankAccountRepository bankAccountRepository;
    @Mock BlockRequestRepository blockRequestRepository;
    @Mock OperationRecordRepository operationRecordRepository;
    @Mock OperationService operationService;

    private AdminService adminService;
    private User admin;
    private User customer;
    private BankAccount account;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(userRepository, bankAccountRepository,
                blockRequestRepository, operationRecordRepository, operationService);

        admin = User.builder()
                .id(UUID.randomUUID())
                .email("admin@bank.local")
                .firstName("System")
                .lastName("Administrator")
                .role(Role.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        customer = User.builder()
                .id(UUID.randomUUID())
                .email("alice@bank.local")
                .firstName("Alice")
                .lastName("Murphy")
                .role(Role.CUSTOMER)
                .accountStatus(AccountStatus.LOCKED_LOGIN_FAILURE)
                .failedLoginAttempts(3)
                .build();

        account = BankAccount.builder()
                .id(UUID.randomUUID())
                .accountNumber("PL10105000997603123456789123")
                .name("Everyday Account")
                .type("Current")
                .owner(customer)
                .currency("EUR")
                .balance(new BigDecimal("1000.00"))
                .status(BankAccountStatus.ACTIVE)
                .build();
    }

    @Test
    void unlockUser_resetsStatusAndFailedAttempts() {
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        adminService.unlockUser(customer.getId(), admin);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(captor.getValue().getFailedLoginAttempts()).isEqualTo(0);
    }

    @Test
    void unlockUser_throwsWhenUserNotFound() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.unlockUser(UUID.randomUUID(), admin))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void blockAccount_setsAccountToBlocked() {
        when(bankAccountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(blockRequestRepository.findByAccountAndStatus(any(), any())).thenReturn(Optional.empty());

        adminService.blockAccount(account.getId(), admin);

        assertThat(account.getStatus()).isEqualTo(BankAccountStatus.BLOCKED);
    }

    @Test
    void unblockAccount_setsAccountToActive() {
        account.setStatus(BankAccountStatus.BLOCKED);
        when(bankAccountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        adminService.unblockAccount(account.getId(), admin);

        assertThat(account.getStatus()).isEqualTo(BankAccountStatus.ACTIVE);
    }

    @Test
    void approveBlockRequest_blocksAccountAndApprovesRequest() {
        BlockRequest blockRequest = BlockRequest.builder()
                .id(UUID.randomUUID())
                .user(customer)
                .account(account)
                .reason("Suspicious")
                .status(BlockRequestStatus.PENDING)
                .build();

        when(blockRequestRepository.findById(blockRequest.getId())).thenReturn(Optional.of(blockRequest));
        when(blockRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        adminService.approveBlockRequest(blockRequest.getId(), admin);

        assertThat(blockRequest.getStatus()).isEqualTo(BlockRequestStatus.APPROVED);
        assertThat(account.getStatus()).isEqualTo(BankAccountStatus.BLOCKED);
    }

    @Test
    void approveBlockRequest_throwsWhenNotPending() {
        BlockRequest blockRequest = BlockRequest.builder()
                .id(UUID.randomUUID())
                .user(customer)
                .account(account)
                .status(BlockRequestStatus.APPROVED)
                .build();

        when(blockRequestRepository.findById(blockRequest.getId())).thenReturn(Optional.of(blockRequest));

        assertThatThrownBy(() -> adminService.approveBlockRequest(blockRequest.getId(), admin))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void rejectBlockRequest_revertsAccountToPendingBlockToActive() {
        account.setStatus(BankAccountStatus.PENDING_BLOCK);
        BlockRequest blockRequest = BlockRequest.builder()
                .id(UUID.randomUUID())
                .user(customer)
                .account(account)
                .status(BlockRequestStatus.PENDING)
                .build();

        when(blockRequestRepository.findById(blockRequest.getId())).thenReturn(Optional.of(blockRequest));
        when(blockRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        adminService.rejectBlockRequest(blockRequest.getId(), admin);

        assertThat(blockRequest.getStatus()).isEqualTo(BlockRequestStatus.REJECTED);
        assertThat(account.getStatus()).isEqualTo(BankAccountStatus.ACTIVE);
    }
}
