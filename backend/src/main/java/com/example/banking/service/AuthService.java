package com.example.banking.service;

import com.example.banking.dto.*;
import com.example.banking.model.*;
import com.example.banking.repository.BankAccountRepository;
import com.example.banking.repository.OtpSessionRepository;
import com.example.banking.repository.UserRepository;
import com.example.banking.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final OtpSessionRepository otpSessionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       OtpSessionRepository otpSessionRepository,
                       BankAccountRepository bankAccountRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.otpSessionRepository = otpSessionRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public void register(RegisterRequest request) {
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

        bankAccountRepository.save(BankAccount.builder()
                .accountNumber("PL" + UUID.randomUUID().toString().replace("-", "").substring(0, 26))
                .owner(user)
                .currency("PLN")
                .balance(BigDecimal.ZERO)
                .status("ACTIVE")
                .build());
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("Invalid credentials"));

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is blocked or locked");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= 3) {
                user.setAccountStatus(AccountStatus.LOCKED_LOGIN_FAILURE);
            }
            userRepository.save(user);
            throw new IllegalArgumentException("Invalid credentials");
        }

        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        String otp = "123456"; // starter placeholder
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
        return AuthResponse.builder()
                .token(jwtService.generateToken(user))
                .user(UserDto.from(user))
                .build();
    }

    public List<AccountSummaryDto> findAccountsByUserEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return bankAccountRepository.findByOwner(user).stream().map(AccountSummaryDto::from).toList();
    }
}
