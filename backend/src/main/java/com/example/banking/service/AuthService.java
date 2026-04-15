package com.example.banking.service;

import com.example.banking.dto.*;
import com.example.banking.model.*;
import com.example.banking.repository.BankAccountRepository;
import com.example.banking.repository.OtpSessionRepository;
import com.example.banking.repository.UserRepository;
import com.example.banking.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OtpSessionRepository otpSessionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OperationService operationService;

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(Role.CUSTOMER)
                .accountStatus(AccountStatus.ACTIVE)
                .failedLoginAttempts(0)
                .enabled(true)
                .build();
        userRepository.save(user);

        String accountNumber = "PL" + UUID.randomUUID().toString().replace("-", "").substring(0, 26).toUpperCase();
        String iban = "PL" + String.format("%026d", Math.abs(accountNumber.hashCode() % (long) 1e26));

        bankAccountRepository.save(BankAccount.builder()
                .accountNumber(accountNumber)
                .iban(iban)
                .name("Current Account")
                .type("Current")
                .owner(user)
                .currency("EUR")
                .balance(BigDecimal.ZERO)
                .status(BankAccountStatus.ACTIVE)
                .build());

        operationService.record(user.getEmail(), Role.CUSTOMER,
                user.getEmail(), OperationType.CUSTOMER_REGISTERED,
                OperationSeverity.INFO,
                "New customer registered: " + user.getFirstName() + " " + user.getLastName());
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("Invalid credentials"));

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is blocked or locked");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            boolean nowLocked = attempts >= 3;
            if (nowLocked) {
                user.setAccountStatus(AccountStatus.LOCKED_LOGIN_FAILURE);
            }
            userRepository.save(user);

            operationService.record(user.getEmail(), Role.CUSTOMER,
                    user.getEmail(), OperationType.LOGIN_FAILURE,
                    nowLocked ? OperationSeverity.CRITICAL : OperationSeverity.WARNING,
                    nowLocked
                            ? "Access blocked after 3 failed login attempts"
                            : "Failed login attempt " + attempts + "/3");

            throw new IllegalArgumentException("Invalid credentials");
        }

        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        String otp = String.format("%06d", new Random().nextInt(1_000_000));
        log.info("[DEV] OTP for {}: {}", user.getEmail(), otp);

        OtpSession session = otpSessionRepository.save(OtpSession.builder()
                .user(user)
                .otpCode(otp)
                .expiresAt(Instant.now().plusSeconds(300))
                .used(false)
                .build());

        return LoginResponse.builder()
                .status("OTP_REQUIRED")
                .message("OTP sent to email")
                .otpSessionId(session.getId())
                .build();
    }

    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        OtpSession session = otpSessionRepository.findByIdAndUsedFalse(request.getOtpSessionId())
                .orElseThrow(() -> new EntityNotFoundException("OTP session not found"));

        if (session.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalStateException("OTP expired");
        }
        if (!session.getOtpCode().equals(request.getOtpCode())) {
            throw new IllegalArgumentException("Invalid OTP code");
        }

        session.setUsed(true);
        otpSessionRepository.save(session);

        User user = session.getUser();
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        operationService.record(user.getEmail(), Role.CUSTOMER,
                user.getEmail(), OperationType.LOGIN_SUCCESS,
                OperationSeverity.SUCCESS,
                "User logged in successfully");

        return AuthResponse.builder()
                .token(jwtService.generateToken(user))
                .user(UserDto.from(user))
                .build();
    }
}
