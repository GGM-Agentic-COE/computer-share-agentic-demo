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

        return errors;
    }
}
