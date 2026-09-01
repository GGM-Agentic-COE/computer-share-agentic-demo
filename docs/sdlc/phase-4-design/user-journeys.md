# User Journeys — Automated Regulatory Filing Module

**Agent:** `L1-design-user-journey-mapper` · **Phase:** 4 · **Correlation ID:** `C885C23C-949E-440C-8B0E-04FDD166A559`
**Inputs:** [stories.json](../phase-3-inception/stories.json) · target personas (vision.md)

## Journey A — Corporate Executive: "My trade, filed for me"

1. **Trigger:** Executive executes a trade in EquatePlus (MVP: simulated via a "Simulate Trade" demo action — WU-09).
2. **Wait (< 5 min):** System detects the transaction, generates the pre-filled Form 4 (STORY-1).
3. **Notified:** Executive sees an in-app notification: "Form 4 generated for your trade — under legal review" (STORY-7).
4. **Visibility:** Executive can open "My Filings" at any time to see status: Generated → Under Review → Approved → Submitted (STORY-6).
5. **Deadline awareness:** If review stalls, executive sees a deadline-risk banner counting down against the real EDGAR cutoff (STORY-8).
6. **Confirmation:** Once Legal approves and the (mocked) submission completes, executive gets a submitted confirmation (STORY-9).

**Drop-off risk:** step 2→3 — if generation silently fails (incomplete data), the executive has no visibility unless the incomplete-data path (STORY-1 AC2) explicitly notifies them, not just legal. *Design decision: the incomplete-data notification goes to both personas, not legal only* — carried into wireframes/API design.

## Journey B — Legal & Compliance Officer: "Review, don't chase"

1. **Entry point:** Officer opens the Compliance Dashboard (STORY-10) and sees all pending filings across executives, sorted by deadline urgency.
2. **Triage:** Filters by status = "Under Review" (STORY-11) to find what needs action now.
3. **Review:** Opens a filing, reviews pre-filled fields against the source transaction (STORY-3).
4. **Correct (if needed):** Edits a field; system re-validates immediately, surfacing any new error before save completes (STORY-3 AC2).
5. **Approve:** Once validation passes, approves — form moves to "Submitted" and a (mocked) EDGAR connector call is logged (STORY-4).
6. **Audit:** Every action in steps 3–5 is written to the audit log with officer identity + timestamp, visible later for compliance review (STORY-5).

**Drop-off risk:** step 2 — with many executives' filings, an officer could miss one buried in the list. *Design decision: the dashboard sorts by deadline urgency by default (not creation date), and the deadline-risk notification (Journey A step 5) is also sent to legal/compliance, not just the executive.* **Revised at Phase 4 design-quality gate:** MVP has no per-filing officer-assignment model, so this broadcasts to all `LEGAL_COMPLIANCE`-role users rather than one "assigned officer" — per-officer assignment (e.g. round-robin or portfolio-based) is a real future enhancement, not built here, and is not claimed as built.
