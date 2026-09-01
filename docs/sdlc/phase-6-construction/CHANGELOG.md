# Changelog — Automated Regulatory Filing Module

**Agent:** `L1-construction-doc-generator` · **Phase:** 6 · **Correlation ID:** `C885C23C-949E-440C-8B0E-04FDD166A559`

## 0.1.0-mvp — Initial construction

Built from [openapi.yaml](../phase-4-design/openapi.yaml), [hld.md](../phase-4-design/hld.md), [lld.md](../phase-4-design/lld.md), and [db-schema.sql](../phase-4-design/db-schema.sql).

**Added**
- Spring Boot API (`api/`) implementing FEAT-1 through FEAT-4, FEAT-6, FEAT-7 (Form 4 generation, validation, review/edit/approve workflow, audit logging, in-app notifications, compliance dashboard aggregation).
- React + TypeScript UI (`ui/`) implementing the four wireframed screens: Executive Home, Compliance Dashboard, Filing Review, Notifications panel.
- Mocked-SSO auth (bearer token resolved against seeded demo users) and mocked EDGAR submission connector — both explicit, documented MVP stand-ins per `hld.md`.
- `DeadlineRiskEvaluator` keyed off the EDGAR 5:30pm ET same-day cutoff (`edgarCutoffAt`), not the raw statutory deadline, per the Phase 0 regulatory finding.
- 9 Spring Boot integration tests (`FilingWorkflowTests.java`), 5 React unit tests (`FilingStatusBadge`, `DeadlineRiskBanner`).

**Fixed during construction's own code-review gate** (see [review-report.md](review-report.md))
- Two IDOR vulnerabilities (audit log, notifications) — added missing ownership/authorization checks.
- A state-machine gap allowing edits to already-`SUBMITTED` filings.
- A timezone bug in deadline computation (JVM-default-zone vs. required America/New_York).
- An authorization gap allowing trade simulation on another executive's behalf.
- Unhandled deserialization errors on malformed PATCH payloads (raw 500 → structured 422).

**Known MVP simplifications (not defects — documented, not silent)**
- H2 in-memory database in place of the target PostgreSQL/JSONB schema (`db-schema.sql`) — data does not persist across restarts.
- Form 4 fields modeled as direct entity columns rather than a generic JSONB blob, since only one `form_type` is registered.
- Notifications are in-app only; email/SMS channels are designed but not built.
- `DeadlineRiskEvaluator` broadcasts to all `LEGAL_COMPLIANCE`-role users rather than a per-filing assigned officer — no officer-assignment data model exists yet.
- `BusinessDayCalculator` skips weekends only; US federal holidays are not accounted for.

**Not built in this MVP** (see [cycle-plan.md](../phase-2-planning/cycle-plan.md) backlog): real-time data streaming (FEAT-5, simplified to polling), export (FEAT-8), full accessibility/HA infrastructure (FEAT-9), sync-error detection (FEAT-10), bulk/rapid-trade handling (FEAT-11), additional regulatory forms (FEAT-12), third-party analytics integration (FEAT-13).
