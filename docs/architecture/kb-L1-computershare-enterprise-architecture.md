# Enterprise Architecture Standards
### kb-L1-computershare-enterprise-architecture v1.0.0
### This KB defines the enterprise-level architecture standards for Computershare's EquatePlus platform. All design and construction agents working on EquatePlus modules MUST ground their decisions in these standards.

> **Provenance note:** this is a *hypothetical* enterprise architecture document — the kind of standing Confluence-hosted EA reference a platform engineering / architecture review board (ARB) at a real Computershare-scale organization would already maintain before any new module's design work begins.  
---

## EA1: Technology Stack

| Layer | Technology | Version | Purpose | Status |
|-------|-----------|---------|---------|--------|
| Frontend | React | 18.x | UI framework | Mandatory |
| Frontend | TypeScript | 5.x | Type-safe JavaScript | Mandatory |
| Frontend | Vite | 5.x | Build tool | Mandatory |
| Frontend | React Router | 6.x | Client-side routing | Mandatory |
| Frontend | TanStack Query | 5.x | Server state management | Approved (not yet adopted by all modules) |
| Frontend | Axios / fetch | — | HTTP client | Mandatory (either acceptable) |
| Frontend | React Hook Form | 7.x | Form management | Approved |
| Frontend | Zod | 3.x | Schema validation | Approved |
| Frontend | Tailwind CSS or plain CSS w/ design tokens | — | Styling | Approved — must consume `computershare-design-system.md` tokens regardless of choice |
| Backend | Java | 17 (LTS) | Primary backend language | Mandatory |
| Backend | Spring Boot | 3.x | Application framework | Mandatory |
| Backend | Spring Data JPA / Hibernate | 6.x | ORM | Mandatory |
| Backend | Spring Security | 6.x | AuthN/AuthZ | Mandatory for any endpoint reachable outside a sandboxed demo |
| Backend | Maven | 3.9.x | Build tool | Mandatory |
| Backend | SLF4J + Logback (JSON encoder) | — | Structured logging | Mandatory |
| Database | PostgreSQL | 15+ | Primary relational database | Mandatory |
| Database | Flyway | 9.x | Schema migrations | Mandatory |
| Database | Redis | 7.x | Caching, session store | Mandatory for any service serving external traffic |
| Messaging | Apache Kafka | 3.x | Event streaming between EquatePlus services (e.g. transaction-executed events) | Mandatory for cross-service integration |
| Identity | Okta (SAML 2.0 / OIDC) | — | Corporate SSO | Mandatory — all EquatePlus modules authenticate via the corporate IdP, never a bespoke login |
| Infrastructure | Docker | Latest | Containerization | Mandatory |
| Infrastructure | Kubernetes (Amazon EKS) | Latest | Container orchestration | Mandatory |
| Infrastructure | AWS ALB / API Gateway | — | Ingress, TLS termination | Mandatory |
| Infrastructure | GitHub Actions | Latest | CI/CD pipelines | Mandatory |
| Infrastructure | HashiCorp Vault | Latest | Secrets management | Mandatory |
| Infrastructure | Prometheus + Grafana | Latest | Monitoring & dashboards | Mandatory |
| Testing | JUnit 5 + Spring Boot Test | Latest | Backend unit/integration testing | Mandatory |
| Testing | Vitest + React Testing Library | Latest | Frontend unit/component testing | Mandatory |
| Testing | Playwright | Latest | E2E browser testing | Mandatory |
| Testing | REST Assured | Latest | API contract/integration testing | Approved |
| Security | OWASP Dependency-Check | Latest | Dependency vulnerability scanning | Mandatory |
| Security | SonarQube | Latest | SAST + code quality gate | Mandatory |
| Security | Trivy | Latest | Container image scanning | Mandatory |

**MANDATORY RULE:** Agents MUST NOT introduce technologies not on this list. If a requirement needs unlisted tech, output `UNLISTED_TECHNOLOGY_REQUIRED:[name]` for ARB review.

---

## EA2: Architecture Patterns

### Backend: Layered Architecture with Spring Boot

Three layers, strict dependency direction (inward only):

1. **Domain** — JPA entities, enums, DTOs (records or POJOs), repository interfaces (Spring Data). Zero dependency on the web layer.
2. **Service** — `@Service`-annotated business logic, use-case orchestration. Depends only on Domain and repository interfaces. Owns transaction boundaries (`@Transactional`).
3. **Web (API)** — `@RestController`s, request/response DTOs, exception handlers (`@RestControllerAdvice`), security filters. Entry point; depends on Service.

```
{module}-api/
  src/main/java/com/computershare/{module}/
    web/                       # REST controllers, DTOs, exception handling
      dto/                     # Request/response records
    service/                   # Business logic, orchestration, transaction boundaries
    domain/                    # JPA entities, enums
    repository/                # Spring Data JPA repositories
    security/                  # Auth filter/config, RBAC
    config/                    # Spring @Configuration classes (CORS, seeding, scheduling)
  src/main/resources/
    application.yml
    db/migration/              # Flyway migrations
  src/test/java/...
```

### Frontend: Feature-Based React Architecture

```
{module}-ui/
  src/
    app/ or root files         # App shell, providers, router
    features/ or pages/        # One folder per screen/domain
    components/                # Shared, reusable UI components (design-system-driven)
    api/                       # HTTP client, typed API functions
    types/                     # Shared TypeScript types
    test/                      # Vitest + RTL tests
```

### Communication
- **Synchronous (REST over HTTPS):** Frontend → Backend via Spring Boot REST endpoints; service-to-service within EquatePlus also REST unless volume/decoupling needs favor Kafka.
- **Asynchronous (Kafka):** Cross-module domain events — e.g. `equateplus.transactions.executed` is the canonical event a Regulatory Filing Module-class service should consume rather than polling or being called synchronously by the transaction engine.
- **No WebSocket standard mandated** at L1; a module MAY adopt one for real-time push (subject to ARB review) but polling with a short TanStack Query interval is the accepted default for dashboard-class UIs under EA7's latency targets.

### State Management (Frontend)
- Server state: TanStack Query (preferred) or direct fetch + local state for smaller modules.
- Form state: React Hook Form + Zod for any form with more than 2 fields.

### Error Handling

**Backend (Spring Boot):**
- `@RestControllerAdvice` global exception handler converts all exceptions to a consistent JSON error envelope (see EA3).
- Custom exceptions map to specific HTTP statuses: `NotFoundException` → 404, `ConflictException` → 409, `ForbiddenException` → 403, `ValidationException` → 422.
- Bean Validation (`jakarta.validation`) errors → 400 with field-level errors.
- Unhandled exceptions → 500 with a correlation ID, never a raw stack trace in the response body.

**Frontend (React):**
- A single fetch/Axios wrapper centralizes error handling: 401 → redirect to SSO login, 5xx → toast notification.
- Route-level error boundaries for unhandled render errors.
- User-facing error copy is written for the reader (what happened, what to do), not the exception message.

---

## EA3: API Standards

### Base URL Pattern
- Development: `http://localhost:8080/api/{resource}`
- Production: `https://api.computershare.com/equateplus/{module}/v1/{resource}`

### REST Conventions
- Resource names: plural nouns (`/filings`, `/notifications`)
- Actions on a resource that aren't pure CRUD: `POST /filings/{id}/approve`
- JSON only; `camelCase` properties in both requests and responses
- ISO 8601 timestamps (UTC, `Z` suffix); enums as uppercase strings

### Error Response Envelope
```json
{
  "message": "Filing 81e0d4bb is not VALIDATED (current status: UNDER_REVIEW) — approval blocked.",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000"
}
```
For field-level validation failures:
```json
{
  "filingId": "81e0d4bb",
  "errors": [ { "field": "shares", "message": "must be greater than 0" } ]
}
```

### Standard Headers
- `Authorization: Bearer {JWT}` — on all authenticated endpoints (issued by corporate SSO in production; see EA5)
- `X-Correlation-Id` — generated at the edge if absent, propagated through all downstream calls and into every log line
- `X-Organization-Id` / tenant-scoping header — required wherever a module is multi-tenant (EquatePlus serves many issuer clients)
- `Idempotency-Key` — required on all state-changing POST/PUT/PATCH requests (UUID), so a retried request after a timeout cannot double-submit a regulatory filing

### Pagination
- `page` (0-based, default 0) + `size` (max 100, default 20) query params, Spring Data `Pageable` convention
- Response includes `totalElements` and `totalPages`

### Rate Limiting
- Read: 200 req/min per user
- Write: 50 req/min per user

### Versioning
- URL path versioning: `/v1/`, `/v2/` once a module is customer-facing; internal demo modules may omit the version segment (as this repo's MVP does) but MUST add it before any production exposure.

---

## EA4: Data Architecture

### PostgreSQL Standards

**Schema per module** (logical schema/namespace, not necessarily a separate physical database):
| Schema | Tables (example) | Purpose |
|--------|--------|---------|
| `equateplus_core` | `users`, `organizations`, `roles` | Shared identity & multi-tenancy |
| `regfiling` | `filings`, `audit_log`, `notifications`, `form_templates` | Automated Regulatory Filing Module's own data |

**Mandatory columns (all tables):**
```sql
id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
created_by    VARCHAR(100) NOT NULL,
updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
updated_by    VARCHAR(100) NOT NULL,
version_num   INTEGER NOT NULL DEFAULT 1   -- optimistic locking
```

**Soft delete:** application-managed deletes set a boolean/status flag; hard `DELETE` is reserved for GDPR/data-subject erasure requests only, never routine application deletes. Regulatory/audit tables (see EA12) are additionally **append-only** — no `UPDATE`/`DELETE` code path may ever target them, by architecture, not just convention.

**Migrations:** Flyway, versioned `V{n}__description.sql` files, additive-only in normal operation (see EA6 zero-downtime strategy). No manual DDL against any environment above local.

**Indexing:** primary key on `id`; foreign keys indexed by default; composite indexes on frequently filtered columns (e.g., `(status, deadline_at)` for any filing-status/deadline query).

**Encryption at rest:** managed via the RDS/Aurora encryption layer (AES-256); no column-level encryption mandated by default, but PII columns (executive names, contact details) MUST be flagged in the data dictionary for the data-protection team's review.

---

## EA5: Security Architecture

### Authentication
- Corporate SSO via **Okta**, SAML 2.0 for legacy integrations / OIDC for new services.
- Access token: JWT (RS256), 30-minute expiry.
- Refresh token: opaque, 8-hour expiry, HttpOnly + Secure + SameSite=Strict cookie.
- No module may implement its own username/password login form — that is a hard EA violation. (The MVP in this repo uses a mocked bearer-token scheme purely as a local stand-in for this — see EA18.)

### Authorization
- Role-Based Access Control (RBAC). For the Regulatory Filing Module specifically:
  | Role | Permissions |
  |------|------------|
  | Corporate Executive | View/generate own filings, view own notifications and audit history |
  | Legal & Compliance Officer | View/filter all filings for their organization, edit and approve filings, view all notifications and audit history for their organization |
- Organization-level isolation: a user only ever sees data for the organization(s) they're assigned to — enforced server-side on every query, never trusted from a client-supplied parameter alone.

### API Security
- All endpoints require a Bearer JWT except a documented health-check endpoint.
- CORS: explicit origin allowlist per environment, never `*`.
- Input validation via Bean Validation / Zod on every request, both server and client.
- SQL injection prevention: JPA parameterized queries only — no string-concatenated JPQL/SQL.

### Data Security
- TLS 1.2+ for every connection: browser↔API, API↔DB, API↔Redis, API↔Kafka.
- No PII in logs — structured logging processors strip or hash sensitive fields before emission.
- Full audit log for every write operation on regulated data (who, what, when, correlation ID) — see EA12.

### Frontend Security
- Access token held in memory (or a short-lived HttpOnly cookie), never `localStorage`.
- CSRF protection via SameSite cookie + CSRF token for any cookie-authenticated flow.
- XSS prevention: React's built-in escaping, plus a Content-Security-Policy header at the edge.

---

## EA6: Infrastructure & Deployment

### Container Strategy
| Container | Base Image | Port |
|-----------|-----------|------|
| {module}-api | `eclipse-temurin:17-jre-alpine` | 8080 |
| {module}-ui | `node:20-alpine` (build) → `nginx:alpine` (serve) | 80 |
| redis | `redis:7-alpine` | 6379 |

- PostgreSQL: managed service (Amazon RDS / Aurora) — not containerized in any environment above local.
- All images: non-root user, health check defined, multi-stage builds for the frontend.

### CI/CD Pipeline
```
lint → unit-test → build → integration-test → SAST (SonarQube) → dependency-scan → docker-build → image-scan (Trivy) → deploy-staging → smoke-test → deploy-prod
```

### Environment Strategy
| Environment | Purpose | Database |
|-------------|---------|----------|
| Local | Developer workstation | H2 (in-memory) or local PostgreSQL |
| CI | Automated testing | PostgreSQL (Docker service container) or H2 for fast unit runs |
| Staging | Pre-production validation | PostgreSQL (staging RDS instance) |
| Production | Live system | PostgreSQL (production RDS instance, Multi-AZ) |

### Zero-Downtime Strategy
- Rolling deployments (`maxSurge: 1`, `maxUnavailable: 0`)
- Flyway migrations run as a pre-deploy job; additive-only so existing pods stay functional mid-migration
- Destructive schema changes follow a two-release expand/contract pattern

---

## EA7: Non-Functional Requirements

| Category | Metric | Target |
|----------|--------|--------|
| Latency (API) | p95 response time | < 500ms for reads, < 2s for writes |
| Filing generation | Transaction event → pre-filled form | < 5 minutes (regulatory driver — SEC Form 4) |
| Dashboard refresh | Status change → visible on dashboard | < 1 minute |
| Availability | System uptime | 99.9% |
| Scalability | Concurrent users | 10,000 |
| Scalability | Trade throughput | 1,000 trades/minute |
| Recovery | Failover time | < 5 minutes |
| Data retention | Regulatory audit logs | 7 years |
| Accessibility | WCAG conformance | 2.1 AA |

---

## EA8: Integration Patterns

### Internal (Backend ↔ Database)
- Spring Data JPA for standard CRUD; native queries only for reporting/aggregation where JPQL is materially worse.
- Redis cache for frequently-read, slow-changing data (e.g. dashboard aggregations), TTL 30–60s.

### Cross-Module (within EquatePlus)
- Kafka topics as the default integration point between EquatePlus modules — e.g. the transaction engine publishes `equateplus.transactions.executed`; the Regulatory Filing Module consumes it rather than being called synchronously.
- Direct REST calls between modules are permitted only for low-volume, synchronous lookups (e.g. resolving an organization's display name), never for triggering business processes.

### External (Regulatory)
- SEC EDGAR submission: either a direct filer-API integration or a handoff to an existing filing-agent relationship — **this specific integration mechanism is an explicit open architectural decision for the Regulatory Filing Module**, not yet resolved at the EA level (see EA18 and the module's own Regulatory Posture finding).

### Async Processing
- Scheduled/event-driven jobs (deadline-risk evaluation, cache warm-up) run as Spring `@Scheduled` tasks for single-instance simplicity, or as a dedicated worker deployment once a module runs multi-instance — the point at which polling-based scheduling must move to a distributed scheduler (e.g. Quartz with a DB lock, or a Kafka-triggered pattern) to avoid duplicate firing.

---

## EA9: Observability Standards

| Aspect | Tool | Standard |
|--------|------|----------|
| Logging | SLF4J + Logback, JSON encoder | Every log entry: timestamp, level, correlationId, userId, organizationId, message |
| Metrics | Micrometer → Prometheus | Counters: requests, errors. Histograms: request latency, business-operation duration |
| Dashboards | Grafana | System health, API performance, business KPIs (e.g. filings generated/day) |
| Tracing | OpenTelemetry | Distributed tracing across API → Service → DB |
| Alerting | Grafana Alerts | P1: API down. P2: latency > 2× target. P3: error rate > 1% |
| Health | Spring Boot Actuator `/actuator/health` | Liveness: process alive. Readiness: DB connection pool healthy |

---

## EA10: Frontend Architecture

### React Standards
- Functional components only.
- TypeScript strict mode enabled.
- Custom hooks for reusable logic.
- No uncontrolled form inputs for anything beyond a single search box.
- Route-level code splitting for any module beyond ~5 screens.
- Error boundaries at route level.

### Component Patterns
- **Atoms:** Button, Badge, Input, StatusPill
- **Molecules:** FilingRow, NotificationItem, DeadlineIndicator
- **Organisms:** FilingTable, FilingReviewPanel, NotificationDrawer
- **Pages:** one per route, composed from organisms + molecules

### Accessibility (WCAG 2.1 AA)
- All interactive elements keyboard-navigable, visible focus states.
- Colour contrast 4.5:1 minimum for text.
- All non-decorative icons carry an `aria-label` or adjacent visible text — **no emoji as a substitute for a real, accessible icon or label anywhere in the product UI.**
- Semantic HTML landmarks (`<nav>`, `<main>`, `<header>`); proper heading hierarchy.

---

## EA11: Design System Standards

- The design-system-of-record for anything customer/user-facing on `computershare.com`-adjacent surfaces is `docs/design/computershare-design-system.md` (reverse-engineered from the live public site — see that document's own provenance/caveats section).
- EquatePlus product surfaces (like this module) are internal-application UI, not the public marketing site, but MUST still derive their palette, type, spacing, and radius scale from that same design system for brand consistency, applying it to data-dense application patterns (tables, forms, status pills) rather than the marketing site's editorial card/carousel patterns.
- **No emoji characters anywhere in product UI, wireframes, or user-facing copy.** Where an icon is needed, use a real icon (inline SVG, or an icon library import) with an accessible name — never a Unicode emoji glyph standing in for an icon.
- Touch targets: 44px minimum.

---

## EA12: Compliance Architecture

| Regulation | Requirement | Implementation |
|-----------|-------------|---------------|
| SEC Section 16 / Regulation S-T | Timely, accurate electronic filing of insider transaction disclosures | Automated form generation, review/approval workflow, audit trail (this module's core purpose) |
| SOX | Internal controls over financial-adjacent reporting processes | Full audit logging (who/what/when), role-segregated review-and-approve workflow (generator ≠ approver) |
| SOC 2 Type II | Security controls | Access controls, audit logging, encryption in transit and at rest |
| GDPR / UK GDPR (for EU/UK executives) | Data protection | PII minimization, retention policy, right-to-erasure process for non-regulatory-record data |

---

## EA13: Code Standards

### Java (Backend)
- Google Java Format or equivalent, enforced in CI.
- No wildcard imports.
- Javadoc on all public service-layer methods.
- Max method length: 50 lines. Max class length: 300 lines.

### TypeScript (Frontend)
- ESLint + Prettier, enforced in CI.
- Strict TypeScript (`strict: true`).
- No `any` — use `unknown` + type guards or a proper type.
- Max component length: 200 lines; extract a hook once logic exceeds ~15 lines.

### SQL
- Lowercase `snake_case` for tables/columns (PostgreSQL convention — differs deliberately from the Oracle-style `UPPERCASE` convention some other Computershare platforms use; each platform's own EA doc governs its own schema, this one governs EquatePlus).
- All DDL via Flyway migrations, never manual.

---

## EA14: Testing Standards

| Level | Tool | Target |
|-------|------|--------|
| Unit (Backend) | JUnit 5 | ≥ 80% coverage on service layer |
| Unit (Frontend) | Vitest + React Testing Library | ≥ 80% coverage on components/hooks |
| Integration (Backend) | Spring Boot Test (`@SpringBootTest` + MockMvc) | All API endpoints, real (test-scoped) database wiring |
| E2E | Playwright | Critical user journeys |
| Security | SonarQube + Trivy + OWASP Dependency-Check | Zero critical/high findings gate |
| Accessibility | axe-core / manual audit | Zero critical violations, WCAG 2.1 AA |

---

## EA15: Feature Flag Standards

- Feature flags via environment variables (simple modules) or a config-service-backed flag (dynamic rollout).
- New, regulated-workflow features MUST launch behind a flag defaulted OFF in production until Legal & Compliance sign-off.

---

## EA16: Dependency Management

- Java: Maven, versions pinned in `pom.xml` (no version ranges).
- Frontend: `package.json`, exact versions preferred over caret ranges for anything touching auth or data serialization.
- Dependency updates: monthly review; no new dependency without an architecture review for anything not already on the EA1 list.

---

## EA17: Documentation Standards

- API docs: OpenAPI 3.0, either hand-authored (as this module does at `docs/sdlc/phase-4-design/openapi.yaml`) or generated from annotations (springdoc-openapi) once a module stabilizes.
- Architecture decisions: ADR format (Context → Decision → Consequences) for any deviation from this document.
- README in every module root.
- Inline comments explain *why*, never *what*.

---

## EA18: Known Deviations — Automated Regulatory Filing Module (this repo)

Per this document's own provenance note, the MVP build in this repository is a demo, not a production EquatePlus deployment, and deliberately deviates from several standards above. Each deviation is intentional and documented at its point of use (`docs/sdlc/phase-6-construction/CHANGELOG.md`, `review-report.md`), summarized here for traceability against this EA doc specifically:

| EA Standard | This MVP's Deviation | Why |
|---|---|---|
| EA1 — PostgreSQL | Runs on H2 in-memory | Zero-external-dependency demo; `db-schema.sql` documents the real target schema |
| EA1 — Redis, Kafka | Not used | No caching/eventing need at demo scale; the transaction-event trigger is a mocked HTTP endpoint, not a Kafka consumer |
| EA5 — Okta SSO | Mocked bearer-token auth | No real IdP available in this environment; interface shape (Bearer JWT) matches the EA5 target so swapping in real SSO is a auth-layer-only change |
| EA6 — Kubernetes/EKS | Runs as two local processes (`mvn spring-boot:run`, `npm run dev`) | Demo scope; no deployment target exists for this repo |
| EA8 — SEC EDGAR integration | Fully mocked (`EdgarSubmissionConnector`) | The real integration mechanism is an open architectural question (regulatory finding from Phase 0), not yet answerable |
| EA3 — API versioning | No `/v1/` path segment | Acceptable for an unreleased internal demo per EA3's own carve-out; must be added before any production exposure |

Nothing above is a silent gap — each is cross-referenced from the module's own design and construction documentation.
