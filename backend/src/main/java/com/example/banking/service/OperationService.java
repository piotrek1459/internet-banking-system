package com.example.banking.service;

import com.example.banking.model.*;
import com.example.banking.repository.OperationRecordRepository;
import com.example.banking.repository.OperationSeverityEntityRepository;
import com.example.banking.repository.OperationTypeEntityRepository;
import com.example.banking.repository.RoleEntityRepository;
import com.example.banking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OperationService {

    private final OperationRecordRepository operationRecordRepository;
    private final UserRepository userRepository;
    private final RoleEntityRepository roleEntityRepository;
    private final OperationTypeEntityRepository operationTypeEntityRepository;
    private final OperationSeverityEntityRepository operationSeverityEntityRepository;

    @Transactional
    public void record(String actorEmail, Role actorRole,
                       String target, OperationType type,
                       OperationSeverity severity, String description) {
        User actor = userRepository.findByEmail(actorEmail).orElse(null);
        RoleEntity roleEntity = roleEntityRepository.findByCode(actorRole.name()).orElseThrow();
        OperationTypeEntity typeEntity = operationTypeEntityRepository.findByCode(type.name()).orElseThrow();
        OperationSeverityEntity severityEntity = operationSeverityEntityRepository.findByCode(severity.name()).orElseThrow();

        operationRecordRepository.save(OperationRecord.builder()
                .actor(actor)
                .actorEmail(actorEmail)
                .actorRole(roleEntity)
                .target(target)
                .type(typeEntity)
                .severity(severityEntity)
                .description(description)
                .build());
    }
}
