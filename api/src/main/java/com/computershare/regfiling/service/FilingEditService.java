package com.computershare.regfiling.service;

import com.computershare.regfiling.domain.AuditAction;
import com.computershare.regfiling.domain.Filing;
import com.computershare.regfiling.domain.FilingStatus;
import com.computershare.regfiling.repository.FilingRepository;
import com.computershare.regfiling.web.dto.ApiExceptions;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

// FR-009/FR-010, STORY-3.
@Service
public class FilingEditService {

    private final FilingRepository filingRepository;
    private final ValidationService validationService;
    private final AuditLogService auditLogService;

    public FilingEditService(FilingRepository filingRepository,
                              ValidationService validationService,
                              AuditLogService auditLogService) {
        this.filingRepository = filingRepository;
        this.validationService = validationService;
        this.auditLogService = auditLogService;
    }

    public Filing edit(String filingId, Map<String, Object> fields, String actorId) {
        Filing filing = filingRepository.findById(filingId)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("No such filing: " + filingId));

        // State-machine guard: SUBMITTED is a terminal state. Found missing at the Phase 6
        // code-review gate — without this, a legal user could PATCH an already-submitted filing
        // back to VALIDATED, enabling a duplicate approve/submit cycle against EdgarSubmissionConnector.
        if (filing.getStatus() == FilingStatus.SUBMITTED) {
            throw new ApiExceptions.ConflictException(
                    "Filing " + filingId + " has already been submitted and cannot be edited.");
        }

        List<String> parseErrors = applyFields(filing, fields);
        if (!parseErrors.isEmpty()) {
            // Malformed payload (e.g. shares: "abc") — surfaced as a structured 422, not a raw 500.
            // Found missing at the Phase 6 code-review gate.
            throw new ApiExceptions.ValidationFailedException(filingId, parseErrors);
        }

        List<String> errors = validationService.validate(filing);
        if (!errors.isEmpty()) {
            filing.setValidationErrors(errors);
            // Field-level errors are returned to the caller (422) without persisting the filing
            // in a half-updated state — see FilingController for the exception handling.
            throw new ApiExceptions.ValidationFailedException(filingId, errors);
        }

        filing.setValidationErrors(List.of());
        filing.setStatus(FilingStatus.VALIDATED);
        filing.setUpdatedAt(Instant.now());
        filingRepository.save(filing);

        auditLogService.log(filingId, AuditAction.EDITED, actorId, "Fields updated: " + fields.keySet());

        return filing;
    }

    /** Applies each provided field, collecting a message for any that can't be parsed/cast rather
     * than throwing — so one bad field in a multi-field PATCH doesn't produce an opaque 500. */
    private List<String> applyFields(Filing filing, Map<String, Object> fields) {
        List<String> errors = new ArrayList<>();
        if (fields == null) {
            return errors;
        }
        try {
            if (fields.containsKey("issuer")) {
                filing.setIssuer((String) fields.get("issuer"));
            }
            if (fields.containsKey("reportingPerson")) {
                filing.setReportingPerson((String) fields.get("reportingPerson"));
            }
        } catch (ClassCastException e) {
            errors.add("issuer/reportingPerson: must be a string");
        }
        try {
            if (fields.containsKey("transactionDate")) {
                filing.setTransactionDate(LocalDate.parse((String) fields.get("transactionDate")));
            }
        } catch (ClassCastException | DateTimeParseException e) {
            errors.add("transactionDate: must be an ISO-8601 date string (e.g. 2026-08-30)");
        }
        try {
            if (fields.containsKey("transactionCode")) {
                filing.setTransactionCode((String) fields.get("transactionCode"));
            }
        } catch (ClassCastException e) {
            errors.add("transactionCode: must be a string");
        }
        try {
            if (fields.containsKey("shares")) {
                filing.setShares(toInt(fields.get("shares")));
            }
        } catch (NumberFormatException e) {
            errors.add("shares: must be a whole number");
        }
        try {
            if (fields.containsKey("pricePerShare")) {
                filing.setPricePerShare(toDouble(fields.get("pricePerShare")));
            }
        } catch (NumberFormatException e) {
            errors.add("pricePerShare: must be a number");
        }
        return errors;
    }

    private static Integer toInt(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof Number number ? number.intValue() : Integer.parseInt(value.toString());
    }

    private static Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof Number number ? number.doubleValue() : Double.parseDouble(value.toString());
    }
}
