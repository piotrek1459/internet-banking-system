package com.example.banking.repository;

import com.example.banking.model.OperationTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface OperationTypeEntityRepository extends JpaRepository<OperationTypeEntity, UUID> {
    Optional<OperationTypeEntity> findByCode(String code);
}
