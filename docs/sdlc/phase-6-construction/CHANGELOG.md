# Changelog — Automated Regulatory Filing Module

**Agent:** `L1-construction-doc-generator` · **Phase:** 6 · **Correlation ID:** `C885C23C-949E-440C-8B0E-04FDD166A559`

## 0.1.0-mvp — Initial construction

Built from [openapi.yaml](../phase-4-design/openapi.yaml), [hld.md](../phase-4-design/hld.md), [lld.md](../phase-4-design/lld.md), and [db-schema.sql](../phase-4-design/db-schema.sql).

**Added**
- Spring Boot API (`api/`) implementing E1.F1 through E1.F4, E2.F2, E2.F3 (Form 4 generation, validation, review/edit/approve workflow, audit logging, in-app notifications, compliance dashboard aggregation).
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

**Not built in this MVP** (see [cycle-plan.md](../phase-2-planning/cycle-plan.md) backlog): real-time data streaming (E2.F1, simplified to polling), export (E2.F4), full accessibility/HA infrastructure (E2.F5), sync-error detection (E3.F1), bulk/rapid-trade handling (E3.F2), additional regulatory forms (E3.F3), third-party analytics integration (E3.F4).

## 0.2.0 — Design system applied via shadcn/ui

Restyled the entire `ui/` app to the real Computershare design system tokens (`docs/design/computershare-design-system.md`) and the `wireframes.html` layout, using shadcn/ui — no functional regressions (all 9 backend + 5 frontend tests still pass; golden path re-verified live in a browser after the restyle).

**Added**
- Tailwind CSS 3 + shadcn/ui (classic HSL CSS-variable theme, not the v4-oriented default the `shadcn` CLI initially scaffolded — rewritten for v3 compatibility, see below).
- `src/components/ui/`: `button`, `card`, `badge`, `input`, `label`, `select` (Radix), `table`, `popover` (Radix), `alert` — hand-adapted from shadcn's registry output.
- Brand tokens in `src/index.css` (HSL, computed directly from the design system's hex values): primary `#93186c`, secondary `#432063`, plus semantic status colors (`--status-incomplete/review/validated/submitted`) kept separate from the brand accent.
- Manrope loaded via Google Fonts in `index.html`, matching the real site's only typeface.
- `lucide-react` for all icons (bell, warning triangle, arrow-left, chevron, check) — zero emoji anywhere, per the design system's EA11 guidance; `NotificationBell` and `DeadlineRiskBanner` (already fixed once for emoji removal) now use these instead of the earlier hand-rolled inline SVGs.

**Fixed during this pass**
- The `shadcn@latest init` CLI (v4.19.1) defaulted to a Tailwind-v4-style setup (`@import "shadcn/tailwind.css"`, oklch colors, `@theme` blocks, `in-*` variants) incompatible with the Tailwind 3.4 already installed — `src/index.css` and `tailwind.config.js` were rewritten to the classic v3 HSL-variable pattern, and `button.tsx` was rewritten to drop v4-only utility syntax that would have silently rendered unstyled.
- Radix `Select.Item` cannot take `value=""`; the dashboard's "All statuses" filter now uses a `ALL` sentinel mapped back to an empty filter, rather than reproducing the native `<select>`'s empty-string convention.

**Not changed**
- No API/business-logic code touched — this pass is UI-presentation only. All existing component behavior, state management, and API contracts are unchanged.
