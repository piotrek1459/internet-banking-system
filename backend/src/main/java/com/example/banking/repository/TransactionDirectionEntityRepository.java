package com.example.banking.repository;

import com.example.banking.model.TransactionDirectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TransactionDirectionEntityRepository extends JpaRepository<TransactionDirectionEntity, UUID> {
    Optional<TransactionDirectionEntity> findByCode(String code);
}
