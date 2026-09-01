# Product Requirements Document — Automated Regulatory Filing Module (composed)

**Agent:** `L1-requirements-prd-composer` · **Phase:** 1 · **Correlation ID:** `C885C23C-949E-440C-8B0E-04FDD166A559`
**Inputs:** [functional-requirements.json](functional-requirements.json) · [nfr-spec.json](nfr-spec.json) · [vision.md](../phase-0-vision/vision.md)
**Supersedes** the root-level `PRD.txt` as the authoritative, atomic-requirement PRD for this run. `PRD.txt` is retained for reference/provenance.

> Zero-drop check: all 20 atomic FRs from `functional-requirements.json` and all 13 NFR classifications from `nfr-spec.json` appear below. Nothing summarized away.

## 1. Problem & users
See [vision.md](../phase-0-vision/vision.md) — carried forward verbatim, not re-derived.

## 2. Functional requirements

| ID | Requirement | Priority | Epic.Feature |
|---|---|---|---|
| FR-001 | Detect eligible securities transaction execution events in EquatePlus | Must | E1.F1 |
| FR-002 | Map executed transaction data to SEC Form 4 fields | Must | E1.F1 |
| FR-003 | Generate a pre-filled SEC Form 4 within 5 minutes of transaction execution | Must | E1.F1 |
| FR-004 | Validate all form fields against regulatory standards; block submission on failure | Must | E1.F2 |
| FR-005 | Securely stream transaction/form data to legal/compliance in real time | Must | E2.F1 |
| FR-006 | Notify executive + legal team when a form is generated | Must | E2.F2 |
| FR-007 | Notify executive + legal team when a form is submitted | Must | E2.F2 |
| FR-008 | Deadline-risk warning when <12h remain (keyed to EDGAR 5:30pm ET cutoff) | Must | E2.F2 |
| FR-009 | Legal/compliance can review a pre-filled form | Must | E1.F3 |
| FR-010 | Legal/compliance can edit a pre-filled form, re-validated on save | Must | E1.F3 |
| FR-011 | Legal/compliance can approve a validated form for submission | Must | E1.F3 |
| FR-012 | Log every generate/edit/approve/submit action with timestamp + user ID | Must | E1.F4 |
| FR-013 | Authorized users can access historical filings and statuses | Must | E1.F4 |
| FR-014 | Bulk-process multiple trades in a short timeframe, no omissions | Should | E3.F2 |
| FR-015 | Real-time dashboard of filing status, deadlines, error alerts | Should | E2.F3 |
| FR-016 | Export filing data as CSV/PDF | Should | E2.F4 |
| FR-017 | Detect transaction data sync failures, notify immediately | Should | E3.F1 |
| FR-018 | Third-party compliance analytics integration (export/API) | Nice | E3.F4 |
| FR-019 | Support additional regulatory forms (Form 5, Form 144) via modular templates | Nice | E3.F3 |
| FR-020 | Submit to SEC EDGAR directly, or hand off to manual filing-agent process — **mechanism open, must be confirmed before Design** | Must | E1.F3 |

## 3. Non-functional requirements (by category)

- **Performance:** form generation <5min (FR-003); streaming <1min (FR-005); dashboard refresh <1min (FR-015); export <2min (FR-016).
- **Security:** TLS 1.2+ everywhere; RBAC + corporate SSO; full audit logging, 7-year retention (retention applicability to issuer records flagged for counsel confirmation — see nfr-spec.json).
- **Scalability:** 10,000 concurrent users; 1,000 trades/minute.
- **Availability:** 99.9% uptime, automated failover/backup.
- **Compliance:** deadline-risk logic must use the EDGAR 5:30pm ET same-day cutoff, not raw calendar deadline (FR-008); EDGAR submission mechanism confirmed before Design (FR-020, open dependency).
- **Usability:** WCAG 2.1 AA, full keyboard/screen-reader support; review UI loads <2s, supports concurrent edits.

## 4. Assumptions, constraints, risks (carried forward from vision.md / PRD.txt)

**Assumptions:** transaction data is accurate and available in real time from EquatePlus; SEC Form 4 template is current; corporate SSO integration is available and stable.

**Constraints:** integration limited to EquatePlus platform APIs only; US regulatory forms only at launch; no manual transaction entry; regulatory form formats/deadlines may change — architecture must be adaptable (reinforced by FR-019/FR-020 openness).

**Risks (from PRD.txt §9, reaffirmed):** regulatory requirements changing unexpectedly (Medium likelihood / High impact); integration challenges with legacy EquatePlus APIs (Medium/Medium); data breach or unauthorized access (Low/High); system downtime during critical filing windows (Low/High); user resistance to adoption (Medium/Medium). **New risk surfaced in Phase 0:** reviewer inaction (Legal/Compliance not acting on a generated form before the deadline) is a distinct failure mode from data-sync failure and needs its own escalation path — not yet covered by an existing epic/feature; recommend Product Lead confirm whether this becomes a new E3 feature or folds into E2.F2 notifications.

## 5. Out of scope
Manual transaction entry; integration with non-EquatePlus platforms; non-US regulatory forms.

## 6. Success metrics
See vision.md — carried forward with the same unverified-baseline caveat (70%→98% on-time rate, 24h→30min time-to-submit, 5→0 errors/month, 90% adoption in 6 months).
