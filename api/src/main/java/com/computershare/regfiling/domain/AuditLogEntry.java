package com.computershare.regfiling.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

// Append-only by convention: no service in this codebase issues an UPDATE or DELETE against
// this entity (see docs/sdlc/phase-4-design/hld.md Security & compliance posture).
@Entity
@Table(name = "audit_log")
public class AuditLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filingId;

    @Enumerated(EnumType.STRING)
    private AuditAction action;

    private String actorId;

    // Default-length VARCHAR(255) was too short once a single PATCH can touch every Form 4 field
    // (the "Fields updated: [...]" detail string lists every key) — found via a live PATCH that
    // sent all ~24 editable fields at once and hit H2's "Value too long" error.
    @Column(length = 2000)
    private String detail;

    private Instant occurredAt;

    protected AuditLogEntry() {
        // JPA
    }

    public AuditLogEntry(String filingId, AuditAction action, String actorId, String detail, Instant occurredAt) {
        this.filingId = filingId;
        this.action = action;
        this.actorId = actorId;
        this.detail = detail;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public String getFilingId() {
        return filingId;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getActorId() {
        return actorId;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
