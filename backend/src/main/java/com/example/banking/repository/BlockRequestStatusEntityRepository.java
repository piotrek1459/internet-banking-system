package com.example.banking.repository;

import com.example.banking.model.BlockRequestStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface BlockRequestStatusEntityRepository extends JpaRepository<BlockRequestStatusEntity, UUID> {
    Optional<BlockRequestStatusEntity> findByCode(String code);
}
