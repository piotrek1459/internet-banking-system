package com.example.banking.repository;

import com.example.banking.model.BankAccount;
import com.example.banking.model.BlockRequest;
import com.example.banking.model.BlockRequestStatusEntity;
import com.example.banking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlockRequestRepository extends JpaRepository<BlockRequest, UUID> {
    List<BlockRequest> findByStatus(BlockRequestStatusEntity status);
    List<BlockRequest> findByUserAndStatus(User user, BlockRequestStatusEntity status);
    Optional<BlockRequest> findByAccountAndStatus(BankAccount account, BlockRequestStatusEntity status);
    long countByStatus(BlockRequestStatusEntity status);
    long countByUserAndStatus(User user, BlockRequestStatusEntity status);
}
