package com.example.banking.service;

import com.example.banking.dto.LoginRequest;
import com.example.banking.dto.RegisterRequest;
import com.example.banking.dto.VerifyOtpRequest;
import com.example.banking.model.*;
import com.example.banking.repository.BankAccountRepository;
import com.example.banking.repository.OtpSessionRepository;
import com.example.banking.repository.UserRepository;
import com.example.banking.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock OtpSessionRepository otpSessionRepository;
    @Mock BankAccountRepository bankAccountRepository;
    @Mock JwtService jwtService;
    @Mock OperationService operationService;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, otpSessionRepository, bankAccountRepository,
                passwordEncoder, jwtService, operationService);
    }

    @Test
    void register_savesUserAndAccount() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RegisterRequest req = new RegisterRequest();
        req.setEmail("new@bank.local");
        req.setPassword("Password123!");
        req.setFirstName("Jane");
        req.setLastName("Doe");

        authService.register(req);

        verify(userRepository).save(any(User.class));
        verify(bankAccountRepository).save(any(BankAccount.class));
    }

    @Test
    void register_throwsWhenEmailAlreadyExists() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(new User()));

        RegisterRequest req = new RegisterRequest();
        req.setEmail("existing@bank.local");
        req.setPassword("Password123!");
        req.setFirstName("Jane");
        req.setLastName("Doe");

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void login_throwsWhenUserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        LoginRequest req = new LoginRequest();
        req.setEmail("nobody@bank.local");
        req.setPassword("wrong");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void login_throwsWhenAccountBlocked() {
        User blocked = User.builder()
                .id(UUID.randomUUID())
                .email("blocked@bank.local")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .accountStatus(AccountStatus.LOCKED_LOGIN_FAILURE)
                .failedLoginAttempts(3)
                .enabled(true)
                .role(Role.CUSTOMER)
                .build();
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(blocked));

        LoginRequest req = new LoginRequest();
        req.setEmail("blocked@bank.local");
        req.setPassword("Password123!");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("blocked");
    }

    @Test
    void login_incrementsFailedAttemptsOnWrongPassword() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@bank.local")
                .passwordHash(passwordEncoder.encode("CorrectPassword!"))
                .accountStatus(AccountStatus.ACTIVE)
                .failedLoginAttempts(0)
                .enabled(true)
                .role(Role.CUSTOMER)
                .build();
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoginRequest req = new LoginRequest();
        req.setEmail("user@bank.local");
        req.setPassword("WrongPassword!");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(IllegalArgumentException.class);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    void login_locksAccountAfter3FailedAttempts() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@bank.local")
                .passwordHash(passwordEncoder.encode("CorrectPassword!"))
                .accountStatus(AccountStatus.ACTIVE)
                .failedLoginAttempts(2)
                .enabled(true)
                .role(Role.CUSTOMER)
                .build();
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoginRequest req = new LoginRequest();
        req.setEmail("user@bank.local");
        req.setPassword("WrongPassword!");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(IllegalArgumentException.class);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getAccountStatus()).isEqualTo(AccountStatus.LOCKED_LOGIN_FAILURE);
    }

    @Test
    void verifyOtp_throwsOnExpiredSession() {
        OtpSession session = OtpSession.builder()
                .id(UUID.randomUUID())
                .otpCode("123456")
                .expiresAt(Instant.now().minusSeconds(60))
                .used(false)
                .build();
        when(otpSessionRepository.findByIdAndUsedFalse(any())).thenReturn(Optional.of(session));

        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setOtpSessionId(session.getId());
        req.setOtpCode("123456");

        assertThatThrownBy(() -> authService.verifyOtp(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void verifyOtp_throwsOnWrongCode() {
        OtpSession session = OtpSession.builder()
                .id(UUID.randomUUID())
                .otpCode("123456")
                .expiresAt(Instant.now().plusSeconds(300))
                .used(false)
                .build();
        when(otpSessionRepository.findByIdAndUsedFalse(any())).thenReturn(Optional.of(session));

        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setOtpSessionId(session.getId());
        req.setOtpCode("999999");

        assertThatThrownBy(() -> authService.verifyOtp(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid OTP");
    }

    @Test
    void verifyOtp_successSetsLastLoginAt() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@bank.local")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .accountStatus(AccountStatus.ACTIVE)
                .failedLoginAttempts(0)
                .enabled(true)
                .role(Role.CUSTOMER)
                .firstName("Jane")
                .lastName("Doe")
                .build();

        OtpSession session = OtpSession.builder()
                .id(UUID.randomUUID())
                .otpCode("654321")
                .expiresAt(Instant.now().plusSeconds(300))
                .used(false)
                .user(user)
                .build();

        when(otpSessionRepository.findByIdAndUsedFalse(any())).thenReturn(Optional.of(session));
        when(otpSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("mock.jwt.token");

        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setOtpSessionId(session.getId());
        req.setOtpCode("654321");

        var response = authService.verifyOtp(req);

        assertThat(response.getToken()).isEqualTo("mock.jwt.token");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getLastLoginAt()).isNotNull();
    }
}
