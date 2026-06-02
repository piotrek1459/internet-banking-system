package com.example.banking.repository;

import com.example.banking.model.OperationSeverityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface OperationSeverityEntityRepository extends JpaRepository<OperationSeverityEntity, UUID> {
    Optional<OperationSeverityEntity> findByCode(String code);
}
