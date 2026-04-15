package com.example.banking.repository;

import com.example.banking.model.BankAccount;
import com.example.banking.model.Transaction;
import com.example.banking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByOwnerOrderByCreatedAtDesc(User owner);
    List<Transaction> findTop6ByOwnerOrderByCreatedAtDesc(User owner);
    List<Transaction> findByOwnerAndAccountOrderByCreatedAtDesc(User owner, BankAccount account);
}
