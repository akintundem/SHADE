# Documentation

Documentation for the Event Planner backend (sade-mono).

## Contents

| Document | Description |
|----------|-------------|
| [architecture.md](architecture.md) | **System architecture:** tech stack, high-level diagram, features and modules, request flow, data flow and persistence, deployment. Single config file and Flyway-only schema. |
| [er-diagram.md](er-diagram.md) | **Entity-relationship diagram:** main domain model and database tables (auth, events, collaboration, tickets, attendees, budget, tasks, feeds, communications). Aligned with Flyway migrations. |
| [security-and-configuration.md](security-and-configuration.md) | **Security and configuration:** gateway (Kong), monolith auth and RBAC, attendee invites, AI service, email/push workers, user directory limits, env and `.env` handling. For deployers and developers. |

## Related

- **STRICT_SECURITY_REVIEW_REPORT.md** (repository root) — Security review findings and remediation table.
- **.env.example** in `ai-service/` and `push-service/` — Templates for local/runtime env; do not commit real `.env` files.
