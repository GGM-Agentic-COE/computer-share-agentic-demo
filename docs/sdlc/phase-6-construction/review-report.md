# Code Review Report — Automated Regulatory Filing Module (MVP Construction)

**Agent:** `L1-construction-code-reviewer` · **Phase:** 6 · **Correlation ID:** `C885C23C-949E-440C-8B0E-04FDD166A559`
**Scope:** `api/src/main/java` (Spring Boot), `ui/src` (React/TypeScript)
**Method:** Independent review — a separate agent with no access to the construction reasoning trace, instructed to hunt for real bugs and vulnerabilities within the MVP's documented simplifications (mocked SSO, mocked EDGAR, H2-in-place-of-Postgres), not to flag those simplifications themselves.

## Findings and resolution

| # | Severity | Finding | File | Status |
|---|---|---|---|---|
| 1 | High | IDOR — any authenticated user could read any filing's audit log by ID, no ownership check | `AuditController.java` | **Fixed** — added the same executive-owns-filing check `FilingController.get()` already had |
| 2 | High | IDOR — `userId` query param on `/notifications` wasn't checked against the authenticated caller | `NotificationController.java` | **Fixed** — endpoint now 403s unless `userId` matches the authenticated user |
| 3 | High | State-machine violation — editing a `SUBMITTED` (terminal) filing was allowed, silently reverting it to `VALIDATED` and enabling a duplicate approve/submit | `FilingEditService.java` | **Fixed** — edit is now blocked with 409 once a filing is `SUBMITTED` |
| 4 | Medium | Deadline correctness — `transactionDate` was computed in the JVM default zone while `BusinessDayCalculator` assumes America/New_York, risking an off-by-a-day deadline/EDGAR-cutoff on a non-ET-deployed server | `FormGenerationService.java`, `BusinessDayCalculator.java` | **Fixed** — `transactionDate` now anchored explicitly to `America/New_York` |
| 5 | Medium | Authorization gap — the trade-simulation request body's `executiveId` wasn't checked against the authenticated caller, letting one user attribute a trade to another executive | `DemoController.java` | **Fixed** — now requires the authenticated user to be an `EXECUTIVE` and `executiveId` to match their own id |
| 6 | Low | Unhandled deserialization errors — a malformed PATCH field (e.g. non-numeric `shares`) threw an uncaught exception, producing a raw 500 instead of the API's structured 422 | `FilingEditService.java` | **Fixed** — field application now collects parse errors and returns them via the existing `ValidationFailedException` (422) path |

All six findings were fixed, not deferred — this is a from-scratch MVP with no production traffic depending on the prior (vulnerable) behavior, so there was no reason to ship known IDORs into even a demo build. Three new regression tests were added (`executiveCannotSimulateTradeForAnotherExecutive`, `notificationsAreScopedToTheAuthenticatedUser`, `submittedFilingCannotBeEditedAgain`) to lock in findings 1/2/5 and 3 specifically, alongside the existing 6.

## Verification after fixes

- `mvn test` — **9/9 passing** (6 original + 3 new regression tests for this review's findings).
- `npm run build` (UI) — clean, no TypeScript errors.
- Manual browser walkthrough of the golden path re-run after the fixes (simulate trade → dashboard → approve → submit → audit trail → notification) — confirmed still working end-to-end.

## What this review did not cover

- Load/concurrency testing against the 1,000-trades/minute NFR — out of scope for a single-instance H2 demo.
- A real penetration test of the mocked-SSO token scheme — it is explicitly a stand-in, not a security boundary intended to survive adversarial testing; a real corporate SSO integration is required before any non-demo use.
- Accessibility (WCAG 2.1 AA) audit — the UI applies conscious patterns (semantic HTML, `aria-live`, labeled inputs) but was not run through an automated or manual accessibility audit tool.
