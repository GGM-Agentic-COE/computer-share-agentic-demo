# Test Scenarios — Automated Regulatory Filing Module

**Agent:** `L1-testing-scenario-writer` · **Phase:** 5 · **Correlation ID:** `C885C23C-949E-440C-8B0E-04FDD166A559`
**Inputs:** [stories.json](../phase-3-inception/stories.json) (with acceptance criteria)

Every scenario traces to a specific story's AC — none are free-floating.

| Scenario | Story | Traces to AC |
|---|---|---|
| S1 — Generate Form 4 after valid trade | STORY-1 | "GIVEN a valid transaction... THEN a pre-filled Form 4 is generated within 5 minutes" |
| S2 — Incomplete transaction data flags the form | STORY-1 | "GIVEN a transaction is missing required data... THEN the form is flagged incomplete and legal is notified" |
| S3 — Submission blocked on validation failure | STORY-2 | "GIVEN a required field is missing or invalid... THEN submission is blocked and a specific error is shown" |
| S4 — Valid submission proceeds | STORY-2 | "GIVEN all required fields are correctly filled... THEN submission proceeds" |
| S5 — Edit pre-filled form field | STORY-3 | "GIVEN a pre-filled form is generated... WHEN I edit a field THEN the change is saved and re-validated" |
| S6 — Invalid edit rejected | STORY-3 | "GIVEN I enter invalid data... THEN a validation error is displayed" |
| S7 — Approve validated form | STORY-4 | "GIVEN a form passes validation... THEN it is marked ready-for-submission" |
| S8 — Approval blocked pre-validation | STORY-4 | "GIVEN a form has not passed validation... THEN approval is blocked" |
| S9 — Audit entry on every action | STORY-5 | "GIVEN a user performs generate/edit/approve/submit... THEN an audit entry is written" |
| S10 — View filing history | STORY-6 | "GIVEN I am authorized... THEN I see all my filings with status and key dates" |
| S11 — Notify on generation | STORY-7 | "GIVEN a form is generated... THEN the executive sees an in-app notification" |
| S12 — Deadline-risk alert fires at 12h against EDGAR cutoff | STORY-8 | "GIVEN a filing is unsubmitted and less than 12 hours remain against the EDGAR 5:30pm ET cutoff... THEN a deadline-risk notification is shown" |
| S13 — Notify on submission | STORY-9 | "GIVEN a form is submitted... THEN a confirmation notification is shown" |
| S14 — Dashboard shows all filings | STORY-10 | "GIVEN I access the dashboard... THEN all filings are displayed with status and deadline countdowns" |
| S15 — Filter/search dashboard | STORY-11 | "GIVEN filings exist across multiple executives/statuses... WHEN I apply a filter THEN only matching filings are shown" |

**Coverage note:** 15 scenarios against 11 stories (STORY-1 through STORY-4 each have 2 scenarios covering their compound AC; the rest have 1). No story is left without at least one scenario.
