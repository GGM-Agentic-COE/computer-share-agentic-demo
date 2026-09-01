package com.computershare.regfiling.repository;

import com.computershare.regfiling.domain.AuditLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLogEntry, Long> {

    List<AuditLogEntry> findByFilingIdOrderByOccurredAtAsc(String filingId);
}
