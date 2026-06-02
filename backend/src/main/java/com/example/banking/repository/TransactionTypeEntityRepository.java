package com.example.banking.repository;

import com.example.banking.model.TransactionTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TransactionTypeEntityRepository extends JpaRepository<TransactionTypeEntity, UUID> {
    Optional<TransactionTypeEntity> findByCode(String code);
}
