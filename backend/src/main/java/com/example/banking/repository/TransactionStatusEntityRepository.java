package com.example.banking.repository;

import com.example.banking.model.TransactionStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TransactionStatusEntityRepository extends JpaRepository<TransactionStatusEntity, UUID> {
    Optional<TransactionStatusEntity> findByCode(String code);
}
