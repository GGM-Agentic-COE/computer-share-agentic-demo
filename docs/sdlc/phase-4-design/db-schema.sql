-- Data model — Automated Regulatory Filing Module
-- Agent: L1-design-data-architect · Phase: 4 · Correlation ID: C885C23C-949E-440C-8B0E-04FDD166A559
-- Inputs: lld.md, functional-requirements.json
-- No live production EquatePlus database exists to diff against in this environment (tool-L1-db-schema-diff
-- not available); this is a fresh schema for the module's own bounded data store, not a diff/migration.

CREATE TABLE users (
    id              VARCHAR(64) PRIMARY KEY,
    display_name    VARCHAR(255) NOT NULL,
    role            VARCHAR(32) NOT NULL CHECK (role IN ('EXECUTIVE', 'LEGAL_COMPLIANCE')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE filings (
    id                  VARCHAR(64) PRIMARY KEY,
    form_type           VARCHAR(32) NOT NULL DEFAULT 'FORM_4', -- form-template-registry key; extensible (FEAT-12)
    executive_id        VARCHAR(64) NOT NULL REFERENCES users(id),
    -- GENERATED and APPROVED are audit_log.action values, not resting statuses here (see openapi.yaml FilingStatus) —
    -- a filing moves UNDER_REVIEW -> VALIDATED -> SUBMITTED, or INCOMPLETE if required data was missing at generation.
    status              VARCHAR(32) NOT NULL CHECK (status IN
                            ('INCOMPLETE','UNDER_REVIEW','VALIDATED','SUBMITTED')),
    fields              JSONB NOT NULL,           -- Form 4 field payload (issuer, reportingPerson, transactionDate, transactionCode, shares, pricePerShare, ...)
    validation_errors   JSONB,                     -- last validation error set, null when VALIDATED
    deadline_at         TIMESTAMPTZ NOT NULL,      -- raw statutory 2-business-day deadline
    edgar_cutoff_at     TIMESTAMPTZ NOT NULL,      -- effective 5:30pm ET same-day EDGAR cutoff (vision.md Regulatory Posture #2) — deadline-risk logic keys off THIS, not deadline_at
    deadline_risk_fired BOOLEAN NOT NULL DEFAULT false, -- idempotency guard for the DEADLINE_RISK notification
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_filings_status ON filings(status);
CREATE INDEX idx_filings_executive ON filings(executive_id);
CREATE INDEX idx_filings_edgar_cutoff ON filings(edgar_cutoff_at) WHERE status <> 'SUBMITTED';

-- Append-only by convention (no application code path issues UPDATE/DELETE against this table) —
-- satisfies the 7-year-retention audit requirement's shape; real retention/immutability enforcement
-- (e.g. a DB-level trigger or WORM storage) is a production hardening item, not built in this MVP.
CREATE TABLE audit_log (
    id          BIGSERIAL PRIMARY KEY,
    filing_id   VARCHAR(64) NOT NULL REFERENCES filings(id),
    action      VARCHAR(32) NOT NULL CHECK (action IN
                    ('GENERATED','EDITED','APPROVED','SUBMITTED','FLAGGED_INCOMPLETE')),
    actor_id    VARCHAR(64) NOT NULL REFERENCES users(id),
    detail      TEXT,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_filing ON audit_log(filing_id);

CREATE TABLE notifications (
    id          VARCHAR(64) PRIMARY KEY,
    user_id     VARCHAR(64) NOT NULL REFERENCES users(id),
    filing_id   VARCHAR(64) REFERENCES filings(id),
    type        VARCHAR(32) NOT NULL CHECK (type IN
                    ('FORM_GENERATED','DEADLINE_RISK','SUBMITTED','INCOMPLETE_DATA')),
    message     TEXT NOT NULL,
    read        BOOLEAN NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_user ON notifications(user_id, read);

-- form_templates: form-template-registry extensibility point (FEAT-12). MVP seeds exactly one row.
CREATE TABLE form_templates (
    form_type   VARCHAR(32) PRIMARY KEY,
    schema      JSONB NOT NULL,   -- field definitions: name, type, required, validation rule ref
    version     VARCHAR(16) NOT NULL
);

-- Demo seed users (WU-09) — satisfies the NOT NULL FKs on filings.executive_id, audit_log.actor_id,
-- notifications.user_id so the golden-path walkthrough can run without a real SSO/EquatePlus feed.
INSERT INTO users (id, display_name, role) VALUES
    ('exec-1', 'J. Alvarez', 'EXECUTIVE'),
    ('exec-2', 'R. Chen', 'EXECUTIVE'),
    ('exec-3', 'M. Okafor', 'EXECUTIVE'),
    ('legal-1', 'S. Kapoor', 'LEGAL_COMPLIANCE'),
    ('legal-2', 'D. Whitfield', 'LEGAL_COMPLIANCE');

INSERT INTO form_templates (form_type, schema, version) VALUES (
    'FORM_4',
    '{"fields": [
        {"name": "issuer", "type": "string", "required": true},
        {"name": "reportingPerson", "type": "string", "required": true},
        {"name": "transactionDate", "type": "date", "required": true},
        {"name": "transactionCode", "type": "string", "required": true, "enum": ["S","P","A","D"]},
        {"name": "shares", "type": "number", "required": true, "min": 0},
        {"name": "pricePerShare", "type": "number", "required": true, "min": 0}
    ]}'::jsonb,
    '2026.1'
);
