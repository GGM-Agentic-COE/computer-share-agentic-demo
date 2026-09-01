package com.computershare.regfiling.service;

import com.computershare.regfiling.domain.AuditAction;
import com.computershare.regfiling.domain.Filing;
import com.computershare.regfiling.domain.FilingStatus;
import com.computershare.regfiling.domain.NotificationType;
import com.computershare.regfiling.domain.User;
import com.computershare.regfiling.repository.FilingRepository;
import com.computershare.regfiling.repository.UserRepository;
import com.computershare.regfiling.web.dto.ApiExceptions;
import com.computershare.regfiling.web.dto.SimulateTradeRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.computershare.regfiling.service.BusinessDayCalculator.ET;

/**
 * FR-001/FR-002/FR-003: detect (via the demo simulate-trade endpoint, standing in for a real
 * EquatePlus transaction event), map to Form 4 fields, generate within 5 minutes (synchronous
 * in this MVP, trivially satisfying the 5-minute NFR).
 *
 * Demo issuer is hardcoded ("Ascendion INC — Demo Issuer") since there is no real multi-issuer
 * EquatePlus feed to source it from in this environment.
 */
@Service
public class FormGenerationService {

    private static final String DEMO_ISSUER = "Ascendion INC — Demo Issuer";
    private static final int FILING_DEADLINE_BUSINESS_DAYS = 2;

    private final FilingRepository filingRepository;
    private final UserRepository userRepository;
    private final ValidationService validationService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final BusinessDayCalculator businessDayCalculator;

    public FormGenerationService(FilingRepository filingRepository,
                                  UserRepository userRepository,
                                  ValidationService validationService,
                                  AuditLogService auditLogService,
                                  NotificationService notificationService,
                                  BusinessDayCalculator businessDayCalculator) {
        this.filingRepository = filingRepository;
        this.userRepository = userRepository;
        this.validationService = validationService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.businessDayCalculator = businessDayCalculator;
    }

    public Filing generate(SimulateTradeRequest request) {
        User executive = userRepository.findById(request.getExecutiveId())
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Unknown executiveId: " + request.getExecutiveId()));

        Filing filing = new Filing(UUID.randomUUID().toString(), executive.getId());
        filing.setIssuer(DEMO_ISSUER);
        filing.setReportingPerson(executive.getDisplayName());
        // Anchored to America/New_York, not the JVM default zone — BusinessDayCalculator's
        // statutory-deadline/EDGAR-cutoff math is ET-based, so transactionDate must be too, or a
        // non-ET-deployed server computes a deadline that's off by up to a day. Found at the
        // Phase 6 code-review gate.
        filing.setTransactionDate(LocalDate.now(ET));
        filing.setTransactionCode(request.getTransactionCode());
        filing.setShares(request.getShares());
        filing.setPricePerShare(request.getPricePerShare());

        Instant now = Instant.now();
        filing.setCreatedAt(now);
        filing.setUpdatedAt(now);
        filing.setDeadlineAt(businessDayCalculator
                .statutoryDeadline(filing.getTransactionDate(), FILING_DEADLINE_BUSINESS_DAYS).toInstant());
        filing.setEdgarCutoffAt(businessDayCalculator
                .edgarCutoff(filing.getTransactionDate(), FILING_DEADLINE_BUSINESS_DAYS).toInstant());

        // FR-001's "missing required data" path (E1.F1-S1 AC2): a required field literally absent
        // from the client's request. Distinct from "field present but invalid value", which
        // ValidationService catches below and results in UNDER_REVIEW-with-errors, not INCOMPLETE.
        boolean requiredFieldMissing = isBlank(filing.getTransactionCode())
                || filing.getShares() == null
                || filing.getPricePerShare() == null;

        if (requiredFieldMissing) {
            filing.setStatus(FilingStatus.INCOMPLETE);
            filing.setValidationErrors(List.of("One or more required transaction fields were not provided."));
            filingRepository.save(filing);
            auditLogService.log(filing.getId(), AuditAction.FLAGGED_INCOMPLETE, executive.getId(),
                    "Missing required field(s) at generation time.");
            notificationService.notifyUser(executive.getId(), filing.getId(), NotificationType.INCOMPLETE_DATA,
                    "Your Form 4 could not be fully generated — missing required data.");
            notificationService.notifyAllLegalCompliance(filing.getId(), NotificationType.INCOMPLETE_DATA,
                    "A Form 4 for " + executive.getDisplayName() + " is missing required data.");
            return filing;
        }

        List<String> errors = validationService.validate(filing);
        filing.setValidationErrors(errors);
        filing.setStatus(errors.isEmpty() ? FilingStatus.VALIDATED : FilingStatus.UNDER_REVIEW);
        filingRepository.save(filing);

        auditLogService.log(filing.getId(), AuditAction.GENERATED, executive.getId(),
                "Form 4 generated from simulated trade.");
        notificationService.notifyUser(executive.getId(), filing.getId(), NotificationType.FORM_GENERATED,
                "Form 4 generated for your " + filing.getTransactionDate() + " trade — under legal review.");

        return filing;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
