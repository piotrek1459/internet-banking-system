package com.example.banking.repository;

import com.example.banking.model.BankAccount;
import com.example.banking.model.BankAccountStatus;
import com.example.banking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {
    List<BankAccount> findByOwner(User owner);
    Optional<BankAccount> findByIdAndOwner(UUID id, User owner);
    Optional<BankAccount> findByAccountNumber(String accountNumber);
    long countByStatus(BankAccountStatus status);
    List<BankAccount> findByStatus(BankAccountStatus status);

    @Query("SELECT COALESCE(SUM(b.balance), 0) FROM BankAccount b")
    BigDecimal sumAllBalances();
}
