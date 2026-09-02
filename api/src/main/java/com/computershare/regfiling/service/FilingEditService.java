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
import java.util.function.BiConsumer;

import org.springframework.stereotype.Service;

// FR-009/FR-010, E1.F3-S1.
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

    private record FieldSpec(BiConsumer<Filing, Object> setter, String errorMessage) {
    }

    // One entry per PATCH-editable field. signedAt is deliberately absent — it's server-stamped
    // only by ApprovalService at approval time, so a PATCH can never forge or backdate it.
    private static final Map<String, FieldSpec> FIELD_SPECS = Map.ofEntries(
            Map.entry("issuer", new FieldSpec((f, v) -> f.setIssuer((String) v), "issuer: must be a string")),
            Map.entry("reportingPerson", new FieldSpec((f, v) -> f.setReportingPerson((String) v), "reportingPerson: must be a string")),
            Map.entry("transactionDate", new FieldSpec((f, v) -> f.setTransactionDate(LocalDate.parse((String) v)), "transactionDate: must be an ISO-8601 date string (e.g. 2026-08-30)")),
            Map.entry("transactionCode", new FieldSpec((f, v) -> f.setTransactionCode((String) v), "transactionCode: must be a string")),
            Map.entry("shares", new FieldSpec((f, v) -> f.setShares(toInt(v)), "shares: must be a whole number")),
            Map.entry("pricePerShare", new FieldSpec((f, v) -> f.setPricePerShare(toDouble(v)), "pricePerShare: must be a number")),

            Map.entry("issuerTicker", new FieldSpec((f, v) -> f.setIssuerTicker((String) v), "issuerTicker: must be a string")),

            Map.entry("reportingPersonLast", new FieldSpec((f, v) -> f.setReportingPersonLast((String) v), "reportingPersonLast: must be a string")),
            Map.entry("reportingPersonFirst", new FieldSpec((f, v) -> f.setReportingPersonFirst((String) v), "reportingPersonFirst: must be a string")),
            Map.entry("reportingPersonMiddle", new FieldSpec((f, v) -> f.setReportingPersonMiddle((String) v), "reportingPersonMiddle: must be a string")),
            Map.entry("reportingPersonStreet", new FieldSpec((f, v) -> f.setReportingPersonStreet((String) v), "reportingPersonStreet: must be a string")),
            Map.entry("reportingPersonCity", new FieldSpec((f, v) -> f.setReportingPersonCity((String) v), "reportingPersonCity: must be a string")),
            Map.entry("reportingPersonState", new FieldSpec((f, v) -> f.setReportingPersonState((String) v), "reportingPersonState: must be a string")),
            Map.entry("reportingPersonZip", new FieldSpec((f, v) -> f.setReportingPersonZip((String) v), "reportingPersonZip: must be a string")),

            Map.entry("relationshipDirector", new FieldSpec((f, v) -> f.setRelationshipDirector((Boolean) v), "relationshipDirector: must be a boolean")),
            Map.entry("relationshipOfficer", new FieldSpec((f, v) -> f.setRelationshipOfficer((Boolean) v), "relationshipOfficer: must be a boolean")),
            Map.entry("relationshipTenPercentOwner", new FieldSpec((f, v) -> f.setRelationshipTenPercentOwner((Boolean) v), "relationshipTenPercentOwner: must be a boolean")),
            Map.entry("relationshipOther", new FieldSpec((f, v) -> f.setRelationshipOther((Boolean) v), "relationshipOther: must be a boolean")),
            Map.entry("officerTitle", new FieldSpec((f, v) -> f.setOfficerTitle((String) v), "officerTitle: must be a string")),

            Map.entry("titleOfSecurity", new FieldSpec((f, v) -> f.setTitleOfSecurity((String) v), "titleOfSecurity: must be a string")),
            Map.entry("acquiredOrDisposed", new FieldSpec((f, v) -> f.setAcquiredOrDisposed((String) v), "acquiredOrDisposed: must be a string")),
            Map.entry("sharesOwnedFollowingTransaction", new FieldSpec((f, v) -> f.setSharesOwnedFollowingTransaction(toInt(v)), "sharesOwnedFollowingTransaction: must be a whole number")),
            Map.entry("ownershipForm", new FieldSpec((f, v) -> f.setOwnershipForm((String) v), "ownershipForm: must be a string")),

            Map.entry("signedBy", new FieldSpec((f, v) -> f.setSignedBy((String) v), "signedBy: must be a string"))
    );

    /** Applies each provided field, collecting a message for any that can't be parsed/cast rather
     * than throwing — so one bad field in a multi-field PATCH doesn't produce an opaque 500. */
    private List<String> applyFields(Filing filing, Map<String, Object> fields) {
        List<String> errors = new ArrayList<>();
        if (fields == null) {
            return errors;
        }
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            FieldSpec spec = FIELD_SPECS.get(entry.getKey());
            if (spec == null) {
                continue; // unknown/unsupported field name — ignored, same as the prior implementation
            }
            try {
                spec.setter().accept(filing, entry.getValue());
            } catch (ClassCastException | NumberFormatException | DateTimeParseException e) {
                errors.add(spec.errorMessage());
            }
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
