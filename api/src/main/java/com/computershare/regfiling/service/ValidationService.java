package com.computershare.regfiling.service;

import com.computershare.regfiling.domain.Filing;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Form 4 field validation rules (FR-004). Assumes required fields are already present — see
 * FormGenerationService for the separate "required field missing" (INCOMPLETE) check, which is
 * distinct from "field present but invalid value" (this service's concern).
 */
@Service
public class ValidationService {

    private static final Set<String> VALID_TRANSACTION_CODES = Set.of("S", "P", "A", "D");
    private static final Set<String> VALID_ACQUIRED_OR_DISPOSED = Set.of("A", "D");
    private static final Set<String> VALID_OWNERSHIP_FORMS = Set.of("D", "I");

    public List<String> validate(Filing filing) {
        List<String> errors = new ArrayList<>();

        if (filing.getIssuer() == null || filing.getIssuer().isBlank()) {
            errors.add("issuer: is required");
        }
        if (filing.getReportingPerson() == null || filing.getReportingPerson().isBlank()) {
            errors.add("reportingPerson: is required");
        }
        if (filing.getTransactionDate() == null) {
            errors.add("transactionDate: is required");
        }
        if (filing.getTransactionCode() == null || !VALID_TRANSACTION_CODES.contains(filing.getTransactionCode())) {
            errors.add("transactionCode: must be one of S, P, A, D");
        }
        if (filing.getShares() == null || filing.getShares() <= 0) {
            errors.add("shares: must be greater than 0");
        }
        if (filing.getPricePerShare() == null || filing.getPricePerShare() < 0) {
            errors.add("pricePerShare: must be greater than or equal to 0");
        }

        // Form 4 field-set expansion — see docs/sdlc/phase-4-design/lld.md §5.
        if (filing.getReportingPersonLast() == null || filing.getReportingPersonLast().isBlank()) {
            errors.add("reportingPersonLast: is required");
        }
        if (filing.getReportingPersonFirst() == null || filing.getReportingPersonFirst().isBlank()) {
            errors.add("reportingPersonFirst: is required");
        }
        if (filing.getReportingPersonStreet() == null || filing.getReportingPersonStreet().isBlank()) {
            errors.add("reportingPersonStreet: is required");
        }
        if (filing.getReportingPersonCity() == null || filing.getReportingPersonCity().isBlank()) {
            errors.add("reportingPersonCity: is required");
        }
        if (filing.getReportingPersonState() == null || filing.getReportingPersonState().isBlank()) {
            errors.add("reportingPersonState: is required");
        }
        if (filing.getReportingPersonZip() == null || filing.getReportingPersonZip().isBlank()) {
            errors.add("reportingPersonZip: is required");
        }
        if (filing.getIssuerTicker() == null || filing.getIssuerTicker().isBlank()) {
            errors.add("issuerTicker: is required");
        }
        if (!(filing.isRelationshipDirector() || filing.isRelationshipOfficer()
                || filing.isRelationshipTenPercentOwner() || filing.isRelationshipOther())) {
            errors.add("relationship: at least one relationship checkbox (director, officer, tenPercentOwner, other) must be selected");
        }
        if (filing.isRelationshipOfficer() && (filing.getOfficerTitle() == null || filing.getOfficerTitle().isBlank())) {
            errors.add("officerTitle: is required when relationshipOfficer is true");
        }
        if (filing.getTitleOfSecurity() == null || filing.getTitleOfSecurity().isBlank()) {
            errors.add("titleOfSecurity: is required");
        }
        if (filing.getAcquiredOrDisposed() == null || !VALID_ACQUIRED_OR_DISPOSED.contains(filing.getAcquiredOrDisposed())) {
            errors.add("acquiredOrDisposed: must be one of A, D");
        }
        if (filing.getSharesOwnedFollowingTransaction() == null || filing.getSharesOwnedFollowingTransaction() < 0) {
            errors.add("sharesOwnedFollowingTransaction: must be greater than or equal to 0");
        }
        if (filing.getOwnershipForm() == null || !VALID_OWNERSHIP_FORMS.contains(filing.getOwnershipForm())) {
            errors.add("ownershipForm: must be one of D, I");
        }
        // signedBy/signedAt are deliberately NOT validated here — signing is an approval-time
        // concern (see ApprovalService), not a validation-time one. Requiring it here would make
        // immediate-VALIDATED generation impossible, since signing always happens after review.

        return errors;
    }
}
