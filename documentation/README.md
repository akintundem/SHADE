# Documentation

Detailed documentation for the **Event Planner backend (sade-mono)** — the SHDE event planning platform API and supporting services.

---

## Contents

| Document | Description |
|----------|-------------|
| [architecture.md](architecture.md) | **System architecture:** tech stack, high-level diagram, features and modules, request flow, data flow and persistence, deployment, API surface, ports, resilience, and observability. Single config file; schema via Flyway (or Hibernate DDL for initial setup). |
| [er-diagram.md](er-diagram.md) | **Entity-relationship diagram:** main domain model and database tables (auth, events, collaboration, tickets, attendees, budget, tasks, feeds, communications). Table descriptions, relationships, and schema notes. Aligned with Flyway migrations when present. |
| [security-and-configuration.md](security-and-configuration.md) | **Security and configuration:** gateway (Kong), monolith auth and RBAC, attendee invites, AI service, email/push workers, user directory limits, environment variables reference, and `.env` handling. For deployers and developers. |
| [getting-started.md](getting-started.md) | **Getting started:** repository layout, prerequisites, local setup (DB, Redis, MinIO, RabbitMQ), running with Docker Compose, running the monolith alone, and verifying the stack. |
| [api-overview.md](api-overview.md) | **API overview:** base URL, versioning, main REST resource paths, authentication, OpenAPI/Swagger, and example flows. |

---

## Document purposes

- **Architecture** — Understand how the monolith, Kong, AI, email, and push services fit together; where configuration lives; and how deployment works.
- **ER diagram** — Understand the domain model and database schema for development, debugging, and writing migrations.
- **Security and configuration** — Configure auth (OIDC/JWT, gateway keys), RBAC, invite flows, and workers; avoid common pitfalls (secrets, CORS, token in URL).
- **Getting started** — Get the repo running locally (with or without full Docker) and run tests.
- **API overview** — Discover main endpoints, auth headers, and how to call the API from clients.

---

## Repository layout (high level)

```
sade-mono/
├── documentation/          # This folder — all written docs
├── src/
│   ├── main/java/          # Spring Boot monolith (eventplanner.*)
│   └── main/resources/
│       ├── application.yml # Single app config
│       ├── db/migration/    # Flyway migrations (when used)
│       └── rbac/            # RBAC_policy.yml
├── infra/kong/             # Kong declarative config (kong.yml)
├── email/                  # Node email worker (Resend + React Email)
├── push-service/           # Node push worker (Firebase)
├── ai-service/             # Python FastAPI (OpenAI cover image)
├── docker-compose.yml      # Full stack: Kong, monolith, AI, email, push, RabbitMQ, MinIO
├── .env.example            # Env template (copy to .env; do not commit .env)
└── pom.xml                 # Java 17, Spring Boot 3
```

---

## Conventions used in this documentation

- **Monolith** — The Java Spring Boot application (`event-planner-monolith`), unless otherwise stated.
- **Gateway** — Kong; all client traffic should go through Kong (ports 8000/8443), not directly to the monolith (8080) or AI service (8000 internal).
- **API base** — REST APIs are under `/api/v1/`; the AI service is exposed under `/ai-service` (Kong strips path and forwards).
- **Secrets** — Never committed; use `.env` (from `.env.example`) or a secret manager. `GATEWAY_SERVICE_API_KEY` and `AI_GATEWAY_SHARED_SECRET` are required for production.
- **Schema** — Documented as Flyway-owned; in some setups the app may use `ddl-auto: create` once for initial schema, then `validate` or Flyway.

---

## Related resources

- **STRICT_SECURITY_REVIEW_REPORT.md** (repository root) — Security review findings and remediation table.
- **.env.example** (repository root) — Template for monolith and services; copy to `.env` and fill in values.
- **ai-service/.env.example** — Template for AI service env (do not commit `ai-service/.env`).
- **push-service/.env.example** — Template for push service env (do not commit `push-service/.env`).
