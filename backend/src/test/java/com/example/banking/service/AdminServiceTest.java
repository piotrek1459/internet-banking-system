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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock UserRepository userRepository;
    @Mock BankAccountRepository bankAccountRepository;
    @Mock BlockRequestRepository blockRequestRepository;
    @Mock OperationRecordRepository operationRecordRepository;
    @Mock OperationService operationService;
    @Mock RoleEntityRepository roleEntityRepository;
    @Mock AccountStatusEntityRepository accountStatusEntityRepository;
    @Mock BankAccountStatusEntityRepository bankAccountStatusEntityRepository;
    @Mock BlockRequestStatusEntityRepository blockRequestStatusEntityRepository;
    @Mock OperationSeverityEntityRepository operationSeverityEntityRepository;

    private AdminService adminService;
    private User admin;
    private User customer;
    private BankAccount account;

    private static AccountStatusEntity accountStatusEntity(AccountStatus s) {
        return AccountStatusEntity.builder().code(s.name()).label(s.name()).build();
    }

    private static BankAccountStatusEntity bankAccountStatusEntity(BankAccountStatus s) {
        return BankAccountStatusEntity.builder().code(s.name()).label(s.name()).build();
    }

    private static BlockRequestStatusEntity blockRequestStatusEntity(BlockRequestStatus s) {
        return BlockRequestStatusEntity.builder().code(s.name()).label(s.name()).build();
    }

    @BeforeEach
    void setUp() {
        adminService = new AdminService(userRepository, bankAccountRepository,
                blockRequestRepository, operationRecordRepository, operationService,
                roleEntityRepository, accountStatusEntityRepository,
                bankAccountStatusEntityRepository, blockRequestStatusEntityRepository,
                operationSeverityEntityRepository);

        admin = User.builder()
                .id(UUID.randomUUID())
                .email("admin@bank.local")
                .firstName("System")
                .lastName("Administrator")
                .role(RoleEntity.builder().code(Role.ADMIN.name()).label("Admin").build())
                .accountStatus(accountStatusEntity(AccountStatus.ACTIVE))
                .build();

        customer = User.builder()
                .id(UUID.randomUUID())
                .email("alice@bank.local")
                .firstName("Alice")
                .lastName("Murphy")
                .role(RoleEntity.builder().code(Role.CUSTOMER.name()).label("Customer").build())
                .accountStatus(accountStatusEntity(AccountStatus.LOCKED_LOGIN_FAILURE))
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
                .status(bankAccountStatusEntity(BankAccountStatus.ACTIVE))
                .build();
    }

    @Test
    void unlockUser_resetsStatusAndFailedAttempts() {
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(accountStatusEntityRepository.findByCode(AccountStatus.ACTIVE.name()))
                .thenReturn(Optional.of(accountStatusEntity(AccountStatus.ACTIVE)));

        adminService.unlockUser(customer.getId(), admin);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getAccountStatus().getCode()).isEqualTo(AccountStatus.ACTIVE.name());
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
        when(bankAccountStatusEntityRepository.findByCode(BankAccountStatus.BLOCKED.name()))
                .thenReturn(Optional.of(bankAccountStatusEntity(BankAccountStatus.BLOCKED)));
        when(blockRequestStatusEntityRepository.findByCode(BlockRequestStatus.PENDING.name()))
                .thenReturn(Optional.of(blockRequestStatusEntity(BlockRequestStatus.PENDING)));
        when(blockRequestStatusEntityRepository.findByCode(BlockRequestStatus.APPROVED.name()))
                .thenReturn(Optional.of(blockRequestStatusEntity(BlockRequestStatus.APPROVED)));
        when(blockRequestRepository.findByAccountAndStatus(any(), any())).thenReturn(Optional.empty());

        adminService.blockAccount(account.getId(), admin);

        assertThat(account.getStatus().getCode()).isEqualTo(BankAccountStatus.BLOCKED.name());
    }

    @Test
    void unblockAccount_setsAccountToActive() {
        account.setStatus(bankAccountStatusEntity(BankAccountStatus.BLOCKED));
        when(bankAccountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bankAccountStatusEntityRepository.findByCode(BankAccountStatus.ACTIVE.name()))
                .thenReturn(Optional.of(bankAccountStatusEntity(BankAccountStatus.ACTIVE)));

        adminService.unblockAccount(account.getId(), admin);

        assertThat(account.getStatus().getCode()).isEqualTo(BankAccountStatus.ACTIVE.name());
    }

    @Test
    void approveBlockRequest_blocksAccountAndApprovesRequest() {
        BlockRequest blockRequest = BlockRequest.builder()
                .id(UUID.randomUUID())
                .user(customer)
                .account(account)
                .reason("Suspicious")
                .status(blockRequestStatusEntity(BlockRequestStatus.PENDING))
                .build();

        when(blockRequestRepository.findById(blockRequest.getId())).thenReturn(Optional.of(blockRequest));
        when(blockRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(blockRequestStatusEntityRepository.findByCode(BlockRequestStatus.PENDING.name()))
                .thenReturn(Optional.of(blockRequestStatusEntity(BlockRequestStatus.PENDING)));
        when(blockRequestStatusEntityRepository.findByCode(BlockRequestStatus.APPROVED.name()))
                .thenReturn(Optional.of(blockRequestStatusEntity(BlockRequestStatus.APPROVED)));
        when(bankAccountStatusEntityRepository.findByCode(BankAccountStatus.BLOCKED.name()))
                .thenReturn(Optional.of(bankAccountStatusEntity(BankAccountStatus.BLOCKED)));

        adminService.approveBlockRequest(blockRequest.getId(), admin);

        assertThat(blockRequest.getStatus().getCode()).isEqualTo(BlockRequestStatus.APPROVED.name());
        assertThat(account.getStatus().getCode()).isEqualTo(BankAccountStatus.BLOCKED.name());
    }

    @Test
    void approveBlockRequest_throwsWhenNotPending() {
        BlockRequest blockRequest = BlockRequest.builder()
                .id(UUID.randomUUID())
                .user(customer)
                .account(account)
                .status(blockRequestStatusEntity(BlockRequestStatus.APPROVED))
                .build();

        when(blockRequestRepository.findById(blockRequest.getId())).thenReturn(Optional.of(blockRequest));
        when(blockRequestStatusEntityRepository.findByCode(BlockRequestStatus.PENDING.name()))
                .thenReturn(Optional.of(blockRequestStatusEntity(BlockRequestStatus.PENDING)));

        assertThatThrownBy(() -> adminService.approveBlockRequest(blockRequest.getId(), admin))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void rejectBlockRequest_revertsAccountToPendingBlockToActive() {
        account.setStatus(bankAccountStatusEntity(BankAccountStatus.PENDING_BLOCK));
        BlockRequest blockRequest = BlockRequest.builder()
                .id(UUID.randomUUID())
                .user(customer)
                .account(account)
                .status(blockRequestStatusEntity(BlockRequestStatus.PENDING))
                .build();

        when(blockRequestRepository.findById(blockRequest.getId())).thenReturn(Optional.of(blockRequest));
        when(blockRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(blockRequestStatusEntityRepository.findByCode(BlockRequestStatus.REJECTED.name()))
                .thenReturn(Optional.of(blockRequestStatusEntity(BlockRequestStatus.REJECTED)));
        when(bankAccountStatusEntityRepository.findByCode(BankAccountStatus.ACTIVE.name()))
                .thenReturn(Optional.of(bankAccountStatusEntity(BankAccountStatus.ACTIVE)));

        adminService.rejectBlockRequest(blockRequest.getId(), admin);

        assertThat(blockRequest.getStatus().getCode()).isEqualTo(BlockRequestStatus.REJECTED.name());
        assertThat(account.getStatus().getCode()).isEqualTo(BankAccountStatus.ACTIVE.name());
    }
}
