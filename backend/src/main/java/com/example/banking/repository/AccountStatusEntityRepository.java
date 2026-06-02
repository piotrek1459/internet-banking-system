package com.example.banking.repository;

import com.example.banking.model.AccountStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AccountStatusEntityRepository extends JpaRepository<AccountStatusEntity, UUID> {
    Optional<AccountStatusEntity> findByCode(String code);
}
