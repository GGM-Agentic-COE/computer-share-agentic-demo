# Impact Assessment — Automated Regulatory Filing Module

**Agent:** `L1-planning-impact-assessor` · **Phase:** 1 · **Correlation ID:** `C885C23C-949E-440C-8B0E-04FDD166A559`
**Inputs:** [prd.md](prd.md) · existing system/service inventory (see note below)

> **Note on fidelity:** the blueprint's `L1-planning-impact-assessor` normally cross-references a real service catalog and CMDB. Neither exists in this demo environment — there is no live EquatePlus service registry available to query. This assessment is therefore a **qualitative, judgment-based pass** grounded in what the PRD/vision state about EquatePlus, not a verified catalog/CMDB lookup. Flagged explicitly rather than presented as a rubber-stamped real integration check.

## 1. Capability duplication check

No existing regulatory-filing-generation capability is referenced anywhere in the source PRD or epics — this is framed throughout as net-new functionality bolted onto EquatePlus's existing transaction system-of-record. No duplication risk identified against the (unavailable) service catalog; this finding should be re-verified against Computershare's actual service catalog before real implementation.

## 2. Technical touch points (qualitative — CMDB unavailable)

| System | Nature of touch | Risk |
|---|---|---|
| **EquatePlus transaction engine** | Source of the trigger event (transaction execution) that FR-001 depends on. This module is a downstream consumer, not a modification of the transaction engine itself — reduces blast radius. | Medium — module has a hard real-time dependency on transaction-event availability; if EquatePlus doesn't already emit a consumable event/webhook for "transaction executed," this is new plumbing on the EquatePlus side, not just this module. |
| **Corporate SSO** | Authentication for both personas (FR-009 through FR-013, FR-015) must align with existing SSO per PRD constraint. | Low — standard integration pattern, but unverified which SSO protocol (SAML/OIDC) EquatePlus currently uses; assumed compatible. |
| **SEC EDGAR (external)** | FR-020's open dependency — direct filer API integration vs. handoff to an existing manual filing-agent relationship. | High — this is the least-understood external touch point and the one Amber regulatory finding most likely to change HLD scope significantly depending on resolution. |
| **Notification infrastructure** | FR-006/007/008 need email/in-app/SMS channels (PRD FR2 "should support multiple channels"). | Low-Medium — MVP construction (per user scope decision) implements in-app notifications only; email/SMS treated as a future integration, not built. |
| **Audit/compliance data store** | FR-012/013, 7-year retention. | Medium — a new, long-retention data store is being introduced; must be designed for immutability and access control (RBAC) from day one, not retrofitted. |

## 3. Enterprise-architecture narrative alignment

Consistent with the "clearinghouse module" framing in PRD.txt §2 (Proposed Solution) — an event-driven add-on that reacts to EquatePlus transactions rather than restructuring the transaction platform itself. No architectural narrative conflict identified. The module should be designed as a **bounded, loosely-coupled service** consuming EquatePlus events and exposing its own API/UI, rather than embedding logic directly into EquatePlus's core codebase — this framing carries into the HLD (Phase 4).

## 4. Blast radius summary

**Contained.** The module reads from EquatePlus (transaction events) and writes nothing back into EquatePlus's own systems of record — it produces and manages its own regulatory-filing artifacts. The highest-blast-radius unknown is FR-020 (EDGAR submission mechanism), which could range from "purely internal, no new external integration" (if handed off to an existing filing-agent process) to "new direct external regulator-facing integration" (if built as direct EDGAR filer API access) — this range must collapse to one answer before HLD.
