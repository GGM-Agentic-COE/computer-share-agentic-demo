package com.computershare.regfiling.web;

import com.computershare.regfiling.domain.AuditLogEntry;
import com.computershare.regfiling.domain.Filing;
import com.computershare.regfiling.domain.User;
import com.computershare.regfiling.domain.UserRole;
import com.computershare.regfiling.repository.FilingRepository;
import com.computershare.regfiling.service.AuditLogService;
import com.computershare.regfiling.web.dto.ApiExceptions;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// openapi.yaml: GET /filings/{id}/audit-log — FR-012.
@RestController
@RequestMapping("/api/filings")
public class AuditController {

    private final AuditLogService auditLogService;
    private final FilingRepository filingRepository;

    public AuditController(AuditLogService auditLogService, FilingRepository filingRepository) {
        this.auditLogService = auditLogService;
        this.filingRepository = filingRepository;
    }

    @GetMapping("/{id}/audit-log")
    public List<AuditLogEntry> auditLog(@PathVariable("id") String filingId, HttpServletRequest request) {
        User currentUser = CurrentUserResolver.require(request);
        Filing filing = filingRepository.findById(filingId)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("No such filing: " + filingId));

        // RBAC: an executive may only view the audit trail of their own filings — mirrors
        // FilingController.get(). Found missing (IDOR) at the Phase 6 code-review gate.
        if (currentUser.getRole() == UserRole.EXECUTIVE && !filing.getExecutiveId().equals(currentUser.getId())) {
            throw new ApiExceptions.ForbiddenException("Executives may only view audit logs for their own filings.");
        }

        return auditLogService.history(filingId);
    }
}
