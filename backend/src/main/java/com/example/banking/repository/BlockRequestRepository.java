package com.example.banking.repository;

import com.example.banking.model.BankAccount;
import com.example.banking.model.BlockRequest;
import com.example.banking.model.BlockRequestStatus;
import com.example.banking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlockRequestRepository extends JpaRepository<BlockRequest, UUID> {
    List<BlockRequest> findByStatus(BlockRequestStatus status);
    List<BlockRequest> findByUserAndStatus(User user, BlockRequestStatus status);
    Optional<BlockRequest> findByAccountAndStatus(BankAccount account, BlockRequestStatus status);
    long countByStatus(BlockRequestStatus status);
    long countByUserAndStatus(User user, BlockRequestStatus status);
}
