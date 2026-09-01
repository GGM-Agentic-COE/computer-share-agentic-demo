# User Flow Diagrams
**Document Version:** 2.0.0 (replaces `user-journeys.md`)
**Baseline Reference:** `kb-L1-computershare-enterprise-architecture.md` v1.0.0
**Wireframes Reference:** `wireframes.html` (categorized by feature)
**Feature Coverage:** E1.F1–F4, E2.F2, E2.F3 (all screens built in this MVP)
**Correlation ID:** `C885C23C-949E-440C-8B0E-04FDD166A559`

> All flows below trace to an actual screen and endpoint in this repo — nothing here is aspirational. Nodes are annotated with the epics-from-prd.md feature and story that deliver them. This document replaces the earlier `user-journeys.md`, which covered the same two personas but as two short prose narratives rather than per-flow diagrams with accessibility notes — too thin to design or test against.

---

## Flow Index

| # | Flow Name | Feature(s) | Primary Actor |
|---|-----------|---------|---------------|
| 1 | Authentication — Mocked SSO Persona Switch | E1 (cross-cutting) | Executive / Legal & Compliance |
| 2 | Role Enforcement & Access Control | E1.F3 | Both |
| 3 | Simulate Trade → Form Generation (golden path) | E1.F1 | Corporate Executive |
| 4 | Incomplete Transaction Data | E1.F1 | Corporate Executive, Legal & Compliance |
| 5 | Compliance Dashboard — Monitor & Filter | E2.F3 | Legal & Compliance |
| 6 | Review & Edit a Filing | E1.F2, E1.F3 | Legal & Compliance |
| 7 | Approve & Submit | E1.F3 | Legal & Compliance |
| 8 | Deadline-Risk Notification | E2.F2 | Both |
| 9 | Audit Trail — Per-Filing History | E1.F3, E1.F4 | Legal & Compliance |
| 10 | Historical Filing Access — "My Filings" | E1.F4 | Corporate Executive |
| 11 | Error Recovery — Validation & State Conflicts | E1.F2, E1.F3 | Legal & Compliance |
| 12 | End-to-End Journey (Composite) | E1, E2 | Both |

---

## Flow 1: Authentication — Mocked SSO Persona Switch
**Feature:** E1 (cross-cutting) · **Screen:** Layout header, all screens

```
                    ┌───────────────────────────────┐
                    │  Layout header (every screen)  │
                    │  "Logged in as:" [ selector ▾]  │
                    └───────────────┬─────────────────┘
                                    │
                          User selects a demo persona
                                    │
                    ┌───────────────┴────────────────┐
                    ▼                                ▼
           Executive persona                 Legal & Compliance persona
        (exec-1 / exec-2 / exec-3)              (legal-1 / legal-2)
                    │                                │
                    ▼                                ▼
        AuthContext sets authHeader           AuthContext sets authHeader
        "Bearer {execId}-token"               "Bearer {legalId}-token"
                    │                                │
                    ▼                                ▼
        Every subsequent fetch carries this Authorization header
        AuthFilter resolves it server-side against the seeded users table
        (see api/.../security/AuthFilter.java)
```

**What this stands in for:** production EquatePlus authenticates via corporate SSO (Okta, SAML/OIDC — `kb-L1-computershare-enterprise-architecture.md` EA5). There is no login form anywhere in this UI — inventing one would misrepresent both the target architecture (no bespoke login is ever built against EA5) and this MVP's actual mechanism (a persona dropdown, not a credential form). The dropdown is the honest MVP stand-in for "already authenticated via SSO, here's who you are."

**Accessibility notes:**
- Persona `<select>` has a visible, associated `<label>` ("Logged in as:")
- Selection change is synchronous, no loading state needed (no network call)
- Keyboard-operable natively via native `<select>` semantics

---

## Flow 2: Role Enforcement & Access Control
**Feature:** E1.F3 (cross-cutting to every write endpoint) · **Screens:** all

```
  User authenticated (→ Flow 1)
         │
         ▼
  Role resolved server-side on EVERY request
  (AuthFilter sets request attribute; each controller re-checks role —
   never trusted from client state alone)
         │
    ┌────┴─────────────────────────────┐
    ▼                                  ▼
  role = EXECUTIVE               role = LEGAL_COMPLIANCE
    │                                  │
    ▼                                  ▼
  GET /filings → own filings      GET /filings → all filings (org-scoped)
  only, forced server-side        PATCH /filings/{id} → allowed
  PATCH /filings/{id} → 403       POST /filings/{id}/approve → allowed
  POST .../approve → 403          GET /filings/{id}/audit-log → any filing
  GET .../audit-log → own only,   GET /notifications?userId= → own only
    403 on another's filing
    (found missing — IDOR — at
     Phase 6 code review, fixed)
  GET /notifications?userId= →
    own only, 403 otherwise
    (found missing — IDOR — at
     Phase 6 code review, fixed)
  POST /demo/simulate-trade →
    own executiveId only, 403
    if body.executiveId differs
    from caller (found missing
    at code review, fixed)
```

**No 403 page exists in the UI** — the MVP's screens simply never render an affordance that would trigger one for the current persona (e.g. `FilingReview.tsx`'s Approve button is present regardless of role, but the API call would 403 if an executive were somehow driving it; the UI does not yet hide it client-side for the wrong role, relying on the server-side check as the real boundary). **Gap, not a silent one:** a UI-level role gate (hiding Approve/Edit controls for executives) is a reasonable follow-up, tracked here rather than assumed done.

**Accessibility notes:**
- Server-side enforcement means no DOM manipulation can bypass role gating — this holds regardless of any future UI-level hiding
- Error responses (403) are currently surfaced generically by `fetch` rejection; the UI does not yet render a dedicated, accessible error message for a 403 specifically (see Flow 11)

---

## Flow 3: Simulate Trade → Form Generation (golden path)
**Feature:** E1.F1 · **Stories:** E1.F1-S1 · **Screen:** Executive Home `/`

```
                         ┌─────────────────────┐
                         │   Executive Home /   │
                         │  [Simulate Trade]     │
                         └──────────┬──────────┘
                                    │
                        Executive clicks Simulate Trade
                                    │
                                    ▼
                  POST /api/demo/simulate-trade
                  { executiveId: self, transactionCode: "S",
                    shares: <random 100-999>, pricePerShare: <random> }
                                    │
                    ┌───────────────┴───────────────┐
                    ▼                                ▼
              201 Created                        4xx error
                    │                                │
                    ▼                                ▼
        Filing status VALIDATED              Toast-style inline message:
        (fields present, all valid —          "Could not generate form
         the common case)                      (HTTP {status})."
                    │
                    ▼
        ┌─────────────────────────────┐
        │ "Form 4 generated for your   │  ← inline status message (role="status")
        │  trade."                     │
        └──────────────┬───────────────┘
                        │
                        ▼
        My Filings list refreshes (GET /filings?executiveId=self)
        New row appears: Form 4 · {date} · {shares} sh @ ${price} · [Validated]
                        │
                        ▼
        NotificationBell count increments on next 5s poll
        (FORM_GENERATED notification created server-side)
```

**Accessibility notes:**
- "Simulate Trade" is a real `<button>`, keyboard-activatable, disabled state (`Simulating…`) announced via its own text change
- The result message renders in a `role="status"` region — screen readers announce it without stealing focus
- Each filing row (`data-testid="filing-row"`) shows status via `FilingStatusBadge`, a text-labeled badge (not color-only) — see `computershare-design-system.md` accessibility guidance

---

## Flow 4: Incomplete Transaction Data
**Feature:** E1.F1 · **Story:** E1.F1-S3 · **Screen:** Executive Home `/` (trigger only reachable via a manually-crafted request in the MVP — the UI's Simulate Trade button always sends complete data)

```
  Required field (e.g. transactionCode) omitted from the request
         │
         ▼
  ┌──────────────────────────────────────────────┐
  │ FormGenerationService.generate()              │
  │ requiredFieldMissing = true                   │
  └─────────────────────┬────────────────────────┘
                        │
                        ▼
  Filing saved: status = INCOMPLETE
  AuditLogEntry: FLAGGED_INCOMPLETE
                        │
          ┌─────────────┴─────────────┐
          ▼                           ▼
  Notification to executive     Notification to ALL
  (INCOMPLETE_DATA)             LEGAL_COMPLIANCE users
  "Your Form 4 could not be     (INCOMPLETE_DATA)
   fully generated — missing    "A Form 4 for {name} is
   required data."               missing required data."
```

**Design decision carried from Phase 4's design-quality gate:** the notification goes to **both** personas, not legal-only, because an executive with an incomplete filing and no visibility into it is a worse outcome than a slightly noisier notification — this was an explicit fix during design review, not an accident of implementation.

**Gap:** no UI path currently *triggers* this flow — the demo's "Simulate Trade" button always sends complete data. It is fully implemented and tested (`FilingWorkflowTests.s2_missingRequiredField...`) but only reachable via a direct API call in this MVP, not a UI affordance. Noted rather than silently glossed over.

**Accessibility notes:**
- Both notifications land in the standard `NotificationDrawer` list — no separate accessible treatment currently exists for "urgent" vs. routine notifications (a `DEADLINE_RISK`/`INCOMPLETE_DATA` item reads identically to a routine `FORM_GENERATED` one in the list markup today)

---

## Flow 5: Compliance Dashboard — Monitor & Filter
**Feature:** E2.F3 · **Stories:** E2.F3-S1, E2.F3-S2 · **Screen:** Compliance Dashboard `/dashboard`

```
  Legal & Compliance opens /dashboard
         │
         ▼
  GET /api/filings   (no filters — all filings in scope)
         │
         ▼
  ┌──────────────────────────────────────────────────────────┐
  │ Compliance Dashboard                                      │
  │ Filter: [ Status ▾ ]  [ Search: issuer or executive__ ]   │
  │                                                            │
  │ Executive     Form   Status         Deadline (EDGAR)       │
  │ exec-1        FORM_4 Under Review   2026-09-03 21:30       │
  │ exec-2        FORM_4 Validated      2026-09-04 21:30       │
  │ exec-3        FORM_4 Submitted      —                      │
  └───────────────────┬────────────────────────────────────────┘
                      │
        ┌─────────────┼─────────────────────┐
        ▼             ▼                     ▼
  Change status    Type in search      Click a row
  filter dropdown  box (issuer or
        │          executive match)         │
        ▼                │                  ▼
  GET /filings?status=X   ▼          Navigate to
        │           GET /filings?    /filings/{id}
        ▼           search=Y              │
  List re-renders,        │               ▼
  sorted by                ▼        → Flow 6
  edgarCutoffAt      List re-renders
  (client-side)      (same sort)
```

**Design decision (Phase 4):** the table sorts by deadline urgency (`edgarCutoffAt`) by default, not creation date — so the row most at risk of missing its filing deadline is always visible near the top without the officer having to sort manually.

**Accessibility notes:**
- Filter `<select>` and search `<input>` both carry `aria-label`s
- Table uses semantic `<table>`/`<thead>`/`<tbody>` markup (see `wireframes.html`), not styled `<div>`s
- Executive-name cells are real `<Link>`s to the filing detail — keyboard-focusable, not click-handlers on a non-interactive element

---

## Flow 6: Review & Edit a Filing
**Feature:** E1.F2, E1.F3 · **Stories:** E1.F2-S1, E1.F3-S1 · **Screen:** Filing Review `/filings/:id`

```
  Legal & Compliance on Filing Review screen
  (arrived via Flow 5's row click)
         │
         ▼
  ┌──────────────────────────────────────────────┐
  │ Form 4 — {executiveId} · {date}   [Status]     │
  │ Issuer: Ascendion INC — Demo Issuer                │
  │ Reporting Person: {name}                        │
  │ Transaction Code: S                             │
  │ Shares:     [____]  ← editable input            │
  │ Price:      [____]  ← editable input            │
  │ [ Save Edits ]   [ Approve & Submit ]           │
  └──────────────────────┬─────────────────────────┘
                         │
              Officer edits Shares to an invalid value (e.g. 0)
              Clicks [ Save Edits ]
                         │
                         ▼
  PATCH /filings/{id} { fields: { shares: 0 } }
                         │
             ┌───────────┴────────────┐
             ▼                        ▼
        200 VALIDATED             422 {errors}
        (if the new value        (if invalid)
         happens to be valid)          │
                                       ▼
                          Inline error rendered under the
                          Shares field: "shares: must be
                          greater than 0" (role="alert"
                          on the enclosing error region)
                          Approve button stays disabled
                          (filing.status !== 'VALIDATED')
```

**Accessibility notes:**
- Each input has a real `<label htmlFor>` (`Shares`, `Price per share`)
- Field-level errors render adjacent to their field, not only in a page-level summary
- `filing.validationErrors` (server-computed, from generation or the last failed edit) renders in a `role="alert"` block above the form when non-empty

---

## Flow 7: Approve & Submit
**Feature:** E1.F3 · **Story:** E1.F3-S2 · **Screen:** Filing Review `/filings/:id`

```
  Filing status = VALIDATED
  [ Approve & Submit ] button is enabled
  (disabled whenever status !== 'VALIDATED' — the only
   client-side gate; server re-checks independently)
         │
         │ Officer clicks Approve & Submit
         ▼
  POST /api/filings/{id}/approve
         │
    ┌────┴────────────────────┐
    ▼                         ▼
  200 SUBMITTED           409 Conflict
    │                    (status changed
    ▼                     concurrently by
  Status badge            another session —
  updates to               not currently
  "Submitted"              surfaced as a
  Audit trail gains         distinct message
  APPROVED + SUBMITTED       in the UI, see
  entries (visible via       Flow 11)
  "Show audit trail")
    │
    ▼
  Notifications created for both the executive
  (SUBMITTED) and all Legal & Compliance users (SUBMITTED)
  — visible in NotificationBell on next poll
```

**Accessibility notes:**
- The Approve button's `disabled` state is a real HTML `disabled` attribute (keyboard/screen-reader correct), not a CSS-only visual disable
- No explicit `aria-live` announcement on successful submission today beyond the status badge re-render — a follow-up should add one so screen-reader users get the same "it worked" signal sighted users get from the badge color/text change

---

## Flow 8: Deadline-Risk Notification
**Feature:** E2.F2 · **Story:** E2.F2-S2 · **Actor:** System (background), surfaced to both personas

```
  DeadlineRiskEvaluator.evaluate() — every 60 seconds (@Scheduled)
         │
         ▼
  Query: filings WHERE status != SUBMITTED
           AND deadline_risk_fired = false
           AND edgar_cutoff_at < now() + 12h
         │
    ┌────┴─────────────────┐
    ▼                       ▼
  none found            one or more found
    │                       │
    ▼                       ▼
  (no-op)          For each: notify executive (DEADLINE_RISK)
                           notify ALL Legal & Compliance (DEADLINE_RISK)
                           set deadline_risk_fired = true
                                    │
                                    ▼
                   Next poll: NotificationBell count increments
                   for both the affected executive and every
                   legal/compliance user, independent of who's
                   currently viewing the app
                                    │
                                    ▼
                   DeadlineRiskBanner also renders inline on any
                   filing row/detail view where hoursRemaining <= 12
                   (computed client-side from edgarCutoffAt —
                    a second, redundant signal alongside the
                    notification, deliberately not the only one)
```

**Why two signals (notification + inline banner) for the same risk:** the notification is push-oriented (you see it even if you're not looking at the affected filing right now); the banner is pull-oriented (you see it the moment you do look at the filing). Relying on only one would either miss users who aren't actively watching the dashboard, or clutter the notification feed with something already visible on-screen.

**Accessibility notes:**
- `DeadlineRiskBanner` renders with `role="alert"` and `aria-live="polite"` — announced without an explicit user action
- The banner's icon is a real inline SVG with `aria-hidden="true"` (decorative — the adjacent text carries the meaning), not an emoji standing in for semantic content (`computershare-design-system.md` EA11)

---

## Flow 9: Audit Trail — Per-Filing History
**Feature:** E1.F3, E1.F4 · **Stories:** E1.F3-S3, E1.F4-S2 · **Screen:** Filing Review `/filings/:id`

```
  Legal & Compliance on Filing Review screen
         │
         │ Clicks [ Show audit trail ]
         ▼
  GET /api/filings/{id}/audit-log
         │
         ▼
  ┌────────────────────────────────────────────────────┐
  │ {timestamp} — GENERATED by exec-1: Form 4 generated  │
  │   from simulated trade.                              │
  │ {timestamp} — EDITED by legal-1: Fields updated:      │
  │   [shares]                                            │
  │ {timestamp} — APPROVED by legal-1: Approved by legal  │
  │   & compliance.                                       │
  │ {timestamp} — SUBMITTED by legal-1: Mock EDGAR         │
  │   submission confirmation: MOCK-EDGAR-{id}             │
  └────────────────────────────────────────────────────┘
```
Entries are ordered `occurred_at ASC` — the story unfolds top to bottom in the order it happened, matching how a compliance reviewer would read it.

**Accessibility notes:**
- Rendered as a plain `<ul>` list — each entry is one list item, screen-reader-navigable by list semantics
- The "Show/Hide audit trail" toggle button's label text itself communicates state (no separate `aria-expanded` yet — a real gap, not claimed as done)

---

## Flow 10: Historical Filing Access — "My Filings"
**Feature:** E1.F4 · **Story:** E1.F4-S1 · **Screen:** Executive Home `/`

```
  Executive on Home screen
         │
         ▼
  GET /api/filings?executiveId=self
         │
         ▼
  ┌─────────────────────────────────────────────┐
  │ My Filings                                    │
  │ Form 4 · 2026-08-28 · SUBMITTED               │
  │ Form 4 · 2026-08-30 · UNDER REVIEW             │
  │   Deadline risk: 9h remaining                  │  ← Flow 8's banner, inline
  └─────────────────────────────────────────────┘
```
Server-side RBAC (Flow 2) means this endpoint returns only the caller's own filings even if `executiveId` were tampered with client-side — the query param is a convenience for the legal/compliance role (which passes it explicitly to scope a lookup), not a trust boundary for the executive role (which is always forced to `self` regardless of what's requested).

**Accessibility notes:** identical pattern to Flow 5's table — see there.

---

## Flow 11: Error Recovery — Validation & State Conflicts
**Feature:** E1.F2, E1.F3 · **Screen:** Filing Review `/filings/:id`

```
  Officer attempts an action that the server rejects
         │
    ┌────┴─────────────────────────────────────┐
    ▼                                           ▼
  422 Validation failure (PATCH)          409 State conflict (approve
    │                                      on non-VALIDATED, or edit
    ▼                                      on SUBMITTED)
  Field-level errors parsed from                 │
  response body, rendered inline                 ▼
  under each affected input               Currently: request simply
  (see Flow 6)                            fails silently in the UI —
                                           the catch block swallows it
                                           with only a code comment
                                           ("nothing additional to
                                           show the user here")
                                           GAP: no user-visible message
                                           for a 409 today. The state
                                           itself can't drift (the
                                           server is the source of
                                           truth and the Approve button
                                           re-disables on next fetch),
                                           but the officer gets no
                                           explanation for *why* the
                                           click did nothing.
```

**This gap is intentionally left visible in this document, not silently fixed while rewriting the flow docs** — the point of a user-flow document is to describe what the system actually does, including where it currently falls short, so it can be prioritized rather than rediscovered.

**Accessibility notes:**
- 422 error rendering: see Flow 6 (this path is properly accessible)
- 409 handling: no accessible error surface exists yet — a `role="alert"` toast for this specific case is a concrete, scoped follow-up

---

## Flow 12: End-to-End Journey (Composite)
**Features:** E1, E2 · **Actors:** Corporate Executive, Legal & Compliance Officer

```
START: Executive opens the app, already "authenticated" (→ Flow 1)
         │
         ▼
   Executive Home ( / )
   Clicks [ Simulate Trade ]                    ← Flow 3
         │
         ▼
   Filing generated, status VALIDATED
   Executive sees it in "My Filings"            ← Flow 10
         │
         │ (switch persona via header dropdown → Flow 1)
         ▼
   Legal & Compliance opens Compliance Dashboard ← Flow 5
   Sorted by deadline urgency, filters to
   status = UNDER_REVIEW / VALIDATED as needed
         │
         │ Clicks the new filing's row
         ▼
   Filing Review screen                          ← Flow 6
   Reviews pre-filled fields against the
   simulated transaction — fields already valid,
   no edit needed this time
         │
         ▼
   Clicks [ Approve & Submit ]                   ← Flow 7
   Status → SUBMITTED, mock EDGAR confirmation
   logged to the audit trail
         │
         ▼
   Show audit trail                              ← Flow 9
   GENERATED → APPROVED → SUBMITTED, each with
   actor and timestamp
         │
         │ (switch back to Executive persona)
         ▼
   Executive sees the SUBMITTED status update on
   Home, and a SUBMITTED notification in the bell ← Flow 8 pattern (push)
         │
         ▼
END: Filing is submitted, audited, and both personas
     have independent confirmation of the outcome.
```

This is the exact path verified live in a browser against the real running application during Phase 6 construction (see `docs/sdlc/quality-gates.md` Phase 6 entry) — not a hypothetical composite, a description of what was actually clicked through and confirmed working.

---

## Screen ↔ Flow Traceability Matrix

| Screen | Route | Feature(s) | Flow(s) | Story IDs |
|--------|-------|------|---------|--------|
| Executive Home | `/` | E1.F1, E1.F4, E2.F2 | 1, 3, 4, 8, 10, 12 | E1.F1-S1, E1.F1-S3, E1.F4-S1, E2.F2-S1, E2.F2-S2, E2.F2-S3 |
| Compliance Dashboard | `/dashboard` | E2.F3 | 1, 2, 5, 8, 12 | E2.F3-S1, E2.F3-S2 |
| Filing Review | `/filings/:id` | E1.F2, E1.F3 | 1, 2, 6, 7, 8, 9, 11, 12 | E1.F2-S1, E1.F3-S1, E1.F3-S2, E1.F3-S3 |
| Notifications panel | (overlay, all screens) | E2.F2 | 4, 8, 12 | E2.F2-S1, E2.F2-S2, E2.F2-S3 |

---

## Accessibility Summary (All Screens)

| Requirement | Implementation | Status |
|-------------|---------------|-------------------|
| Colour contrast | Not measured against WCAG 2.1 AA thresholds for this MVP's ad hoc inline styles | Gap — see hld.md §7 |
| Keyboard navigation | All interactive elements are real `<button>`/`<select>`/`<input>`/`<a>` — no click-handler-on-`<div>` patterns | Met |
| Focus management | No explicit focus-trap or focus-return logic anywhere (no modals exist in this MVP to require it) | N/A currently |
| Screen reader announcements | `role="alert"` + `aria-live="polite"` on `DeadlineRiskBanner` and field-level PATCH errors; nothing yet for the 409 case (Flow 11) or the Approve success case (Flow 7) | Partial |
| Minimum touch target | Not explicitly enforced (no 44px minimum applied) — default browser control sizing only | Gap |
| Semantic HTML | `<table>`/`<thead>`/`<tbody>` for lists, `<label htmlFor>` on every form input, landmark-free (no `<nav>`/`<main>` wrapper yet in `Layout.tsx`) | Partial |
| Form accessibility | Labels present; `aria-describedby` for help text not used (no help text exists in this MVP's forms) | Met for what exists |
| No emoji as icon substitute | Enforced — `NotificationBell` and `DeadlineRiskBanner` use inline SVG with `aria-hidden`/`aria-label`, per `computershare-design-system.md` EA11 | Met |
| Automated audit | axe-core / Playwright accessibility audit | Not run — gap, see hld.md §7 |

> **Honesty note:** unlike a mature product's accessibility summary (which can claim a completed Sprint-4-style audit), this MVP's summary is a mix of "met," "partial," and "gap" because that's what's actually true of the code today. Overstating this table would undermine the rest of the document's credibility.
