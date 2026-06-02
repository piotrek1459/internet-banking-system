package com.example.banking.repository;

import com.example.banking.model.BankAccountStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface BankAccountStatusEntityRepository extends JpaRepository<BankAccountStatusEntity, UUID> {
    Optional<BankAccountStatusEntity> findByCode(String code);
}
