package com.example.banking.service;

import com.example.banking.model.AccountStatus;
import com.example.banking.model.Role;
import com.example.banking.model.User;
import com.example.banking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BootstrapService implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public BootstrapService(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            @Value("${app.admin-email}") String adminEmail,
                            @Value("${app.admin-password}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByRole(Role.ADMIN)) {
            userRepository.save(User.builder()
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .firstName("System")
                    .lastName("Admin")
                    .role(Role.ADMIN)
                    .accountStatus(AccountStatus.ACTIVE)
                    .failedLoginAttempts(0)
                    .enabled(true)
                    .build());
        }
    }
}
