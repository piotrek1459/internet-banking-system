package com.example.banking.service;

import com.example.banking.model.*;
import com.example.banking.repository.OperationRecordRepository;
import com.example.banking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OperationService {

    private final OperationRecordRepository operationRecordRepository;
    private final UserRepository userRepository;

    @Transactional
    public void record(String actorEmail, Role actorRole,
                       String target, OperationType type,
                       OperationSeverity severity, String description) {
        User actor = userRepository.findByEmail(actorEmail).orElse(null);
        operationRecordRepository.save(OperationRecord.builder()
                .actor(actor)
                .actorEmail(actorEmail)
                .actorRole(actorRole)
                .target(target)
                .type(type)
                .severity(severity)
                .description(description)
                .build());
    }
}
