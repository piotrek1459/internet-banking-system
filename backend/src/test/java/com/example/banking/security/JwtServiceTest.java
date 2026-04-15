package com.example.banking.security;

import com.example.banking.model.AccountStatus;
import com.example.banking.model.Role;
import com.example.banking.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("test-secret-must-be-at-least-32-chars-long!!");
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@bank.local")
                .role(Role.CUSTOMER)
                .accountStatus(AccountStatus.ACTIVE)
                .firstName("Test")
                .lastName("User")
                .failedLoginAttempts(0)
                .enabled(true)
                .build();
    }

    @Test
    void generateToken_producesValidToken() {
        String token = jwtService.generateToken(testUser);
        assertThat(token).isNotBlank();
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void extractSubject_returnsUserEmail() {
        String token = jwtService.generateToken(testUser);
        assertThat(jwtService.extractSubject(token)).isEqualTo("test@bank.local");
    }

    @Test
    void extractRole_returnsUserRole() {
        String token = jwtService.generateToken(testUser);
        assertThat(jwtService.extractRole(token)).isEqualTo("CUSTOMER");
    }

    @Test
    void isTokenValid_returnsFalseForTamperedToken() {
        String token = jwtService.generateToken(testUser);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForEmptyString() {
        assertThat(jwtService.isTokenValid("")).isFalse();
    }
}
