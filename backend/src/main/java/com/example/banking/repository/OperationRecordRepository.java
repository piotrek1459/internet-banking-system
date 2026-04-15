package com.example.banking.repository;

import com.example.banking.model.OperationRecord;
import com.example.banking.model.OperationSeverity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OperationRecordRepository extends JpaRepository<OperationRecord, UUID> {
    List<OperationRecord> findAllByOrderByCreatedAtDesc();
    List<OperationRecord> findTop6BySeverityInOrderByCreatedAtDesc(Collection<OperationSeverity> severities);
}
