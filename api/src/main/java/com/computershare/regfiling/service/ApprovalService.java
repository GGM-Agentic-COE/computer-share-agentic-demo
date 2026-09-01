package com.computershare.regfiling.service;

import com.computershare.regfiling.domain.AuditAction;
import com.computershare.regfiling.domain.Filing;
import com.computershare.regfiling.domain.FilingStatus;
import com.computershare.regfiling.domain.NotificationType;
import com.computershare.regfiling.domain.User;
import com.computershare.regfiling.repository.FilingRepository;
import com.computershare.regfiling.repository.UserRepository;
import com.computershare.regfiling.web.dto.ApiExceptions;
import org.springframework.stereotype.Service;

import java.time.Instant;

// FR-011/FR-020, STORY-4.
@Service
public class ApprovalService {

    private final FilingRepository filingRepository;
    private final UserRepository userRepository;
    private final EdgarSubmissionConnector edgarSubmissionConnector;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public ApprovalService(FilingRepository filingRepository,
                            UserRepository userRepository,
                            EdgarSubmissionConnector edgarSubmissionConnector,
                            AuditLogService auditLogService,
                            NotificationService notificationService) {
        this.filingRepository = filingRepository;
        this.userRepository = userRepository;
        this.edgarSubmissionConnector = edgarSubmissionConnector;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    public Filing approve(String filingId, String actorId) {
        Filing filing = filingRepository.findById(filingId)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("No such filing: " + filingId));

        if (filing.getStatus() != FilingStatus.VALIDATED) {
            throw new ApiExceptions.ConflictException(
                    "Filing " + filingId + " is not VALIDATED (current status: " + filing.getStatus() + ") — approval blocked.");
        }

        auditLogService.log(filingId, AuditAction.APPROVED, actorId, "Approved by legal & compliance.");

        EdgarSubmissionConnector.SubmissionResult result = edgarSubmissionConnector.submit(filing);

        filing.setStatus(FilingStatus.SUBMITTED);
        filing.setUpdatedAt(Instant.now());
        filingRepository.save(filing);

        auditLogService.log(filingId, AuditAction.SUBMITTED, actorId,
                "Mock EDGAR submission confirmation: " + result.confirmationId());

        User executive = userRepository.findById(filing.getExecutiveId()).orElse(null);
        String execId = executive != null ? executive.getId() : filing.getExecutiveId();
        notificationService.notifyUser(execId, filingId, NotificationType.SUBMITTED,
                "Your Form 4 has been submitted (confirmation " + result.confirmationId() + ").");
        notificationService.notifyAllLegalCompliance(filingId, NotificationType.SUBMITTED,
                "Form 4 for filing " + filingId + " has been submitted.");

        return filing;
    }
}
