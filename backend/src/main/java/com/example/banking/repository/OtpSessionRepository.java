package com.example.banking.repository;

import com.example.banking.model.OtpSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OtpSessionRepository extends JpaRepository<OtpSession, UUID> {
    Optional<OtpSession> findByIdAndUsedFalse(UUID id);
}
