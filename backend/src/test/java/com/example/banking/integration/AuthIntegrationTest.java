package com.example.banking.integration;

import com.example.banking.model.AccountStatus;
import com.example.banking.model.Role;
import com.example.banking.model.User;
import com.example.banking.repository.OtpSessionRepository;
import com.example.banking.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired OtpSessionRepository otpSessionRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void register_returns201() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"newuser@bank.local","password":"Password123!",
                             "firstName":"New","lastName":"User"}
                            """))
                .andExpect(status().isCreated());
    }

    @Test
    void login_withUnknownEmail_returns404() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"nobody@bank.local","password":"Password123!"}
                            """))
                .andExpect(status().isNotFound());
    }

    @Test
    void login_withWrongPassword_returns400() throws Exception {
        userRepository.save(User.builder()
                .email("testlogin@bank.local")
                .passwordHash(passwordEncoder.encode("CorrectPassword123!"))
                .firstName("Test")
                .lastName("User")
                .role(Role.CUSTOMER)
                .accountStatus(AccountStatus.ACTIVE)
                .failedLoginAttempts(0)
                .enabled(true)
                .build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"testlogin@bank.local","password":"WrongPassword!"}
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_withBlockedAccount_returns423() throws Exception {
        userRepository.save(User.builder()
                .email("blocked.int@bank.local")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Blocked")
                .lastName("User")
                .role(Role.CUSTOMER)
                .accountStatus(AccountStatus.LOCKED_LOGIN_FAILURE)
                .failedLoginAttempts(3)
                .enabled(true)
                .build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"blocked.int@bank.local","password":"Password123!"}
                            """))
                .andExpect(status().isLocked());
    }

    @Test
    void verifyOtp_withInvalidSessionId_returns404() throws Exception {
        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"otpSessionId":"00000000-0000-0000-0000-000000000000","otpCode":"000000"}
                            """))
                .andExpect(status().isNotFound());
    }
}
