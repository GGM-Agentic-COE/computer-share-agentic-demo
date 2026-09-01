# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

An MVP implementation of Computershare EquatePlus's **Automated Regulatory Filing Module** — auto-generates SEC Form 4 filings from executed securities transactions, routes them through a legal review/edit/approve workflow, and tracks them through submission. Two components:

- **`api/`** — Spring Boot 3 (Java 17) backend, H2 in-memory DB (dev/demo stand-in for the documented PostgreSQL target)
- **`ui/`** — React 18 + TypeScript + Vite frontend, styled with Tailwind CSS + shadcn/ui (Radix primitives)

This was built via a simulated agentic SDLC (see `docs/sdlc/`) — the planning/design docs there (PRD, epics, HLD, LLD, user flows, wireframes) are the authoritative source for *why* the code looks the way it does, not just historical artifacts. When making a non-trivial change, check `docs/sdlc/phase-4-design/lld.md` and `hld.md` first — they document the real state machine, RBAC matrix, and per-endpoint contracts in depth.

`PRD.txt` and `epics-from-prd.md` (repo root) are the real product requirements / epic-feature-story breakdown (AAVA™ Product Studio output, marked confidential) — treat epic/feature IDs (`E1`, `E1.F1`, `E1.F1-S1`, etc.) as the organizing structure for any new work, and don't copy their contents into external services.

## Commands

**Backend** (from `api/`):
```bash
mvn spring-boot:run                          # run the API on :8080
mvn test                                     # run all tests
mvn test -Dtest=FilingWorkflowTests#s1_generateFormWithValidTrade_isImmediatelyValidated   # single test
mvn compile                                  # compile only
```

**Frontend** (from `ui/`):
```bash
npm install       # first time only
npm run dev       # Vite dev server on :5173
npm run build     # tsc -b && vite build — type-checks then builds
npm test          # vitest run (all tests)
npx vitest run src/test/FilingStatusBadge.test.tsx   # single test file
```

Start the backend before the frontend — the UI calls `http://localhost:8080/api` directly (hardcoded in `ui/src/api.ts`), and CORS is locked to `http://localhost:5173` in `application.yml`.

No linter is configured in either project (no ESLint/Prettier config, no Checkstyle/Spotless in `pom.xml`) — don't invent lint commands.

## Architecture

### Backend layering (`api/src/main/java/com/computershare/regfiling/`)

Strict `web → service → repository/domain` dependency direction:
- `domain/` — JPA entities (`Filing`, `AuditLogEntry`, `Notification`, `User`) and enums (`FilingStatus`, `AuditAction`, `NotificationType`, `UserRole`)
- `repository/` — Spring Data JPA interfaces
- `service/` — business logic (`FormGenerationService`, `ValidationService`, `FilingEditService`, `ApprovalService`, `AuditLogService`, `NotificationService`, `DeadlineRiskEvaluator`, `BusinessDayCalculator`, `EdgarSubmissionConnector`)
- `web/` — `@RestController`s + `dto/` request/response types + `GlobalExceptionHandler` (`@RestControllerAdvice`, maps typed exceptions in `ApiExceptions` to 404/403/409/422)
- `security/AuthFilter.java` — mocked-SSO auth: parses `Bearer {userId}-token`, resolves against `DataSeeder`'s 5 seeded users, sets a request attribute read via `web/CurrentUserResolver.java`. **Not a real auth scheme** — a placeholder for real corporate SSO (see `docs/architecture/kb-L1-computershare-enterprise-architecture.md` EA5). Explicitly bypasses `OPTIONS` requests so CORS preflight isn't blocked.
- `config/` — `DataSeeder` (seeds demo users on startup) and `WebConfig` (CORS + registers `AuthFilter` scoped to `/api/*`)

**Filing state machine** (the core business logic — see `lld.md` §3/§6 for the full transition table): `INCOMPLETE` (required field missing at generation) / `UNDER_REVIEW` (fields present, a value invalid) / `VALIDATED` (fields present and valid) / `SUBMITTED` (terminal, after approve). `GENERATED`/`EDITED`/`APPROVED`/`SUBMITTED`/`FLAGGED_INCOMPLETE` are `AuditAction` values — action names, never resting `FilingStatus` values; don't conflate the two enums.

**Deadline-risk logic is deliberately non-obvious**: `DeadlineRiskEvaluator` (a `@Scheduled` job, 60s interval) fires against `Filing.edgarCutoffAt` (the EDGAR 5:30pm ET same-day filing cutoff), **not** `Filing.deadlineAt` (the raw 2-business-day statutory deadline) — the cutoff is stricter. `BusinessDayCalculator` computes both, anchored to `America/New_York` (not the JVM default zone — this was a real bug found during construction).

RBAC is enforced server-side in every controller (not just at the filter layer) — `EXECUTIVE` role is always scoped to its own data regardless of query params; `LEGAL_COMPLIANCE` can act on any filing. See the authorization matrix in `docs/sdlc/phase-4-design/hld.md` §5 before adding a new endpoint.

### Frontend structure (`ui/src/`)

- `pages/` — one per route: `ExecutiveHome` (`/`), `ComplianceDashboard` (`/dashboard`), `FilingReview` (`/filings/:id`)
- `components/` — shared UI (`Layout`, `NotificationBell`, `FilingStatusBadge`, `DeadlineRiskBanner`)
- `components/ui/` — shadcn/ui primitives (button, card, badge, input, label, select, table, popover, alert), hand-adapted for Tailwind v3 (not the v4-style output `shadcn@latest init` produces by default — see the design-tokens note below)
- `AuthContext.tsx` — mocked-SSO persona switcher (`DEMO_USERS`); no real login form exists anywhere, by design (mirrors the backend's mocked auth)
- `api.ts` — one typed function per backend endpoint, all going through a shared `fetch` wrapper
- Path alias `@/*` → `src/*` (configured in both `vite.config.ts` and `tsconfig.json`)

**Design tokens**: `src/index.css` defines HSL CSS variables (brand primary `#93186c`, secondary `#432063`, plus separate semantic status colors) computed from `docs/design/computershare-design-system.md`. `tailwind.config.js` maps them to Tailwind color utilities in the classic v3 pattern (`primary: 'hsl(var(--primary))'`, etc.) — if adding a new shadcn component via the CLI, its default output will assume Tailwind v4 (oklch colors, `@theme` blocks, `in-*` variants) and needs adapting to this v3 setup, not used as-is.

### Cross-cutting

- **OpenAPI spec**: `docs/sdlc/phase-4-design/openapi.yaml` — the contract both sides were built from; update it alongside any endpoint change.
- **DB schema**: `api` runs on H2 with Hibernate `ddl-auto: update` (schema regenerates from JPA annotations on every restart — no persistence, no migrations). `docs/sdlc/phase-4-design/db-schema.sql` documents the target PostgreSQL/Flyway schema; the two are kept conceptually aligned but H2 does not execute the `.sql` file.
- **Everything is mocked at the integration boundary, on purpose**: `EdgarSubmissionConnector` never calls a real external system; `AuthFilter` never calls a real IdP. Both are documented gaps against `docs/architecture/kb-L1-computershare-enterprise-architecture.md` (a hypothetical enterprise-architecture reference for the target stack), not silent shortcuts — see that doc's EA18 for the full list of MVP-vs-target deviations.
- **No emoji anywhere** in code, UI, or docs — icons are `lucide-react` (frontend) or real SVG, never a Unicode glyph standing in for one.
