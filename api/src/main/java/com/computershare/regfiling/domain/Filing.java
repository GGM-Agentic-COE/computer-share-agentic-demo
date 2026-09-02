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
    private String issuerTicker;

    private String reportingPerson;
    private String reportingPersonLast;
    private String reportingPersonFirst;
    private String reportingPersonMiddle;
    private String reportingPersonStreet;
    private String reportingPersonCity;
    private String reportingPersonState;
    private String reportingPersonZip;

    private boolean relationshipDirector;
    private boolean relationshipOfficer;
    private boolean relationshipTenPercentOwner;
    private boolean relationshipOther;
    private String officerTitle;

    private String titleOfSecurity;
    private LocalDate transactionDate;
    private String transactionCode;
    private String acquiredOrDisposed;
    private Integer shares;
    private Double pricePerShare;
    private Integer sharesOwnedFollowingTransaction;
    private String ownershipForm;

    private String signedBy;
    private Instant signedAt;

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

    public String getIssuerTicker() {
        return issuerTicker;
    }

    public void setIssuerTicker(String issuerTicker) {
        this.issuerTicker = issuerTicker;
    }

    public String getReportingPersonLast() {
        return reportingPersonLast;
    }

    public void setReportingPersonLast(String reportingPersonLast) {
        this.reportingPersonLast = reportingPersonLast;
    }

    public String getReportingPersonFirst() {
        return reportingPersonFirst;
    }

    public void setReportingPersonFirst(String reportingPersonFirst) {
        this.reportingPersonFirst = reportingPersonFirst;
    }

    public String getReportingPersonMiddle() {
        return reportingPersonMiddle;
    }

    public void setReportingPersonMiddle(String reportingPersonMiddle) {
        this.reportingPersonMiddle = reportingPersonMiddle;
    }

    public String getReportingPersonStreet() {
        return reportingPersonStreet;
    }

    public void setReportingPersonStreet(String reportingPersonStreet) {
        this.reportingPersonStreet = reportingPersonStreet;
    }

    public String getReportingPersonCity() {
        return reportingPersonCity;
    }

    public void setReportingPersonCity(String reportingPersonCity) {
        this.reportingPersonCity = reportingPersonCity;
    }

    public String getReportingPersonState() {
        return reportingPersonState;
    }

    public void setReportingPersonState(String reportingPersonState) {
        this.reportingPersonState = reportingPersonState;
    }

    public String getReportingPersonZip() {
        return reportingPersonZip;
    }

    public void setReportingPersonZip(String reportingPersonZip) {
        this.reportingPersonZip = reportingPersonZip;
    }

    public boolean isRelationshipDirector() {
        return relationshipDirector;
    }

    public void setRelationshipDirector(boolean relationshipDirector) {
        this.relationshipDirector = relationshipDirector;
    }

    public boolean isRelationshipOfficer() {
        return relationshipOfficer;
    }

    public void setRelationshipOfficer(boolean relationshipOfficer) {
        this.relationshipOfficer = relationshipOfficer;
    }

    public boolean isRelationshipTenPercentOwner() {
        return relationshipTenPercentOwner;
    }

    public void setRelationshipTenPercentOwner(boolean relationshipTenPercentOwner) {
        this.relationshipTenPercentOwner = relationshipTenPercentOwner;
    }

    public boolean isRelationshipOther() {
        return relationshipOther;
    }

    public void setRelationshipOther(boolean relationshipOther) {
        this.relationshipOther = relationshipOther;
    }

    public String getOfficerTitle() {
        return officerTitle;
    }

    public void setOfficerTitle(String officerTitle) {
        this.officerTitle = officerTitle;
    }

    public String getTitleOfSecurity() {
        return titleOfSecurity;
    }

    public void setTitleOfSecurity(String titleOfSecurity) {
        this.titleOfSecurity = titleOfSecurity;
    }

    public String getAcquiredOrDisposed() {
        return acquiredOrDisposed;
    }

    public void setAcquiredOrDisposed(String acquiredOrDisposed) {
        this.acquiredOrDisposed = acquiredOrDisposed;
    }

    public Integer getSharesOwnedFollowingTransaction() {
        return sharesOwnedFollowingTransaction;
    }

    public void setSharesOwnedFollowingTransaction(Integer sharesOwnedFollowingTransaction) {
        this.sharesOwnedFollowingTransaction = sharesOwnedFollowingTransaction;
    }

    public String getOwnershipForm() {
        return ownershipForm;
    }

    public void setOwnershipForm(String ownershipForm) {
        this.ownershipForm = ownershipForm;
    }

    public String getSignedBy() {
        return signedBy;
    }

    public void setSignedBy(String signedBy) {
        this.signedBy = signedBy;
    }

    public Instant getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(Instant signedAt) {
        this.signedAt = signedAt;
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
