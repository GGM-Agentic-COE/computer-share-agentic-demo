# Prioritized Backlog — Automated Regulatory Filing Module

**Agent:** `L1-planning-backlog-prioritizer` · **Phase:** 2 · **Correlation ID:** `C885C23C-949E-440C-8B0E-04FDD166A559`
**Inputs:** [features.json](features.json) · [dependency-graph.json](../phase-1-requirements/dependency-graph.json)

WSJF-style ranking (Value × Time-Criticality / Effort), constrained so no feature is ranked ahead of a feature its component `depends_on` in the dependency graph.

| Rank | Feature | Value | Time-criticality | Effort (size) | Rationale |
|---|---|---|---|---|---|
| 1 | FEAT-1 Automatic SEC Form 4 Generation | High | High | L | Root of the critical path; nothing else can be reviewed, validated, or submitted without it. |
| 2 | FEAT-2 Form Field Validation & Compliance Logic | High | High | M | Regulatory correctness gate; blocks FEAT-3 by dependency graph. |
| 3 | FEAT-3 Legal Review & Approval Workflow | High | High | L (sliced) | Core FR4 workflow; the EDGAR-connector slice carries the one open regulatory dependency (FR-020) — sliced out so review/edit/approve isn't blocked waiting on that answer. |
| 4 | FEAT-4 Audit Logging & Historical Filing Access | High | Medium | M | Compliance-mandatory (7yr retention framing), and needed before the dashboard can show real history. |
| 5 | FEAT-7 Compliance Dashboard for Monitoring & Alerts | High | Medium | L | Highest-visibility MVP deliverable for a client demo; depends on FEAT-1, FEAT-4, FEAT-6. |
| 6 | FEAT-6 Notification System (in-app) | Medium | Medium | S | Small effort, directly supports AC3/AC9 demo moments (deadline-risk, submission confirmation). |
| 7 | FEAT-5 Real-Time Data Streaming to Legal Teams | Medium | Low | M | Simplified to direct API calls for MVP (see features.json note); full streaming component deferred. |
| 8 | FEAT-9 Accessibility & High Availability | Medium | Low | M | Applied partially where free in MVP UI; full compliance/HA infra deferred — not a blocker for demo value. |
| 9 | FEAT-8 Export Filing Data | Low | Low | S | "Should have," not needed for the golden-path demo narrative. |
| 10 | FEAT-12 Extensibility for Additional Regulatory Forms | Low | Low | M | Registry abstraction retained in code; templates deferred. |
| 11 | FEAT-11 Edge Case Handling — Rapid Trade Succession | Low | Low | M | Real risk, but not required to demonstrate the golden path. |
| 12 | FEAT-10 Transaction Data Sync Error Detection | Low | Low | S | Operational hardening, not demo-critical. |
| 13 | FEAT-13 Third-Party Compliance Analytics Integration | Low | Low | S | Explicitly "nice to have" (FR-018); no near-term commercial ask identified. |

**MVP construction commitment** (rank 1–6, matching `mvp_construction: true` in features.json): FEAT-1, FEAT-2, FEAT-3, FEAT-4, FEAT-6, FEAT-7. Ranks 7–13 are designed at the epic/feature level (visible in Phase 4 HLD) but not built as code in this run — explicitly not silently dropped, see [cycle-plan.md](cycle-plan.md).
