# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository state

This repository currently contains **planning artifacts only** — there is no application code, build system, package manifest, or test suite yet. Do not assume or invent build/lint/test commands; none exist. When source code is added to this repo, this file should be updated with the actual commands and architecture.

## Contents

- [PRD.txt](PRD.txt) — the Product Requirements Document for the "Automated Regulatory Filing Module," an AAVA™ Product Studio output (marked **CONFIDENTIAL — Internal Use Only**). Defines the problem statement, target users, user stories, functional/non-functional requirements, scope, success metrics, risks, and acceptance criteria.
- [epics-from-prd.md](epics-from-prd.md) — the epic/feature/story breakdown derived from the PRD, organized as E1/E2/E3 epics, each with features (`E#.F#`) and user stories, including their own acceptance criteria and execution order.

## Product context

The product being planned is a module for Computershare's **EquatePlus** platform that automatically generates and delivers pre-filled regulatory filings (starting with SEC Form 4) when a securities transaction executes, streams the data to corporate legal/compliance teams in real time, and tracks the filing through review, approval, and submission.

Key domain facts to keep in mind when working on this repo:

- **Primary regulatory driver**: SEC Form 4 must be filed within 2 business days of a trade; the system targets generating a pre-filled form within 5 minutes of transaction execution.
- **Two personas drive requirements**: Corporate Executives (insiders who file) and Legal & Compliance Officers (who review/approve/submit and monitor via a dashboard).
- **Epic structure** (see [epics-from-prd.md](epics-from-prd.md)) mirrors the intended build order:
  - **E1 — Automated Regulatory Form Generation and Submission**: form generation, field validation/compliance logic, legal review & approval workflow, audit logging/history.
  - **E2 — Real-Time Data Streaming, Notifications, and Monitoring**: streaming form/transaction data to legal teams, notifications (generation/deadline-risk/submission), the compliance dashboard, data export (CSV/PDF), accessibility (WCAG 2.1 AA) and high-availability requirements.
  - **E3 — Error Handling, Edge Cases, and Extensibility**: transaction data sync error detection, rapid-trade-succession/bulk handling, extensibility to future regulatory forms (Form 5, Form 144), third-party compliance-analytics integration.
- **Non-functional constraints** repeated throughout: TLS 1.2+ end-to-end encryption, role-based access control, full audit logging (7-year retention), 99.9% uptime with automated failover, support for 10,000 concurrent users / 1,000 trades per minute, WCAG 2.1 AA accessibility.
- **Explicitly out of scope**: manual transaction entry, integration with non-EquatePlus platforms, non-US regulatory forms.
- **Integration boundary**: all functionality is constrained to EquatePlus platform APIs; authentication must align with existing corporate SSO.

## Working with these documents

- Treat [PRD.txt](PRD.txt) as the source of truth for *why* and *what*; treat [epics-from-prd.md](epics-from-prd.md) as the source of truth for *how the work is decomposed* (epic → feature → story → acceptance criteria).
- These documents are marked confidential — do not copy their contents into external services or public locations.
- If asked to scaffold an implementation, use the epic/feature/story IDs (e.g. `E1.F1`, `E2.F3`) as the organizing structure, and carry forward each feature's stated functional/non-functional requirements and acceptance criteria rather than re-deriving them.
