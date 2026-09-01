# High-Level Design — Automated Regulatory Filing Module

**Agent:** `L1-design-hld` · **Phase:** 4 · **Correlation ID:** `C885C23C-949E-440C-8B0E-04FDD166A559`
**Inputs:** [prd.md](../phase-1-requirements/prd.md) · [nfr-spec.json](../phase-1-requirements/nfr-spec.json) · [dependency-graph.json](../phase-1-requirements/dependency-graph.json) · [openapi.yaml](openapi.yaml)

## C4 — System Context

```
┌────────────┐       transaction event        ┌──────────────────────────────┐
│ EquatePlus │ ───────────────────────────────▶│  Automated Regulatory Filing  │
│ (external, │        (mocked in MVP:          │  Module                       │
│  system of │       POST /demo/simulate-trade)│                                │
│  record)   │                                 └───────────────┬────────────────┘
└────────────┘                                                 │
                                                                 │ (mocked in MVP)
┌────────────┐                                                  ▼
│ Corporate  │◀── notifications, filings ────────────  ┌──────────────┐
│ SSO        │──── auth token (mocked) ────────────────▶│ SEC EDGAR    │
└────────────┘                                          │ (external,   │
                                                          │  mocked)     │
┌──────────────┐        ┌──────────────────────┐        └──────────────┘
│ Corp.        │        │ Legal & Compliance    │
│ Executive    │───────▶│ Officer                │
│ (browser)    │        │ (browser)              │
└──────────────┘        └──────────────────────┘
```

## C4 — Containers

- **UI** (React SPA) — served statically, calls the API over HTTPS/TLS 1.2+ (NFR: security). Two persona-facing surfaces (Executive Home, Compliance Dashboard) share the Filing Review and Notifications components.
- **API** (Spring Boot) — single deployable in MVP (see "MVP simplification" below); owns all business logic and mocked external connectors.
- **Database** (PostgreSQL) — system of record for filings, audit log, notifications. 7-year retention requirement (NFR-security) implies append-only audit rows in production; MVP schema supports this shape without enforcing retention infra.

## C4 — Components (API), mapped to dependency-graph.json

| Component | Dependency-graph id | MVP status |
|---|---|---|
| Transaction Event Listener | `transaction-event-listener` | Built as a mocked ingestion endpoint (`POST /demo/simulate-trade`), not a real EquatePlus webhook consumer. |
| Form Template Registry | `form-template-registry` | Built as a registry abstraction with one entry (FORM_4) — real extensibility point for FEAT-12, not populated with Form 5/144. |
| Form Generation Engine | `form-generation-engine` | Built. |
| Validation Engine | `validation-engine` | Built. |
| Review & Approval Workflow | `review-approval-workflow` | Built. |
| EDGAR Submission Connector | `edgar-submission-connector` | **Built as a mock** — logs a simulated submission event; does not call any real external SEC system. This directly reflects the still-open FR-020 regulatory dependency (vision.md Regulatory Posture #1) — the interface is real, the implementation behind it is a stand-in pending Legal & Compliance confirming the real mechanism. |
| Audit Log Service | `audit-log-service` | Built. |
| Notification Service | `notification-service` | Built, in-app channel only (email/SMS out of MVP per features.json FEAT-6 note). |
| Deadline Risk Evaluator | *(sub-component of `notification-service` in dependency-graph.json — not a separate top-level node there; called out here explicitly since lld.md treats it as its own service class)* | Built. Broadcasts `DEADLINE_RISK` to **all `LEGAL_COMPLIANCE`-role users** in MVP, not a per-filing "assigned officer" — no officer-assignment data model exists yet (see user-journeys.md note, revised at design-quality-gate review). |
| Data Streaming Service | `data-streaming-service` | **Not built** — MVP dashboard polls `GET /filings` directly instead of a push/stream layer. Real-time (<1min) NFR is achievable via polling at MVP scale; a genuine streaming component is the correct future upgrade path, captured here so it isn't lost. |
| Compliance Dashboard (aggregation) | `compliance-dashboard` | Built (as an API aggregation endpoint + UI, not a separate deployable). |
| Sync Error Monitor | `sync-error-monitor` | Not built — design-only, no real EquatePlus feed to monitor in MVP. |
| Bulk Trade Handler | `bulk-trade-handler` | Not built — design-only. |
| Export Service | `export-service` | Not built — design-only. |
| Analytics Integration | `analytics-integration` | Not built — design-only. |

## MVP simplification vs. target architecture

The target architecture (post-MVP) separates the Data Streaming Service and each API component into independently scalable services to meet the 1,000-trades/minute, 10,000-concurrent-user NFRs. The MVP collapses everything into a single Spring Boot deployable with clean internal service boundaries (see LLD) so the seams for that future split already exist in the code, without paying the operational cost of real distribution for a demo.

## Data flow (golden path)

1. `POST /demo/simulate-trade` → Transaction Event Listener → Form Generation Engine builds a `Filing`. Three outcomes, refined during Phase 6 construction (see below):
   - **Required field missing** (e.g. no transaction code supplied) → status `INCOMPLETE`, `AuditLogEntry(FLAGGED_INCOMPLETE)`, `INCOMPLETE_DATA` notification to **both** executive and legal.
   - **All required fields present, all pass validation** → status `VALIDATED` directly (immediately approvable — this is the common case for a correctly-formed trade and is what lets the golden-path demo go straight from generation to approval), `AuditLogEntry(GENERATED)`, `FORM_GENERATED` notification to the executive.
   - **All required fields present, but a value is invalid** (e.g. zero shares) → status `UNDER_REVIEW` with `validationErrors` populated, same audit/notification as the VALIDATED case — the officer must edit before approval is possible (re-validation on `PATCH` can then promote it to `VALIDATED`).
   `GENERATED` and `APPROVED` are audit-log **action** names, never resting statuses (see openapi.yaml `FilingStatus`).
2. Legal & Compliance Officer's Dashboard (`GET /filings?status=UNDER_REVIEW`) surfaces it.
3. `PATCH /filings/{id}` (optional edit) → Validation Engine re-runs → `VALIDATED` or `422` with field errors.
4. `POST /filings/{id}/approve` → blocked unless `VALIDATED` → EDGAR Submission Connector (mock) → status `SUBMITTED` → Audit Log entry → Notifications (`SUBMITTED`) to both personas.
5. A background check (simulated via a scheduled/deferred check in MVP) evaluates `edgarCutoffAt` and raises a `DEADLINE_RISK` notification if a filing is still not `SUBMITTED` within 12 hours of that cutoff.

## Security & compliance posture (from nfr-spec.json)

TLS termination at the API boundary; RBAC enforced per-endpoint (Executive can only PATCH/approve their own filings — Legal/Compliance role required for `/approve` and full `/filings` list); mocked-SSO bearer token stands in for real corporate SSO. Audit log is write-once from the application's perspective (no `UPDATE`/`DELETE` code path against `audit_log` — see db-schema.sql).
