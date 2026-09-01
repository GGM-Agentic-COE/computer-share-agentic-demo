package com.computershare.regfiling.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * MVP simplification vs. docs/sdlc/phase-4-design/db-schema.sql: Form 4 fields are modeled as
 * direct columns (issuer, reportingPerson, ...) rather than a generic JSONB "fields" blob, since
 * the MVP only registers one form_type (FORM_4) — see form-template-registry extensibility note
 * in hld.md. The documented JSONB approach remains the target for real multi-form support.
 */
@Entity
@Table(name = "filings")
public class Filing {

    @Id
    private String id;

    private String formType = "FORM_4";

    private String executiveId;

    @Enumerated(EnumType.STRING)
    private FilingStatus status;

    private String issuer;
    private String reportingPerson;
    private LocalDate transactionDate;
    private String transactionCode;
    private Integer shares;
    private Double pricePerShare;

    // EAGER: this entity is serialized directly to JSON outside an open Hibernate session
    // (open-in-view is disabled — see application.yml), so a LAZY collection would throw on
    // serialization. The collection is small (a handful of error strings at most), so eager
    // fetch has no meaningful cost here.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "filing_validation_errors", joinColumns = @JoinColumn(name = "filing_id"))
    private List<String> validationErrors = new ArrayList<>();

    private Instant deadlineAt;
    private Instant edgarCutoffAt;
    private boolean deadlineRiskFired;

    private Instant createdAt;
    private Instant updatedAt;

    protected Filing() {
        // JPA
    }

    public Filing(String id, String executiveId) {
        this.id = id;
        this.executiveId = executiveId;
    }

    public String getId() {
        return id;
    }

    public String getFormType() {
        return formType;
    }

    public String getExecutiveId() {
        return executiveId;
    }

    public FilingStatus getStatus() {
        return status;
    }

    public void setStatus(FilingStatus status) {
        this.status = status;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getReportingPerson() {
        return reportingPerson;
    }

    public void setReportingPerson(String reportingPerson) {
        this.reportingPerson = reportingPerson;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getTransactionCode() {
        return transactionCode;
    }

    public void setTransactionCode(String transactionCode) {
        this.transactionCode = transactionCode;
    }

    public Integer getShares() {
        return shares;
    }

    public void setShares(Integer shares) {
        this.shares = shares;
    }

    public Double getPricePerShare() {
        return pricePerShare;
    }

    public void setPricePerShare(Double pricePerShare) {
        this.pricePerShare = pricePerShare;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<String> validationErrors) {
        this.validationErrors = validationErrors;
    }

    public Instant getDeadlineAt() {
        return deadlineAt;
    }

    public void setDeadlineAt(Instant deadlineAt) {
        this.deadlineAt = deadlineAt;
    }

    public Instant getEdgarCutoffAt() {
        return edgarCutoffAt;
    }

    public void setEdgarCutoffAt(Instant edgarCutoffAt) {
        this.edgarCutoffAt = edgarCutoffAt;
    }

    public boolean isDeadlineRiskFired() {
        return deadlineRiskFired;
    }

    public void setDeadlineRiskFired(boolean deadlineRiskFired) {
        this.deadlineRiskFired = deadlineRiskFired;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
