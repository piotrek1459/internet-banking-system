package com.example.banking.repository;

import com.example.banking.model.AccountStatus;
import com.example.banking.model.Role;
import com.example.banking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByRole(Role role);
    List<User> findByRole(Role role);
    long countByRole(Role role);
    long countByAccountStatus(AccountStatus accountStatus);
}
