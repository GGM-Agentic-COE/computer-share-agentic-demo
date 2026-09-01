package com.computershare.regfiling.service;

import com.computershare.regfiling.domain.AuditAction;
import com.computershare.regfiling.domain.AuditLogEntry;
import com.computershare.regfiling.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String filingId, AuditAction action, String actorId, String detail) {
        auditLogRepository.save(new AuditLogEntry(filingId, action, actorId, detail, Instant.now()));
    }

    public List<AuditLogEntry> history(String filingId) {
        return auditLogRepository.findByFilingIdOrderByOccurredAtAsc(filingId);
    }
}
