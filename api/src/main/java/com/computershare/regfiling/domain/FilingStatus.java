package com.computershare.regfiling.domain;

// Matches docs/sdlc/phase-4-design/openapi.yaml FilingStatus (trimmed enum — see design-quality
// gate remediation: GENERATED/APPROVED are AuditAction values, not resting statuses).
public enum FilingStatus {
    INCOMPLETE,
    UNDER_REVIEW,
    VALIDATED,
    SUBMITTED
}
