# Wireframes — Automated Regulatory Filing Module

**Agent:** `L1-design-ux-wireframe` · **Phase:** 4 · **Correlation ID:** `C885C23C-949E-440C-8B0E-04FDD166A559`
**Inputs:** [stories.json](../phase-3-inception/stories.json) · [user-journeys.md](user-journeys.md)
**Output tool:** N/A in this environment (no Figma connection) — component hierarchy and layout described as markdown/ASCII, which Phase 6's React build implements directly.

## Screen 1 — Executive Home (`/`)

```
┌───────────────────────────────────────────────┐
│ Regulatory Filing · Executive          [🔔 2]  │
├───────────────────────────────────────────────┤
│ [ Simulate Trade ▾ ]  (demo-only trigger)      │
├───────────────────────────────────────────────┤
│ My Filings                                     │
│ ┌─────────────────────────────────────────┐   │
│ │ Form 4 · 2026-08-28 · SUBMITTED      ✅  │   │
│ │ Form 4 · 2026-08-30 · UNDER REVIEW   ⏳  │   │
│ │   ⚠ Deadline risk: 9h remaining          │   │
│ └─────────────────────────────────────────┘   │
└───────────────────────────────────────────────┘
```
Components: `TradeSimulatorButton`, `FilingList`, `FilingStatusBadge`, `DeadlineRiskBanner`, `NotificationBell` (shared).

## Screen 2 — Compliance Dashboard (`/dashboard`)

```
┌───────────────────────────────────────────────┐
│ Compliance Dashboard                   [🔔 5]  │
├───────────────────────────────────────────────┤
│ Filter: [ Status ▾ ] [ Executive ▾ ] [Search]  │
├───────────────────────────────────────────────┤
│ Executive     Form   Status        Deadline    │
│ J. Alvarez    F4     UNDER REVIEW  ⚠ 9h        │
│ R. Chen       F4     UNDER REVIEW  22h         │
│ M. Okafor     F4     SUBMITTED     —           │
└───────────────────────────────────────────────┘
```
Components: `FilterBar`, `FilingTable` (sorted by deadline urgency by default per user-journeys.md), `DeadlineCell`, `StatusBadge`. Row click → Screen 3.

## Screen 3 — Filing Review (`/filings/:id`)

```
┌───────────────────────────────────────────────┐
│ ← Back      Form 4 — J. Alvarez · 2026-08-30   │
├───────────────────────────────────────────────┤
│ Issuer: Acme Corp          [validated ✅]      │
│ Reporting Person: J. Alvarez                   │
│ Transaction Date: 2026-08-30  [edit]           │
│ Transaction Code: S            [edit]          │
│ Shares: 1,200                  [edit]          │
│ Price: $42.10                  [edit]          │
│ ...                                            │
├───────────────────────────────────────────────┤
│ [ Save Edits ]     [ Approve & Submit ]        │
└───────────────────────────────────────────────┘
```
Components: `FormFieldEditor` (per-field, inline validation), `ValidationSummary`, `ApproveButton` (disabled until `validated === true`), `AuditTrailPanel` (collapsible, shows STORY-5 log entries for this filing).

## Screen 4 — Notifications Panel (shared component, both personas)

```
┌───────────────────────────────┐
│ Notifications              [x]│
├───────────────────────────────┤
│ • Form 4 generated for your   │
│   2026-08-30 trade      2h ago│
│ • ⚠ Deadline risk: 9h left    │
│   on 2026-08-30 filing  1h ago│
└───────────────────────────────┘
```
Component: `NotificationDrawer`, opened from `NotificationBell` on any screen.

## Component hierarchy (shared)

```
App
 ├─ AuthGate (mocked SSO)
 ├─ NotificationBell → NotificationDrawer
 ├─ ExecutiveHome
 │   ├─ TradeSimulatorButton
 │   └─ FilingList → FilingStatusBadge, DeadlineRiskBanner
 ├─ ComplianceDashboard
 │   ├─ FilterBar
 │   └─ FilingTable → StatusBadge, DeadlineCell
 └─ FilingReview
     ├─ FormFieldEditor (×N fields)
     ├─ ValidationSummary
     ├─ ApproveButton
     └─ AuditTrailPanel
```

## Accessibility notes (FEAT-9, partial per cycle-plan.md)

Semantic HTML landmarks, labeled form fields, keyboard-navigable table rows and buttons, `aria-live` region for `DeadlineRiskBanner`/validation errors. Full WCAG 2.1 AA audit is out of MVP scope (see features.json FEAT-9).
