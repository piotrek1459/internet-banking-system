package com.example.banking.repository;

import com.example.banking.model.AccountStatusEntity;
import com.example.banking.model.RoleEntity;
import com.example.banking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByRole(RoleEntity role);
    List<User> findByRole(RoleEntity role);
    long countByRole(RoleEntity role);
    long countByAccountStatus(AccountStatusEntity accountStatus);
}
